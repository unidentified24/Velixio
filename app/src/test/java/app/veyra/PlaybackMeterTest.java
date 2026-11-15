package app.veyra;
import android.app.Activity;import android.content.Context;import org.json.JSONObject;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowSystemClock;import java.time.Duration;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class PlaybackMeterTest {
 @Test public void dailyTotalsSplitAtMidnightAndStayWithTheirProfile(){
  Context context=RuntimeEnvironment.getApplication();
  java.util.Calendar midnight=java.util.Calendar.getInstance();
  midnight.clear();midnight.set(2026,10,15,0,0,0);
  ViewingActivityLog.record(context,5000,midnight.getTimeInMillis()+1000);
  assertArrayEquals(new long[]{4000,1000},ViewingActivityLog.recent(context,2,midnight));
  Context other=ProfileStore.fixed(context,ProfileStore.create(context,"Second viewer"));
  assertArrayEquals(new long[2],ViewingActivityLog.recent(other,2,midnight));
  ViewingActivityLog.clear(context);
  assertArrayEquals(new long[2],ViewingActivityLog.recent(context,2,midnight));
  assertFalse(PersonalStore.prefs(context).contains("stat:watchMs"));
 }
 @Test public void countsElapsedPlayingTimeAndExcludesPause(){Context c=RuntimeEnvironment.getApplication();PlaybackMeter meter=new PlaybackMeter();meter.state(c,true);ShadowSystemClock.advanceBy(Duration.ofSeconds(2));meter.state(c,false);ShadowSystemClock.advanceBy(Duration.ofSeconds(30));meter.flush(c);assertEquals(2000,PersonalStore.prefs(c).getLong("stat:watchMs",0));meter.state(c,true);ShadowSystemClock.advanceBy(Duration.ofSeconds(3));meter.state(c,false);assertEquals(5000,PersonalStore.prefs(c).getLong("stat:watchMs",0));}
 @Test public void privateViewingDoesNotRecordWatchTime(){Context c=RuntimeEnvironment.getApplication();ProfileStore.prefs(c,"privacy").edit().putBoolean("pauseHistory",true).commit();PlaybackMeter meter=new PlaybackMeter();meter.state(c,true);ShadowSystemClock.advanceBy(Duration.ofSeconds(5));meter.state(c,false);assertFalse(PersonalStore.prefs(c).contains("stat:watchMs"));}
 @Test public void trailersDoNotRecordWatchTime(){Activity a=Robolectric.buildActivity(Activity.class).create().get();a.getIntent().putExtra("trailer",true);PlaybackMeter meter=new PlaybackMeter();meter.state(a,true);ShadowSystemClock.advanceBy(Duration.ofSeconds(5));meter.state(a,false);assertFalse(PersonalStore.prefs(a).contains("stat:watchMs"));}
 @Test public void backupRestoresWatchTimeWithoutRollingBackNewerTotal()throws Exception{Context c=RuntimeEnvironment.getApplication();PersonalStore.prefs(c).edit().putLong("stat:watchMs",2000).commit();JSONObject old=VelixioData.validate(VelixioData.export(c).toString());PersonalStore.prefs(c).edit().putLong("stat:watchMs",5000).commit();VelixioData.restore(c,old);assertEquals(5000,PersonalStore.prefs(c).getLong("stat:watchMs",0));}
}
