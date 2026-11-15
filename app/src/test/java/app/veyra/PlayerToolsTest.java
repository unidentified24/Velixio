package app.veyra;
import android.app.*;import android.content.*;import android.os.*;import android.view.View;import android.widget.Button;import androidx.media3.exoplayer.ExoPlayer;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.android.controller.ActivityController;import java.lang.reflect.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class PlayerToolsTest {
 @Test public void playerShowsBoundedGesturePercentAndPlacesToolsAtTop()throws Exception{
  try(ActivityController<PlayerActivity> c=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()){
   PlayerActivity activity=c.get();
   View tools=activity.findViewById(R.id.velixio_player_tools);
   View header=(View)field("header").get(activity);
   assertSame(header,tools.getParent().getParent());
   assertNotNull(activity.findViewById(androidx.media3.ui.R.id.exo_progress));
   assertEquals(67,PlayerActivity.levelPercent(2f/3));
   assertEquals(0,PlayerActivity.levelPercent(-.1f));
   assertEquals(100,PlayerActivity.levelPercent(1.1f));
  }
 }
 @Test public void privatePlaybackDoesNotWriteResumeHistory()throws Exception{
  try(ActivityController<PlayerActivity> c=Robolectric.buildActivity(PlayerActivity.class,
      playback().putExtra("privateViewing",true)).setup()){
   PlayerActivity activity=c.get();
   ((ExoPlayer)field("player").get(activity)).seekTo(5000);
   c.pause().stop();
   assertTrue(activity.getSharedPreferences("watch-history",0).getAll().isEmpty());
   assertTrue(activity.getSharedPreferences("resume",0).getAll().isEmpty());
  }
 }
 @Test public void languagePreferencesAreProfileSpecificAndAllowTitleOverrides(){
  Context context=RuntimeEnvironment.getApplication();
  ViewingPreferences.prefs(context).edit().putString("audio","hi").apply();
  ViewingPreferences.prefs(context).edit().putString("audio:current:movie:1","ml").apply();
  assertEquals("hi",ViewingPreferences.language(context,"current:movie:2","audio"));
  assertEquals("ml",ViewingPreferences.language(context,"current:movie:1","audio"));
  String profile=ProfileStore.create(context,"Other viewer");
  assertEquals("",ViewingPreferences.language(ProfileStore.fixed(context,profile),"current:movie:1","audio"));
 }
 private Intent playback(){return new Intent(RuntimeEnvironment.getApplication(),PlayerActivity.class).putExtra("playback","{\"schemaVersion\":1,\"contentId\":\"current:movie:42:id:1\",\"title\":\"Test\",\"sources\":[{\"id\":\"original\",\"label\":\"Original\",\"url\":\"https://127.0.0.1:9/test.mp4\",\"mimeType\":\"video/mp4\"}]}").putExtra("provider","current").putExtra("kind","movie").putExtra("vodId","42").putExtra("collection","id:1");}
 private Field field(String name)throws Exception{Field f=PlayerActivity.class.getDeclaredField(name);f.setAccessible(true);return f;}
 @Test public void lockedPlayerHidesTouchOverlayAndUnlockButtonInPip()throws Exception{try(ActivityController<PlayerActivity> c=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()){PlayerActivity a=c.get();field("touchLocked").setBoolean(a,true);Method update=PlayerActivity.class.getDeclaredMethod("updateChrome");update.setAccessible(true);update.invoke(a);assertEquals(View.VISIBLE,((View)field("unlock").get(a)).getVisibility());a.enterPictureInPictureMode(new PictureInPictureParams.Builder().build());a.onPictureInPictureModeChanged(true,a.getResources().getConfiguration());assertEquals(View.GONE,((View)field("gestureLayer").get(a)).getVisibility());assertEquals(View.GONE,((View)field("unlock").get(a)).getVisibility());}}
 @Test public void playbackKeepsItsProfileWhenAnotherProfileIsSelected()throws Exception{try(ActivityController<PlayerActivity> c=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()){PlayerActivity a=c.get();((ExoPlayer)field("player").get(a)).seekTo(5000);String other=ProfileStore.create(a,"Other");ProfileStore.select(a,other);c.pause().stop();assertTrue(a.getSharedPreferences("watch-history",0).contains("current:movie:42"));assertTrue(ProfileStore.prefs(RuntimeEnvironment.getApplication(),"watch-history").getAll().isEmpty());}}
 @Test public void restoredSleepTimerPausesPlaybackAtItsDeadline()throws Exception{Bundle saved=new Bundle();saved.putLong("sleepDeadline",SystemClock.elapsedRealtime()+15000);saved.putBoolean("playing",true);try(ActivityController<PlayerActivity> c=Robolectric.buildActivity(PlayerActivity.class,playback()).create(saved).start().resume()){PlayerActivity a=c.get();org.robolectric.shadows.ShadowLooper.idleMainLooper(15000,java.util.concurrent.TimeUnit.MILLISECONDS);assertFalse(((ExoPlayer)field("player").get(a)).getPlayWhenReady());assertEquals(0,field("sleepDeadline").getLong(a));}}
}
