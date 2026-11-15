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
 private final Map<String,android.os.Parcelable> railPositions=new HashMap<>();
 private final Map<String,CatalogAdapter> railAdapters=new HashMap<>();
 private final Set<String> discoveryShown=new HashSet<>();
 private final List<List<JSONObject>> rows=new ArrayList<>();private final List<String> headings=new ArrayList<>();private final RecyclerView.RecycledViewPool pool=new RecyclerView.RecycledViewPool();
 HomeAdapter(Activity a,PosterLoader p,List<JSONObject> titles,CatalogAdapter.Open o){owner=a;posters=p;open=o;
  int year=Calendar.getInstance().get(Calendar.YEAR);
  List<JSONObject> curated=new ArrayList<>();
  for(JSONObject item:titles)if(homeCandidate(item,year)&&!PersonalStore.hidden(a,item)
      &&!ProfileStore.prefs(a,"hidden-titles").getBoolean(LibraryStore.key(item),false))curated.add(item);
  DiscoveryPreferences.Model taste=new DiscoveryPreferences.Model(a);
  curated.sort(Comparator.comparingDouble(HomeAdapter::score).reversed()
      .thenComparing(Comparator.comparingDouble((JSONObject t)->taste.score(t)).reversed())
      .thenComparing(Comparator.comparingInt(CatalogDates::releaseYear).reversed())
      .thenComparing(t->t.optString("vod_name"),String.CASE_INSENSITIVE_ORDER));
  List<JSONObject> rated=select(curated,t->score(t)>=7);
  List<JSONObject> recent=select(curated,t->CatalogDates.releaseYear(t)>=year-1);
  List<JSONObject> classics=select(curated,t->CatalogDates.releaseYear(t)<2010&&score(t)>=7);
  recent.sort(Comparator.comparingInt(CatalogDates::releaseYear).reversed());
  List<JSONObject> added=select(curated,t->CatalogDates.added(t)!=null && CatalogDates.added(t).compareTo(CatalogDates.today())<=0);added.sort(Comparator.comparing(CatalogDates::added).reversed());
  featured.addAll(select(rated,t->!t.optString("vod_pic").isEmpty()));
  if(featured.isEmpty())featured.addAll(select(curated,t->!t.optString("vod_pic").isEmpty()));
  if(featured.size()>12)featured.subList(12,featured.size()).clear();
  if(!HomeLayout.visible(owner,"Featured"))featured.clear();
  setSpotlight();
  if(!featured.isEmpty())discoveryShown.add(app.veyra.data.CombinedCatalog.indexKey(featured.get(featuredIndex)));
  List<JSONObject> watching=new ArrayList<>();for(JSONObject record:app.veyra.data.WatchHistory.list(a))try{watching.add(new JSONObject().put("id",record.optString("id")).put("_provider",record.optString("provider")).put("_kind",record.optString("kind")).put("vod_name",record.optString("title")).put("vod_pic",record.optString("poster")).put("_resume",record));}catch(Exception ignored){}
  add("Continue watching",watching);add("My list",LibraryStore.list(a));add("Highly rated",rated);
  add("New Releases",recent);add("Recently Added",added);add("For your taste",select(curated,t->taste.score(t)>0));
  add("Worth revisiting",classics);add("Explore the collection",curated);
  List<String> order=HomeLayout.order(owner);List<String> originalNames=new ArrayList<>(headings);List<List<JSONObject>> originalRows=new ArrayList<>(rows);headings.clear();rows.clear();for(String name:order){int index=originalNames.indexOf(name);if(index>=0){headings.add(name);rows.add(originalRows.get(index));}}
 }
 static boolean homeCandidate(JSONObject title,int year){
  String name=title.optString("vod_name").trim();
  String kind=title.optString("_kind","movie");
  String category=SearchFilters.genres(title);
  int release=CatalogDates.releaseYear(title);
  String date=CatalogDates.date(title,"release_date");
  if(release<1888||release>year||date!=null&&date.compareTo(CatalogDates.today())>0)return false;
  if(name.isEmpty()||name.toLowerCase(Locale.ROOT).contains("baddies")||!kind.equals("movie")
      ||category.matches(".*\\b(reality|variety|sports?|news|talk show|live tv)\\b.*"))return false;
  String quality=app.veyra.data.CombinedCatalog.quality(title);
  if(SearchFilters.isCam(quality)||name.matches("(?i).*\\((?:HD[- ]?)?(?:CAM|TC|TS)\\).*"))
      return SearchFilters.matchesVersion(title,"",true);
  return score(title)>=6.5||release>=year-1&&!title.optString("vod_pic").isEmpty()&&(score(title)==0);
 }
 private interface Match {boolean test(JSONObject t);}
 private static List<JSONObject> select(List<JSONObject> list,Match match){List<JSONObject> out=new ArrayList<>();for(JSONObject t:list)if(match.test(t))out.add(t);return out;}
 private static double score(JSONObject t){double value=t.optDouble("vod_douban_score",0);return Double.isFinite(value)&&value<=10?value:0;}
 private void setSpotlight(){if(featured.isEmpty())return;JSONObject title=featured.get(featuredIndex);spotlight=new CatalogAdapter(owner,posters,Collections.singletonList(title),true,open,null,null);}
 private void add(String label,List<JSONObject> items){if(!HomeLayout.visible(owner,label))return;boolean personal=label.equals("My list")||label.equals("Continue watching");List<JSONObject> selected=new ArrayList<>();for(JSONObject item:items){String identity=app.veyra.data.CombinedCatalog.indexKey(item);if(!personal&&!label.equals("New Releases")&&!label.equals("Recently Added")&&discoveryShown.contains(identity))continue;selected.add(item);if(!personal)discoveryShown.add(identity);if(selected.size()>=8)break;}if(selected.isEmpty())return;headings.add(label);rows.add(selected);}
 static final class Holder extends RecyclerView.ViewHolder{CatalogAdapter.Holder hero;TextView heading;RecyclerView rail;String row;Holder(View v){super(v);}}
 public int getItemCount(){return Math.max(1,rows.size()+(featured.isEmpty()?0:1));}public int getItemViewType(int pos){return rows.isEmpty()&&featured.isEmpty()?2:pos==0&&!featured.isEmpty()?1:0;}
 public Holder onCreateViewHolder(ViewGroup parent,int type){if(type==2){TextView empty=Ui.text(owner,"No recommendations match your preferences. Browse Films or Series to explore the catalog.",18,Ui.MUTED);return new Holder(empty);}if(type==1){LinearLayout block=Ui.column(owner);Holder h=new Holder(block);LinearLayout heading=new LinearLayout(owner);heading.setGravity(Gravity.CENTER_VERTICAL);TextView label=Ui.text(owner,"Featured",25,Ui.INK);Ui.display(label);heading.addView(label,new LinearLayout.LayoutParams(0,-2,1));Button next=Ui.icon(owner,"updates","Show another featured title");next.setContentDescription("Show another featured title");next.setOnClickListener(v->{if(featured.size()>1){featuredIndex=(featuredIndex+1)%featured.size();setSpotlight();notifyItemChanged(0);}});heading.addView(next,new LinearLayout.LayoutParams(Ui.dp(owner,48),Ui.dp(owner,48)));block.addView(heading);h.hero=spotlight.onCreateViewHolder(parent,1);block.addView(h.hero.itemView);return h;}
  LinearLayout section=Ui.column(owner);section.setPadding(0,Ui.dp(owner,Ui.compact(owner)?14:26),0,0);Holder h=new Holder(section);h.heading=Ui.text(owner,"",21,Ui.INK);Ui.display(h.heading);section.addView(h.heading);h.rail=new RecyclerView(owner);h.rail.setLayoutManager(new LinearLayoutManager(owner,LinearLayoutManager.HORIZONTAL,false));h.rail.setRecycledViewPool(pool);h.rail.setItemAnimator(null);h.rail.setNestedScrollingEnabled(false);h.rail.setClipToPadding(false);section.addView(h.rail,new LinearLayout.LayoutParams(-1,Ui.dp(owner,(int)(Ui.posterWidth(owner)*1.5)+42)+Math.round(44*owner.getResources().getDisplayMetrics().scaledDensity)));section.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));return h;
 }
 public void onBindViewHolder(Holder h,int pos){if(rows.isEmpty()&&featured.isEmpty())return;if(pos==0&&!featured.isEmpty()){spotlight.onBindViewHolder(h.hero,0);return;}int row=pos-(featured.isEmpty()?0:1);h.row=headings.get(row);h.heading.setText(h.row);CatalogAdapter adapter=railAdapters.get(h.row);if(adapter==null){adapter=new CatalogAdapter(owner,posters,rows.get(row),false,open,null,null,true);adapter.presentation=h.row.equals("Continue watching")?5:h.row.equals("New Releases")||h.row.equals("Recently Added")?3:h.row.equals("Highly rated")?4:0;railAdapters.put(h.row,adapter);}h.rail.getLayoutParams().height=Ui.dp(owner,adapter.presentation==3?Math.round(Ui.landscapePosterWidth(owner)*9f/16)+36:(int)(Ui.posterWidth(owner)*1.5)+(adapter.presentation==5?58:42))+Math.round(44*owner.getResources().getDisplayMetrics().scaledDensity);h.rail.setAdapter(adapter);if(railPositions.containsKey(h.row))h.rail.getLayoutManager().onRestoreInstanceState(railPositions.get(h.row));else h.rail.scrollToPosition(0);}
 public void onViewRecycled(Holder h){if(h.hero!=null)spotlight.onViewRecycled(h.hero);if(h.rail!=null){if(h.row!=null)railPositions.put(h.row,h.rail.getLayoutManager().onSaveInstanceState());h.rail.setAdapter(null);}}
}
