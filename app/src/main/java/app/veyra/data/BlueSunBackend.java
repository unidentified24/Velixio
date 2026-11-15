package app.veyra.data;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Consumes a provider-authorized HTTPS catalog gateway. */
final class BlueSunBackend implements StreamingBackend {
    interface Transport { JSONObject get(String path,String... parameters) throws Exception; }

    private final Transport transport;
    private final boolean scoped;

    BlueSunBackend(android.content.Context context,String asset)throws Exception {
        JSONObject config;
        try(InputStream input=context.getAssets().open(asset)){
            config=new JSONObject(CatalogBackend.read(input,8192));
        }
        URI base=URI.create(config.getString("baseUrl"));
        if(!"https".equalsIgnoreCase(base.getScheme())||base.getHost()==null||base.getRawUserInfo()!=null
            ||base.getRawQuery()!=null||base.getRawFragment()!=null){
            throw new IOException("Invalid BlueSun gateway configuration");
        }
        String prefix=base.toASCIIString().replaceAll("/+$","");
        scoped=config.optBoolean("channelScopedSearch");
        transport=(path,parameters)->request(prefix,path,parameters);
    }

    BlueSunBackend(Transport transport){this(transport,false);}
    BlueSunBackend(Transport transport,boolean scoped){this.transport=transport;this.scoped=scoped;}

    @Override public JSONArray channels()throws Exception {return transport.get("/channels").getJSONArray("items");}

    @Override public JSONArray catalog(String channel,int page)throws Exception {
        return transport.get("/catalog","channel",channel,"page",String.valueOf(page)).getJSONArray("items");
    }

    @Override public Object search(String text,int page)throws Exception {
        return transport.get("/search","q",text,"page",String.valueOf(page)).getJSONArray("items");
    }

    @Override public boolean channelScopedSearch(){return scoped;}

    @Override public Object searchInChannel(String channel,String text,int page)throws Exception {
        return transport.get("/search","channel",channel,"q",text,"page",String.valueOf(page)).getJSONArray("items");
    }

    @Override public JSONObject detail(String id,String audioType,String kind)throws Exception {
        JSONObject title=transport.get("/detail","id",id,"audioType",audioType,"kind",kind);
        if(!id.equals(title.getString("id"))||title.optString("vod_name").isEmpty()){
            throw new IOException("BlueSun returned a different title");
        }
        if(!title.optString("audio_type","0").equals(audioType)){
            throw new IOException("Requested BlueSun audio version is unavailable");
        }
        title.getJSONArray("vod_collection");
        return title.put("_kind",kind);
    }

    @Override public String playbackJson(JSONObject detail,JSONObject episode,String label)throws Exception {
        String id=detail.getString("id"),key=EpisodeSelection.key(episode);
        JSONObject result=transport.get("/playback","id",id,"episode",key,
            "audioType",detail.optString("audio_type","0"));
        if(!id.equals(result.getString("id"))||!key.equals(result.getString("episode"))){
            throw new IOException("BlueSun returned a different playback entry");
        }
        if(!detail.optString("audio_type","0").equals(result.getString("audioType"))){
            throw new IOException("BlueSun returned a different audio version");
        }
        String kind=detail.optString("_kind","movie");
        String playback=new JSONObject()
            .put("schemaVersion",1).put("contentId","bluesun:"+kind+":"+id+":"+key)
            .put("audioType",detail.optString("audio_type","0"))
            .put("title",detail.getString("vod_name")).put("sources",result.getJSONArray("sources")).toString();
        PlaybackJson.parse(playback);
        return playback;
    }

    private static JSONObject request(String prefix,String path,String... parameters)throws Exception {
        StringBuilder url=new StringBuilder(prefix).append(path);
        for(int i=0;i<parameters.length;i+=2){
            url.append(i==0?'?':'&').append(parameters[i]).append('=')
                .append(URLEncoder.encode(Objects.toString(parameters[i+1],""),"UTF-8"));
        }
        HttpsURLConnection connection=(HttpsURLConnection)new URL(url.toString()).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("Accept","application/json");
        try{
            int status=connection.getResponseCode();
            if(status==401||status==403)throw new IOException("BlueSun access was denied by the provider");
            if(status!=200)throw new IOException("BlueSun gateway unavailable (HTTP "+status+")");
            try(InputStream input=connection.getInputStream()){
                return new JSONObject(CatalogBackend.read(input,2*1024*1024));
            }catch(JSONException e){throw new IOException("BlueSun returned invalid catalog data",e);}
        }finally{connection.disconnect();}
    }
}
