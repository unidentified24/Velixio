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
    private final ExecutorService io=Executors.newFixedThreadPool(3);
    private LinearLayout root, content, tabs;
    private TextView status;
    private ScrollView detailScroll;
    private androidx.recyclerview.widget.RecyclerView collection;
    private EditText search;
    private Button retry;
    private final List<NavItem> navigationItems=new ArrayList<>();
    private Runnable repeatRequest;
    private String channel="", query="", screen="catalog";
    private int page=1, generation;
    private JSONObject currentDetail;
    private CombinedCatalog catalog;
    private String selectedProvider="current", selectedKind="movie";
    private Future<?> pending;
    private String restoreScreen="",restoreId="",restoreType="0";
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if(saved!=null){
            channel=saved.getString("channel","");query=saved.getString("query","");page=saved.getInt("page",1);
            restoreScreen=saved.getString("screen","");restoreId=saved.getString("detailId","");restoreType=saved.getString("audioType","0");
            selectedProvider=saved.getString("provider","current");selectedKind=saved.getString("kind","movie");
        }
        catalog=new CombinedCatalog(this);
        root=Ui.column(this);
        LinearLayout brand=new LinearLayout(this);BrandView logo=new BrandView(this);brand.addView(logo,new LinearLayout.LayoutParams(Ui.dp(this,146),Ui.dp(this,52)));View brandSpace=new View(this);brand.addView(brandSpace,new LinearLayout.LayoutParams(0,1,1));
        Button menu=Ui.chip(this,"•••");menu.setContentDescription("Library options");menu.setOnClickListener(v->options());brand.addView(menu);root.addView(brand);
        search=new EditText(this);search.setSingleLine(true);search.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(256)});search.setHint("Search movies, series and short dramas");search.setTextColor(Ui.INK);search.setHintTextColor(Ui.MUTED);search.setText(query);search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_SEARCH){query=search.getText().toString().trim();page=1;((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();loadCatalog();return true;}return false;});root.addView(search);search.setVisibility(View.GONE);
        HorizontalScrollView navigation=new HorizontalScrollView(this);tabs=new LinearLayout(this);navigation.addView(tabs);navigation.setHorizontalScrollBarEnabled(false);root.addView(navigation);
        status=Ui.text(this,"Connecting your library…",14,Ui.MUTED);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(status);retry=Ui.chip(this,"Try again");retry.setVisibility(View.GONE);retry.setOnClickListener(v->{if(repeatRequest!=null)repeatRequest.run();});root.addView(retry);
        FrameLayout body=new FrameLayout(this);detailScroll=new ScrollView(this);content=Ui.column(this);detailScroll.addView(content);body.addView(detailScroll);collection=new androidx.recyclerview.widget.RecyclerView(this);collection.setClipToPadding(false);collection.setItemAnimator(null);body.addView(collection,new FrameLayout.LayoutParams(-1,-1));root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout navigationBar=new LinearLayout(this);navigationBar.setPadding(0,Ui.dp(this,10),0,0);
        for(String label:new String[]{"Home","Search","My Library"}){NavItem tab=new NavItem(this,label,navigationItems.size());navigationItems.add(tab);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(Ui.dp(this,3),0,Ui.dp(this,3),0);navigationBar.addView(tab,lp);tab.setOnClickListener(v->{for(NavItem item:navigationItems)item.setActive(item==tab);if(label.equals("Home")){search.setVisibility(View.GONE);query="";search.setText("");channel="discover";page=1;loadCatalog();}else if(label.equals("Search")){search.setVisibility(View.VISIBLE);search.requestFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);}else favorites();});}
        root.addView(navigationBar);setContentView(root);Ui.insets(this,root,16);
        if(saved==null){List<JSONObject> cached=catalog.cachedHome();if(!cached.isEmpty()){renderTitles(cached);status.setText("Refreshing your library…");}}
        loadChannels();
    }
    private interface Work { Object run() throws Exception; }
    private interface Done { void accept(Object result) throws Exception; }
    private void work(String message,Work task,Done done) {
        repeatRequest=()->work(message,task,done);retry.setVisibility(View.GONE);
        int ticket=++generation;if(pending!=null)pending.cancel(true);status.setText(message);
        pending=io.submit(()->{try {Object result=task.run();runOnUiThread(()->{
            if(isDestroyed() || isFinishing() || ticket!=generation)return;
            try{done.accept(result);}catch(Exception e){showError("The server returned an unsupported response.");}
        });}catch(Exception|LinkageError e){runOnUiThread(()->{if(!isDestroyed() && !isFinishing() && ticket==generation){showError(errorMessage(e));}});}});
    }
    private String errorMessage(Throwable e) {
        if(e instanceof FileNotFoundException)return "The private backend configuration is missing from this build.";
        if(e instanceof java.net.UnknownHostException)return "Cannot reach the catalog. Check your connection and retry.";
        if(e instanceof java.net.SocketTimeoutException)return "The server took too long to respond. Please retry.";
        if(e instanceof javax.net.ssl.SSLException)return "A secure connection could not be verified.";
        if(e instanceof IOException && e.getMessage()!=null && (e.getMessage().startsWith("The streaming") || e.getMessage().startsWith("The server") || e.getMessage().startsWith("Session expired")))return e.getMessage();
        return "This request could not complete. Please retry.";
    }
    private void showError(String message) {
        status.setText(message);retry.setVisibility(View.VISIBLE);
        if(message.contains("(S")){
            long pageSize=0;try{pageSize=android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE);}catch(Exception ignored){}
            String report=message+"\n\nVeyra 0.4.2 · Android "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\n"+android.os.Build.MODEL+" · "+android.text.TextUtils.join(", ",android.os.Build.SUPPORTED_ABIS)+" · page "+pageSize;
            new AlertDialog.Builder(this).setTitle("Playback couldn't start").setMessage(report).setPositiveButton("Close",null).setNeutralButton("Copy details",(dialog,which)->{
                android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Veyra playback diagnostics",report));
                Toast.makeText(this,"Details copied",Toast.LENGTH_SHORT).show();
            }).show();
        }
    }
    private void loadChannels() {
        work("Connecting your library…",()->catalog.channels(),value->{
            JSONArray channels=(JSONArray)value;tabs.removeAllViews();boolean valid=false;for(int i=0;i<channels.length();i++)if(channels.getJSONObject(i).optString("id").equals(channel))valid=true;if(!valid)channel="";
            for(int i=0;i<channels.length();i++) {JSONObject c=channels.getJSONObject(i);String id=c.getString("id");if(channel.isEmpty())channel=id;
                Button b=Ui.chip(this,c.optString("channel_name","Browse"));b.setOnClickListener(v->{channel=id;query="";search.setText("");page=1;loadCatalog();});LinearLayout.LayoutParams chipSpace=new LinearLayout.LayoutParams(-2,-2);chipSpace.setMargins(0,0,Ui.dp(this,8),0);tabs.addView(b,chipSpace);}
            String restored=restoreScreen;restoreScreen="";
            if(restored.equals("detail") && !restoreId.isEmpty())loadDetail(restoreId,restoreType,selectedProvider,selectedKind);
            else if(restored.equals("favorites"))favorites();
            else if(restored.equals("history"))history();
            else loadCatalog();
        });
    }
    private void loadCatalog() {
        screen="catalog";currentDetail=null;String wantedQuery=query,wantedChannel=channel;int wantedPage=page;
        if(wantedChannel.isEmpty()){loadChannels();return;}
        work("Loading stories…",()->catalog.titles(wantedChannel,wantedQuery,wantedPage),value->{
            posters.clearPending();content.removeAllViews();
            @SuppressWarnings("unchecked") List<JSONObject> titles=(List<JSONObject>)value;
            status.setText(catalog.warning.isEmpty()?(wantedQuery.isEmpty()?"Find your next story":"Results for “"+wantedQuery+"”"):catalog.warning);
            if(titles.isEmpty())content.addView(Ui.text(this,"No titles found.",22,Ui.INK));
            renderTitles(titles);

        });
    }
    private void renderTitles(List<JSONObject> titles) {
        detailScroll.setVisibility(View.GONE);collection.setVisibility(View.VISIBLE);
        boolean browsing=screen.equals("catalog");
        if(browsing && (channel.isEmpty()||channel.equals("discover")) && query.isEmpty() && page==1 && !titles.isEmpty()){
            collection.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            collection.setAdapter(new HomeAdapter(this,posters,titles,item->openEdition(item,item.optString("id"))));return;
        }
        CatalogAdapter adapter=new CatalogAdapter(this,posters,titles,browsing&&query.isEmpty()&&page==1,
            item->openEdition(item,item.optString("id")),browsing&&page>1?()->{page--;loadCatalog();}:null,
            browsing&&!titles.isEmpty()?()->{page++;loadCatalog();}:null);
        int columns=getResources().getConfiguration().screenWidthDp>=600?5:3;
        androidx.recyclerview.widget.GridLayoutManager manager=new androidx.recyclerview.widget.GridLayoutManager(this,columns);
        manager.setSpanSizeLookup(new androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup(){public int getSpanSize(int position){return adapter.fullWidth(position)?columns:1;}});
        collection.setLayoutManager(manager);collection.setAdapter(adapter);collection.scrollToPosition(0);
        if(titles.isEmpty())status.setText(screen.equals("favorites")?"Save a title to start your collection.":"No titles found. Try another search.");
    }
    private void showContent(){collection.setVisibility(View.GONE);detailScroll.setVisibility(View.VISIBLE);detailScroll.scrollTo(0,0);}
    private void openEdition(JSONObject item,String id) {
        String kind=item.optString("_kind","movie");
        JSONArray choices=item.optJSONArray("_editions");
        work("Opening title…",()->{
            Exception failure=null;List<String> failures=new ArrayList<>();
            int count=choices==null?1:Math.max(1,choices.length());
            for(int i=0;i<count;i++){
                if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                JSONObject choice=choices==null?null:choices.optJSONObject(i);
                String provider=choice==null?item.optString("_provider","current"):choice.optString("provider","current");
                String titleId=choice==null?id:choice.optString("id",id);
                try{return new Object[]{CatalogBackend.get(this,provider).detail(titleId,"0",kind),provider};}
                catch(Exception e){failure=e;failures.add("Connection "+(i+1)+": "+errorMessage(e));}
            }
            if(failures.size()>1)throw new IOException("The streaming connections could not open this title.\n"+android.text.TextUtils.join("\n",failures));
            throw failure==null?new IOException("The server did not supply this title."):failure;
        },value->{Object[] result=(Object[])value;selectedProvider=(String)result[1];selectedKind=kind;showDetail((JSONObject)result[0]);});
    }

    private void loadDetail(String id,String audioType,String provider,String kind) {
        work("Loading title and playback sources…",()->CatalogBackend.get(this,provider).detail(id,audioType,kind),value->{selectedProvider=provider;selectedKind=kind;showDetail((JSONObject)value);});
    }
    private void showDetail(JSONObject detail) throws Exception {
        showContent();currentDetail=detail;final String detailProvider=selectedProvider,detailKind=selectedKind;screen="detail";content.removeAllViews();status.setText("Choose an episode");
        Button back=Ui.button(this,"‹ Back to library");back.setOnClickListener(v->loadCatalog());content.addView(back);
        ImageView artwork=new ImageView(this);artwork.setScaleType(ImageView.ScaleType.CENTER_CROP);artwork.setContentDescription(null);content.addView(artwork,new LinearLayout.LayoutParams(-1,Ui.dp(this,220)));posters.load(this,artwork,detail.optString("vod_pic"));
        content.addView(Ui.text(this,detail.optString("vod_name","Video"),30,Ui.INK));
        String description=detail.optString("vod_blurb");content.addView(Ui.text(this,android.text.Html.fromHtml(description,android.text.Html.FROM_HTML_MODE_LEGACY).toString(),16,Ui.MUTED));
        Button save=Ui.button(this,favoriteLabel(detail,detailProvider,detailKind));save.setOnClickListener(v->{toggleFavorite(detail,detailProvider,detailKind);save.setText(favoriteLabel(detail,detailProvider,detailKind));});content.addView(save);
        JSONArray audio=detail.optJSONArray("audio_type_option");String selected=detail.optString("audio_type","0"),label="Default";
        LinearLayout variants=new LinearLayout(this);
        if(audio!=null)for(int i=0;i<audio.length();i++){JSONObject a=audio.getJSONObject(i);String type=a.optString("type"),name=a.optString("type_name",type);if(type.equals(selected))label=name;
            Button b=Ui.button(this,name+(type.equals(selected)?" ✓":""));b.setOnClickListener(v->loadDetail(detail.optString("id"),type,detailProvider,detailKind));variants.addView(b);}
        HorizontalScrollView sourceChoices=new HorizontalScrollView(this);sourceChoices.setHorizontalScrollBarEnabled(false);sourceChoices.addView(variants);content.addView(sourceChoices);final String sourceLabel=label;
        JSONArray seasons=detail.optJSONArray("series_info");
        if(seasons!=null && seasons.length()>1){content.addView(Ui.text(this,"Seasons",18,Ui.INK));for(int i=0;i<seasons.length();i++){JSONObject season=seasons.getJSONObject(i);String sid=season.optString("vod_id");if(!sid.isEmpty()){Button b=Ui.button(this,season.optString("series","Season "+(i+1)));b.setOnClickListener(v->loadDetail(sid,"0",detailProvider,detailKind));content.addView(b);}}}
        JSONArray episodes=detail.optJSONArray("vod_collection");
        if(episodes==null || episodes.length()==0){content.addView(Ui.text(this,"The server has not supplied playable episodes for this title.",17,Ui.MUTED));return;}
        for(int i=0;i<episodes.length();i++) {JSONObject episode=episodes.getJSONObject(i);Button play=Ui.button(this,"▶  "+episode.optString("title","Episode "+(i+1)));
            play.setOnClickListener(v->work("Opening episode…",()->CatalogBackend.get(this,detailProvider).playbackJson(detail,episode,sourceLabel),json->launchPlayer(detail,episode,(String)json,detailProvider,detailKind)));content.addView(play);}
    }
    private void launchPlayer(JSONObject detail,JSONObject episode,String json,String provider,String kind) throws Exception {
        status.setText("Choose an episode");
        startActivity(new Intent(this,PlayerActivity.class).putExtra("playback",json).putExtra("audioType",detail.optString("audio_type","0"))
            .putExtra("kind",kind).putExtra("provider",provider).putExtra("vodId",detail.optString("id")).putExtra("collection",EpisodeSelection.key(episode))
            .putExtra("catalogTitle",detail.optString("vod_name","Video")).putExtra("poster",detail.optString("vod_pic"))
            .putExtra("variants",detail.optJSONArray("audio_type_option")==null?"[]":detail.optJSONArray("audio_type_option").toString()));
    }
    private String favoriteLabel(JSONObject detail,String provider,String kind) {
        return getSharedPreferences("favorites",MODE_PRIVATE).contains(provider+":"+kind+":"+detail.optString("id"))?"✓ Saved to My Library":"+ Save to My Library";
    }
    private void toggleFavorite(JSONObject detail,String provider,String kind) {
        try {String id=provider+":"+kind+":"+detail.optString("id");android.content.SharedPreferences prefs=getSharedPreferences("favorites",MODE_PRIVATE);
            if(prefs.contains(id)){prefs.edit().remove(id).apply();Toast.makeText(this,"Removed from My Library",Toast.LENGTH_SHORT).show();}
            else {JSONObject record=new JSONObject();for(String key:new String[]{"id","vod_name","vod_pic","vod_year","vod_area","vod_douban_score"})record.put(key,detail.optString(key));record.put("_provider",provider).put("_kind",kind);prefs.edit().putString(id,record.toString()).apply();Toast.makeText(this,"Saved to My Library",Toast.LENGTH_SHORT).show();}
        }catch(JSONException ignored){status.setText("Could not save this title.");}
    }
    private void favorites() {
        retry.setVisibility(View.GONE);generation++;if(pending!=null)pending.cancel(true);currentDetail=null;screen="favorites";posters.clearPending();content.removeAllViews();status.setText("My Library");
        List<JSONObject> saved=new ArrayList<>();for(Object value:getSharedPreferences("favorites",MODE_PRIVATE).getAll().values())try{saved.add(new JSONObject((String)value));}catch(Exception ignored){}
        renderTitles(saved);tabs.removeAllViews();Button savedTab=Ui.chip(this,"Saved titles");savedTab.setOnClickListener(v->favorites());tabs.addView(savedTab);Button progress=Ui.chip(this,"Continue watching");progress.setOnClickListener(v->history());tabs.addView(progress);
    }
    private void history() {
        showContent();
        retry.setVisibility(View.GONE);generation++;if(pending!=null)pending.cancel(true);currentDetail=null;screen="history";posters.clearPending();content.removeAllViews();status.setText("Continue watching");
        List<JSONObject> records=WatchHistory.list(this);
        if(records.isEmpty()){content.addView(Ui.text(this,"Your unfinished movies and episodes will appear here.",20,Ui.INK));return;}
        for(JSONObject record:records) {
            LinearLayout card=Ui.card(this);LinearLayout.LayoutParams place=new LinearLayout.LayoutParams(-1,-2);place.setMargins(0,0,0,Ui.dp(this,12));content.addView(card,place);
            card.addView(Ui.text(this,record.optString("title","Video"),21,Ui.INK));
            long position=record.optLong("position"),duration=record.optLong("duration");
            String collection=record.optString("collection");String episode=collection.startsWith("id:")?"Current episode":"Episode "+collection;
            card.addView(Ui.text(this,episode+" · "+(position/60000)+" min watched",14,Ui.MUTED));
            if(duration>0){ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);progress.setProgress((int)Math.min(1000,(double)position/duration*1000));card.addView(progress);}
            Button resume=Ui.button(this,"Resume");resume.setOnClickListener(v->resume(record));card.addView(resume);
        }
    }
    private void resume(JSONObject record) {
        String provider=record.optString("provider","current"),kind=record.optString("kind","movie");
        work("Resuming your episode…",()->{
            CatalogBackend backend=CatalogBackend.get(this,provider);JSONObject detail=backend.detail(record.optString("id"),record.optString("audioType","0"),kind);
            JSONObject episode=EpisodeSelection.select(detail.getJSONArray("vod_collection"),record.optString("collection"),0);
            return new Object[]{detail,episode,backend.playbackJson(detail,episode,"Selected source")};
        },result->{Object[] values=(Object[])result;launchPlayer((JSONObject)values[0],(JSONObject)values[1],(String)values[2],provider,kind);});
    }
    private void settings() {
        android.content.SharedPreferences prefs=getSharedPreferences("settings",MODE_PRIVATE);LinearLayout panel=Ui.column(this);int padding=Ui.dp(this,20);panel.setPadding(padding,padding,padding,padding);
        Switch autoplay=new Switch(this);autoplay.setText("Play the next episode automatically");autoplay.setTextColor(Ui.INK);autoplay.setChecked(prefs.getBoolean("autoplay",false));autoplay.setOnCheckedChangeListener((v,checked)->prefs.edit().putBoolean("autoplay",checked).apply());panel.addView(autoplay);
        Switch remember=new Switch(this);remember.setText("Remember playback speed");remember.setTextColor(Ui.INK);remember.setChecked(prefs.getBoolean("rememberSpeed",true));remember.setPadding(0,Ui.dp(this,18),0,Ui.dp(this,18));remember.setOnCheckedChangeListener((v,checked)->prefs.edit().putBoolean("rememberSpeed",checked).apply());panel.addView(remember);
        Button quality=Ui.button(this,"Default video quality");quality.setOnClickListener(v->{
            String[] labels={"Auto","Data saver · up to 480p","HD · up to 720p","Full HD · up to 1080p","Highest available"};int[] heights={0,480,720,1080,-1};int selected=0;
            for(int i=0;i<heights.length;i++)if(heights[i]==prefs.getInt("qualityHeight",0))selected=i;
            new AlertDialog.Builder(this).setTitle("Default video quality").setSingleChoiceItems(labels,selected,(dialog,index)->{prefs.edit().putInt("qualityHeight",heights[index]).apply();dialog.dismiss();}).setNegativeButton("Close",null).show();
        });panel.addView(quality);
        panel.addView(Ui.text(this,"Available quality and captions depend on each stream. You can change them in the player.",14,Ui.MUTED));
        Button clear=Ui.button(this,"Clear watch history");clear.setOnClickListener(v->new AlertDialog.Builder(this).setMessage("Clear watch history and saved playback positions?").setPositiveButton("Clear",(dialog,which)->{WatchHistory.clear(this);Toast.makeText(this,"Watch history cleared",Toast.LENGTH_SHORT).show();if(screen.equals("history"))history();}).setNegativeButton("Cancel",null).show());panel.addView(clear);
        new AlertDialog.Builder(this).setTitle("Playback settings").setView(panel).setPositiveButton("Done",null).show();
    }
    private void options() {
        new AlertDialog.Builder(this).setTitle("Veyra").setItems(new String[]{"Continue watching","My Library","Playback settings","Refresh library"},(dialog,index)->{
            if(index==0)history();else if(index==1)favorites();else if(index==2)settings();else loadChannels();
        }).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=10 || result!=RESULT_OK || data==null || data.getData()==null)return;
        work("Opening playback file…",()->{try(InputStream in=getContentResolver().openInputStream(data.getData())){if(in==null)throw new IOException();String json=CatalogBackend.read(in,65536);PlaybackJson.parse(json);return json;}},value->{status.setText("Ready");startActivity(new Intent(this,PlayerActivity.class).putExtra("playback",(String)value));});
    }
    @Override public void onBackPressed() {if(!screen.equals("catalog")){loadCatalog();}else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle state) {state.putString("channel",channel);state.putString("query",query);state.putInt("page",page);
        state.putString("screen",screen);state.putString("provider",selectedProvider);state.putString("kind",selectedKind);
        if(currentDetail!=null){state.putString("detailId",currentDetail.optString("id"));state.putString("audioType",currentDetail.optString("audio_type","0"));}
        super.onSaveInstanceState(state);}
    @Override protected void onDestroy() {generation++;if(pending!=null)pending.cancel(true);posters.close();catalog.close();io.shutdownNow();super.onDestroy();}
}
