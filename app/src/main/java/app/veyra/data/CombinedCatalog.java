package app.veyra.data;
import android.content.Context;
import org.json.*;
import java.util.*;
import java.io.IOException;
import java.util.concurrent.*;

/** Catalog connections retain independent sessions and title identities. */
public final class CombinedCatalog {
    private final Context context;
    private final Map<String,List<Route>> routes=new LinkedHashMap<>();
    public String warning="";
    private final Set<String> failedRoutes=new LinkedHashSet<>();
    private boolean incomplete;
    private final ExecutorService requests=Executors.newFixedThreadPool(3);
    private final LinkedHashMap<String,Cached> cache=new LinkedHashMap<>();
    private List<Route> blueSunSearchRoutes;
    private static final class Cached {
        final List<JSONObject> titles;final String warning;final long time=android.os.SystemClock.elapsedRealtime();
        Cached(List<JSONObject> titles,String warning){this.titles=titles;this.warning=warning;}
    }
    static final class Route {final String provider,id;Route(String p,String i){provider=p;id=i;}}
    public CombinedCatalog(Context context){this.context=context.getApplicationContext();}
    public synchronized JSONArray channels() throws Exception {
        routes.clear();cache.clear();blueSunSearchRoutes=null;warning="";int failed=0;
        Map<String,Future<JSONArray>> channelCalls=new LinkedHashMap<>();
        for(String provider:CatalogProviders.configured(context))
            channelCalls.put(provider,requests.submit(()->CatalogProviders.get(context,provider).channels()));
        for(String provider:channelCalls.keySet())try {
            JSONArray list=channelCalls.get(provider).get();
            for(int i=0;i<list.length();i++){
                JSONObject entry=list.optJSONObject(i);
                if(entry==null||entry.optString("id").isEmpty())continue;
                String section=sectionFor(entry.optString("channel_name"));
                if(section.isEmpty())section="discover";
                if(!routes.containsKey(section))routes.put(section,new ArrayList<>());
                routes.get(section).add(new Route(provider,entry.optString("id")));
            }
            if(provider.equals("current"))routes.put("shorts",Arrays.asList(new Route("current","$short")));
        }catch(InterruptedException e){for(Future<?> call:channelCalls.values())call.cancel(true);Thread.currentThread().interrupt();throw e;}catch(Exception e){failed++;}
        List<Route> discovery=new ArrayList<>();
        for(String section:new String[]{"discover","films","series","animation"})if(routes.containsKey(section))discovery.addAll(routes.get(section));
        if(!discovery.isEmpty())routes.put("discover",discovery);
        if(routes.isEmpty())throw new IOException("No catalog could be reached. Check the provider configuration and connection.");
        incomplete=failed>0;
        if(incomplete)warning="Some catalog sources are unavailable. Retry to reconnect.";
        JSONArray result=new JSONArray();
        String[] ids={"discover","films","series","animation","shorts"},labels={"Explore","Films","Series","Animation","Shorts"};
        for(int i=0;i<ids.length;i++)if(routes.containsKey(ids[i]))result.put(new JSONObject().put("id",ids[i]).put("channel_name",labels[i]));
        return result;
    }
    static String sectionFor(String heading){
        String name=heading.trim().toLowerCase(Locale.ROOT);
        if(name.contains("anime")||name.contains("animation"))return "animation";
        if(name.matches(".*\\b(series|television|tv|shows?)\\b.*"))return "series";
        if(name.matches(".*\\b(movies?|films?|cinema|bollywood|hollywood|tollywood|kollywood|mollywood)\\b.*"))return "films";
        if(name.matches(".*\\b(recommend\\w*|featured|new|latest|trending|hindi|tamil|telugu|malayalam|punjabi|english)\\b.*"))return "discover";
        return "";
    }
    public synchronized List<JSONObject> titles(String channel,String query,int page) throws Exception {
        if(routes.isEmpty() && query.isEmpty())channels();
        String cacheKey=channel+"\n"+query+"\n"+page;
        Cached saved=cache.get(cacheKey);
        failedRoutes.clear();
        if(saved!=null && android.os.SystemClock.elapsedRealtime()-saved.time<120000){warning=saved.warning;return saved.titles;}
        List<Route> selected=routes.get(channel);if(query.isEmpty()&&selected==null)throw new IOException("Unknown catalog section");
        List<JSONObject> output=new ArrayList<>();int failures=0;
        List<Future<List<JSONObject>>> calls=new ArrayList<>();
        List<Route> active;
        if(query.isEmpty())active=selected;
        else{
            active=new ArrayList<>();
            for(String provider:CatalogProviders.configured(context)){
                if(!provider.equals("bluesun"))active.add(new Route(provider,""));
                else try{
                    if(blueSunSearchRoutes==null)blueSunSearchRoutes=searchRoutes(provider,CatalogProviders.get(context,provider));
                    active.addAll(blueSunSearchRoutes);
                }catch(InterruptedException e){throw e;}
                catch(Exception e){failedRoutes.add(providerName(provider));}
            }
            active.add(new Route("current","$short"));
        }
        for(Route route:active)calls.add(requests.submit(()->fetch(route,query,page)));
        try {
            for(int i=0;i<calls.size();i++)try{output.addAll(calls.get(i).get());}catch(ExecutionException e){
                failures++;
                Route route=active.get(i);
                failedRoutes.add(route.id.equals("$short")?"Current shorts":providerName(route.provider));
            }
        }catch(InterruptedException e){for(Future<?> call:calls)call.cancel(true);Thread.currentThread().interrupt();throw e;}
        if(failures==active.size())throw new CatalogFailure(failedRoutes);
        warning=!failedRoutes.isEmpty() || (query.isEmpty()&&incomplete)?"Some catalog sources could not respond. Results may be incomplete.":"";
        List<JSONObject> grouped=group(output);
        if(channel.equals("discover") && query.isEmpty()){grouped.sort(Comparator.comparingDouble(CombinedCatalog::rank).reversed());if(page==1 && failures==0)saveHome(grouped);}
        if(failedRoutes.isEmpty()){if(cache.size()>=12)cache.remove(cache.keySet().iterator().next());cache.put(cacheKey,new Cached(grouped,warning));}
        return grouped;
    }
    static List<Route> searchRoutes(String provider,StreamingBackend backend)throws Exception{
        if(!backend.channelScopedSearch())return Collections.singletonList(new Route(provider,""));
        List<Route> result=new ArrayList<>();
        for(String id:searchChannelIds(backend.channels()))result.add(new Route(provider,id));
        if(result.isEmpty())throw new IOException("Provider returned no searchable channels");
        return result;
    }
    static Set<String> searchChannelIds(JSONArray channels){
        Set<String> ids=new LinkedHashSet<>();
        for(int i=0;i<channels.length();i++){
            JSONObject channel=channels.optJSONObject(i);
            if(channel!=null&&!channel.optString("id").isEmpty()&&channel.optBoolean("searchable",true))
                ids.add(channel.optString("id"));
        }
        return ids;
    }
    private List<JSONObject> fetch(Route route,String query,int page) throws Exception {
        StreamingBackend backend=CatalogProviders.get(context,route.provider);List<JSONObject> titles=new ArrayList<>();
        if(route.id.equals("$short")){
            Object result=backend.shortCatalog(query,page);if(!(result instanceof JSONArray))throw new IOException("Unsupported catalog response");
            JSONArray array=(JSONArray)result;for(int i=0;i<array.length();i++)titles.add(CatalogBackend.normalizeShort(array.getJSONObject(i)));
        }else if(query.isEmpty())titles.addAll(CatalogBackend.titles(backend.catalog(route.id,page)));
        else titles.addAll(CatalogBackend.titles(route.id.isEmpty()?backend.search(query,page):backend.searchInChannel(route.id,query,page)));
        for(JSONObject item:titles){
            item.remove("_editions");
            item.put("_provider",route.provider);
        }
        return titles;
    }
    private static double rank(JSONObject item){
        double rating=item.optDouble("vod_douban_score",0);if(!Double.isFinite(rating)||rating<0||rating>10)rating=0;
        int year=item.optInt("vod_year",0),current=Calendar.getInstance().get(Calendar.YEAR);
        return rating+Math.max(0,Math.min(2,(year-current+10)/5.0));
    }
    private void saveHome(List<JSONObject> titles) throws JSONException {
        JSONArray safe=new JSONArray();
        for(JSONObject title:titles){JSONObject entry=new JSONObject();
            for(String key:new String[]{"id","vod_name","vod_pic","vod_year","vod_douban_score","vod_lang","vod_quality","release_date","added_at","imdb_id","vod_imdb_id","tmdb_id","vod_tmdb_id","tmdb_type","media_type","vod_actor","cast","vod_director","director","_provider","_kind","_editions"})if(title.has(key))entry.put(key,title.get(key));
            safe.put(entry);if(safe.length()>=100)break;
        }
        String value=safe.toString();if(value.length()<=524288)context.getSharedPreferences("catalog-preview",Context.MODE_PRIVATE).edit().putString("home",value).putLong("saved",System.currentTimeMillis()).apply();
    }
    public List<JSONObject> cachedHome(){
        List<JSONObject> result=new ArrayList<>();android.content.SharedPreferences prefs=context.getSharedPreferences("catalog-preview",Context.MODE_PRIVATE);
        if(System.currentTimeMillis()-prefs.getLong("saved",0)>7L*24*60*60*1000)return result;
        try{JSONArray array=new JSONArray(prefs.getString("home","[]"));for(int i=0;i<Math.min(100,array.length());i++)result.add(array.getJSONObject(i));}catch(JSONException ignored){}
        return result;
    }
    public void close(){requests.shutdownNow();}
    private static final class CatalogFailure extends IOException {
        final Set<String> sources;
        CatalogFailure(Set<String> sources){
            super("The catalog sources could not respond. Please retry.");
            this.sources=new LinkedHashSet<>(sources);
        }
    }
    public static final class SearchPage {
        public final List<JSONObject> titles;
        public final int page;
        public final boolean more;
        public final String warning;
        public final Set<String> failed;
        SearchPage(List<JSONObject> titles,int page,boolean more,String warning){
            this(titles,page,more,warning,Collections.emptySet());
        }
        SearchPage(List<JSONObject> titles,int page,boolean more,String warning,Set<String> failed){
            this.titles=titles;this.page=page;this.more=more;this.warning=warning;this.failed=new LinkedHashSet<>(failed);
        }
        public Map<String,Integer> sourceCounts(){
            Map<String,Integer> counts=new LinkedHashMap<>();
            counts.put("Current",0);counts.put("Classic",0);counts.put("Current shorts",0);
            for(JSONObject title:titles){
                Set<String> sources=new HashSet<>();
                JSONArray editions=title.optJSONArray("_editions");
                boolean shorts="short".equals(title.optString("_kind"));
                if(editions!=null)for(int i=0;i<editions.length();i++){
                    JSONObject edition=editions.optJSONObject(i);
                    if(edition!=null){
                        String provider=edition.optString("provider");
                        sources.add(shorts&&provider.equals("current")?"Current shorts":providerName(provider));
                    }
                }
                for(String source:sources)counts.put(source,counts.getOrDefault(source,0)+1);
            }
            return counts;
        }
    }
    public synchronized SearchPage search(String query,int previousPage,boolean loadMore) throws Exception {
        return collectSearch(previousPage,loadMore,page->{
            List<JSONObject> results=titles("",query,page);
            return new SearchPage(results,page,true,warning,failedRoutes);
        });
    }
    interface SearchSource { SearchPage fetch(int page)throws Exception; }
    static SearchPage collectSearch(int previousPage,boolean loadMore,SearchSource source)throws Exception {
        int first=Math.max(1,previousPage);
        int limit=loadMore?Math.min(20,first+3):first;
        ArrayList<JSONObject> found=new ArrayList<>();
        boolean partial=false;
        Set<String> failed=new LinkedHashSet<>();
        for(int page=1;page<=limit;page++){
            int before=found.size();
            try{
                SearchPage response=source.fetch(page);
                found.addAll(response.titles);
                partial|=!response.warning.isEmpty();
                failed.addAll(response.failed);
            }catch(IOException failure){
                if(found.isEmpty())throw failure;
                if(failure instanceof CatalogFailure)failed.addAll(((CatalogFailure)failure).sources);
                return searchResult(found,Math.min(first,page-1),true,true,failed);
            }
            if(loadMore&&page>first&&group(found).size()>group(found.subList(0,before)).size()){
                return searchResult(found,page,page<20||partial,partial,failed);
            }
        }
        return searchResult(found,limit,limit<20||partial,partial,failed);
    }
    private static SearchPage searchResult(List<JSONObject> titles,int page,boolean more,boolean partial,Set<String> failed)throws JSONException {
        return new SearchPage(group(titles),page,more,partial?"Some catalog sources could not respond. Results may be incomplete.":"",failed);
    }
    public static String providerName(String provider){
        if(provider.equals("current"))return "Current";
        if(provider.equals("classic"))return "Classic";
        if(provider.equals("bluesun"))return "BlueSun";
        return provider;
    }
    public static List<JSONObject> group(List<JSONObject> input) throws JSONException {
        LinkedHashMap<String,JSONObject> result=new LinkedHashMap<>();
        for(JSONObject item:input){
            String provider=item.optString("_provider");
            String identity=indexKey(item);
            JSONObject existing=result.get(identity);
            if(existing==null){existing=new JSONObject(item.toString());existing.put("_editions",new JSONArray());result.put(identity,existing);}
            JSONArray editions=existing.getJSONArray("_editions");
            JSONArray previous=item.optJSONArray("_editions");
            if(previous==null||previous.length()==0)
                addEdition(editions,new JSONObject().put("provider",provider).put("id",item.optString("id"))
                    .put("language",language(item)).put("quality",quality(item)).put("audioType",item.optString("audio_type","0")));
            if(previous!=null)for(int i=0;i<previous.length();i++){
                JSONObject edition=previous.optJSONObject(i);
                if(edition!=null)addEdition(editions,edition);
            }
        }
        return new ArrayList<>(result.values());
    }
    public static String indexKey(JSONObject item){
        String provider=item.optString("_provider"),kind=item.optString("_kind","movie");
        String external=externalId(item);
        if(!external.isEmpty()&&!kind.equals("short"))return new JSONArray().put(kind).put(external).toString();
        String year=item.optString("vod_year"),title=item.optString("vod_name");
        boolean shared=(provider.equals("current")||provider.equals("classic"))&&year.matches("[1-9][0-9]{3}")&&!title.trim().isEmpty();
        String director=firstText(item,"vod_director","director").toLowerCase(Locale.ROOT);
        String cast=firstText(item,"vod_actor","cast").toLowerCase(Locale.ROOT);
        if(shared&&!kind.equals("short")&&!director.isEmpty()&&!cast.isEmpty()){
            return new JSONArray().put("legacy-credits").put(kind).put(title.trim().toLowerCase(Locale.ROOT))
                .put(year).put(director).put(cast).toString();
        }
        return new JSONArray().put(shared?"current/classic":provider).put(kind).put(item.optString("id")).put(title).put(year)
            .put(language(item).toLowerCase(Locale.ROOT)).put(quality(item).toLowerCase(Locale.ROOT)).toString();
    }
    private static String externalId(JSONObject item){
        String imdb=firstText(item,"imdb_id","vod_imdb_id").toLowerCase(Locale.ROOT);
        if(imdb.matches("tt[0-9]{5,12}"))return "imdb:"+imdb;
        String tmdb=firstText(item,"tmdb_id","vod_tmdb_id");
        String type=firstText(item,"tmdb_type","media_type").toLowerCase(Locale.ROOT);
        return tmdb.matches("[1-9][0-9]{0,11}")&&(type.equals("movie")||type.equals("tv"))?"tmdb:"+type+":"+tmdb:"";
    }
    private static void addEdition(JSONArray editions,JSONObject choice)throws JSONException {
        for(int i=0;i<editions.length();i++){
            JSONObject edition=editions.getJSONObject(i);
            if(choice.optString("provider").equals(edition.optString("provider"))&&choice.optString("id").equals(edition.optString("id"))
                &&choice.optString("audioType","0").equals(edition.optString("audioType","0"))
                &&choice.optString("language").equalsIgnoreCase(edition.optString("language"))
                &&choice.optString("quality").equalsIgnoreCase(edition.optString("quality")))return;
        }
        editions.put(choice);
    }
    public static String editionLabel(JSONObject edition){
        String label=providerName(edition.optString("provider"));
        for(String key:new String[]{"language","quality"}){
            String value=edition.optString(key);
            if(!value.isEmpty())label+=" · "+value;
        }
        return label;
    }
    public static String language(JSONObject item){
        return firstText(item,"vod_lang","language","lang");
    }
    public static String quality(JSONObject item){
        return firstText(item,"vod_quality","quality");
    }
    private static String firstText(JSONObject item,String... keys){
        for(String key:keys){
            String value=item.optString(key).trim();
            if(!value.isEmpty()&&!value.equals("0")&&!value.equalsIgnoreCase("null"))return value;
        }
        return "";
    }
}
