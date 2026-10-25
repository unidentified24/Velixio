package app.veyra;

import android.app.*;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import app.veyra.data.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private final PosterLoader posters=new PosterLoader();
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private LinearLayout root, content, tabs;
    private TextView status;
    private EditText search;
    private String channel="", query="", screen="catalog";
    private int page=1, generation;
    private JSONObject currentDetail;
    private CombinedCatalog catalog;
    private String selectedProvider="current", selectedKind="movie";
    private boolean busy;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if(saved!=null){channel=saved.getString("channel","");query=saved.getString("query","");page=saved.getInt("page",1);}
        catalog=new CombinedCatalog(this);
        root=Ui.column(this);
        LinearLayout brand=new LinearLayout(this);TextView logo=Ui.text(this,"V E Y R A",26,Ui.INK);brand.addView(logo,new LinearLayout.LayoutParams(0,-2,1));
        Button menu=Ui.button(this,"•••");menu.setContentDescription("Library options");menu.setOnClickListener(v->options());brand.addView(menu);root.addView(brand);
        search=new EditText(this);search.setSingleLine(true);search.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(256)});search.setHint("Search movies and series");search.setTextColor(Ui.INK);search.setHintTextColor(Ui.MUTED);search.setText(query);search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_SEARCH){query=search.getText().toString().trim();page=1;loadCatalog();return true;}return false;});root.addView(search);
        HorizontalScrollView navigation=new HorizontalScrollView(this);tabs=new LinearLayout(this);navigation.addView(tabs);navigation.setHorizontalScrollBarEnabled(false);root.addView(navigation);
        status=Ui.text(this,"Connecting your library…",14,Ui.MUTED);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(status);
        ScrollView scroll=new ScrollView(this);content=Ui.column(this);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);Ui.insets(this,root,20);loadChannels();
    }
    private interface Work { Object run() throws Exception; }
    private interface Done { void accept(Object result) throws Exception; }
    private void work(String message,Work task,Done done) {
        int ticket=++generation;busy=true;status.setText(message);
        io.execute(()->{try {Object result=task.run();runOnUiThread(()->{
            if(isDestroyed() || ticket!=generation)return;busy=false;
            try{done.accept(result);}catch(Exception e){showError("The server returned an unsupported response.");}
        });}catch(Exception|LinkageError e){runOnUiThread(()->{if(!isDestroyed() && ticket==generation){busy=false;showError(errorMessage(e));}});}});
    }
    private String errorMessage(Throwable e) {
        if(e instanceof FileNotFoundException)return "The private backend configuration is missing from this build.";
        if(e instanceof java.net.UnknownHostException)return "Cannot reach the catalog. Check your connection and retry.";
        if(e instanceof java.net.SocketTimeoutException)return "The server took too long to respond. Please retry.";
        if(e instanceof javax.net.ssl.SSLException)return "A secure connection could not be verified.";
        if(e instanceof IOException && e.getMessage()!=null && (e.getMessage().startsWith("The original") || e.getMessage().startsWith("The server") || e.getMessage().startsWith("Session expired")))return e.getMessage();
        return "This request could not complete. Please retry.";
    }
    private void showError(String message) {status.setText(message);}
    private LegacyBackend backend() throws Exception {return LegacyBackend.get(this,selectedProvider);}
    private void loadChannels() {
        work("Connecting your library…",()->catalog.channels(),value->{
            JSONArray channels=(JSONArray)value;tabs.removeAllViews();
            for(int i=0;i<channels.length();i++) {JSONObject c=channels.getJSONObject(i);String id=c.getString("id");if(channel.isEmpty())channel=id;
                Button b=Ui.button(this,c.optString("channel_name","Browse"));b.setOnClickListener(v->{channel=id;query="";search.setText("");page=1;loadCatalog();});tabs.addView(b);}
            loadCatalog();
        });
    }
    private void loadCatalog() {
        screen="catalog";currentDetail=null;String wantedQuery=query,wantedChannel=channel;int wantedPage=page;
        if(wantedChannel.isEmpty()){loadChannels();return;}
        work("Loading stories…",()->catalog.titles(wantedChannel,wantedQuery,wantedPage),value->{
            posters.clearPending();content.removeAllViews();
            @SuppressWarnings("unchecked") List<JSONObject> titles=(List<JSONObject>)value;
            status.setText(catalog.warning+" "+(wantedQuery.isEmpty()?"Discover":"Results for “"+wantedQuery+"”")+" · Page "+wantedPage);
            if(titles.isEmpty())content.addView(Ui.text(this,"No titles found.",22,Ui.INK));
            for(JSONObject item:titles)addTitle(item);
            LinearLayout pages=new LinearLayout(this);
            if(page>1){Button prev=Ui.button(this,"Previous");prev.setOnClickListener(v->{page--;loadCatalog();});pages.addView(prev);}
            if(!titles.isEmpty()){Button next=Ui.button(this,"Next page");next.setOnClickListener(v->{page++;loadCatalog();});pages.addView(next);}
            content.addView(pages);
        });
    }
    private void addTitle(JSONObject item) {
        LinearLayout row=new LinearLayout(this);row.setPadding(0,Ui.dp(this,10),0,Ui.dp(this,14));
        ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setBackgroundColor(android.graphics.Color.rgb(29,27,43));image.setContentDescription(null);
        LinearLayout.LayoutParams cover=new LinearLayout.LayoutParams(Ui.dp(this,88),Ui.dp(this,132));cover.setMargins(0,0,Ui.dp(this,14),0);row.addView(image,cover);posters.load(this,image,item.optString("vod_pic"));
        LinearLayout card=Ui.column(this);row.addView(card,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=Ui.text(this,item.optString("vod_name","Untitled"),20,Ui.INK);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(title);
        String info=item.optString("vod_year")+" · "+item.optString("vod_area")+"  "+item.optString("vod_douban_score");card.addView(Ui.text(this,info,13,Ui.MUTED));
        Button open=Ui.button(this,"View episodes");String id=item.optString("id");open.setOnClickListener(v->openEdition(item,id));card.addView(open);
        content.addView(row);
    }
    private void openEdition(JSONObject item,String id) {
        selectedKind=item.optString("_kind","movie");
        JSONArray editions=item.optJSONArray("_editions");
        if(editions==null || editions.length()<2){selectedProvider=item.optString("_provider","current");loadDetail(id,"0");return;}
        String[] labels=new String[editions.length()];for(int i=0;i<labels.length;i++)labels[i]="Library edition "+(i+1);
        new AlertDialog.Builder(this).setTitle("Choose an edition").setItems(labels,(d,i)->{try{JSONObject edition=editions.getJSONObject(i);selectedProvider=edition.getString("provider");loadDetail(edition.optString("id"),"0");}catch(JSONException ignored){}}).show();
    }
    private void loadDetail(String id,String audioType) {
        final String provider=selectedProvider,kind=selectedKind;
        work("Loading title and playback sources…",()->LegacyBackend.get(this,provider).detail(id,audioType,kind),value->showDetail((JSONObject)value));
    }
    private void showDetail(JSONObject detail) throws Exception {
        currentDetail=detail;screen="detail";content.removeAllViews();status.setText("Choose an episode");
        Button back=Ui.button(this,"‹ Back to library");back.setOnClickListener(v->loadCatalog());content.addView(back);
        content.addView(Ui.text(this,detail.optString("vod_name","Video"),30,Ui.INK));
        String description=detail.optString("vod_blurb");content.addView(Ui.text(this,android.text.Html.fromHtml(description,android.text.Html.FROM_HTML_MODE_LEGACY).toString(),16,Ui.MUTED));
        Button save=Ui.button(this,"Save / remove from My list");save.setOnClickListener(v->toggleFavorite(detail));content.addView(save);
        JSONArray audio=detail.optJSONArray("audio_type_option");String selected=detail.optString("audio_type","0"),label="Default";
        LinearLayout variants=new LinearLayout(this);
        if(audio!=null)for(int i=0;i<audio.length();i++){JSONObject a=audio.getJSONObject(i);String type=a.optString("type"),name=a.optString("type_name",type);if(type.equals(selected))label=name;
            Button b=Ui.button(this,name+(type.equals(selected)?" ✓":""));b.setOnClickListener(v->loadDetail(detail.optString("id"),type));variants.addView(b);}
        content.addView(variants);final String sourceLabel=label;
        JSONArray seasons=detail.optJSONArray("series_info");
        if(seasons!=null && seasons.length()>1){content.addView(Ui.text(this,"Seasons",18,Ui.INK));for(int i=0;i<seasons.length();i++){JSONObject season=seasons.getJSONObject(i);String sid=season.optString("vod_id");if(!sid.isEmpty()){Button b=Ui.button(this,season.optString("series","Season "+(i+1)));b.setOnClickListener(v->loadDetail(sid,"0"));content.addView(b);}}}
        JSONArray episodes=detail.optJSONArray("vod_collection");
        if(episodes==null || episodes.length()==0){content.addView(Ui.text(this,"The server has not supplied playable episodes for this title.",17,Ui.MUTED));return;}
        for(int i=0;i<episodes.length();i++) {JSONObject episode=episodes.getJSONObject(i);Button play=Ui.button(this,"▶  "+episode.optString("title","Episode "+(i+1)));
            play.setOnClickListener(v->work("Opening episode…",()->backend().playbackJson(detail,episode,sourceLabel),json->{status.setText("Choose an episode");startActivity(new Intent(this,PlayerActivity.class).putExtra("playback",(String)json).putExtra("audioType",detail.optString("audio_type","0")).putExtra("kind",selectedKind).putExtra("provider",selectedProvider).putExtra("vodId",detail.optString("id")).putExtra("collection",episode.optString("collection","1")).putExtra("variants",detail.optJSONArray("audio_type_option")==null?"[]":detail.optJSONArray("audio_type_option").toString()));}));content.addView(play);}
    }
    private void toggleFavorite(JSONObject detail) {
        try {String id=selectedProvider+":"+selectedKind+":"+detail.optString("id");android.content.SharedPreferences prefs=getSharedPreferences("favorites",MODE_PRIVATE);
            if(prefs.contains(id)){prefs.edit().remove(id).apply();Toast.makeText(this,"Removed from My list",Toast.LENGTH_SHORT).show();}
            else {JSONObject record=new JSONObject();for(String key:new String[]{"id","vod_name","vod_year","vod_area","vod_douban_score"})record.put(key,detail.optString(key));record.put("_provider",selectedProvider).put("_kind",selectedKind);prefs.edit().putString(id,record.toString()).apply();Toast.makeText(this,"Saved to My list",Toast.LENGTH_SHORT).show();}
        }catch(JSONException ignored){status.setText("Could not save this title.");}
    }
    private void favorites() {
        generation++;screen="favorites";content.removeAllViews();status.setText("My list");
        for(Object value:getSharedPreferences("favorites",MODE_PRIVATE).getAll().values())try{addTitle(new JSONObject((String)value));}catch(Exception ignored){}
        if(content.getChildCount()==0)content.addView(Ui.text(this,"Save titles here to find them later.",20,Ui.INK));
    }
    private void options() {
        new AlertDialog.Builder(this).setTitle("Veyra").setItems(new String[]{"My list","Refresh library","Open playback file","Clear watch progress"},(d,i)->{
            if(i==0)favorites();else if(i==1)loadChannels();else if(i==2){Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(intent,10);}else new AlertDialog.Builder(this).setMessage("Clear saved playback positions?").setPositiveButton("Clear",(x,y)->getSharedPreferences("resume",MODE_PRIVATE).edit().clear().apply()).setNegativeButton("Cancel",null).show();
        }).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=10 || result!=RESULT_OK || data==null || data.getData()==null)return;
        work("Opening playback file…",()->{try(InputStream in=getContentResolver().openInputStream(data.getData())){if(in==null)throw new IOException();String json=LegacyBackend.read(in,65536);PlaybackJson.parse(json);return json;}},value->{status.setText("Ready");startActivity(new Intent(this,PlayerActivity.class).putExtra("playback",(String)value));});
    }
    @Override public void onBackPressed() {if(!screen.equals("catalog")){loadCatalog();}else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle state) {state.putString("channel",channel);state.putString("query",query);state.putInt("page",page);super.onSaveInstanceState(state);}
    @Override protected void onDestroy() {generation++;posters.close();io.shutdownNow();super.onDestroy();}
}
