package app.veyra;

import android.app.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.media.MediaMetadata;
import android.os.*;
import android.util.Rational;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.datasource.DataSource;
import app.veyra.data.StreamHttp;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import app.veyra.data.PlaybackJson;
import app.veyra.data.LegacyBackend;
import app.veyra.data.EpisodeSelection;
import org.json.*;
import java.util.concurrent.*;
import app.veyra.model.*;
import java.util.*;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class PlayerActivity extends Activity {
    private PlaybackRequest request;
    private final ExecutorService resolver=Executors.newSingleThreadExecutor();
    private boolean resolving, started;
    private ExoPlayer player;
    private PlayerView view;
    private LinearLayout root, header, footer;
    private TextView status, titleView;
    private Button sourceButton, miniButton;
    private MediaSession session;
    private int sourceIndex;
    private long position;
    private float speed=1f;
    private boolean playWhenReady=true, enteringPip;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable saveProgress=new Runnable() {
        public void run() { remember(); handler.postDelayed(this,5000); }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if(saved!=null){position=saved.getLong("position",0);playWhenReady=saved.getBoolean("playing",true);speed=saved.getFloat("speed",1f);}
        else try{position=getSharedPreferences("resume",MODE_PRIVATE).getLong(new JSONObject(getIntent().getStringExtra("playback")).optString("contentId"),0);}catch(Exception ignored){}
        try { request=PlaybackJson.parseInternal(saved!=null && saved.containsKey("playback")?saved.getString("playback"):getIntent().getStringExtra("playback")); }
        catch(Exception e) { if(getIntent().hasExtra("vodId"))recoverPlayback();else finish(); return; }
        position=getSharedPreferences("resume",MODE_PRIVATE).getLong(request.contentId,0);
        if(saved!=null) { sourceIndex=saved.getInt("source",0); position=saved.getLong("position",position); playWhenReady=saved.getBoolean("playing",true); speed=saved.getFloat("speed",1f); }
        if(sourceIndex<0 || sourceIndex>=request.sources.size()) sourceIndex=0;
        createPlayerUi();
    }
    private void createPlayerUi() {
        root=Ui.column(this); header=Ui.column(this); footer=Ui.column(this);
        Button back=Ui.button(this,"‹  Library"); back.setOnClickListener(v->finish()); header.addView(back);
        titleView=Ui.text(this,request.title,22,Ui.INK); header.addView(titleView); root.addView(header);
        view=new PlayerView(this); view.setShowSubtitleButton(true); view.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        view.setContentDescription("Video player"); root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
        view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->updatePip());
        status=Ui.text(this,"",14,Ui.MUTED); footer.addView(status);
        HorizontalScrollView scroll=new HorizontalScrollView(this); scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout actions=new LinearLayout(this); scroll.addView(actions); footer.addView(scroll);
        sourceButton=action(actions,"Source",()->chooseSource());
        if(getIntent().hasExtra("vodId")) {
            action(actions,"Previous episode",()->resolveSource(selectedType(),-1));
            action(actions,"Next episode",()->resolveSource(selectedType(),1));
        }
        action(actions,"Quality",()->chooseTracks(C.TRACK_TYPE_VIDEO));
        action(actions,"Audio",()->chooseTracks(C.TRACK_TYPE_AUDIO));
        action(actions,"Captions",()->chooseTracks(C.TRACK_TYPE_TEXT));
        miniButton=action(actions,"Mini player",()->enterMiniPlayer());
        miniButton.setVisibility(supportsPip()?View.VISIBLE:View.GONE);
        action(actions,"Full screen",()->{setRequestedOrientation(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE ? android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);});
        action(actions,"Retry",()->{ if(getIntent().hasExtra("vodId"))refreshSource(request.sources.get(sourceIndex).id);else{release();startPlayer();} });
        root.addView(footer); setContentView(root);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            if(isMini()) v.setPadding(0,0,0,0);
            else { int p=Ui.dp(this,12); v.setPadding(p+insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),p+insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom()); }
            return insets;
        });
    }
    private void recoverPlayback() {
        LinearLayout loading=Ui.column(this);loading.addView(Ui.text(this,"Restoring playback…",22,Ui.INK));
        TextView message=Ui.text(this,"Reconnecting to the current episode",16,Ui.MUTED);loading.addView(message);
        Button back=Ui.button(this,"Back to library");back.setOnClickListener(v->finish());loading.addView(back);setContentView(loading);Ui.insets(this,loading,20);
        resolver.execute(()->{try {
            String provider=getIntent().getStringExtra("provider");LegacyBackend backend=LegacyBackend.get(this,provider==null?"current":provider);
            JSONObject detail=backend.detail(getIntent().getStringExtra("vodId"),getIntent().getStringExtra("audioType")==null?"0":getIntent().getStringExtra("audioType"),getIntent().getStringExtra("kind"));
            JSONObject chosen=EpisodeSelection.select(detail.getJSONArray("vod_collection"),getIntent().getStringExtra("collection"),0);
            String json=backend.playbackJson(detail,chosen,"Selected source");
            PlaybackRequest restored=PlaybackJson.parseInternal(json);
            runOnUiThread(()->{
                if(isDestroyed() || isFinishing())return;
                request=restored;sourceIndex=0;getIntent().putExtra("playback",json);
                createPlayerUi();applyPlayerLayout();if(started)startPlayer();
            });
        }catch(Exception|LinkageError e){runOnUiThread(()->{if(!isDestroyed() && !isFinishing())message.setText("Playback could not be restored. Return to the title to reconnect.");});}});
    }
    private Button action(LinearLayout row,String label,Runnable run) {
        Button b=Ui.button(this,label); b.setOnClickListener(v->run.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2); lp.setMargins(0,0,Ui.dp(this,8),0); row.addView(b,lp); return b;
    }
    private String selectedType() {
        String type=getIntent().getStringExtra("audioType");return type==null?"0":type;
    }
    private void refreshSource(String type) { resolveSource(type,0); }
    private void resolveSource(String type,int episodeOffset) {
        if(resolving)return;
        resolving=true;status.setText(episodeOffset==0?"Refreshing source…":"Opening episode…");
        String id=getIntent().getStringExtra("vodId"), collection=getIntent().getStringExtra("collection");
        String provider=getIntent().getStringExtra("provider"),kind=getIntent().getStringExtra("kind");
        resolver.execute(()->{
            try {
                LegacyBackend backend=LegacyBackend.get(this,provider==null?"current":provider);
                JSONObject detail=backend.detail(id,type,kind);
                JSONObject episode=EpisodeSelection.select(detail.getJSONArray("vod_collection"),collection,episodeOffset);
                String nextCollection=EpisodeSelection.key(episode);
                String label=type;JSONArray options=detail.optJSONArray("audio_type_option");
                if(options!=null)for(int i=0;i<options.length();i++){JSONObject o=options.getJSONObject(i);if(o.optString("type").equals(type))label=o.optString("type_name",type);}
                String json=backend.playbackJson(detail,episode,label);PlaybackRequest replacement=PlaybackJson.parseInternal(json);
                String variants=options==null?"[]":options.toString();
                runOnUiThread(()->completeResolution(replacement,json,type,nextCollection,variants,episodeOffset));
            }catch(Exception|LinkageError e){runOnUiThread(()->{
                if(!isDestroyed() && !isFinishing()){
                    resolving=false;
                    String message=e.getMessage();
                    status.setText("This is the first episode".equals(message) || "This is the last episode".equals(message)?message:"The source could not be refreshed. Please retry or return to the library.");
                }
            });}
        });
    }
    // Called on the UI thread after the backend resolves fresh playback data.
    void completeResolution(PlaybackRequest replacement,String json,String type,String collection,String variants,int offset) {
        if(isDestroyed() || isFinishing())return;
        resolving=false;release();request=replacement;sourceIndex=0;
        if(offset!=0){position=getSharedPreferences("resume",MODE_PRIVATE).getLong(request.contentId,0);playWhenReady=true;}
        titleView.setText(request.title);
        getIntent().putExtra("playback",json).putExtra("audioType",type).putExtra("collection",collection).putExtra("variants",variants);
        if(started)startPlayer();
    }
    private void chooseSource() {
        if(getIntent().hasExtra("vodId")) {
            try {JSONArray variants=new JSONArray(getIntent().getStringExtra("variants"));
                if(variants.length()>0){String[] labels=new String[variants.length()];for(int i=0;i<labels.length;i++)labels[i]=variants.getJSONObject(i).getString("type_name");
                    new AlertDialog.Builder(this).setTitle("Playback source").setItems(labels,(d,i)->{try{refreshSource(variants.getJSONObject(i).optString("type"));}catch(JSONException ignored){}}).show();return;}
            }catch(Exception ignored){}
        }

        new AlertDialog.Builder(this).setTitle("Playback source")
            .setSingleChoiceItems(request.sources.stream().map(s->s.label).toArray(String[]::new),sourceIndex,
                (dialog,index)->{ if(index!=sourceIndex) { release(); sourceIndex=index; startPlayer(); } dialog.dismiss(); })
            .setNegativeButton("Close",null).show();
    }
    private void chooseTracks(int type) {
        if(player==null) return;
        List<String> labels=new ArrayList<>(); List<Runnable> choices=new ArrayList<>();
        labels.add(type==C.TRACK_TYPE_TEXT?"Off":"Auto");
        choices.add(()->player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type,type==C.TRACK_TYPE_TEXT).build()));
        for(Tracks.Group group:player.getCurrentTracks().getGroups()) {
            if(group.getType()!=type) continue;
            for(int i=0;i<group.length;i++) {
                if(!group.isTrackSupported(i)) continue;
                Format f=group.getTrackFormat(i); final int index=i;
                String label=type==C.TRACK_TYPE_VIDEO ? (f.height>0?f.height+"p":"Video")+(f.bitrate>0?String.format(Locale.US," · %.1f Mbps",f.bitrate/1000000f):"") : (f.label!=null?f.label:(f.language!=null?f.language:"Track "+(i+1)));
                labels.add(label+(group.isTrackSelected(i)?" ✓":""));
                choices.add(()->player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(type,false).setOverrideForType(new TrackSelectionOverride(group.getMediaTrackGroup(),index)).build()));
            }
        }
        if(labels.size()==1) { Toast.makeText(this,"No additional tracks in this stream",Toast.LENGTH_SHORT).show(); return; }
        new AlertDialog.Builder(this).setTitle(type==C.TRACK_TYPE_VIDEO?"Stream quality":type==C.TRACK_TYPE_AUDIO?"Audio":"Captions").setItems(labels.toArray(new String[0]),(d,i)->{if(player!=null)choices.get(i).run();}).show();
    }
    private void startPlayer() {
        if(!started || request==null || player!=null || view==null || isFinishing()) return;
        StreamSource source=request.sources.get(sourceIndex); sourceButton.setText(source.label+" ▾");
        DataSource.Factory http=StreamHttp.factory(source);
        player=new ExoPlayer.Builder(this).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).setMediaSourceFactory(new DefaultMediaSourceFactory(http)).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.setHandleAudioBecomingNoisy(true);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                status.setText(state==Player.STATE_BUFFERING ? "Loading…" : state==Player.STATE_ENDED ? "Playback finished" : source.label);
                syncSession(); updatePip();
            }
            @Override public void onIsPlayingChanged(boolean playing) { view.setKeepScreenOn(playing); syncSession(); updatePip(); }
            @Override public void onPlayWhenReadyChanged(boolean ready,int reason) { syncSession(); updatePip(); }
            @Override public void onVideoSizeChanged(VideoSize size) { updatePip(); }
            @Override public void onPlaybackParametersChanged(PlaybackParameters params) { speed=params.speed; syncSession(); }
            @Override public void onPlayerError(PlaybackException error) {
                status.setText("This stream could not play. It may have expired, lost its connection or require an unsupported format. Use Retry to reconnect."); syncSession(); updatePip();
            }
        });
        player.setMediaItem(new MediaItem.Builder().setMediaId(request.contentId+":"+source.id).setUri(source.url).setMimeType(source.mimeType).build());
        view.setPlayer(player); player.seekTo(position); player.setPlaybackSpeed(speed); player.prepare(); player.setPlayWhenReady(playWhenReady);
        session=new MediaSession(this,"VeyraPlayer");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { if(player!=null)player.play(); }
            @Override public void onPause() { if(player!=null)player.pause(); }
            @Override public void onSeekTo(long p) { if(player!=null)player.seekTo(Math.max(0,p)); }
            @Override public void onStop() { finish(); }
        },handler);
        session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,request.title).build());
        session.setActive(true); syncSession(); handler.post(saveProgress);
    }
    private void syncSession() {
        if(session==null || player==null)return;
        int state=player.getPlayerError()!=null?PlaybackState.STATE_ERROR:player.isPlaying()?PlaybackState.STATE_PLAYING:player.getPlaybackState()==Player.STATE_BUFFERING?PlaybackState.STATE_BUFFERING:PlaybackState.STATE_PAUSED;
        session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_SEEK_TO|PlaybackState.ACTION_STOP).setState(state,Math.max(0,player.getCurrentPosition()),player.isPlaying()?speed:0f).build());
    }

    private boolean supportsPip() { return Build.VERSION.SDK_INT>=26 && getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE); }
    private boolean isMini() { return enteringPip || (Build.VERSION.SDK_INT>=26 && isInPictureInPictureMode()); }
    private boolean canAutoPip() { return player!=null && PipPolicy.autoEnter(player.getPlayWhenReady(),player.getPlayerError()!=null,player.getPlaybackState()==Player.STATE_IDLE,player.getPlaybackState()==Player.STATE_ENDED); }
    @androidx.annotation.RequiresApi(26)
    private PictureInPictureParams pipParams() {
        VideoSize size=player==null?VideoSize.UNKNOWN:player.getVideoSize();
        PictureInPictureParams.Builder b=new PictureInPictureParams.Builder().setAspectRatio(new Rational(PipPolicy.ratioThousandths(size.width,size.height,size.pixelWidthHeightRatio),1000));
        Rect rect=new Rect(); if(view.getGlobalVisibleRect(rect) && !rect.isEmpty())b.setSourceRectHint(rect);
        if(Build.VERSION.SDK_INT>=31)b.setAutoEnterEnabled(canAutoPip()).setSeamlessResizeEnabled(true);
        return b.build();
    }
    private void updatePip() {
        if(Build.VERSION.SDK_INT<26 || !supportsPip() || view==null || isFinishing())return;
        try { setPictureInPictureParams(pipParams()); } catch(IllegalArgumentException|IllegalStateException ignored) { /* Device policy can disable PiP. */ }
    }
    private void enterMiniPlayer() {
        if(Build.VERSION.SDK_INT<26 || !supportsPip() || player==null)return;
        try { if(!enterPictureInPictureMode(pipParams()))Toast.makeText(this,"Allow picture-in-picture for Veyra in Android settings",Toast.LENGTH_LONG).show(); }
        catch(IllegalStateException|IllegalArgumentException e) { Toast.makeText(this,"Picture-in-picture is unavailable on this device",Toast.LENGTH_LONG).show(); }
    }
    @Override protected void onUserLeaveHint() { super.onUserLeaveHint(); if(Build.VERSION.SDK_INT<31 && canAutoPip())enterMiniPlayer(); }
    @Override public void onPictureInPictureModeChanged(boolean inPip,Configuration config) { super.onPictureInPictureModeChanged(inPip,config); enteringPip=false; applyPlayerLayout(); }
    @androidx.annotation.RequiresApi(31)
    @Override public void onPictureInPictureUiStateChanged(PictureInPictureUiState state) {
        super.onPictureInPictureUiStateChanged(state);
        if(Build.VERSION.SDK_INT>=35 && state.isTransitioningToPip()) { enteringPip=true; applyPlayerLayout(); }
    }
    private void applyPlayerLayout() {
        if(root==null)return;
        boolean mini=isMini(), landscape=getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE;
        header.setVisibility(mini||landscape?View.GONE:View.VISIBLE); footer.setVisibility(mini?View.GONE:View.VISIBLE);
        view.setUseController(!mini); if(mini)view.hideController(); root.requestApplyInsets();
    }
    @Override public void onConfigurationChanged(Configuration config) { super.onConfigurationChanged(config); applyPlayerLayout(); updatePip(); }
    private void remember() {
        if(player==null || request==null)return;
        long current=player.getPlaybackState()==Player.STATE_ENDED?0:Math.max(0,player.getCurrentPosition());
        android.content.SharedPreferences prefs=getSharedPreferences("resume",MODE_PRIVATE);
        android.content.SharedPreferences.Editor e=prefs.edit();
        if(prefs.getAll().size()>200)e.clear();
        e.putLong(request.contentId,current).apply();
    }
    private void release() {
        handler.removeCallbacks(saveProgress);
        if(player==null)return;
        remember(); position=player.getPlaybackState()==Player.STATE_ENDED?0:Math.max(0,player.getCurrentPosition()); playWhenReady=player.getPlayWhenReady(); speed=player.getPlaybackParameters().speed;
        if(session!=null) { session.setActive(false); session.release(); session=null; }
        view.setPlayer(null); player.release(); player=null; view.setKeepScreenOn(false); updatePip();
    }
    @Override protected void onStart() { super.onStart(); started=true; startPlayer(); applyPlayerLayout(); }
    // PiP pauses the Activity, but keeps it visible: release only when it stops.
    @Override protected void onStop() { started=false; release(); super.onStop(); }
    @Override protected void onDestroy() { resolver.shutdownNow(); release(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle saved) {
        saved.putString("playback",getIntent().getStringExtra("playback"));
        saved.putInt("source",sourceIndex); saved.putLong("position",player==null?position:player.getCurrentPosition());
        saved.putBoolean("playing",player==null?playWhenReady:player.getPlayWhenReady()); saved.putFloat("speed",speed); super.onSaveInstanceState(saved);
    }
}

