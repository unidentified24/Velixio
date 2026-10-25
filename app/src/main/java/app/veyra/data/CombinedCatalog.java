package app.veyra.data;
import android.content.Context;
import org.json.*;
import java.util.*;
import java.io.IOException;

/** Both APK backends retain independent sessions and catalog identities. */
public final class CombinedCatalog {
    private final Context context;
    private final Map<String,List<Route>> routes=new LinkedHashMap<>();
    public String warning="";
    private boolean incomplete;
    private static final class Route {final String provider,id;Route(String p,String i){provider=p;id=i;}}
    public CombinedCatalog(Context context){this.context=context.getApplicationContext();}
    public JSONArray channels() throws Exception {
        routes.clear();warning="";int failed=0;
        for(String provider:new String[]{"current","classic"})try {
            JSONArray list=LegacyBackend.get(context,provider).channels();
            for(int i=0;i<list.length();i++){JSONObject channel=list.getJSONObject(i);String name=channel.optString("channel_name");
                if(!routes.containsKey(name))routes.put(name,new ArrayList<>());routes.get(name).add(new Route(provider,channel.optString("id")));}
            if(provider.equals("current"))routes.put("Short dramas",Arrays.asList(new Route("current","$short")));
        }catch(Exception e){failed++;}
        if(routes.isEmpty())throw new IOException("Neither catalog could be reached. Check the private build configuration and connection.");
        incomplete=failed>0;
        if(incomplete)warning="One library is unavailable. Some titles may be missing; retry to reconnect.";
        JSONArray result=new JSONArray();for(String name:routes.keySet())result.put(new JSONObject().put("id",name).put("channel_name",name));return result;
    }
    public List<JSONObject> titles(String channel,String query,int page) throws Exception {
        if(routes.isEmpty())channels();
        List<Route> selected=routes.get(channel);if(selected==null)throw new IOException("Unknown catalog section");
        List<JSONObject> output=new ArrayList<>();int failures=0;
        List<Route> searches=Arrays.asList(new Route("current",""),new Route("classic",""),new Route("current","$short"));
        for(Route route:query.isEmpty()?selected:searches)try {
            LegacyBackend backend=LegacyBackend.get(context,route.provider);
            if(route.id.equals("$short")) {
                Object result=backend.shortCatalog(query,page);
                if(!(result instanceof JSONArray))throw new IOException("Unsupported short catalog response");
                JSONArray array=(JSONArray)result;for(int i=0;i<array.length();i++){JSONObject item=LegacyBackend.normalizeShort(array.getJSONObject(i));item.put("_provider",route.provider);output.add(item);}continue;
            }
            Object response=query.isEmpty()?backend.catalog(route.id,page):backend.search(query,page);
            for(JSONObject item:LegacyBackend.titles(response)){item.put("_provider",route.provider);output.add(item);}
        }catch(Exception e){failures++;}
        if(failures==(query.isEmpty()?selected:searches).size())throw new IOException("Both catalog requests failed. Please retry.");
        warning=failures>0 || incomplete?"One library is unavailable. These results are incomplete.":"";
        return group(output);
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
