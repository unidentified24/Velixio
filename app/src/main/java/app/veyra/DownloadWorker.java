package app.veyra;

import android.app.*;
import android.content.*;
import android.os.*;
import androidx.media3.datasource.cache.*;
import androidx.media3.exoplayer.offline.*;
import app.veyra.data.StreamHttp;
import app.veyra.model.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class DownloadWorker extends Service {
 private static volatile String activeId,processingId;
 private static final Semaphore TRANSFER=new Semaphore(1,true);
 private static volatile boolean draining;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Handler main=new Handler(Looper.getMainLooper());
 private volatile Downloader downloader;
 private volatile Thread transferThread;
 private volatile String stopReason="";
 private volatile boolean destroyed;
 private int lastStartId;

 static boolean active(){return draining||processingId!=null;}
 static boolean isActive(String id){return id!=null&&(id.equals(activeId)||id.equals(processingId));}
 static void start(Activity activity,String playback){start(activity,playback,"");}
 static void start(Activity activity,String playback,String poster){try{String id=DownloadQueue.add(activity,playback,poster);String state=OfflineStore.get(activity,id).optString("state");if(state.equals("complete")){android.widget.Toast.makeText(activity,"Already available offline",android.widget.Toast.LENGTH_SHORT).show();return;}kick(activity);android.widget.Toast.makeText(activity,"Added to downloads",android.widget.Toast.LENGTH_SHORT).show();}catch(Exception e){android.widget.Toast.makeText(activity,e.getMessage()==null?"This title could not be queued":e.getMessage(),android.widget.Toast.LENGTH_LONG).show();}}
 static void kick(Context c){Intent i=new Intent(c,DownloadWorker.class).setAction("run");if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i);else c.startService(i);}
 static void resume(Activity a,String id){try{DownloadQueue.resume(a,id);kick(a);}catch(Exception e){android.widget.Toast.makeText(a,e.getMessage(),android.widget.Toast.LENGTH_LONG).show();}}
 static void control(Context c,String id,String action){if(isActive(id))c.startService(new Intent(c,DownloadWorker.class).setAction(action).putExtra("id",id));else try{if(action.equals("pause"))DownloadQueue.state(c,id,"paused","Paused");}catch(Exception ignored){}}

 private Notification notification(String title,int percent){PendingIntent content=PendingIntent.getActivity(this,2,new Intent(this,DownloadsActivity.class).putExtra("showTransfers",true),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);PendingIntent pause=PendingIntent.getService(this,3,new Intent(this,DownloadWorker.class).setAction("pause").putExtra("id",activeId),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"downloads"):new Notification.Builder(this);return b.setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(getSharedPreferences("settings",0).getBoolean("privateDownloadNotices",false)?"Velixio download":title).setContentText(percent<0?"Preparing the next download":percent+"% downloaded").setProgress(100,Math.max(0,percent),percent<0).setContentIntent(content).setOngoing(true).addAction(android.R.drawable.ic_media_pause,"Pause",pause).build();}
 private void notifyProgress(String title,int percent){if(Build.VERSION.SDK_INT<33||checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(91,notification(title,percent));}
 @Override public int onStartCommand(Intent intent,int flags,int startId){lastStartId=startId;String action=intent==null?"run":intent.getAction();if("pause".equals(action)||"cancel".equals(action)){String id=intent.getStringExtra("id");if(activeId!=null&&(id==null||id.equals(activeId))){stopReason=action;Downloader current=downloader;if(current!=null)current.cancel();Thread thread=transferThread;if(thread!=null)thread.interrupt();}return START_NOT_STICKY;}if(draining)return START_NOT_STICKY;DownloadQueue.recover(this);if(Build.VERSION.SDK_INT>=26)((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("downloads","Offline downloads",NotificationManager.IMPORTANCE_LOW));startForeground(91,notification("Velixio downloads",-1));begin();return START_NOT_STICKY;}
 private void begin(){draining=true;worker.execute(()->{boolean held=false;try{TRANSFER.acquire();held=true;String id;while(!destroyed&&(id=DownloadQueue.next(this))!=null){activeId=id;processingId=id;stopReason="";transferThread=Thread.currentThread();transfer(id);Thread.interrupted();transferThread=null;processingId=null;if(!destroyed)activeId=null;}}catch(InterruptedException e){Thread.currentThread().interrupt();}finally{if(held){processingId=null;TRANSFER.release();}main.post(()->{if(destroyed)return;activeId=null;downloader=null;draining=false;if(DownloadQueue.next(this)!=null){begin();return;}stopForeground(true);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(91);stopSelfResult(lastStartId);});}});}
 private void transfer(String id){JSONObject record=null;String policy="";try{
  record=OfflineStore.get(this,id);policy=DownloadPolicy.blocked(this,record.optLong("bytes"),id);if(!policy.isEmpty()){DownloadQueue.state(this,id,"waiting",policy);return;}
  DownloadQueue.state(this,id,"downloading","Resolving a fresh stream…");PlaybackRequest request=DownloadQueue.resolve(this,record);if(destroyed||!stopReason.isEmpty()||Thread.currentThread().isInterrupted())throw new InterruptedException();StreamSource source=request.sources.get(0);
  boolean changed=record.has("url")&&!source.url.equals(record.optString("url"));if(changed){OfflineStore.clearPartial(this,id);record.put("bytes",0).put("percent",0).put("message","Stream changed; restarting this transfer safely");}
  record.put("title",request.title).put("versionLabel",source.label).put("url",source.url).put("mime",source.mimeType).put("state","downloading");OfflineStore.save(this,id,record);notifyProgress(request.title,-1);
  CacheDataSource.Factory factory=new CacheDataSource.Factory().setCache(OfflineStore.cache(this,id)).setUpstreamDataSourceFactory(StreamHttp.factory(source));DownloadRequest download=new DownloadRequest.Builder(id,android.net.Uri.parse(source.url)).setMimeType(source.mimeType).build();downloader=new DefaultDownloaderFactory(factory).createDownloader(download);final JSONObject progress=record;final long[] last={0};final String[] policyFailure={""};
  downloader.download((length,bytes,percent)->{String blocked=DownloadPolicy.blocked(this,bytes,id);if(!blocked.isEmpty()||getFilesDir().getUsableSpace()<256L*1024*1024||bytes>8L*1024*1024*1024){policyFailure[0]=blocked.isEmpty()?"Storage limit reached; free space or adjust your download budget":blocked;stopReason="waiting";downloader.cancel();if(transferThread!=null)transferThread.interrupt();return;}if(destroyed||!stopReason.isEmpty()){downloader.cancel();return;}long now=System.currentTimeMillis();if(now-last[0]>1000){last[0]=now;try{progress.put("percent",Math.max(0,(int)percent)).put("bytes",bytes);OfflineStore.save(this,id,progress);notifyProgress(request.title,(int)percent);}catch(Exception ignored){}}});
  policy=policyFailure[0];if(destroyed||!stopReason.isEmpty()||Thread.currentThread().isInterrupted())throw new InterruptedException();
  record.put("bytes",OfflineStore.cache(this,id).getCacheSpace()).put("state","complete").put("percent",100).put("message","");OfflineStore.save(this,id,record);
 }catch(Exception|LinkageError e){Thread.interrupted();try{if(record==null)record=OfflineStore.get(this,id);String reason=stopReason;String state=destroyed||reason.equals("pause")?"paused":reason.equals("cancel")?"cancelled":reason.equals("waiting")?"waiting":"failed";if(state.equals("cancelled")){OfflineStore.clearPartial(this,id);record.put("bytes",0).put("percent",0);}else record.put("bytes",OfflineStore.cache(this,id).getCacheSpace());String message=state.equals("paused")?"Paused; partial data is kept":state.equals("cancelled")?"Cancelled":state.equals("waiting")?DownloadPolicy.blocked(this,record.optLong("bytes"),id):"Could not download this stream. Tap Retry to resolve it again.";if(state.equals("waiting")&&message.isEmpty())message="Storage limit reached; free space or adjust your budget";record.put("state",state).put("message",message);OfflineStore.save(this,id,record);}catch(Exception ignored){}
 }finally{downloader=null;OfflineStore.releaseIdle(id);}}
 @Override public void onTimeout(int startId,int fgsType){stopReason="pause";if(downloader!=null)downloader.cancel();if(transferThread!=null)transferThread.interrupt();stopSelf();}
 @Override public void onDestroy(){destroyed=true;stopReason="pause";if(downloader!=null)downloader.cancel();if(transferThread!=null)transferThread.interrupt();worker.shutdownNow();draining=false;activeId=null;((VeyraApplication)getApplication()).downloadFinished();super.onDestroy();}
 @Override public IBinder onBind(Intent intent){return null;}
}
