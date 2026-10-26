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
    private boolean incomplete;
    private final ExecutorService requests=Executors.newFixedThreadPool(3);
    private final LinkedHashMap<String,Cached> cache=new LinkedHashMap<>();
    private static final class Cached {
        final List<JSONObject> titles;final String warning;final long time=android.os.SystemClock.elapsedRealtime();
        Cached(List<JSONObject> titles,String warning){this.titles=titles;this.warning=warning;}
    }
    private static final class Route {final String provider,id;Route(String p,String i){provider=p;id=i;}}
    public CombinedCatalog(Context context){this.context=context.getApplicationContext();}
    public synchronized JSONArray channels() throws Exception {
        routes.clear();cache.clear();warning="";int failed=0;
        Map<String,Future<JSONArray>> channelCalls=new LinkedHashMap<>();
        for(String provider:new String[]{"current","classic"})channelCalls.put(provider,requests.submit(()->CatalogBackend.get(context,provider).channels()));
        for(String provider:channelCalls.keySet())try {
            JSONArray list=channelCalls.get(provider).get();
            for(int i=0;i<list.length();i++){
                JSONObject entry=list.getJSONObject(i);String name=entry.optString("channel_name").toLowerCase(Locale.ROOT);
                String section=name.contains("recommend")?"discover":name.equals("movies")?"films":name.contains("tv show")?"series":name.contains("anime")?"animation":"";
                if(section.isEmpty())continue;
                if(!routes.containsKey(section))routes.put(section,new ArrayList<>());
                routes.get(section).add(new Route(provider,entry.optString("id")));
            }
            if(provider.equals("current"))routes.put("shorts",Arrays.asList(new Route("current","$short")));
        }catch(InterruptedException e){for(Future<?> call:channelCalls.values())call.cancel(true);Thread.currentThread().interrupt();throw e;}catch(Exception e){failed++;}
        List<Route> discovery=new ArrayList<>();
        for(String section:new String[]{"films","series"})if(routes.containsKey(section))discovery.addAll(routes.get(section));
        if(!discovery.isEmpty())routes.put("discover",discovery);
        if(routes.isEmpty())throw new IOException("Neither catalog could be reached. Check the private build configuration and connection.");
        incomplete=failed>0;
        if(incomplete)warning="One library is unavailable. Some titles may be missing; retry to reconnect.";
        JSONArray result=new JSONArray();
        String[] ids={"discover","films","series","animation","shorts"},labels={"Explore","Films","Series","Animation","Shorts"};
        for(int i=0;i<ids.length;i++)if(routes.containsKey(ids[i]))result.put(new JSONObject().put("id",ids[i]).put("channel_name",labels[i]));
        return result;
    }
    public synchronized List<JSONObject> titles(String channel,String query,int page) throws Exception {
        if(routes.isEmpty())channels();
        String cacheKey=channel+"\n"+query+"\n"+page;
        Cached saved=cache.get(cacheKey);
        if(saved!=null && android.os.SystemClock.elapsedRealtime()-saved.time<120000){warning=saved.warning;return saved.titles;}
        List<Route> selected=routes.get(channel);if(selected==null)throw new IOException("Unknown catalog section");
        List<JSONObject> output=new ArrayList<>();int failures=0;
        List<Route> searches=Arrays.asList(new Route("current",""),new Route("classic",""),new Route("current","$short"));
        List<Future<List<JSONObject>>> calls=new ArrayList<>();
        for(Route route:query.isEmpty()?selected:searches)calls.add(requests.submit(()->fetch(route,query,page)));
        try {
            for(Future<List<JSONObject>> call:calls)try{output.addAll(call.get());}catch(ExecutionException e){failures++;}
        }catch(InterruptedException e){for(Future<?> call:calls)call.cancel(true);Thread.currentThread().interrupt();throw e;}
        if(failures==(query.isEmpty()?selected:searches).size())throw new IOException("Both catalog requests failed. Please retry.");
        warning=failures>0 || incomplete?"One library is unavailable. These results are incomplete.":"";
        List<JSONObject> grouped=group(output);
        if(channel.equals("discover") && query.isEmpty()){grouped.sort(Comparator.comparingDouble(CombinedCatalog::rank).reversed());if(page==1 && failures==0)saveHome(grouped);}
        if(failures==0){if(cache.size()>=12)cache.remove(cache.keySet().iterator().next());cache.put(cacheKey,new Cached(grouped,warning));}
        return grouped;
    }
    private List<JSONObject> fetch(Route route,String query,int page) throws Exception {
        CatalogBackend backend=CatalogBackend.get(context,route.provider);List<JSONObject> titles=new ArrayList<>();
        if(route.id.equals("$short")){
            Object result=backend.shortCatalog(query,page);if(!(result instanceof JSONArray))throw new IOException("Unsupported catalog response");
            JSONArray array=(JSONArray)result;for(int i=0;i<array.length();i++)titles.add(CatalogBackend.normalizeShort(array.getJSONObject(i)));
        }else titles.addAll(CatalogBackend.titles(query.isEmpty()?backend.catalog(route.id,page):backend.search(query,page)));
        for(JSONObject item:titles)item.put("_provider",route.provider);
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
            for(String key:new String[]{"id","vod_name","vod_pic","vod_year","vod_douban_score","_provider","_kind","_editions"})if(title.has(key))entry.put(key,title.get(key));
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
    public static List<JSONObject> group(List<JSONObject> input) throws JSONException {
        LinkedHashMap<String,JSONObject> result=new LinkedHashMap<>();
        for(JSONObject item:input){
            // Shared numeric IDs alone do not prove shared content across providers.
            String identity=item.optString("_kind","movie")+"|"+item.optString("id")+"|"+item.optString("vod_name")+"|"+item.optString("vod_year");
            JSONObject existing=result.get(identity);
            if(existing==null){existing=new JSONObject(item.toString());existing.put("_editions",new JSONArray());result.put(identity,existing);}
            JSONArray editions=existing.getJSONArray("_editions");boolean seen=false;
            for(int i=0;i<editions.length();i++)if(editions.getJSONObject(i).optString("provider").equals(item.optString("_provider")))seen=true;
            if(!seen)editions.put(new JSONObject().put("provider",item.optString("_provider")).put("id",item.optString("id")));
        }
        return new ArrayList<>(result.values());
    }
}
