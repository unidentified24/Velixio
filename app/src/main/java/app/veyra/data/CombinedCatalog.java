package app.veyra.data;
import android.content.Context;
import org.json.*;
import java.util.*;
import java.io.IOException;

/** Catalog connections retain independent sessions and title identities. */
public final class CombinedCatalog {
    private final Context context;
    private final Map<String,List<Route>> routes=new LinkedHashMap<>();
    public String warning="";
    private boolean incomplete;
    private final LinkedHashMap<String,Cached> cache=new LinkedHashMap<>();
    private static final class Cached {
        final List<JSONObject> titles;final String warning;final long time=android.os.SystemClock.elapsedRealtime();
        Cached(List<JSONObject> titles,String warning){this.titles=titles;this.warning=warning;}
    }
    private static final class Route {final String provider,id;Route(String p,String i){provider=p;id=i;}}
    public CombinedCatalog(Context context){this.context=context.getApplicationContext();}
    public synchronized JSONArray channels() throws Exception {
        routes.clear();cache.clear();warning="";int failed=0;
        for(String provider:new String[]{"current","classic"})try {
            JSONArray list=CatalogBackend.get(context,provider).channels();
            for(int i=0;i<list.length();i++){
                JSONObject entry=list.getJSONObject(i);String name=entry.optString("channel_name").toLowerCase(Locale.ROOT);
                String section=name.contains("recommend")?"discover":name.equals("movies")?"films":name.contains("tv show")?"series":name.contains("anime")?"animation":"";
                if(section.isEmpty())continue;
                if(!routes.containsKey(section))routes.put(section,new ArrayList<>());
                routes.get(section).add(new Route(provider,entry.optString("id")));
            }
            if(provider.equals("current"))routes.put("shorts",Arrays.asList(new Route("current","$short")));
        }catch(Exception e){failed++;}
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
        for(Route route:query.isEmpty()?selected:searches)try {
            CatalogBackend backend=CatalogBackend.get(context,route.provider);
            if(route.id.equals("$short")) {
                Object result=backend.shortCatalog(query,page);
                if(!(result instanceof JSONArray))throw new IOException("Unsupported short catalog response");
                JSONArray array=(JSONArray)result;for(int i=0;i<array.length();i++){JSONObject item=CatalogBackend.normalizeShort(array.getJSONObject(i));item.put("_provider",route.provider);output.add(item);}continue;
            }
            Object response=query.isEmpty()?backend.catalog(route.id,page):backend.search(query,page);
            for(JSONObject item:CatalogBackend.titles(response)){item.put("_provider",route.provider);output.add(item);}
        }catch(Exception e){failures++;}
        if(failures==(query.isEmpty()?selected:searches).size())throw new IOException("Both catalog requests failed. Please retry.");
        warning=failures>0 || incomplete?"One library is unavailable. These results are incomplete.":"";
        List<JSONObject> grouped=group(output);
        if(failures==0){if(cache.size()>=12)cache.remove(cache.keySet().iterator().next());cache.put(cacheKey,new Cached(grouped,warning));}
        return grouped;
    }
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
