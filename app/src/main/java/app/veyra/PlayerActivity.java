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
import app.veyra.data.CatalogBackend;
import app.veyra.data.EpisodeSelection;
import app.veyra.data.WatchHistory;
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
    private FrameLayout root;
    private LinearLayout header, footer;
    private TextView status, titleView;
    private Button miniButton, sourceButton;
    private String offlineId;private boolean offlinePinned;
    private int sizing;
    private final Runnable delayedBuffer=()->{if(player!=null&&player.getPlaybackState()==Player.STATE_BUFFERING)view.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);};
    private boolean controlsVisible=true, cropVideo;
    private MediaSession session;
    private int sourceIndex;
    private long position;
    private float speed=1f;
    private TrackSelectionParameters trackPreferences;
    private boolean playWhenReady=true, enteringPip;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable saveProgress=new Runnable() {
        public void run() { remember(); handler.postDelayed(this,5000); }
    };
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);Ui.configure(this);
        if(saved!=null){cropVideo=saved.getBoolean("cropVideo",false);sizing=saved.getInt("sizing",cropVideo?1:0);}
        else if(!"short".equals(getIntent().getStringExtra("kind")))setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        if(saved==null && getSharedPreferences("settings",MODE_PRIVATE).getBoolean("rememberSpeed",true))speed=getSharedPreferences("settings",MODE_PRIVATE).getFloat("speed",1f);
        if(saved!=null){position=saved.getLong("position",0);playWhenReady=saved.getBoolean("playing",true);speed=saved.getFloat("speed",1f);}
        else try{position=getSharedPreferences("resume",MODE_PRIVATE).getLong(new JSONObject(getIntent().getStringExtra("playback")).optString("contentId"),0);}catch(Exception ignored){}
        offlineId=getIntent().getStringExtra("offlineId");
        try { request=offlineId!=null?OfflineStore.request(this,offlineId):PlaybackJson.parseInternal(saved!=null && saved.containsKey("playback")?saved.getString("playback"):getIntent().getStringExtra("playback")); }
        catch(Exception e) { if(getIntent().hasExtra("vodId"))recoverPlayback();else finish(); return; }
        position=getSharedPreferences("resume",MODE_PRIVATE).getLong(request.contentId,0);
        if(saved!=null) { sourceIndex=saved.getInt("source",0); position=saved.getLong("position",position); playWhenReady=saved.getBoolean("playing",true); speed=saved.getFloat("speed",1f); }
        if(sourceIndex<0 || sourceIndex>=request.sources.size()) sourceIndex=0;
        createPlayerUi();
    }
    private void createPlayerUi() {
        root=new FrameLayout(this);root.setBackgroundColor(android.graphics.Color.BLACK);
        view=new PlayerView(this);view.setShowSubtitleButton(true);view.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER);
        view.setContentDescription("Video player");view.setControllerShowTimeoutMs(3000);view.setControllerHideOnTouch(true);
        applySizing();
        root.addView(view,new FrameLayout.LayoutParams(-1,-1));
        header=Ui.column(this);header.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xcc000000,0x00000000}));
        LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);
        Button back=Ui.quiet(this,"‹");back.setContentDescription("Back to title");back.setOnClickListener(v->finish());bar.addView(back,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        titleView=Ui.text(this,request.title,14,Ui.INK);titleView.setMaxLines(1);titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);bar.addView(titleView,new LinearLayout.LayoutParams(0,-2,1));
        sourceButton=Ui.chip(this,sourceName());sourceButton.setContentDescription("Change Original, Dub or available version");sourceButton.setOnClickListener(v->chooseSource());if(offlineId==null)bar.addView(sourceButton,new LinearLayout.LayoutParams(Ui.dp(this,92),Ui.dp(this,48)));
        Button rotate=Ui.icon(this,"rotate","Rotate screen");rotate.setOnClickListener(v->setRequestedOrientation(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE));bar.addView(rotate,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        miniButton=Ui.icon(this,"pip","Picture in picture · floating mini player");miniButton.setOnClickListener(v->enterMiniPlayer());miniButton.setVisibility(supportsPip()?View.VISIBLE:View.GONE);bar.addView(miniButton,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        Button settings=Ui.icon(this,"options","Player controls");settings.setContentDescription("Playback options");settings.setOnClickListener(v->playbackOptions());bar.addView(settings,new LinearLayout.LayoutParams(Ui.dp(this,48),Ui.dp(this,48)));
        header.addView(bar);root.addView(header,new FrameLayout.LayoutParams(-1,-2,Gravity.TOP));
        footer=Ui.column(this);footer.setBackgroundColor(0x99000000);status=Ui.text(this,"",13,Ui.INK);status.setMaxLines(3);footer.addView(status);
        FrameLayout.LayoutParams notice=new FrameLayout.LayoutParams(-1,-2,Gravity.TOP);notice.topMargin=Ui.dp(this,56);root.addView(footer,notice);
        status.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence t,int start,int count,int after){} public void onTextChanged(CharSequence t,int start,int before,int count){updateChrome();}public void afterTextChanged(android.text.Editable t){}});
        view.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener)visibility->{controlsVisible=visibility==View.VISIBLE;updateChrome();});
        view.setFullscreenButtonClickListener(fullscreen->setRequestedOrientation(fullscreen?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT));
        view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->updatePip());
        setContentView(root);root.setOnApplyWindowInsetsListener((v,insets)->{
            int left=insets.getSystemWindowInsetLeft(),right=insets.getSystemWindowInsetRight(),top=insets.getSystemWindowInsetTop();
            if(Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){android.view.DisplayCutout cutout=insets.getDisplayCutout();left=Math.max(left,cutout.getSafeInsetLeft());right=Math.max(right,cutout.getSafeInsetRight());top=Math.max(top,cutout.getSafeInsetTop());}
            View controller=view.findViewById(androidx.media3.ui.R.id.exo_controller);if(controller!=null)controller.setPadding(left,0,right,insets.getSystemWindowInsetBottom());
            header.setPadding(left+Ui.dp(this,8),top,right+Ui.dp(this,8),Ui.dp(this,8));footer.setPadding(left+Ui.dp(this,16),0,right+Ui.dp(this,16),0);
            FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)footer.getLayoutParams();lp.topMargin=top+Ui.dp(this,56);footer.setLayoutParams(lp);
            return insets;
        });applyPlayerLayout();
    }
    private String sourceName(){if(offlineId!=null)return "Offline";if(request==null)return "Version";return request.sources.get(Math.max(0,Math.min(sourceIndex,request.sources.size()-1))).label;}
    private void applySizing(){int[] modes={androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT,androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FILL};sizing=Math.max(0,Math.min(2,sizing));view.setResizeMode(modes[sizing]);cropVideo=sizing==1;}
    private void playbackOptions(){
        view.showController();List<String> labels=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        labels.add("Picture size · "+new String[]{"Fit","Fill / crop","Stretch"}[sizing]);actions.add(()->ActionSheet.show(this,"Picture size",new String[]{"Fit · complete picture","Fill · crop edges","Stretch · fill without bars"},index->{sizing=index;applySizing();}));
        labels.add("Video quality");actions.add(()->chooseTracks(C.TRACK_TYPE_VIDEO));
        if(player!=null&&player.getCurrentTracks().isTypeSupported(C.TRACK_TYPE_TEXT)){labels.add("Captions");actions.add(()->chooseTracks(C.TRACK_TYPE_TEXT));}
        if(offlineId==null){labels.add("Download this episode");actions.add(()->DownloadWorker.start(this,getIntent().getStringExtra("playback")));}
        if(getIntent().hasExtra("vodId")){labels.add("Episodes & connection");actions.add(()->ActionSheet.show(this,"Episode controls",new String[]{"Previous episode","Next episode","Reconnect current stream"},index->{if(index<2)resolveSource(selectedType(),index==0?-1:1);else refreshSource(selectedType());}));}
        ActionSheet.show(this,"Watching controls",labels.toArray(new String[0]),index->actions.get(index).run());
    }
    private void updateChrome(){
        if(header==null||footer==null)return;
        boolean visible=controlsVisible&&!isMini();header.setVisibility(visible?View.VISIBLE:View.GONE);
        footer.setVisibility(visible&&status.length()>0?View.VISIBLE:View.GONE);
    }
    private void immersive(){
        if(root==null||isMini())return;
        if(Build.VERSION.SDK_INT>=30){getWindow().setDecorFitsSystemWindows(false);WindowInsetsController bars=getWindow().getInsetsController();if(bars!=null){bars.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);bars.hide(WindowInsets.Type.systemBars());}}
        else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if(Build.VERSION.SDK_INT>=28){WindowManager.LayoutParams attributes=getWindow().getAttributes();attributes.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;getWindow().setAttributes(attributes);}
    }
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)immersive();}
    private void recoverPlayback() {
        if(resolving)return;resolving=true;
        LinearLayout loading=Ui.column(this);loading.addView(Ui.text(this,"Restoring playback…",22,Ui.INK));
        TextView message=Ui.text(this,"Reconnecting to the current episode",16,Ui.MUTED);loading.addView(message);
        Button back=Ui.button(this,"Back to library");back.setOnClickListener(v->finish());loading.addView(back);setContentView(loading);Ui.insets(this,loading,20);
        resolver.execute(()->{try {
            String provider=getIntent().getStringExtra("provider");CatalogBackend backend=CatalogBackend.get(this,provider==null?"current":provider);
            JSONObject detail=backend.detail(getIntent().getStringExtra("vodId"),getIntent().getStringExtra("audioType")==null?"0":getIntent().getStringExtra("audioType"),getIntent().getStringExtra("kind"));
            JSONObject chosen=EpisodeSelection.select(detail.getJSONArray("vod_collection"),getIntent().getStringExtra("collection"),0);
            String json=backend.playbackJson(detail,chosen,"Selected source");
            PlaybackRequest restored=PlaybackJson.parseInternal(json);
            runOnUiThread(()->{
                if(isDestroyed() || isFinishing())return;
                resolving=false;request=restored;sourceIndex=0;getIntent().putExtra("playback",json);
                createPlayerUi();applyPlayerLayout();if(started)startPlayer();
            });
        }catch(Exception|LinkageError e){runOnUiThread(()->{if(!isDestroyed() && !isFinishing()){resolving=false;message.setText("Playback could not be restored. Return to the title to reconnect.");}});}});
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
                CatalogBackend backend=CatalogBackend.get(this,provider==null?"current":provider);
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
        if(trackPreferences!=null)trackPreferences=trackPreferences.buildUpon().clearOverrides().build();
        titleView.setText(request.title);sourceButton.setText(sourceName());
        getIntent().putExtra("playback",json).putExtra("audioType",type).putExtra("collection",collection).putExtra("variants",variants);
        if(started)startPlayer();
    }
    private void chooseSource() {
        try{JSONArray connections=new JSONArray(getIntent().getStringExtra("connections"));if(connections.length()>1){ActionSheet.show(this,"Playback source",new String[]{"Original / Dub / available versions","Change connection"},i->{if(i==0)chooseVersion();else{String[] labels=new String[connections.length()];for(int n=0;n<labels.length;n++)labels[n]="Connection "+(n+1)+(connections.optJSONObject(n).optString("provider").equals(getIntent().getStringExtra("provider"))?" · current":"");ActionSheet.show(this,"Available connections",labels,index->switchConnection(connections.optJSONObject(index)));}});return;}}catch(Exception ignored){}chooseVersion();
    }
    private void switchConnection(JSONObject choice){if(resolving||choice==null)return;resolving=true;status.setText("Opening connection…");String provider=choice.optString("provider"),id=choice.optString("id"),kind=getIntent().getStringExtra("kind");resolver.execute(()->{try{CatalogBackend backend=CatalogBackend.get(this,provider);JSONObject detail=backend.detail(id,"0",kind);JSONObject episode=EpisodeSelection.select(detail.getJSONArray("vod_collection"),getIntent().getStringExtra("collection"),0);String json=backend.playbackJson(detail,episode,"Selected version");PlaybackRequest replacement=PlaybackJson.parseInternal(json);String episodeKey=EpisodeSelection.key(episode);runOnUiThread(()->{if(isDestroyed()||isFinishing())return;getIntent().putExtra("provider",provider).putExtra("vodId",id);completeResolution(replacement,json,"0",episodeKey,detail.optJSONArray("audio_type_option")==null?"[]":detail.optJSONArray("audio_type_option").toString(),0);});}catch(Exception e){runOnUiThread(()->{if(!isDestroyed()){resolving=false;status.setText("This connection could not open the episode. Current playback is preserved.");}});}});}
    private void chooseVersion() {
        if(getIntent().hasExtra("vodId")) {
            try {JSONArray variants=new JSONArray(getIntent().getStringExtra("variants"));
                if(variants.length()>0){String[] labels=new String[variants.length()];for(int i=0;i<labels.length;i++)labels[i]=variants.getJSONObject(i).getString("type_name");
                    ActionSheet.show(this,"Choose version",labels,i->{try{refreshSource(variants.getJSONObject(i).optString("type"));}catch(JSONException ignored){}});return;}
            }catch(Exception ignored){}
        }

        ActionSheet.show(this,"Choose version",request.sources.stream().map(source->source.label).toArray(String[]::new),index->{if(index!=sourceIndex){release();sourceIndex=index;startPlayer();sourceButton.setText(sourceName());}});
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
        ActionSheet.show(this,type==C.TRACK_TYPE_VIDEO?"Stream quality":"Captions",labels.toArray(new String[0]),i->{if(player!=null)choices.get(i).run();});
    }
    private void startPlayer() {
        if(!started || request==null || player!=null || view==null || isFinishing()) return;
        StreamSource source=request.sources.get(sourceIndex);
        if(offlineId==null && source.url.startsWith("http://127.0.0.1:") && !CatalogBackend.isActiveProxy(source.url)) {recoverPlayback();return;}

        if(offlineId!=null&&!offlinePinned){OfflineStore.pin(offlineId);offlinePinned=true;}
        DataSource.Factory http=offlineId==null?StreamHttp.factory(source):OfflineStore.readOnly(this,offlineId);
        player=new ExoPlayer.Builder(this).setSeekBackIncrementMs(10000).setSeekForwardIncrementMs(10000).setMediaSourceFactory(new DefaultMediaSourceFactory(http)).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.setHandleAudioBecomingNoisy(true);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                handler.removeCallbacks(delayedBuffer);view.setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER);if(state==Player.STATE_BUFFERING)handler.postDelayed(delayedBuffer,800);status.setText(state==Player.STATE_ENDED?"Playback finished":"");
                syncSession(); updatePip();
                if(state==Player.STATE_ENDED && getIntent().hasExtra("vodId") && getSharedPreferences("settings",MODE_PRIVATE).getBoolean("autoplay",false)) {
                    remember();resolveSource(selectedType(),1);
                }
            }
            @Override public void onIsPlayingChanged(boolean playing) { view.setKeepScreenOn(playing); syncSession(); updatePip(); }
            @Override public void onPlayWhenReadyChanged(boolean ready,int reason) { syncSession(); updatePip(); }
            @Override public void onVideoSizeChanged(VideoSize size) { updatePip(); }
            @Override public void onPlaybackParametersChanged(PlaybackParameters params) { speed=params.speed;if(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("rememberSpeed",true))getSharedPreferences("settings",MODE_PRIVATE).edit().putFloat("speed",speed).apply();syncSession(); }
            @Override public void onPlayerError(PlaybackException error) {
                status.setText(offlineId!=null?"This offline copy could not play. Delete it and download the title again.":"This stream could not play. It may have expired, lost its connection or require an unsupported format. Open Playback options to reconnect."); syncSession(); updatePip();
            }
        });
        List<MediaItem.SubtitleConfiguration> captions=new ArrayList<>();
        for(SubtitleTrack caption:source.subtitles)captions.add(new MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(caption.url)).setId(caption.id).setLabel(caption.label).setLanguage(caption.language.isEmpty()?null:caption.language).setMimeType(caption.mimeType).build());
        if(trackPreferences==null) {
            TrackSelectionParameters.Builder preferences=player.getTrackSelectionParameters().buildUpon();
            int height=getSharedPreferences("settings",MODE_PRIVATE).getInt("qualityHeight",0);
            if(height>0)preferences.setMaxVideoSize(Integer.MAX_VALUE,height);
            if(height==-1)preferences.setForceHighestSupportedBitrate(true);
            trackPreferences=preferences.build();
        }
        player.setTrackSelectionParameters(trackPreferences);
        player.setMediaItem(new MediaItem.Builder().setMediaId(request.contentId+":"+source.id).setUri(source.url).setMimeType(source.mimeType).setSubtitleConfigurations(captions).build());
        view.setPlayer(player); player.seekTo(position); player.setPlaybackSpeed(speed); player.prepare(); player.setPlayWhenReady(playWhenReady);
        session=new MediaSession(this,"ZunoPlayer");
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
        try { if(!enterPictureInPictureMode(pipParams()))Toast.makeText(this,"Allow picture-in-picture for Zuno in Android settings",Toast.LENGTH_LONG).show(); }
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
        boolean mini=isMini();view.setUseController(!mini);if(mini)view.hideController();
        updateChrome();view.setFullscreenButtonState(getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE);
        immersive();root.requestApplyInsets();
    }
    @Override public void onConfigurationChanged(Configuration config) { super.onConfigurationChanged(config); applyPlayerLayout(); updatePip(); }
    private void remember() {
        if(player==null || request==null)return;
        long current=player.getPlaybackState()==Player.STATE_ENDED?0:Math.max(0,player.getCurrentPosition());
        android.content.SharedPreferences prefs=getSharedPreferences("resume",MODE_PRIVATE);
        android.content.SharedPreferences.Editor e=prefs.edit();
        if(prefs.getAll().size()>200)e.clear();
        e.putLong(request.contentId,current).apply();
        if(getIntent().hasExtra("vodId"))WatchHistory.remember(this,getIntent().getStringExtra("provider"),getIntent().getStringExtra("kind"),getIntent().getStringExtra("vodId"),getIntent().getStringExtra("collection"),selectedType(),getIntent().getStringExtra("catalogTitle")==null?request.title:getIntent().getStringExtra("catalogTitle"),getIntent().getStringExtra("poster"),current,player.getDuration(),player.getPlaybackState()==Player.STATE_ENDED);
    }
    private void release() {
        handler.removeCallbacks(saveProgress);handler.removeCallbacks(delayedBuffer);
        if(player==null){if(offlinePinned){OfflineStore.unpin(offlineId);offlinePinned=false;}return;}
        trackPreferences=player.getTrackSelectionParameters();
        remember(); position=player.getPlaybackState()==Player.STATE_ENDED?0:Math.max(0,player.getCurrentPosition()); playWhenReady=player.getPlayWhenReady(); speed=player.getPlaybackParameters().speed;
        if(session!=null) { session.setActive(false); session.release(); session=null; }
        view.setPlayer(null); player.release(); player=null;if(offlinePinned){OfflineStore.unpin(offlineId);offlinePinned=false;} view.setKeepScreenOn(false); updatePip();
    }
    @Override protected void onStart() { super.onStart(); started=true; startPlayer(); applyPlayerLayout(); }
    // PiP pauses the Activity, but keeps it visible: release only when it stops.
    @Override protected void onStop() { started=false; release(); super.onStop(); }
    @Override protected void onDestroy() { resolver.shutdownNow(); release(); super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle saved) {
        saved.putInt("sizing",sizing);saved.putBoolean("cropVideo",cropVideo);saved.putString("playback",getIntent().getStringExtra("playback"));
        saved.putInt("source",sourceIndex); saved.putLong("position",player==null?position:player.getCurrentPosition());
        saved.putBoolean("playing",player==null?playWhenReady:player.getPlayWhenReady()); saved.putFloat("speed",speed); super.onSaveInstanceState(saved);
    }
}

