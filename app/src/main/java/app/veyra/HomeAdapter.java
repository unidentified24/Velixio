package app.veyra;
import android.app.Activity;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import org.json.JSONObject;
import java.util.*;
/** Metadata-based discovery; no fabricated popularity or personalization claims. */
final class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.Holder> {
 private final Activity owner;private final PosterLoader posters;private final CatalogAdapter.Open open;
 private CatalogAdapter spotlight;private final List<JSONObject> featured=new ArrayList<>();private int featuredIndex;
 private final Set<String> discoveryShown=new HashSet<>();
 private final List<List<JSONObject>> rows=new ArrayList<>();private final List<String> headings=new ArrayList<>();private final RecyclerView.RecycledViewPool pool=new RecyclerView.RecycledViewPool();
 HomeAdapter(Activity a,PosterLoader p,List<JSONObject> titles,CatalogAdapter.Open o){owner=a;posters=p;open=o;
  long seed=a.getSharedPreferences("discovery",0).getLong("visit",0);List<JSONObject> shuffled=new ArrayList<>(titles);Collections.shuffle(shuffled,new Random(seed));
  List<JSONObject> rated=select(shuffled,t->score(t)>=7),recent=select(shuffled,t->t.optInt("vod_year")>=Calendar.getInstance().get(Calendar.YEAR)-2),classics=select(shuffled,t->t.optInt("vod_year")>0&&t.optInt("vod_year")<2010&&score(t)>=6),unseen=select(shuffled,t->!LibraryStore.saved(a,t));
  featured.addAll(select(shuffled,t->score(t)>=7&&!t.optString("vod_pic").isEmpty()));if(featured.isEmpty())featured.addAll(shuffled);if(featured.size()>12)featured.subList(12,featured.size()).clear();
  // Persist the last feature so refreshes avoid repeating it when alternatives exist.
  String last=a.getSharedPreferences("discovery",0).getString("lastFeature","");if(featured.size()>1&&LibraryStore.key(featured.get(0)).equals(last))Collections.rotate(featured,-1);setSpotlight();if(!featured.isEmpty())discoveryShown.add(LibraryStore.key(featured.get(featuredIndex)));
  List<JSONObject> watching=new ArrayList<>();for(JSONObject record:app.veyra.data.WatchHistory.list(a))try{watching.add(new JSONObject().put("id",record.optString("id")).put("_provider",record.optString("provider")).put("_kind",record.optString("kind")).put("vod_name",record.optString("title")).put("vod_pic",record.optString("poster")).put("_resume",record));}catch(Exception ignored){}
  add("Continue watching",watching);add("My list",LibraryStore.list(a));add("Top rated picks",rated);add("From the last two years",recent);add("Worth revisiting",classics);add("Outside your list",unseen);add("Explore the collection",shuffled);add("Discover something different",shuffled);add("Keep exploring",shuffled);
 }
 private interface Match {boolean test(JSONObject t);}
 private static List<JSONObject> select(List<JSONObject> list,Match match){List<JSONObject> out=new ArrayList<>();for(JSONObject t:list)if(match.test(t))out.add(t);return out;}
 private static double score(JSONObject t){double value=t.optDouble("vod_douban_score",0);return Double.isFinite(value)&&value<=10?value:0;}
 private void setSpotlight(){if(featured.isEmpty())return;JSONObject title=featured.get(featuredIndex);spotlight=new CatalogAdapter(owner,posters,Collections.singletonList(title),true,open,null,null);owner.getSharedPreferences("discovery",0).edit().putString("lastFeature",LibraryStore.key(title)).apply();}
 private void add(String label,List<JSONObject> items){boolean personal=label.equals("My list")||label.equals("Continue watching");List<JSONObject> selected=new ArrayList<>();for(JSONObject item:items){String identity=LibraryStore.key(item);if(!personal&&discoveryShown.contains(identity))continue;selected.add(item);if(!personal)discoveryShown.add(identity);if(selected.size()>=12)break;}if(selected.isEmpty())return;headings.add(label);rows.add(selected);}
 static final class Holder extends RecyclerView.ViewHolder{CatalogAdapter.Holder hero;TextView heading;RecyclerView rail;Holder(View v){super(v);}}
 public int getItemCount(){return rows.size()+1;}public int getItemViewType(int pos){return pos==0?1:0;}
 public Holder onCreateViewHolder(ViewGroup parent,int type){if(type==1){LinearLayout block=Ui.column(owner);Holder h=new Holder(block);LinearLayout heading=new LinearLayout(owner);TextView label=Ui.text(owner,"Tonight on Zuno",20,Ui.INK);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button next=Ui.quiet(owner,"Next pick  ›");next.setContentDescription("Show another featured title");next.setOnClickListener(v->{if(featured.size()>1){featuredIndex=(featuredIndex+1)%featured.size();setSpotlight();notifyItemChanged(0);}});heading.addView(next);block.addView(heading);h.hero=spotlight.onCreateViewHolder(parent,1);block.addView(h.hero.itemView);return h;}
  LinearLayout section=Ui.column(owner);section.setPadding(0,Ui.dp(owner,14),0,0);Holder h=new Holder(section);h.heading=Ui.text(owner,"",19,Ui.INK);h.heading.setTypeface(null,android.graphics.Typeface.BOLD);section.addView(h.heading);h.rail=new RecyclerView(owner);h.rail.setLayoutManager(new LinearLayoutManager(owner,LinearLayoutManager.HORIZONTAL,false));h.rail.setRecycledViewPool(pool);h.rail.setItemAnimator(null);h.rail.setNestedScrollingEnabled(false);h.rail.setClipToPadding(false);section.addView(h.rail,new LinearLayout.LayoutParams(-1,Ui.dp(owner,(int)(Ui.posterWidth(owner)*1.5)+42)+Math.round(44*owner.getResources().getDisplayMetrics().scaledDensity)));section.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));return h;
 }
 public void onBindViewHolder(Holder h,int pos){if(pos==0){spotlight.onBindViewHolder(h.hero,0);return;}h.heading.setText(headings.get(pos-1));h.rail.setAdapter(new CatalogAdapter(owner,posters,rows.get(pos-1),false,open,null,null,true));}
 public void onViewRecycled(Holder h){if(h.hero!=null)spotlight.onViewRecycled(h.hero);if(h.rail!=null)h.rail.setAdapter(null);}
}
