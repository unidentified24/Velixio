package app.veyra;
import android.content.*;import androidx.media3.datasource.cache.*;import androidx.media3.database.StandaloneDatabaseProvider;import app.veyra.model.*;import org.json.*;import java.io.*;import java.util.*;
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class OfflineStore {
 private static final Map<String,Integer> readers=new HashMap<>();
 static synchronized void pin(String id){readers.put(id,readers.getOrDefault(id,0)+1);}
 static synchronized void unpin(String id){int n=readers.getOrDefault(id,0)-1;if(n<=0)readers.remove(id);else readers.put(id,n);}
 private static final Map<String,SimpleCache> caches=new HashMap<>();
 static String checked(String id){if(id==null||!id.matches("[a-f0-9-]{36}"))throw new IllegalArgumentException("Invalid download");return id;}
 static synchronized SimpleCache cache(Context c,String id){checked(id);SimpleCache existing=caches.get(id);if(existing==null){existing=new SimpleCache(new File(c.getFilesDir(),"downloads/"+id),new NoOpCacheEvictor(),new StandaloneDatabaseProvider(c.getApplicationContext()));caches.put(id,existing);}return existing;}
 static void save(Context c,String id,JSONObject record)throws Exception{checked(id);c.getSharedPreferences("downloads",0).edit().putString(id,record.toString()).commit();}
 static JSONObject get(Context c,String id)throws Exception{return new JSONObject(c.getSharedPreferences("downloads",0).getString(checked(id),"{}"));}
 static List<JSONObject> list(Context c){List<JSONObject> out=new ArrayList<>();for(Map.Entry<String,?> e:c.getSharedPreferences("downloads",0).getAll().entrySet())try{JSONObject record=new JSONObject((String)e.getValue());record.put("downloadId",checked(e.getKey()));out.add(record);}catch(Exception ignored){}out.sort((a,b)->Long.compare(b.optLong("created"),a.optLong("created")));return out;}
 static PlaybackRequest request(Context c,String id)throws Exception{JSONObject r=get(c,id);if(!r.optString("state").equals("complete"))throw new IOException("Download is not ready");String url=r.getString("url"),mime=r.getString("mime");StreamSource source=url.startsWith("http://127.0.0.1:")?StreamSource.localProxy("offline","Downloaded",url,mime):new StreamSource("offline","Downloaded",url,mime,Collections.emptyMap());return new PlaybackRequest("offline:"+checked(id),r.getString("title"),Collections.singletonList(source));}
 static CacheDataSource.Factory readOnly(Context c,String id){return new CacheDataSource.Factory().setCache(cache(c,id)).setUpstreamDataSourceFactory(null).setCacheWriteDataSinkFactory(null);}
 static synchronized void remove(Context c,String id)throws Exception{if(readers.getOrDefault(id,0)>0)throw new IOException("Close offline playback before deleting");if(DownloadWorker.isActive(id))throw new IOException("Cancel this download first");SimpleCache cache=cache(c,id);for(String key:new HashSet<>(cache.getKeys()))cache.removeResource(key);cache.release();caches.remove(id);new File(c.getFilesDir(),"download-art/"+checked(id)+".png").delete();c.getSharedPreferences("downloads",0).edit().remove(checked(id)).commit();}
}
