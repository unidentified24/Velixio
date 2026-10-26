package app.veyra;

import android.app.Instrumentation;
import android.content.Intent;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

/** Public Media3 test clip only. No private backend inputs, original streams or native SDK are used. */
@RunWith(AndroidJUnit4.class)
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class PlayerDeviceTest {
    private final Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
    private final UiDevice device=UiDevice.getInstance(instrumentation);
    private Intent intent() throws Exception {
        JSONObject source=new JSONObject().put("id","fixture").put("label","Test fixture")
            .put("url","https://storage.googleapis.com/exoplayer-test-media-0/BigBuckBunny_320x180.mp4").put("mimeType","video/mp4");
        String json=new JSONObject().put("schemaVersion",1).put("contentId","device-test:fixture").put("title","Player verification")
            .put("sources",new JSONArray().put(source)).toString();
        instrumentation.getTargetContext().getSharedPreferences("resume",0).edit().clear().commit();
        return new Intent(instrumentation.getTargetContext(),PlayerActivity.class).putExtra("playback",json).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }
    private static Object field(Object target,String name) {
        try {Field field=target.getClass().getDeclaredField(name);field.setAccessible(true);return field.get(target);}
        catch(ReflectiveOperationException e){throw new AssertionError(e);}
    }
    private ExoPlayer player(PlayerActivity activity){return (ExoPlayer)field(activity,"player");}
    private void main(Runnable action){instrumentation.runOnMainSync(action);}
    private void await(String message,BooleanSupplier condition,long timeout) {
        long deadline=SystemClock.elapsedRealtime()+timeout;
        do {
            AtomicBoolean result=new AtomicBoolean();main(()->result.set(condition.getAsBoolean()));
            if(result.get())return;
            SystemClock.sleep(100);
        }while(SystemClock.elapsedRealtime()<deadline);
        fail(message);
    }
    private PlayerActivity ready(ActivityScenario<PlayerActivity> scenario) {
        AtomicReference<PlayerActivity> ref=new AtomicReference<>();AtomicBoolean frame=new AtomicBoolean();
        scenario.onActivity(activity->{ref.set(activity);player(activity).addListener(new Player.Listener(){@Override public void onRenderedFirstFrame(){frame.set(true);}});});
        PlayerActivity activity=ref.get();
        await("Video must decode a first frame",()->frame.get() && player(activity)!=null && player(activity).isPlaying(),45000);
        return activity;
    }
    private Button findButton(View view,String label) {
        if(view instanceof Button && ((Button)view).getText().toString().equals(label))return (Button)view;
        if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){Button found=findButton(group.getChildAt(i),label);if(found!=null)return found;}}
        return null;
    }
    @Test public void videoDecodesSeeksAndKeepsPlayingInManualPip() throws Exception {
        try(ActivityScenario<PlayerActivity> scenario=ActivityScenario.launch(intent())) {
            PlayerActivity activity=ready(scenario);
            main(()->player(activity).seekTo(10000));
            await("Seek must resume playback",()->player(activity).isPlaying() && player(activity).getCurrentPosition()>=10000,15000);
            main(()->{((androidx.media3.ui.PlayerView)field(activity,"view")).showController();Button mini=(Button)field(activity,"miniButton");assertNotNull(mini);assertEquals(View.VISIBLE,mini.getVisibility());mini.performClick();});
            await("Manual mini player must enter Android PiP",activity::isInPictureInPictureMode,10000);
            await("Playback must continue inside PiP",()->player(activity)!=null && player(activity).isPlaying(),10000);
            AtomicReference<MediaController> controls=new AtomicReference<>();
            main(()->controls.set(new MediaController(activity,((MediaSession)field(activity,"session")).getSessionToken())));
            controls.get().getTransportControls().pause();
            await("Media-session pause must pause PiP",()->!player(activity).getPlayWhenReady(),5000);
            controls.get().getTransportControls().play();
            await("Media-session play must resume PiP",()->player(activity).isPlaying(),10000);
            main(activity::finish);
            await("Closing the mini player must release playback",()->activity.isDestroyed() && player(activity)==null,10000);
        }
    }
    @Test public void homeWhilePlayingAutomaticallyEntersPip() throws Exception {
        try(ActivityScenario<PlayerActivity> scenario=ActivityScenario.launch(intent())) {
            PlayerActivity activity=ready(scenario);device.pressHome();
            await("Home while playing must enter PiP",activity::isInPictureInPictureMode,10000);
            await("Home-to-PiP must keep playback",()->player(activity)!=null && player(activity).isPlaying(),10000);
        }
    }
    @Test public void homeWhilePausedDoesNotEnterPipAndReleasesPlayer() throws Exception {
        try(ActivityScenario<PlayerActivity> scenario=ActivityScenario.launch(intent())) {
            PlayerActivity activity=ready(scenario);main(()->player(activity).pause());device.pressHome();
            await("Paused Home must stop/release playback",()->player(activity)==null,10000);
            main(()->assertFalse(activity.isInPictureInPictureMode()));
        }
    }
}
