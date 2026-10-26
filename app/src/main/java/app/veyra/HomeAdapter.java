package app.veyra;

import android.app.Activity;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.*;
import org.json.JSONObject;
import java.util.*;

/** Discovery rows use real catalog metadata, without inventing personalized or popularity signals. */
final class HomeAdapter extends RecyclerView.Adapter<HomeAdapter.Holder> {
    private final Activity owner;
    private final PosterLoader posters;
    private final CatalogAdapter.Open open;
    private final CatalogAdapter spotlight;
    private final List<List<JSONObject>> rows=new ArrayList<>();
    private final List<String> headings=new ArrayList<>();
    private final RecyclerView.RecycledViewPool pool=new RecyclerView.RecycledViewPool();
    HomeAdapter(Activity owner,PosterLoader posters,List<JSONObject> titles,CatalogAdapter.Open open){
        this.owner=owner;this.posters=posters;this.open=open;
        spotlight=new CatalogAdapter(owner,posters,titles,true,open,null,null);
        List<JSONObject> rated=new ArrayList<>(),recent=new ArrayList<>();
        for(JSONObject title:titles){double score=title.optDouble("vod_douban_score",0);if(score>=7&&score<=10)rated.add(title);if(title.optInt("vod_year",0)>0)recent.add(title);}
        rated.sort(Comparator.comparingDouble((JSONObject o)->o.optDouble("vod_douban_score",0)).reversed());
        recent.sort(Comparator.comparingInt((JSONObject o)->o.optInt("vod_year",0)).reversed());
        if(!rated.isEmpty())add("Highly rated",rated);
        if(!recent.isEmpty())add("Recent releases",recent);
        add("More to discover",titles);
    }
    private void add(String title,List<JSONObject> items){headings.add(title);rows.add(new ArrayList<>(items.subList(0,Math.min(30,items.size()))));}
    static final class Holder extends RecyclerView.ViewHolder {
        CatalogAdapter.Holder hero;TextView heading;RecyclerView rail;
        Holder(View view){super(view);}
    }
    public int getItemCount(){return rows.size()+1;}
    public int getItemViewType(int position){return position==0?1:0;}
    public Holder onCreateViewHolder(ViewGroup parent,int type){
        if(type==1){CatalogAdapter.Holder hero=spotlight.onCreateViewHolder(parent,1);Holder h=new Holder(hero.itemView);h.hero=hero;return h;}
        LinearLayout section=Ui.column(owner);Holder h=new Holder(section);h.heading=Ui.text(owner,"",18,Ui.INK);h.heading.setTypeface(null,android.graphics.Typeface.BOLD);section.addView(h.heading);
        h.rail=new RecyclerView(owner);h.rail.setLayoutManager(new LinearLayoutManager(owner,LinearLayoutManager.HORIZONTAL,false));h.rail.setRecycledViewPool(pool);h.rail.setItemAnimator(null);h.rail.setNestedScrollingEnabled(false);h.rail.setClipToPadding(false);
        section.addView(h.rail,new LinearLayout.LayoutParams(-1,Ui.dp(owner,224)+Math.round(32*owner.getResources().getDisplayMetrics().scaledDensity)));section.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));return h;
    }
    public void onBindViewHolder(Holder h,int position){
        if(position==0){spotlight.onBindViewHolder(h.hero,0);return;}
        h.heading.setText(headings.get(position-1));h.rail.setAdapter(new CatalogAdapter(owner,posters,rows.get(position-1),false,open,null,null,true));
    }
    public void onViewRecycled(Holder h){if(h.hero!=null)spotlight.onViewRecycled(h.hero);if(h.rail!=null)h.rail.setAdapter(null);}
}
