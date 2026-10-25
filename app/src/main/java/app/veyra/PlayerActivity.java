package app.veyra;
import android.app.*;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import app.veyra.data.PlaybackJson;
import app.veyra.model.*;
@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public final class PlayerActivity extends Activity {
    private PlaybackRequest request;
    private ExoPlayer player;
    private PlayerView view;
    private TextView status;
    private Button sourceButton;
    private int sourceIndex;
    private long position;
    private boolean playWhenReady=true;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        try { request=PlaybackJson.parse(getIntent().getStringExtra("playback")); }
        catch(Exception e) { finish(); return; }
        if(saved!=null) { sourceIndex=saved.getInt("source",0); position=saved.getLong("position",0); playWhenReady=saved.getBoolean("playing",true); }
        if(sourceIndex<0 || sourceIndex>=request.sources.size()) sourceIndex=0;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout root=Ui.column(this);
        Button back=Ui.button(this,"‹  Veyra"); back.setContentDescription("Back to Veyra"); back.setOnClickListener(v->finish()); root.addView(back);
        root.addView(Ui.text(this,request.title,25,Ui.INK));
        view=new PlayerView(this); view.setShowSubtitleButton(true); view.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        root.addView(view,new LinearLayout.LayoutParams(-1,0,1));
        status=Ui.text(this,"",14,Ui.MUTED); root.addView(status);
        sourceButton=Ui.button(this,"");
        sourceButton.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Playback source")
            .setSingleChoiceItems(request.sources.stream().map(s->s.label).toArray(String[]::new),sourceIndex,
                (dialog,index)->{ if(index!=sourceIndex) { release(); sourceIndex=index; startPlayer(); } dialog.dismiss(); })
            .setNegativeButton("Close",null).show());
        root.addView(sourceButton);
        Button retry=Ui.button(this,"Retry playback"); retry.setOnClickListener(v->{release();startPlayer();}); root.addView(retry);
        setContentView(root); Ui.insets(this,root,16);
    }
    private void startPlayer() {
        if(request==null || player!=null) return;
        StreamSource source=request.sources.get(sourceIndex); sourceButton.setText(source.label+"  ▾");
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setDefaultRequestProperties(source.headers).setAllowCrossProtocolRedirects(false);
        player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(http)).build();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);
        player.setHandleAudioBecomingNoisy(true);
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                status.setText(state==Player.STATE_BUFFERING ? "Loading…" : state==Player.STATE_ENDED ? "Playback finished" : source.label);
            }
            @Override public void onPlayerError(PlaybackException error) {
                status.setText("Playback unavailable. The source may have expired or be unsupported. Try another source or open a fresh playback file.");
            }
        });
        // Preserve backend URLs and headers; no transcoding or quality cap.
        player.setMediaItem(new MediaItem.Builder().setMediaId(request.contentId+":"+source.id).setUri(source.url).setMimeType(source.mimeType).build());
        view.setPlayer(player); player.seekTo(position); player.prepare(); player.setPlayWhenReady(playWhenReady);
    }
    private void release() {
        if(player==null) return;
        position=Math.max(0,player.getCurrentPosition()); playWhenReady=player.getPlayWhenReady();
        view.setPlayer(null); player.release(); player=null;
    }
    @Override protected void onStart() { super.onStart(); startPlayer(); }
    @Override protected void onStop() { release(); super.onStop(); }
    @Override protected void onSaveInstanceState(Bundle saved) {
        saved.putInt("source",sourceIndex); saved.putLong("position",player==null?position:player.getCurrentPosition());
        saved.putBoolean("playing",player==null?playWhenReady:player.getPlayWhenReady()); super.onSaveInstanceState(saved);
    }
}

