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
    private int appearanceTheme;
    private boolean appearanceCompact;
    private final List<NavItem> navigationItems=new ArrayList<>();
    private Runnable repeatRequest;
    private String channel="", query="", screen="catalog";
    private int page=1, generation;
    private JSONObject currentDetail;
    private CombinedCatalog catalog;
    private JSONArray channelOptions=new JSONArray();
    private String selectedProvider="current", selectedKind="movie";
    private Future<?> pending;
    private final LinkedHashMap<String,List<JSONObject>> browseCache=new LinkedHashMap<>();
    private final Map<String,Long> browseTimes=new HashMap<>();
    private final Map<String,String> browseWarnings=new HashMap<>();
    private String browseKey(){return channel+"|"+page+"|"+query;}
    private void cacheBrowse(String key,List<JSONObject> titles,String warning){if(!browseCache.containsKey(key)&&browseCache.size()>=8){String first=browseCache.keySet().iterator().next();browseCache.remove(first);browseTimes.remove(first);browseWarnings.remove(first);}browseCache.put(key,new ArrayList<>(titles));browseTimes.put(key,android.os.SystemClock.elapsedRealtime());browseWarnings.put(key,warning);}

    private String restoreScreen="",restoreId="",restoreType="0",returnScreen="catalog";
    private android.os.Parcelable returnScroll;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);Ui.configure(this);appearanceTheme=getSharedPreferences("settings",0).getInt("theme",0);appearanceCompact=getSharedPreferences("settings",0).getBoolean("compactPosters",false);
        if(saved==null){android.content.SharedPreferences discovery=getSharedPreferences("discovery",0);discovery.edit().putLong("visit",discovery.getLong("visit",0)+1).apply();}
        if(saved!=null){
            channel=saved.getString("channel","");query=saved.getString("query","");page=saved.getInt("page",1);
            returnScreen=saved.getString("returnScreen","catalog");returnScroll=saved.getParcelable("returnScroll");restoreScreen=saved.getString("screen","");restoreId=saved.getString("detailId","");restoreType=saved.getString("audioType","0");
            selectedProvider=saved.getString("provider","current");selectedKind=saved.getString("kind","movie");
        }
        catalog=new CombinedCatalog(this);
        root=Ui.column(this);
        LinearLayout brand=new LinearLayout(this);BrandView logo=new BrandView(this);brand.addView(logo,new LinearLayout.LayoutParams(Ui.dp(this,120),Ui.dp(this,48)));View brandSpace=new View(this);brand.addView(brandSpace,new LinearLayout.LayoutParams(0,1,1));
        Button menu=Ui.quiet(this,"Settings");menu.setContentDescription("Open Settings");menu.setOnClickListener(v->options());brand.addView(menu);root.addView(brand);
        search=new EditText(this);search.setSingleLine(true);search.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(256)});search.setHint("Search movies, series and short dramas");search.setTextColor(Ui.INK);search.setHintTextColor(Ui.MUTED);search.setText(query);search.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        search.setOnEditorActionListener((v,action,event)->{if(action==EditorInfo.IME_ACTION_SEARCH){query=search.getText().toString().trim();page=1;((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();loadCatalog();return true;}return false;});root.addView(search);search.setVisibility(View.GONE);
        HorizontalScrollView navigation=new HorizontalScrollView(this);tabs=new LinearLayout(this);navigation.addView(tabs);navigation.setHorizontalScrollBarEnabled(false);root.addView(navigation);
        status=Ui.text(this,"Connecting your library…",14,Ui.MUTED);status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(status);retry=Ui.chip(this,"Try again");retry.setVisibility(View.GONE);retry.setOnClickListener(v->{if(repeatRequest!=null)repeatRequest.run();});root.addView(retry);
        FrameLayout body=new FrameLayout(this);detailScroll=new ScrollView(this);content=Ui.column(this);detailScroll.addView(content);body.addView(detailScroll);collection=new androidx.recyclerview.widget.RecyclerView(this);collection.setClipToPadding(false);collection.setItemAnimator(null);body.addView(collection,new FrameLayout.LayoutParams(-1,-1));root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout navigationBar=new LinearLayout(this);navigationBar.setPadding(0,Ui.dp(this,2),0,0);
        for(String label:new String[]{"Home","Search","My Library"}){NavItem tab=new NavItem(this,label,navigationItems.size());navigationItems.add(tab);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(Ui.dp(this,3),0,Ui.dp(this,3),0);navigationBar.addView(tab,lp);tab.setOnClickListener(v->{returnScroll=null;for(NavItem item:navigationItems)item.setActive(item==tab);if(label.equals("Home"))goHome();else if(label.equals("Search"))openSearch();else favorites();});}
        root.addView(navigationBar);setContentView(root);Ui.insets(this,root,16);
        if(saved==null){List<JSONObject> cached=catalog.cachedHome();if(!cached.isEmpty()){renderTitles(cached);status.setText("Refreshing your library…");}}
        loadChannels();
    }
    private void selectNavigation(int index){for(int i=0;i<navigationItems.size();i++)navigationItems.get(i).setActive(i==index);}
    private void hideKeyboard(){((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(search.getWindowToken(),0);search.clearFocus();}
    private void goHome(){hideKeyboard();search.setVisibility(View.GONE);query="";search.setText("");channel="discover";page=1;returnScroll=null;selectNavigation(0);loadCatalog();}
    private void openSearch(){
        generation++;if(pending!=null)pending.cancel(true);screen="search";currentDetail=null;retry.setVisibility(View.GONE);status.setVisibility(View.GONE);selectNavigation(1);
        ((View)tabs.getParent()).setVisibility(View.GONE);search.setVisibility(View.VISIBLE);showContent();content.removeAllViews();
        content.addView(Ui.text(this,"Find your next watch",26,Ui.INK));content.addView(Ui.text(this,"Search films, series and shorts",14,Ui.MUTED));
        search.requestFocus();((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search,android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }
    private void libraryTabs(){
        selectNavigation(2);hideKeyboard();search.setVisibility(View.GONE);((View)tabs.getParent()).setVisibility(View.VISIBLE);tabs.removeAllViews();
        Button saved=Ui.chip(this,"My list");saved.setSelected(screen.equals("favorites"));saved.setOnClickListener(v->favorites());tabs.addView(saved);
        Button watched=Ui.chip(this,"Continue watching");watched.setSelected(screen.equals("history"));watched.setOnClickListener(v->history());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=Ui.dp(this,8);tabs.addView(watched,lp);
    }
    private void backFromTitle(){if(returnScreen.equals("favorites"))favorites();else if(returnScreen.equals("history"))history();else loadCatalog();}
    private void restoreBrowsePosition(){if(returnScroll!=null&&collection.getLayoutManager()!=null){collection.getLayoutManager().onRestoreInstanceState(returnScroll);returnScroll=null;}}
    private interface Work { Object run() throws Exception; }
    private interface Done { void accept(Object result) throws Exception; }
    private void work(String message,Work task,Done done) {
        repeatRequest=()->work(message,task,done);retry.setVisibility(View.GONE);
        int ticket=++generation;if(pending!=null)pending.cancel(true);status.setVisibility(View.VISIBLE);status.setText(message);
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
            String report=message+"\n\nZuno 0.9.0 · Android "+android.os.Build.VERSION.RELEASE+" (API "+android.os.Build.VERSION.SDK_INT+")\n"+android.os.Build.MODEL+" · "+android.text.TextUtils.join(", ",android.os.Build.SUPPORTED_ABIS)+" · page "+pageSize;
            new AlertDialog.Builder(this).setTitle("Playback couldn't start").setMessage(report).setPositiveButton("Close",null).setNeutralButton("Copy details",(dialog,which)->{
                android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Zuno playback diagnostics",report));
                Toast.makeText(this,"Details copied",Toast.LENGTH_SHORT).show();
            }).show();
        }
    }
    private void loadChannels() {
        work("Connecting your library…",()->catalog.channels(),value->{
            JSONArray channels=(JSONArray)value;tabs.removeAllViews();boolean valid=false;for(int i=0;i<channels.length();i++)if(channels.getJSONObject(i).optString("id").equals(channel))valid=true;if(!valid)channel="";
            channelOptions=channels;renderBrowseTabs();
            String restored=restoreScreen;restoreScreen="";
            if(restored.equals("detail") && !restoreId.isEmpty())loadDetail(restoreId,restoreType,selectedProvider,selectedKind);
            else if(restored.equals("favorites"))favorites();
            else if(restored.equals("history"))history();
            else if(restored.equals("search")&&query.isEmpty())openSearch();
            else loadCatalog();
        });
    }
    private void renderBrowseTabs() throws JSONException {
        JSONArray channels=channelOptions;tabs.removeAllViews();
            for(int i=0;i<channels.length();i++) {JSONObject c=channels.getJSONObject(i);String id=c.getString("id");if(channel.isEmpty())channel=id;
                Button b=Ui.chip(this,c.optString("channel_name","Browse"));b.setSelected(id.equals(channel));b.setOnClickListener(v->{for(int n=0;n<tabs.getChildCount();n++)tabs.getChildAt(n).setSelected(tabs.getChildAt(n)==b);channel=id;query="";search.setText("");page=1;loadCatalog();});LinearLayout.LayoutParams chipSpace=new LinearLayout.LayoutParams(-2,-2);chipSpace.setMargins(0,0,Ui.dp(this,8),0);tabs.addView(b,chipSpace);}
    }
    private void loadCatalog() {
        try{renderBrowseTabs();}catch(JSONException ignored){}
        ((View)tabs.getParent()).setVisibility(query.isEmpty()?View.VISIBLE:View.GONE);screen=query.isEmpty()?"catalog":"search";search.setVisibility(query.isEmpty()?View.GONE:View.VISIBLE);selectNavigation(query.isEmpty()?0:1);currentDetail=null;String wantedQuery=query,wantedChannel=channel;int wantedPage=page;
        if(wantedChannel.isEmpty()){loadChannels();return;}
        String key=browseKey();List<JSONObject> cached=browseCache.get(key);
        if(cached!=null&&android.os.SystemClock.elapsedRealtime()-browseTimes.get(key)<120000){generation++;if(pending!=null)pending.cancel(true);retry.setVisibility(View.GONE);content.removeAllViews();renderTitles(cached);String warning=browseWarnings.get(key);status.setText(warning.isEmpty()?(query.isEmpty()?"":"Results for “"+query+"”"):warning);status.setVisibility(status.length()==0?View.GONE:View.VISIBLE);return;}
        work("Loading stories…",()->catalog.titles(wantedChannel,wantedQuery,wantedPage),value->{
            posters.clearPending();content.removeAllViews();
            @SuppressWarnings("unchecked") List<JSONObject> titles=(List<JSONObject>)value;cacheBrowse(key,titles,catalog.warning);
            status.setText(catalog.warning.isEmpty()?(wantedQuery.isEmpty()?"Find your next story":"Results for “"+wantedQuery+"”"):catalog.warning);
            if(titles.isEmpty())content.addView(Ui.text(this,"No titles found.",22,Ui.INK));
            renderTitles(titles);if(!titles.isEmpty()&&catalog.warning.isEmpty()&&wantedQuery.isEmpty())status.setVisibility(View.GONE);
            if(wantedQuery.isEmpty()&&wantedChannel.equals("discover")&&wantedPage==1)expandHome(titles);

        });
    }
    private void renderTitles(List<JSONObject> titles) {
        detailScroll.setVisibility(View.GONE);collection.setVisibility(View.VISIBLE);
        boolean browsing=screen.equals("catalog")||screen.equals("search");
        if(browsing && (channel.isEmpty()||channel.equals("discover")) && query.isEmpty() && page==1 && !titles.isEmpty()){
            collection.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
            collection.setAdapter(new HomeAdapter(this,posters,titles,item->{if(item.has("_resume"))resume(item.optJSONObject("_resume"));else openEdition(item,item.optString("id"));}));restoreBrowsePosition();return;
        }
        CatalogAdapter adapter=new CatalogAdapter(this,posters,titles,browsing&&query.isEmpty()&&page==1,
            item->openEdition(item,item.optString("id")),browsing&&page>1?()->{page--;loadCatalog();}:null,
            browsing&&!titles.isEmpty()?()->{page++;loadCatalog();}:null);
        int columns=Ui.columns(this);
        androidx.recyclerview.widget.GridLayoutManager manager=new androidx.recyclerview.widget.GridLayoutManager(this,columns);
        manager.setSpanSizeLookup(new androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup(){public int getSpanSize(int position){return adapter.fullWidth(position)?columns:1;}});
        collection.setLayoutManager(manager);collection.setAdapter(adapter);if(returnScroll!=null)restoreBrowsePosition();else collection.scrollToPosition(0);
        if(titles.isEmpty())status.setText(screen.equals("favorites")?"Save a title to start your collection.":"No titles found. Try another search.");
    }
    private void showContent(){collection.setVisibility(View.GONE);detailScroll.setVisibility(View.VISIBLE);detailScroll.scrollTo(0,0);}
    private void openEdition(JSONObject item,String id) {
        returnScreen=screen;returnScroll=collection.getVisibility()==View.VISIBLE&&collection.getLayoutManager()!=null?collection.getLayoutManager().onSaveInstanceState():null;
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
        showContent();currentDetail=detail;final String provider=selectedProvider,kind=selectedKind;screen="detail";
        content.removeAllViews();status.setVisibility(View.GONE);search.setVisibility(View.GONE);((View)tabs.getParent()).setVisibility(View.GONE);
        Button back=Ui.quiet(this,"‹  Back");back.setOnClickListener(v->backFromTitle());content.addView(back,new LinearLayout.LayoutParams(-2,Ui.dp(this,48)));
        LinearLayout summary=new LinearLayout(this);summary.setGravity(Gravity.CENTER_VERTICAL);
        ImageView art=new ImageView(this);art.setScaleType(ImageView.ScaleType.CENTER_CROP);art.setContentDescription(null);
        android.graphics.drawable.GradientDrawable frame=new android.graphics.drawable.GradientDrawable();frame.setColor(0xff18212b);frame.setCornerRadius(Ui.dp(this,8));art.setBackground(frame);art.setClipToOutline(true);
        summary.addView(art,new LinearLayout.LayoutParams(Ui.dp(this,108),Ui.dp(this,162)));posters.load(this,art,detail.optString("vod_pic"));
        LinearLayout titleBlock=Ui.column(this);titleBlock.setPadding(Ui.dp(this,16),0,0,0);
        TextView title=Ui.text(this,detail.optString("vod_name","Video"),25,Ui.INK);title.setTypeface(null,android.graphics.Typeface.BOLD);title.setMaxLines(4);title.setEllipsize(android.text.TextUtils.TruncateAt.END);titleBlock.addView(title);
        List<String> metadata=new ArrayList<>();for(String key:new String[]{"vod_year","vod_area","vod_douban_score"}){String value=detail.optString(key);if(!value.isEmpty()&&!value.equals("0"))metadata.add(value);}
        titleBlock.addView(Ui.text(this,android.text.TextUtils.join(" · ",metadata),12,Ui.MUTED));summary.addView(titleBlock,new LinearLayout.LayoutParams(0,-2,1));content.addView(summary);
        JSONArray audio=detail.optJSONArray("audio_type_option");String selected=detail.optString("audio_type","0"),label="Default";
        if(audio!=null)for(int i=0;i<audio.length();i++){JSONObject choice=audio.getJSONObject(i);if(choice.optString("type").equals(selected))label=choice.optString("type_name","Default");}
        final String sourceLabel=label;JSONArray episodes=detail.optJSONArray("vod_collection");
        LinearLayout actions=new LinearLayout(this);actions.setPadding(0,Ui.dp(this,12),0,Ui.dp(this,4));
        if(episodes!=null&&episodes.length()>0){JSONObject first=episodes.getJSONObject(0);Button play=Ui.primary(this,"▶  Play");play.setOnClickListener(v->playEpisode(detail,first,sourceLabel,provider,kind));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,Ui.dp(this,48),1);lp.setMargins(0,0,Ui.dp(this,8),0);actions.addView(play,lp);}
        Button save=Ui.chip(this,favoriteLabel(detail,provider,kind));save.setOnClickListener(v->{toggleFavorite(detail,provider,kind);save.setText(favoriteLabel(detail,provider,kind));});actions.addView(save,new LinearLayout.LayoutParams(0,Ui.dp(this,48),1));content.addView(actions);
        String description=android.text.Html.fromHtml(detail.optString("vod_blurb"),android.text.Html.FROM_HTML_MODE_LEGACY).toString().trim();
        if(!description.isEmpty()){
            TextView synopsis=Ui.text(this,description,14,Ui.MUTED);synopsis.setLineSpacing(Ui.dp(this,3),1);synopsis.setMaxLines(4);synopsis.setEllipsize(android.text.TextUtils.TruncateAt.END);content.addView(synopsis);
            Button more=Ui.quiet(this,"Read more");more.setOnClickListener(v->{boolean expanded=more.getText().equals("Read less");synopsis.setMaxLines(expanded?4:Integer.MAX_VALUE);more.setText(expanded?"Read more":"Read less");});content.addView(more,new LinearLayout.LayoutParams(-2,Ui.dp(this,48)));
        }
        LinearLayout selectors=new LinearLayout(this);
        if(audio!=null&&audio.length()>1){Button variant=Ui.chip(this,sourceLabel+"  ▾");variant.setContentDescription("Playback version: "+sourceLabel);
            String[] names=new String[audio.length()],types=new String[audio.length()];int active=0;
            for(int i=0;i<audio.length();i++){JSONObject choice=audio.getJSONObject(i);names[i]=choice.optString("type_name");types[i]=choice.optString("type");if(types[i].equals(selected))active=i;}
            final int checked=active;variant.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Playback version").setSingleChoiceItems(names,checked,(dialog,index)->{dialog.dismiss();loadDetail(detail.optString("id"),types[index],provider,kind);}).setNegativeButton("Cancel",null).show());selectors.addView(variant);}
        JSONArray seasons=detail.optJSONArray("series_info");
        if(seasons!=null&&seasons.length()>1){List<String> names=new ArrayList<>(),ids=new ArrayList<>();for(int i=0;i<seasons.length();i++){JSONObject season=seasons.getJSONObject(i);if(!season.optString("vod_id").isEmpty()){ids.add(season.optString("vod_id"));names.add(season.optString("series","Season "+(i+1)));}}
            Button season=Ui.chip(this,"Seasons  ▾");season.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("Choose season").setItems(names.toArray(new String[0]),(dialog,index)->loadDetail(ids.get(index),"0",provider,kind)).show());LinearLayout.LayoutParams gap=new LinearLayout.LayoutParams(-2,-2);gap.setMargins(Ui.dp(this,8),0,0,0);selectors.addView(season,gap);}
        HorizontalScrollView choices=new HorizontalScrollView(this);choices.setHorizontalScrollBarEnabled(false);choices.addView(selectors);content.addView(choices);
        if(episodes==null||episodes.length()==0){content.addView(Ui.text(this,"No playable episodes are available for this title.",14,Ui.MUTED));return;}
        if(episodes.length()>1){content.addView(Ui.text(this,"Episodes · "+episodes.length(),18,Ui.INK));appendEpisodes(detail,episodes,0,sourceLabel,provider,kind);}
    }
    private void playEpisode(JSONObject detail,JSONObject episode,String label,String provider,String kind){
        work("Opening episode…",()->CatalogBackend.get(this,provider).playbackJson(detail,episode,label),json->launchPlayer(detail,episode,(String)json,provider,kind));
    }
    private void appendEpisodes(JSONObject detail,JSONArray episodes,int start,String label,String provider,String kind){
        int end=Math.min(start+20,episodes.length());
        for(int i=start;i<end;i++){JSONObject episode=episodes.optJSONObject(i);if(episode==null)continue;
            Button row=Ui.quiet(this,String.format(java.util.Locale.ROOT,"%02d",i+1)+"     "+episode.optString("title","Episode "+(i+1))+"    ›");
            row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);row.setContentDescription("Play "+episode.optString("title","Episode "+(i+1)));row.setOnClickListener(v->playEpisode(detail,episode,label,provider,kind));content.addView(row,new LinearLayout.LayoutParams(-1,Ui.dp(this,52)));
            View divider=new View(this);divider.setBackgroundColor(0xff1c2631);content.addView(divider,new LinearLayout.LayoutParams(-1,Ui.dp(this,1)));
        }
        if(end<episodes.length()){Button more=Ui.quiet(this,"More episodes  ↓");more.setOnClickListener(v->{content.removeView(more);appendEpisodes(detail,episodes,end,label,provider,kind);});content.addView(more,new LinearLayout.LayoutParams(-1,Ui.dp(this,48)));}
    }
    private void launchPlayer(JSONObject detail,JSONObject episode,String json,String provider,String kind) throws Exception {
        status.setVisibility(View.GONE);
        startActivity(new Intent(this,PlayerActivity.class).putExtra("playback",json).putExtra("audioType",detail.optString("audio_type","0"))
            .putExtra("kind",kind).putExtra("provider",provider).putExtra("vodId",detail.optString("id")).putExtra("collection",EpisodeSelection.key(episode))
            .putExtra("catalogTitle",detail.optString("vod_name","Video")).putExtra("poster",detail.optString("vod_pic"))
            .putExtra("variants",detail.optJSONArray("audio_type_option")==null?"[]":detail.optJSONArray("audio_type_option").toString()));
    }
    private String favoriteLabel(JSONObject detail,String provider,String kind) {
        return getSharedPreferences("favorites",MODE_PRIVATE).contains(provider+":"+kind+":"+detail.optString("id"))?"✓  Saved":"+  My list";
    }
    private void toggleFavorite(JSONObject detail,String provider,String kind) {
        try {String id=provider+":"+kind+":"+detail.optString("id");android.content.SharedPreferences prefs=getSharedPreferences("favorites",MODE_PRIVATE);
            if(prefs.contains(id)){prefs.edit().remove(id).apply();Toast.makeText(this,"Removed from My Library",Toast.LENGTH_SHORT).show();}
            else {JSONObject record=new JSONObject();for(String key:new String[]{"id","vod_name","vod_pic","vod_year","vod_area","vod_douban_score"})record.put(key,detail.optString(key));record.put("_provider",provider).put("_kind",kind);prefs.edit().putString(id,record.toString()).apply();Toast.makeText(this,"Saved to My Library",Toast.LENGTH_SHORT).show();}
        }catch(JSONException ignored){status.setText("Could not save this title.");}
    }
    private void favorites() {
        status.setVisibility(View.VISIBLE);((View)tabs.getParent()).setVisibility(View.VISIBLE);
        retry.setVisibility(View.GONE);generation++;if(pending!=null)pending.cancel(true);currentDetail=null;screen="favorites";posters.clearPending();content.removeAllViews();status.setText("My Library");
        List<JSONObject> saved=new ArrayList<>();for(Object value:getSharedPreferences("favorites",MODE_PRIVATE).getAll().values())try{saved.add(new JSONObject((String)value));}catch(Exception ignored){}
        libraryTabs();if(saved.isEmpty()){emptyState("Your next watch starts here","Tap + on any poster to build a collection for later.");}else renderTitles(saved);
    }
    private void emptyState(String title,String copy){showContent();content.removeAllViews();status.setVisibility(View.GONE);LinearLayout state=Ui.column(this);state.setPadding(Ui.dp(this,12),Ui.dp(this,44),Ui.dp(this,12),Ui.dp(this,28));TextView symbol=Ui.text(this,"◇",56,Ui.ACCENT);state.addView(symbol);TextView heading=Ui.text(this,title,27,Ui.INK);heading.setTypeface(null,android.graphics.Typeface.BOLD);state.addView(heading);TextView description=Ui.text(this,copy,15,Ui.MUTED);description.setLineSpacing(Ui.dp(this,4),1);state.addView(description);Button browse=Ui.primary(this,"Find your next watch");browse.setOnClickListener(v->goHome());LinearLayout.LayoutParams action=new LinearLayout.LayoutParams(-2,Ui.dp(this,48));action.topMargin=Ui.dp(this,20);state.addView(browse,action);content.addView(state);}
    private void history() {
        status.setVisibility(View.VISIBLE);((View)tabs.getParent()).setVisibility(View.VISIBLE);
        showContent();
        retry.setVisibility(View.GONE);generation++;if(pending!=null)pending.cancel(true);currentDetail=null;screen="history";posters.clearPending();content.removeAllViews();status.setText("Continue watching");libraryTabs();
        List<JSONObject> records=WatchHistory.list(this);
        if(records.isEmpty()){emptyState("Pick up where you left off","Start a film or series. Your unfinished stories will be waiting here.");return;}
        for(JSONObject record:records) {
            LinearLayout card=Ui.card(this);LinearLayout.LayoutParams place=new LinearLayout.LayoutParams(-1,-2);place.setMargins(0,0,0,Ui.dp(this,12));content.addView(card,place);
            LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);ImageView poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);android.graphics.drawable.GradientDrawable outline=new android.graphics.drawable.GradientDrawable();outline.setColor(Ui.SURFACE);outline.setCornerRadius(Ui.dp(this,8));poster.setBackground(outline);poster.setClipToOutline(true);row.addView(poster,new LinearLayout.LayoutParams(Ui.dp(this,76),Ui.dp(this,114)));posters.load(this,poster,record.optString("poster"));LinearLayout copy=Ui.column(this);copy.setBackgroundColor(android.graphics.Color.TRANSPARENT);copy.setPadding(Ui.dp(this,14),0,0,0);TextView title=Ui.text(this,record.optString("title","Video"),17,Ui.INK);title.setMaxLines(3);title.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(title);
            long position=record.optLong("position"),duration=record.optLong("duration");copy.addView(Ui.text(this,(position/60000)+" min watched",12,Ui.MUTED));
            if(duration>0){ProgressBar progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));progress.setProgress((int)Math.min(1000,(double)position/duration*1000));copy.addView(progress);}
            Button resume=Ui.quiet(this,"▷  Continue watching");resume.setOnClickListener(v->resume(record));copy.addView(resume,new LinearLayout.LayoutParams(-2,Ui.dp(this,48)));row.addView(copy,new LinearLayout.LayoutParams(0,-2,1));card.addView(row);
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
    private void expandHome(List<JSONObject> first){
        final int ticket=generation;pending=io.submit(()->{
            LinkedHashMap<String,JSONObject> all=new LinkedHashMap<>();for(JSONObject item:first)all.put(LibraryStore.key(item),item);boolean partial=false;
            for(int next=2;next<=3;next++){if(Thread.currentThread().isInterrupted())return;try{for(JSONObject item:catalog.titles("discover","",next))all.put(LibraryStore.key(item),item);if(!catalog.warning.isEmpty())partial=true;}catch(Exception e){partial=true;}}
            final boolean incomplete=partial;List<JSONObject> expanded=new ArrayList<>(all.values());runOnUiThread(()->{if(isDestroyed()||isFinishing()||ticket!=generation||!screen.equals("catalog"))return;
                cacheBrowse("discover|1|",expanded,incomplete?"Some discovery rows could not refresh. Available titles are shown.":catalog.warning);
                android.os.Parcelable position=collection.getLayoutManager()==null?null:collection.getLayoutManager().onSaveInstanceState();renderTitles(expanded);if(position!=null)collection.getLayoutManager().onRestoreInstanceState(position);
                if(incomplete){status.setText("Some discovery rows could not refresh. Available titles are shown.");status.setVisibility(View.VISIBLE);}
            });
        });
    }
    private void options(){startActivityForResult(new Intent(this,SettingsActivity.class),50);}
    @Override protected void onResume(){super.onResume();if(root!=null&&(appearanceTheme!=getSharedPreferences("settings",0).getInt("theme",0)||appearanceCompact!=getSharedPreferences("settings",0).getBoolean("compactPosters",false)))recreate();}
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);if(request==50){if(result==RESULT_OK){if(screen.equals("favorites"))favorites();else if(screen.equals("history"))history();else if(screen.equals("catalog")||screen.equals("search"))loadCatalog();}return;}if(result!=RESULT_OK||data==null||data.getData()==null)return;
        android.net.Uri uri=data.getData();
        if(request!=10)return;
        work("Opening playback file…",()->{try(InputStream in=getContentResolver().openInputStream(uri)){if(in==null)throw new IOException();String json=CatalogBackend.read(in,65536);PlaybackJson.parse(json);return json;}},value->{status.setText("Ready");startActivity(new Intent(this,PlayerActivity.class).putExtra("playback",(String)value));});
    }
    @Override public void onBackPressed() {if(screen.equals("detail"))backFromTitle();else if(!screen.equals("catalog"))goHome();else super.onBackPressed();}
    @Override protected void onSaveInstanceState(Bundle state) {state.putString("channel",channel);state.putString("query",query);state.putInt("page",page);
        state.putString("returnScreen",returnScreen);state.putParcelable("returnScroll",returnScroll);state.putString("screen",screen);state.putString("provider",selectedProvider);state.putString("kind",selectedKind);
        if(currentDetail!=null){state.putString("detailId",currentDetail.optString("id"));state.putString("audioType",currentDetail.optString("audio_type","0"));}
        super.onSaveInstanceState(state);}
    @Override protected void onDestroy() {generation++;if(pending!=null)pending.cancel(true);posters.close();catalog.close();io.shutdownNow();super.onDestroy();}
}
