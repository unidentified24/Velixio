package app.veyra;

import android.content.Context;
import app.veyra.data.*;
import app.veyra.model.*;
import org.json.*;
import java.io.IOException;
import java.util.*;

/** Device-private queue. Resolvable jobs store episode identity instead of treating proxy URLs as permanent. */
final class DownloadQueue {
 static synchronized String add(Context c,String json,String poster)throws Exception{
  if(json==null||json.length()>2*1024*1024)throw new IOException("Invalid download request");
  PlaybackRequest request=PlaybackJson.parseInternal(json);StreamSource source=request.sources.get(0);
  JSONObject job=new JSONObject().put("contentId",request.contentId).put("audioType",source.id).put("versionLabel",source.label);
  String[] identity=request.contentId.split(":",4);
  if(identity.length==4&&(identity[0].equals("current")||identity[0].equals("classic"))&&(identity[1].equals("movie")||identity[1].equals("short"))&&!identity[2].isEmpty()&&!identity[3].isEmpty()){
   job.put("provider",identity[0]).put("kind",identity[1]).put("titleId",identity[2]).put("collection",identity[3]);
  }else{
   if(source.url.startsWith("http://127.0.0.1:"))throw new IOException("Reopen the title before downloading");
   job.put("direct",json);
  }
  int pending=0;for(JSONObject record:ordered(c)){
   if(record.optString("contentId").equals(request.contentId)&&record.optString("audioType").equals(source.id)){
    String id=record.getString("downloadId"),state=record.optString("state");
    if(state.equals("failed")||state.equals("cancelled")||state.equals("paused")||state.equals("waiting")){java.util.Iterator<String> fields=job.keys();while(fields.hasNext()){String key=fields.next();record.put(key,job.get(key));}OfflineStore.save(c,id,record);resume(c,id);}
    return id;
   }
   if(!record.optString("state").matches("complete|failed|cancelled"))pending++;
  }
  if(pending>=50)throw new IOException("The queue is full. Remove or finish some transfers first");
  String id=UUID.randomUUID().toString();job.put("title",request.title).put("created",System.currentTimeMillis()).put("state","queued").put("percent",0).put("bytes",0);
  OfflineStore.save(c,id,job);PosterLoader.saveAvailableArtwork(c,id,poster);
  List<String> order=ids(c);order.add(id);saveOrder(c,order);return id;
 }
 private static List<String> ids(Context c){LinkedHashSet<String> out=new LinkedHashSet<>();try{JSONArray stored=new JSONArray(c.getSharedPreferences("download-queue",0).getString("order","[]"));for(int i=0;i<stored.length();i++)try{String id=OfflineStore.checked(stored.getString(i));if(c.getSharedPreferences("downloads",0).contains(id))out.add(id);}catch(Exception ignored){}}catch(Exception ignored){}for(JSONObject r:OfflineStore.list(c))out.add(r.optString("downloadId"));return new ArrayList<>(out);}
 private static void saveOrder(Context c,List<String> order){c.getSharedPreferences("download-queue",0).edit().putString("order",new JSONArray(order).toString()).commit();}
 static synchronized List<JSONObject> ordered(Context c){List<JSONObject> out=new ArrayList<>();for(String id:ids(c))try{out.add(OfflineStore.get(c,id).put("downloadId",id));}catch(Exception ignored){}return out;}
 static synchronized String next(Context c){for(JSONObject record:ordered(c))if(record.optString("state").equals("queued"))return record.optString("downloadId");return null;}
 static synchronized void move(Context c,String id,int offset){List<String> order=ids(c),pending=new ArrayList<>();for(String candidate:order)try{if(!OfflineStore.get(c,candidate).optString("state").equals("complete"))pending.add(candidate);}catch(Exception ignored){}int index=pending.indexOf(OfflineStore.checked(id));if(index<0)return;int target=Math.max(0,Math.min(pending.size()-1,index+offset));String other=pending.get(target);Collections.swap(order,order.indexOf(id),order.indexOf(other));saveOrder(c,order);}
 static synchronized void state(Context c,String id,String state,String message)throws Exception{JSONObject record=OfflineStore.get(c,id);record.put("state",state).put("message",message);OfflineStore.save(c,id,record);}
 static synchronized void resume(Context c,String id)throws Exception{JSONObject r=OfflineStore.get(c,id);if(r.optString("state").equals("complete")||DownloadWorker.isActive(id))return;if(!r.has("contentId"))throw new IOException("Open this title again to restart its download");state(c,id,"queued","");}
 static synchronized void recover(Context c){for(JSONObject r:ordered(c))try{String state=r.optString("state");if(state.equals("downloading"))state(c,r.getString("downloadId"),"paused","Interrupted when the app closed. Tap Resume.");else if(state.equals("waiting")&&DownloadPolicy.blocked(c,r.optLong("bytes"),r.getString("downloadId")).isEmpty())state(c,r.getString("downloadId"),"queued","");}catch(Exception ignored){}}
 static PlaybackRequest resolve(Context c,JSONObject job)throws Exception{
  if(job.has("provider")){
   String provider=job.getString("provider"),kind=job.getString("kind");if(!(provider.equals("current")||provider.equals("classic"))||!(kind.equals("movie")||kind.equals("short")))throw new IOException("Invalid catalog identity");
   CatalogBackend backend=CatalogBackend.get(c,provider);JSONObject detail=backend.detail(job.getString("titleId"),job.getString("audioType"),kind);JSONObject episode=EpisodeSelection.select(detail.getJSONArray("vod_collection"),job.getString("collection"),0);
   String label=job.optString("versionLabel","Selected version");JSONArray versions=detail.optJSONArray("audio_type_option");if(versions!=null)for(int i=0;i<versions.length();i++){JSONObject version=versions.optJSONObject(i);if(version!=null&&version.optString("type").equals(job.getString("audioType")))label=version.optString("type_name",label);}return PlaybackJson.parseInternal(backend.playbackJson(detail,episode,label));
  }
  PlaybackRequest direct=PlaybackJson.parseInternal(job.getString("direct"));if(direct.sources.get(0).url.startsWith("http://127.0.0.1:"))throw new IOException("A local proxy must be refreshed through its catalog");return direct;
 }
}
