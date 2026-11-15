package app.veyra;

import android.content.Intent;
import android.app.PictureInPictureParams;
import app.veyra.data.PlaybackJson;
import androidx.media3.ui.PlayerView;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
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
    @Test public void changingQualityClearsOnlyVideoOverrideAndResetsOldCap() {
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).create()){
            PlayerActivity activity=controller.get();
            TrackGroup video=new TrackGroup(new Format.Builder().setSampleMimeType("video/avc").setHeight(720).build());
            TrackGroup audio=new TrackGroup(new Format.Builder().setSampleMimeType("audio/mp4a-latm").setLanguage("hi").build());
            TrackSelectionParameters previous=new TrackSelectionParameters.Builder().setMaxVideoSize(854,480)
                .setOverrideForType(new TrackSelectionOverride(video,0))
                .setOverrideForType(new TrackSelectionOverride(audio,0)).build();
            TrackSelectionParameters highest=activity.qualityMode(previous,-1);
            assertTrue(highest.forceHighestSupportedBitrate);
            assertEquals(Integer.MAX_VALUE,highest.maxVideoHeight);
            assertEquals(Integer.MAX_VALUE,highest.viewportHeight);
            assertFalse(highest.overrides.containsKey(video));
            assertTrue(highest.overrides.containsKey(audio));
            TrackSelectionParameters automatic=activity.qualityMode(highest,0);
            assertFalse(automatic.forceHighestSupportedBitrate);
            assertEquals(Integer.MAX_VALUE,automatic.maxVideoHeight);
            assertEquals(C.TRACK_TYPE_AUDIO,automatic.overrides.get(audio).getType());
        }
    }
    @Test public void settingsSheetShowsSelectedOptionWithoutChangingItsLabel() {
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).create()){
            int[] chosen={-1};
            android.app.Dialog sheet=ActionSheet.show(controller.get(),"Video quality","Playing 720p",
                new String[]{"Auto (adaptive)","Highest available"},1,index->chosen[0]=index);
            java.util.ArrayList<View> matches=new java.util.ArrayList<>();
            sheet.getWindow().getDecorView().findViewsWithText(matches,"Highest available",View.FIND_VIEWS_WITH_TEXT);
            assertEquals(1,matches.size());
            Button option=(Button)matches.get(0);
            assertTrue(option.isSelected());
            assertEquals("Highest available",option.getText().toString());
            assertTrue(option.performClick());
            assertEquals(1,chosen[0]);
            assertFalse(sheet.isShowing());
        }
    }
    @Test public void nextEpisodeVisibilityFollowsCatalogAvailabilityAfterSwitching() throws Exception {
        org.json.JSONObject first=new org.json.JSONObject().put("id","first");
        org.json.JSONObject last=new org.json.JSONObject().put("id","last");
        org.json.JSONObject detail=new org.json.JSONObject().put("vod_collection",new org.json.JSONArray().put(first).put(last));
        Intent intent=EpisodeAvailability.apply(playback().putExtra("vodId","series-1"),detail,first);
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,intent).create()) {
            PlayerActivity activity=controller.get();
            View next=activity.findViewById(R.id.player_next_episode);
            View previous=activity.findViewById(R.id.player_previous_episode);
            assertEquals(View.VISIBLE,next.getVisibility());
            assertEquals(View.INVISIBLE,previous.getVisibility());
            assertEquals("Next episode",next.getContentDescription().toString());

            EpisodeAvailability.apply(activity.getIntent(),detail,last);
            String json=intent.getStringExtra("playback");
            activity.completeResolution(PlaybackJson.parseInternal(json),json,"0","last","[]",1);
            assertEquals(View.INVISIBLE,next.getVisibility());
            assertEquals(View.VISIBLE,previous.getVisibility());
            assertEquals("Previous episode",previous.getContentDescription().toString());
        }
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).create()) {
            assertEquals(View.INVISIBLE,controller.get().findViewById(R.id.player_next_episode).getVisibility());
        }
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

    @Test public void videoUsesEntireViewportWithControlsShownOrHidden() throws Exception {
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()) {
            PlayerActivity activity=controller.get();Field rootField=PlayerActivity.class.getDeclaredField("root");rootField.setAccessible(true);
            android.widget.FrameLayout root=(android.widget.FrameLayout)rootField.get(activity);
            Field viewField=PlayerActivity.class.getDeclaredField("view");viewField.setAccessible(true);PlayerView video=(PlayerView)viewField.get(activity);
            video.setControllerAnimationEnabled(false);
            for(boolean show:new boolean[]{true,false,true}){
                if(show)video.showController();else video.hideController();
                root.measure(android.view.View.MeasureSpec.makeMeasureSpec(960,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(540,android.view.View.MeasureSpec.EXACTLY));root.layout(0,0,960,540);
                assertEquals(960,video.getWidth());assertEquals(540,video.getHeight());assertEquals(0,video.getLeft());assertEquals(0,video.getTop());
            }
        }
    }
    @Test public void hidingPlaybackControlsAlsoHidesAppChrome() throws Exception {
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()) {
            PlayerActivity activity=controller.get();Field vf=PlayerActivity.class.getDeclaredField("view");vf.setAccessible(true);PlayerView video=(PlayerView)vf.get(activity);video.setControllerAnimationEnabled(false);
            Field hf=PlayerActivity.class.getDeclaredField("header");hf.setAccessible(true);Field ff=PlayerActivity.class.getDeclaredField("footer");ff.setAccessible(true);
            Field sf=PlayerActivity.class.getDeclaredField("status");sf.setAccessible(true);((android.widget.TextView)sf.get(activity)).setText("Loading…");
            video.showController();assertEquals(android.view.View.VISIBLE,((android.view.View)hf.get(activity)).getVisibility());
            video.hideController();assertEquals(android.view.View.GONE,((android.view.View)hf.get(activity)).getVisibility());assertEquals(android.view.View.GONE,((android.view.View)ff.get(activity)).getVisibility());
            video.showController();assertEquals(android.view.View.VISIBLE,((android.view.View)hf.get(activity)).getVisibility());
            assertTrue((activity.getWindow().getDecorView().getSystemUiVisibility()&android.view.View.SYSTEM_UI_FLAG_FULLSCREEN)!=0);
        }
    }

    @Test public void sizingModesIncludeFitFillAndStretch()throws Exception{try(ActivityController<PlayerActivity> c=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()){PlayerActivity a=c.get();Field mode=PlayerActivity.class.getDeclaredField("sizing");mode.setAccessible(true);Field vf=PlayerActivity.class.getDeclaredField("view");vf.setAccessible(true);java.lang.reflect.Method apply=PlayerActivity.class.getDeclaredMethod("applySizing");apply.setAccessible(true);int[] expected={androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL};for(int i=0;i<3;i++){mode.setInt(a,i);apply.invoke(a);assertEquals(expected[i],((PlayerView)vf.get(a)).getResizeMode());}}}
    @Test @Config(qualifiers="w320dp-h640dp")
    public void compactToolbarOffersSizingThroughItsOverflow() throws Exception {
        try(ActivityController<PlayerActivity> controller=Robolectric.buildActivity(PlayerActivity.class,playback()).setup()){
            PlayerActivity activity=controller.get();
            android.widget.LinearLayout tools=activity.findViewById(R.id.velixio_player_tools);
            assertNull(activity.findViewById(androidx.media3.ui.R.id.exo_settings));
            assertEquals(1,((ViewGroup)tools.getParent().getParent()).getChildCount());
            Button more=null;
            for(int i=0;i<tools.getChildCount();i++){
                View child=tools.getChildAt(i);
                if("Video and tracks".equals(child.getContentDescription()))more=(Button)child;
            }
            assertNotNull(more);
            assertEquals(View.VISIBLE,more.getVisibility());
            more.performClick();
            android.app.Dialog sheet=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            java.util.ArrayList<View> sizes=new java.util.ArrayList<>();
            sheet.getWindow().getDecorView().findViewsWithText(sizes,"Picture size",View.FIND_VIEWS_WITH_TEXT);
            assertEquals(1,sizes.size());
            sizes.get(0).performClick();
            Field field=PlayerActivity.class.getDeclaredField("view");
            field.setAccessible(true);
            assertEquals(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,((PlayerView)field.get(activity)).getResizeMode());
        }
    }
}
