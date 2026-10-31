package app.veyra;
import android.content.Context;
/** Counts elapsed playing time, excluding pauses, buffering, trailers and private viewing. Never uses seek position. */
final class PlaybackMeter {
 private long last=-1;
 void state(Context c,boolean playing){flush(c);last=playing?android.os.SystemClock.elapsedRealtime():-1;}
 void flush(Context c){if(last<0)return;long now=android.os.SystemClock.elapsedRealtime(),delta=Math.max(0,Math.min(86400000L,now-last));last=now;if(ProfileStore.prefs(c,"privacy").getBoolean("pauseHistory",false)||c instanceof android.app.Activity&&((android.app.Activity)c).getIntent().getBooleanExtra("trailer",false))return;long total=PersonalStore.prefs(c).getLong("stat:watchMs",0);if(delta>0)PersonalStore.prefs(c).edit().putLong("stat:watchMs",Math.min(3600000000000L,Math.max(0,total)+delta)).apply();}
}
