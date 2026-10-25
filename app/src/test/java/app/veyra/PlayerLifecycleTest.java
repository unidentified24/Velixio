package app.veyra;

import android.content.Intent;
import android.app.PictureInPictureParams;
import app.veyra.data.PlaybackJson;
import androidx.media3.ui.PlayerView;
import android.os.Bundle;
import androidx.media3.exoplayer.ExoPlayer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import static org.junit.Assert.*;

/** Android lifecycle regression tests; these do not assert native SDK/media compatibility. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class PlayerLifecycleTest {
    private Intent playback() {
        return new Intent(RuntimeEnvironment.getApplication(),PlayerActivity.class).putExtra("playback",
            "{\"schemaVersion\":1,\"contentId\":\"test:episode\",\"title\":\"Test episode\",\"sources\":[{\"id\":\"original\",\"label\":\"Original\",\"url\":\"https://127.0.0.1:9/test.mp4\",\"mimeType\":\"video/mp4\"}]}");
    }
    private ExoPlayer player(PlayerActivity activity) throws Exception {
        Field field=PlayerActivity.class.getDeclaredField("player");field.setAccessible(true);return (ExoPlayer)field.get(activity);
    }
    @Test public void pauseKeepsPlayerForPictureInPictureAndStopReleasesIt() throws Exception {
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()) {
            ExoPlayer original=player(controller.get());assertNotNull(original);
            assertTrue(controller.get().enterPictureInPictureMode(new PictureInPictureParams.Builder().build()));
            controller.get().onPictureInPictureModeChanged(true,controller.get().getResources().getConfiguration());
            Field viewField=PlayerActivity.class.getDeclaredField("view");viewField.setAccessible(true);
            assertFalse(((PlayerView)viewField.get(controller.get())).getUseController());
            controller.pause();assertSame(original,player(controller.get()));
            controller.stop();assertNull(player(controller.get()));
        }
    }
    @Test public void stoppedPlayerRestoresPositionSpeedAndPausedState() throws Exception {
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()) {
            ExoPlayer original=player(controller.get());original.seekTo(12000);original.setPlaybackSpeed(1.5f);original.pause();
            controller.pause().stop().start().resume();
            ExoPlayer restored=player(controller.get());assertNotSame(original,restored);
            assertEquals(12000,restored.getCurrentPosition());assertEquals(1.5f,restored.getPlaybackParameters().speed,0.001f);assertFalse(restored.getPlayWhenReady());
        }
    }
    @Test public void savedStateWinsOverOlderPersistedResumePosition() throws Exception {
        Bundle saved=new Bundle();saved.putLong("position",37000);saved.putBoolean("playing",false);saved.putFloat("speed",1.25f);
        RuntimeEnvironment.getApplication().getSharedPreferences("resume",0).edit().putLong("test:episode",5000).commit();
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).create(saved).start().resume()) {
            ExoPlayer restored=player(controller.get());assertEquals(37000,restored.getCurrentPosition());assertFalse(restored.getPlayWhenReady());assertEquals(1.25f,restored.getPlaybackParameters().speed,0.001f);
        }
    }
    @Test public void malformedPlaybackClosesWithoutStartingPlayer() throws Exception {
        Intent invalid=playback().putExtra("playback","invalid");
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,invalid).create().start()) {
            assertTrue(controller.get().isFinishing());assertNull(player(controller.get()));
        }
    }
    @Test public void resolvedSourceRestartsWhileVisibleWithoutWindowFocus() throws Exception {
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()) {
            PlayerActivity activity=controller.get();ExoPlayer original=player(activity);original.seekTo(19000);original.pause();
            assertFalse(activity.hasWindowFocus());
            String json=playback().getStringExtra("playback");
            activity.completeResolution(PlaybackJson.parse(json),json,"sub","1","[]",0);
            ExoPlayer replacement=player(activity);assertNotNull(replacement);assertNotSame(original,replacement);
            assertEquals(19000,replacement.getCurrentPosition());assertFalse(replacement.getPlayWhenReady());
        }
    }
    @Test public void resolvedSourceWaitsUntilStoppedActivityReturns() throws Exception {
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()) {
            PlayerActivity activity=controller.get();player(activity).seekTo(25000);player(activity).pause();
            controller.pause().stop();String json=playback().getStringExtra("playback");
            activity.completeResolution(PlaybackJson.parse(json),json,"sub","1","[]",0);
            assertNull(player(activity));
            controller.start().resume();assertEquals(25000,player(activity).getCurrentPosition());assertFalse(player(activity).getPlayWhenReady());
        }
    }

}
