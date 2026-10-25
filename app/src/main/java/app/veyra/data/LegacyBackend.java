package app.veyra.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

/** APK-derived protocol. Secrets and optional SDK binaries are private build inputs, never repository content. */
public final class LegacyBackend implements StreamingBackend {
    @android.annotation.SuppressLint("StaticFieldLeak") // Application contexts only.
    private static final Map<String,LegacyBackend> instances=new HashMap<>();
    public final String provider;
    private final Context context;
    private final JSONObject config;
    private final String deviceId;
    private String token="", nativeConfig="";
    private final NativeEngine engine;
    public static LegacyBackend get(Context context) throws Exception {return get(context,"current");}
    public static synchronized LegacyBackend get(Context context,String provider) throws Exception {
        if(!provider.equals("current") && !provider.equals("classic"))throw new IOException("Unknown catalog");
        LegacyBackend value=instances.get(provider);
        if(value==null){value=new LegacyBackend(context.getApplicationContext(),provider);instances.put(provider,value);}return value;
    }
    private LegacyBackend(Context context,String provider) throws Exception {
        this.context=context;this.provider=provider;this.engine=NativeEngine.get(context,provider);
        try(InputStream in=context.getAssets().open(provider.equals("classic")?"backend-classic.json":"backend.json")) { config=new JSONObject(read(in,65536)); }
        URI base=URI.create(config.getString("baseUrl"));
        if(!"https".equals(base.getScheme()) || base.getHost()==null || base.getRawUserInfo()!=null)throw new IOException("Invalid backend configuration");
        SharedPreferences prefs=context.getSharedPreferences("installation-"+provider,Context.MODE_PRIVATE);
        String id=prefs.getString("id",null);
        if(id==null) { id=UUID.randomUUID().toString().replace("-","");prefs.edit().putString("id",id).apply(); }
        deviceId=id;
    }
    private synchronized void session() throws Exception {
        if(!token.isEmpty())return;
        JSONObject result=(JSONObject)request("/api/public/init",map("invited_by","","is_install","0"));
        token=result.getJSONObject("user_info").getString("token");
        // Streaming SDK config is populated only by a verified server response.
        nativeConfig="";
    }
    public synchronized JSONArray channels() throws Exception { session();return (JSONArray)request("/api/channel/get_list",map()); }
    public synchronized JSONArray catalog(String channel,int page) throws Exception { session();return (JSONArray)request("/api/channel/get_info",map("channel_id",channel,"pn",String.valueOf(page))); }
    public synchronized Object search(String text,int page) throws Exception { session();return request("/api/search/result",map("kw",text,"pn",String.valueOf(page))); }
    public synchronized Object shortCatalog(String query,int page) throws Exception {
        session();return query.isEmpty()?request("/api/playlet/screen",map("pn",String.valueOf(page),"psize","20")):request("/api/playlet/result",map("kw",query,"pn",String.valueOf(page),"type_pid","99"));
    }
    public synchronized JSONObject detail(String id,String audioType,String kind) throws Exception {
        if(!"short".equals(kind))return detail(id,audioType);
        session();JSONObject detail=normalizeShort((JSONObject)request("/api/playlet/info",map("id",id)));
        JSONArray input=detail.optJSONArray("collection"),episodes=new JSONArray();
        if(input!=null)for(int i=0;i<input.length();i++){JSONObject episode=new JSONObject(input.getJSONObject(i).toString());episode.put("vod_url",episode.optString("video_url"));episodes.put(episode);}
        detail.put("vod_collection",episodes);return detail;
    }
    public static JSONObject normalizeShort(JSONObject input) throws JSONException {
        JSONObject result=new JSONObject(input.toString());result.put("vod_name",input.optString("title")).put("vod_pic",input.optString("cover")).put("vod_blurb",input.optString("blurb")).put("_kind","short");return result;
    }
    public synchronized JSONObject detail(String id,String audioType) throws Exception {
        session(); int localPort=streamingPort();
        String time=String.valueOf(System.currentTimeMillis());
        URL url=new URL("http://127.0.0.1:"+localPort+"/control?msg=verify&device_id="+enc(deviceId+id)+"&ts="+time);
        HttpURLConnection connection=(HttpURLConnection)url.openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setInstanceFollowRedirects(false);
        String signature;
        try { if(connection.getResponseCode()!=200)throw new IOException("Playback authorization failed");try(InputStream in=connection.getInputStream()){signature=read(in,8192);} }
        finally {connection.disconnect();}
        if(signature.isEmpty())throw new IOException("Playback authorization unavailable");
        return (JSONObject)request("/api/vod/info_new",map("vod_id",id,"audio_type",audioType,"cur_time",time,"sign",signature));
    }
    private int streamingPort() throws Exception {
        if(engine.port()>0)return engine.port();
        if(nativeConfig.isEmpty())nativeConfig=String.valueOf(request("/api/public/get_sys_conf",map("conf_key","p2p_config")));
        return engine.start(nativeConfig);
    }
    public synchronized String playbackJson(JSONObject detail,JSONObject episode,String sourceLabel) throws Exception {
        String url=episode.optString("vod_url");
        if(url.isEmpty())url=episode.optString("orginal_url");
        if(url.isEmpty())throw new IOException("No stream was supplied for this episode");
        // Preserve the native SDK resource encoding exactly, without replacing its content server.
        int localPort=streamingPort();
        String path=android.net.Uri.parse(url).getPath();
        String mime=path!=null && path.toLowerCase(Locale.ROOT).endsWith(".mp4")?"video/mp4":"application/x-mpegURL";
        String proxy="http://127.0.0.1:"+localPort+(mime.equals("video/mp4")?"/resource.mp4?src=":"/resource.m3u8?src=")+Base64.encodeToString(url.getBytes(StandardCharsets.UTF_8),Base64.NO_WRAP);
        JSONObject source=new JSONObject().put("id",detail.optString("audio_type","0")).put("label",sourceLabel).put("url",proxy).put("mimeType",mime).put("localProxy",true);
        String caption=episode.optString("srt_url","");
        if(!caption.isEmpty())try {
            URI uri=URI.create(caption);String captionPath=uri.getPath()==null?"":uri.getPath().toLowerCase(Locale.ROOT);
            String captionMime=captionPath.endsWith(".vtt")?"text/vtt":captionPath.endsWith(".ttml") || captionPath.endsWith(".xml")?"application/ttml+xml":"application/x-subrip";
            app.veyra.model.SubtitleTrack checked=new app.veyra.model.SubtitleTrack("sidecar","Additional captions",caption,captionMime,episode.optString("srt_language",""));
            source.put("subtitles",new JSONArray().put(new JSONObject().put("id",checked.id).put("label",checked.label).put("url",checked.url).put("mimeType",checked.mimeType).put("language",checked.language)));
        }catch(IllegalArgumentException ignored){ /* Preserve video if an optional caption URL is unsupported. */ }
        return new JSONObject().put("schemaVersion",1).put("contentId",provider+":"+detail.optString("_kind","movie")+":"+detail.optString("id")+":"+EpisodeSelection.key(episode)).put("title",detail.optString("vod_name","Video")+" · "+episode.optString("title","Episode")).put("sources",new JSONArray().put(source)).toString();
    }
    public boolean ownsProxy(URI uri) { int port=engine.port();return port>0 && "http".equals(uri.getScheme()) && "127.0.0.1".equals(uri.getHost()) && uri.getPort()==port && ("/resource.m3u8".equals(uri.getPath()) || "/resource.mp4".equals(uri.getPath())); }
    public static synchronized boolean isActiveProxy(String url) { try {for(LegacyBackend value:instances.values())if(value.ownsProxy(URI.create(url)))return true;return false;}catch(IllegalArgumentException e){return false;} }
    private Object request(String route,Map<String,String> fields) throws Exception {
        URL url=new URL(config.getString("baseUrl").replaceAll("/+$","")+route);
        HttpURLConnection c=(HttpURLConnection)url.openConnection();c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setInstanceFollowRedirects(false);c.setUseCaches(false);
        String time=String.valueOf(System.currentTimeMillis());
        Map<String,String> headers=map("app_id",config.getString("appId"),"package_name",config.getString("packageName"),"version",config.getString("version"),"sys_platform","2","device_id",deviceId,"cur_time",time,"token",token,"channel_code",config.optString("channel"),"mob_mfr",Build.MANUFACTURER,"mobmodel",Build.MODEL,"sysrelease",Build.VERSION.RELEASE,"app_language","en","is_language","en","is_display","en","en_al","0","gaid","","androidid",deviceId.substring(0,16));
        byte[] digest=MessageDigest.getInstance("MD5").digest((config.getString("signingSecret")+deviceId+time).getBytes(StandardCharsets.UTF_8));
        StringBuilder sign=new StringBuilder();for(byte b:digest)sign.append(String.format(Locale.ROOT,"%02X",b&255));headers.put("sign",sign.toString());
        for(Map.Entry<String,String> e:headers.entrySet())c.setRequestProperty(e.getKey(),e.getValue());
        c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");StringBuilder form=new StringBuilder();
        for(Map.Entry<String,String> e:fields.entrySet()){if(form.length()>0)form.append('&');form.append(enc(e.getKey())).append('=').append(enc(e.getValue()));}
        try {
            try(OutputStream out=c.getOutputStream()){out.write(form.toString().getBytes(StandardCharsets.UTF_8));}
            int code=c.getResponseCode(); if(code==401){token="";throw new IOException("Session expired. Please retry.");}
            if(code!=200)throw new IOException("The catalog server returned HTTP "+code);
            String body;try(InputStream in=c.getInputStream()){body=read(in,4*1024*1024);}
            if(!body.trim().startsWith("{")){
                Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(config.getString("responseKey").getBytes(StandardCharsets.UTF_8),"AES"),new IvParameterSpec(config.getString("responseIv").getBytes(StandardCharsets.UTF_8)));
                body=new String(cipher.doFinal(Base64.decode(body,Base64.DEFAULT)),StandardCharsets.UTF_8);
            }
            JSONObject response=new JSONObject(body);
            if(response.optInt("code")!=10000)throw new IOException("The server declined this request (code "+response.optInt("code")+")");
            return response.get("result");
        } finally {c.disconnect();}
    }
    public static List<JSONObject> titles(Object data) throws JSONException {
        LinkedHashMap<String,JSONObject> items=new LinkedHashMap<>();collect(data,items,0);return new ArrayList<>(items.values());
    }
    private static void collect(Object data,Map<String,JSONObject> items,int depth) throws JSONException {
        if(depth>12 || items.size()>=500)return;
        if(data instanceof JSONObject){JSONObject o=(JSONObject)data;
            if(!o.optString("vod_name").isEmpty() && o.has("id")){items.put(o.optString("id"),o);return;}
            Iterator<String> keys=o.keys();while(keys.hasNext())collect(o.get(keys.next()),items,depth+1);
        }else if(data instanceof JSONArray){JSONArray a=(JSONArray)data;for(int i=0;i<a.length();i++)collect(a.get(i),items,depth+1);}
    }
    public static String read(InputStream in,int limit) throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[4096];int n;
        while((n=in.read(b))!=-1){if(out.size()+n>limit)throw new IOException("Response exceeds size limit");out.write(b,0,n);}return out.toString("UTF-8");
    }
    private static String enc(String text) throws UnsupportedEncodingException {return URLEncoder.encode(text,"UTF-8");}
    private static Map<String,String> map(String... fields) {Map<String,String> m=new LinkedHashMap<>();for(int i=0;i<fields.length;i+=2)m.put(fields[i],fields[i+1]);return m;}
}
