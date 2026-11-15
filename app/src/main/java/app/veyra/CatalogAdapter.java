package app.veyra;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONObject;
import java.util.*;

/** Only visible artwork cards are inflated; title metadata remains available without images. */
final class CatalogAdapter extends RecyclerView.Adapter<CatalogAdapter.Holder> {
    interface Open { void open(JSONObject title); }
    private final Activity owner;
    private final PosterLoader posters;
    private final List<JSONObject> titles;
    private final Open open;
    private final boolean hero;
    private final boolean rail;int presentation;
    private final Runnable previous,next;
    CatalogAdapter(Activity owner,PosterLoader posters,List<JSONObject> titles,boolean hero,Open open,Runnable previous,Runnable next) {
        this(owner,posters,titles,hero,open,previous,next,false);
    }
    CatalogAdapter(Activity owner,PosterLoader posters,List<JSONObject> titles,boolean hero,Open open,Runnable previous,Runnable next,boolean rail) {
        this.owner=owner;this.posters=posters;this.titles=new ArrayList<>(titles);this.hero=hero&&!titles.isEmpty();this.open=open;this.previous=previous;this.next=next;this.rail=rail;
    }
    public int getItemCount(){return titles.size()+(hero?1:0)+(rail?0:1);}
    public int getItemViewType(int position){return hero&&position==0?1:!rail&&position==getItemCount()-1?2:rail?presentation:0;}
    boolean fullWidth(int position){return getItemViewType(position)!=0;}
    static final class Holder extends RecyclerView.ViewHolder {
        ImageView image;TextView title,meta,badge;Button play,save,resume;ProgressBar progress;
        Holder(View view){super(view);}
    }
    public Holder onCreateViewHolder(ViewGroup parent,int type) {
        int gap=Ui.dp(owner,Ui.compact(owner)?3:6);
        if(type==2){LinearLayout row=new LinearLayout(owner);row.setGravity(Gravity.CENTER);row.setPadding(0,Ui.dp(owner,20),0,Ui.dp(owner,24));
            if(previous!=null){Button b=Ui.chip(owner,"Previous");b.setOnClickListener(v->previous.run());row.addView(b);}
            if(next!=null){Button b=Ui.chip(owner,owner instanceof MainActivity&&((MainActivity)owner).isSearching()?"Load more results":"Explore more");b.setOnClickListener(v->next.run());row.addView(b);}
            return new Holder(row);
        }
        if(type==1){
            FrameLayout frame=new FrameLayout(owner);int height=Math.min(480,Math.max(320,owner.getResources().getConfiguration().screenHeightDp-265));frame.setLayoutParams(new RecyclerView.LayoutParams(-1,Ui.dp(owner,height)));
            GradientDrawable outline=new GradientDrawable();outline.setColor(Ui.SURFACE);outline.setCornerRadius(Ui.dp(owner,24));frame.setBackground(outline);frame.setClipToOutline(true);
            Holder h=new Holder(frame);h.image=new ImageView(owner);h.image.setScaleType(ImageView.ScaleType.CENTER_CROP);frame.addView(h.image,new FrameLayout.LayoutParams(-1,-1));
            View shade=new View(owner);shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0x00000000,0x66000000,0xf5000000}));frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));
            LinearLayout copy=Ui.column(owner);copy.setBackgroundColor(Color.TRANSPARENT);copy.setPadding(Ui.dp(owner,22),Ui.dp(owner,20),Ui.dp(owner,22),Ui.dp(owner,22));
            h.title=Ui.text(owner,"",38,Ui.INK);Ui.display(h.title);h.title.setMaxLines(3);h.title.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(h.title);h.meta=Ui.text(owner,"",12,0xffd9dce4);copy.addView(h.meta);
            LinearLayout actions=new LinearLayout(owner);h.play=Ui.primary(owner,"View details");Ui.buttonGlyph(owner,h.play,"forward",Ui.BG);LinearLayout.LayoutParams action=new LinearLayout.LayoutParams(0,Ui.dp(owner,52),1);action.rightMargin=Ui.dp(owner,10);actions.addView(h.play,action);h.save=Ui.chip(owner,"My list");actions.addView(h.save,new LinearLayout.LayoutParams(-2,Ui.dp(owner,52)));copy.addView(actions);frame.addView(copy,new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));return h;
        }
        LinearLayout card=Ui.column(owner);card.setPadding(gap,gap,gap,Ui.dp(owner,Ui.compact(owner)?4:8));Holder h=new Holder(card);
        h.image=new ImageView(owner);h.image.setBackgroundColor(Color.rgb(23,31,41));h.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable rounded=new GradientDrawable();rounded.setColor(Color.rgb(23,31,41));rounded.setCornerRadius(Ui.dp(owner,14));h.image.setBackground(rounded);h.image.setClipToOutline(true);
        int columns=Ui.columns(owner);
        int cardWidth=rail?Ui.dp(owner,type==3?Ui.landscapePosterWidth(owner):Ui.posterWidth(owner))
            :Ui.dp(owner,owner.getResources().getConfiguration().screenWidthDp-32)/columns;
        int posterWidth=Math.max(1,cardWidth-2*gap);
        int height=Math.round(posterWidth*(type==3?9f/16:1.5f));
        FrameLayout artwork=new FrameLayout(owner);artwork.addView(h.image,new FrameLayout.LayoutParams(-1,-1));h.save=Ui.quiet(owner,"");h.save.setPadding(0,0,0,0);FrameLayout.LayoutParams savePos=new FrameLayout.LayoutParams(Ui.dp(owner,48),Ui.dp(owner,48),Gravity.TOP|Gravity.END);artwork.addView(h.save,savePos);if(type==4){h.badge=Ui.text(owner,"",42,Ui.INK);Ui.display(h.badge);h.badge.setShadowLayer(6,1,1,Color.BLACK);FrameLayout.LayoutParams badgePos=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.START);badgePos.leftMargin=Ui.dp(owner,8);artwork.addView(h.badge,badgePos);}if(type==5){h.image.setScaleType(ImageView.ScaleType.FIT_CENTER);h.resume=Ui.playerButton(owner,"play","Resume watching",false);FrameLayout.LayoutParams resumePos=new FrameLayout.LayoutParams(Ui.dp(owner,48),Ui.dp(owner,48),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);resumePos.bottomMargin=Ui.dp(owner,10);artwork.addView(h.resume,resumePos);}card.addView(artwork,new LinearLayout.LayoutParams(-1,height));if(type==5){h.progress=new ProgressBar(owner,null,android.R.attr.progressBarStyleHorizontal);h.progress.setMax(1000);h.progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT));LinearLayout.LayoutParams progressSpace=new LinearLayout.LayoutParams(-1,Ui.dp(owner,3));progressSpace.topMargin=Ui.dp(owner,6);card.addView(h.progress,progressSpace);}
        h.title=Ui.text(owner,"",13,Ui.INK);Ui.emphasis(h.title);h.title.setMaxLines(2);h.title.setMinLines(1);h.title.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(h.title);
        h.meta=Ui.text(owner,"",11,Ui.MUTED);h.meta.setPadding(0,0,0,0);h.meta.setMaxLines(2);h.meta.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(h.meta);
        card.setLayoutParams(new RecyclerView.LayoutParams(rail?Ui.dp(owner,type==3?Ui.landscapePosterWidth(owner):Ui.posterWidth(owner)):-1,-2));return h;
    }
    public void onBindViewHolder(Holder h,int position) {
        if(getItemViewType(position)==2)return;
        JSONObject item=titles.get(hero?Math.max(0,position-1):position);
        h.title.setText(item.optString("vod_name","Untitled"));if(h.badge!=null)h.badge.setText(String.valueOf(position+1));
        String year=item.optString("vod_year"),score=item.optString("vod_douban_score");
        List<String> metadata=new ArrayList<>();
        if(!year.isEmpty()&&!year.equals("0"))metadata.add(year);
        String language=app.veyra.data.CombinedCatalog.language(item),quality=app.veyra.data.CombinedCatalog.quality(item);
        if(!language.isEmpty())metadata.add(language);
        if(!quality.isEmpty())metadata.add(quality);
        if(metadata.size()<2&&!score.isEmpty()&&!score.equals("0"))metadata.add(score);
        h.meta.setText(android.text.TextUtils.join(" · ",metadata));
        if(h.progress!=null){JSONObject record=item.optJSONObject("_resume");long watched=record==null?0:Math.max(0,record.optLong("position")),duration=record==null?0:record.optLong("duration");h.progress.setProgress(duration>0?(int)Math.min(1000,watched*1000.0/duration):0);h.progress.setVisibility(duration>0?View.VISIBLE:View.INVISIBLE);h.meta.setText(duration>watched?Math.max(1,(long)Math.ceil((duration-watched)/60000.0))+" min left":watched/60000+" min watched");h.resume.setContentDescription("Resume "+item.optString("vod_name"));h.resume.setOnClickListener(v->open.open(item));}
        posters.load(owner,h.image,item.optString("vod_pic"));
        h.itemView.setOnLongClickListener(v->{ActionSheet.show(owner,item.optString("vod_name"),new String[]{"Hide from Home recommendations","Keep showing this title"},index->{owner.getSharedPreferences("hidden-titles",0).edit().putBoolean(LibraryStore.key(item),index==0).apply();Toast.makeText(owner,"Your preference will apply when Home refreshes",Toast.LENGTH_SHORT).show();});return true;});
        Ui.focus(owner,h.itemView);h.itemView.setContentDescription("View "+item.optString("vod_name"));h.itemView.setOnClickListener(v->open.open(item));
        if(h.play!=null)h.play.setOnClickListener(v->open.open(item));
        if(h.save!=null){updateSave(h,item);h.save.setOnClickListener(v->{LibraryStore.toggle(owner,item);updateSave(h,item);Toast.makeText(owner,LibraryStore.saved(owner,item)?"Added to My list":"Removed from My list",Toast.LENGTH_SHORT).show();});}
    }
    private void updateSave(Holder h,JSONObject item){boolean saved=LibraryStore.saved(owner,item);h.save.setSelected(saved);if(h.play!=null){h.save.setText("My list");Ui.buttonGlyph(owner,h.save,saved?"check":"add");}else Ui.posterBookmark(owner,h.save,saved);h.save.setContentDescription((saved?"Remove from My list: ":"Add to My list: ")+item.optString("vod_name"));}
    public void onViewRecycled(Holder h){if(h.image!=null){h.image.setTag(null);h.image.setImageDrawable(null);}}
}
