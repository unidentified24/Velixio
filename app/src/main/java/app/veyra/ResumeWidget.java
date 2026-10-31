package app.veyra;
import android.app.*;import android.appwidget.*;import android.content.*;import android.widget.RemoteViews;import org.json.*;import java.util.*;
public final class ResumeWidget extends AppWidgetProvider {
 private static long refreshed;
 public static synchronized void refresh(Context c,boolean force){long now=android.os.SystemClock.elapsedRealtime();if(!force&&now-refreshed<60000)return;refreshed=now;AppWidgetManager manager=AppWidgetManager.getInstance(c);int[] ids=manager.getAppWidgetIds(new ComponentName(c,ResumeWidget.class));if(ids.length>0)new ResumeWidget().onUpdate(c,manager,ids);}
 static String title(Context c){if(c.getSharedPreferences("settings",0).getBoolean("privateWidget",false))return "Your library is waiting";List<JSONObject> history=app.veyra.data.WatchHistory.list(c);return history.isEmpty()?"Find your next story":history.get(0).optString("title","Continue watching");}
 public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids){RemoteViews views=new RemoteViews(c.getPackageName(),R.layout.resume_widget);views.setTextViewText(R.id.widget_title,title(c));views.setTextViewText(R.id.widget_profile,ProfileStore.name(c));views.setOnClickPendingIntent(R.id.widget_root,PendingIntent.getActivity(c,10000+id,new Intent(c,MainActivity.class).putExtra("destination","continue"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));manager.updateAppWidget(id,views);}}
}
