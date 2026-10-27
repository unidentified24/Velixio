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
    private final boolean rail;
    private final Runnable previous,next;
    CatalogAdapter(Activity owner,PosterLoader posters,List<JSONObject> titles,boolean hero,Open open,Runnable previous,Runnable next) {
        this(owner,posters,titles,hero,open,previous,next,false);
    }
    CatalogAdapter(Activity owner,PosterLoader posters,List<JSONObject> titles,boolean hero,Open open,Runnable previous,Runnable next,boolean rail) {
        this.owner=owner;this.posters=posters;this.titles=new ArrayList<>(titles);this.hero=hero&&!titles.isEmpty();this.open=open;this.previous=previous;this.next=next;this.rail=rail;
    }
    public int getItemCount(){return titles.size()+(hero?1:0)+(rail?0:1);}
    public int getItemViewType(int position){return hero&&position==0?1:!rail&&position==getItemCount()-1?2:0;}
    boolean fullWidth(int position){return getItemViewType(position)!=0;}
    static final class Holder extends RecyclerView.ViewHolder {
        ImageView image;TextView title,meta;Button play,save;
        Holder(View view){super(view);}
    }
    public Holder onCreateViewHolder(ViewGroup parent,int type) {
        int gap=Ui.dp(owner,5);
        if(type==2){LinearLayout row=new LinearLayout(owner);row.setGravity(Gravity.CENTER);row.setPadding(0,Ui.dp(owner,20),0,Ui.dp(owner,24));
            if(previous!=null){Button b=Ui.chip(owner,"Previous");b.setOnClickListener(v->previous.run());row.addView(b);}
            if(next!=null){Button b=Ui.chip(owner,"Explore more");b.setOnClickListener(v->next.run());row.addView(b);}
            return new Holder(row);
        }
        if(type==1){
            LinearLayout frame=Ui.column(owner);GradientDrawable surface=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Ui.SURFACE,Ui.BG});surface.setCornerRadius(Ui.dp(owner,18));frame.setBackground(surface);frame.setPadding(Ui.dp(owner,16),Ui.dp(owner,16),Ui.dp(owner,16),Ui.dp(owner,12));frame.setLayoutParams(new RecyclerView.LayoutParams(-1,-2));
            Holder h=new Holder(frame);LinearLayout feature=new LinearLayout(owner);feature.setGravity(Gravity.CENTER_VERTICAL);
            h.image=new ImageView(owner);h.image.setScaleType(ImageView.ScaleType.CENTER_CROP);GradientDrawable imageShape=new GradientDrawable();imageShape.setColor(Ui.SURFACE);imageShape.setCornerRadius(Ui.dp(owner,10));h.image.setBackground(imageShape);h.image.setClipToOutline(true);feature.addView(h.image,new LinearLayout.LayoutParams(Ui.dp(owner,122),Ui.dp(owner,183)));
            LinearLayout copy=Ui.column(owner);copy.setBackgroundColor(Color.TRANSPARENT);copy.setPadding(Ui.dp(owner,16),0,0,0);TextView eyebrow=Ui.text(owner,"IN THE SPOTLIGHT",10,Ui.ACCENT);eyebrow.setLetterSpacing(.12f);copy.addView(eyebrow);
            h.title=Ui.text(owner,"",23,Ui.INK);h.title.setTypeface(null,Typeface.BOLD);h.title.setMaxLines(4);h.title.setEllipsize(android.text.TextUtils.TruncateAt.END);copy.addView(h.title);h.meta=Ui.text(owner,"",12,Ui.MUTED);copy.addView(h.meta);feature.addView(copy,new LinearLayout.LayoutParams(0,-2,1));frame.addView(feature);
            LinearLayout actions=new LinearLayout(owner);actions.setPadding(0,Ui.dp(owner,12),0,0);h.play=Ui.primary(owner,"View title  ›");LinearLayout.LayoutParams action=new LinearLayout.LayoutParams(0,Ui.dp(owner,48),1);action.rightMargin=Ui.dp(owner,8);actions.addView(h.play,action);h.save=Ui.chip(owner,"+ My list");actions.addView(h.save,new LinearLayout.LayoutParams(0,Ui.dp(owner,48),1));frame.addView(actions);return h;
        }
        LinearLayout card=Ui.column(owner);card.setPadding(gap,gap,gap,Ui.dp(owner,16));Holder h=new Holder(card);
        h.image=new ImageView(owner);h.image.setBackgroundColor(Color.rgb(23,31,41));h.image.setScaleType(ImageView.ScaleType.CENTER_CROP);
        GradientDrawable rounded=new GradientDrawable();rounded.setColor(Color.rgb(23,31,41));rounded.setCornerRadius(Ui.dp(owner,8));h.image.setBackground(rounded);h.image.setClipToOutline(true);
        int columns=Ui.columns(owner);
        int height=rail?(int)(Ui.posterWidth(owner)*1.5f):(int)((owner.getResources().getConfiguration().screenWidthDp-32f)/columns*1.5f);
        FrameLayout artwork=new FrameLayout(owner);artwork.addView(h.image,new FrameLayout.LayoutParams(-1,-1));h.save=Ui.chip(owner,"+");FrameLayout.LayoutParams savePos=new FrameLayout.LayoutParams(Ui.dp(owner,48),Ui.dp(owner,48),Gravity.TOP|Gravity.END);artwork.addView(h.save,savePos);card.addView(artwork,new LinearLayout.LayoutParams(-1,Ui.dp(owner,height)));
        h.title=Ui.text(owner,"",13,Ui.INK);h.title.setMaxLines(2);h.title.setMinLines(2);h.title.setEllipsize(android.text.TextUtils.TruncateAt.END);card.addView(h.title);
        h.meta=Ui.text(owner,"",11,Ui.MUTED);h.meta.setPadding(0,0,0,0);h.meta.setMaxLines(1);card.addView(h.meta);
        card.setLayoutParams(new RecyclerView.LayoutParams(rail?Ui.dp(owner,Ui.posterWidth(owner)):-1,-2));return h;
    }
    public void onBindViewHolder(Holder h,int position) {
        if(getItemViewType(position)==2)return;
        JSONObject item=titles.get(hero?Math.max(0,position-1):position);
        h.title.setText(item.optString("vod_name","Untitled"));
        String year=item.optString("vod_year"),score=item.optString("vod_douban_score");
        h.meta.setText(year+(score.isEmpty()||score.equals("0")?"":"  ·  ★ "+score));
        posters.load(owner,h.image,item.optString("vod_pic"));
        h.itemView.setOnLongClickListener(v->{ActionSheet.show(owner,item.optString("vod_name"),new String[]{"Hide from Home recommendations","Keep showing this title"},index->{owner.getSharedPreferences("hidden-titles",0).edit().putBoolean(LibraryStore.key(item),index==0).apply();Toast.makeText(owner,"Your preference will apply when Home refreshes",Toast.LENGTH_SHORT).show();});return true;});
        h.itemView.setContentDescription("View "+item.optString("vod_name"));h.itemView.setOnClickListener(v->open.open(item));
        if(h.play!=null)h.play.setOnClickListener(v->open.open(item));
        if(h.save!=null){updateSave(h,item);h.save.setOnClickListener(v->{LibraryStore.toggle(owner,item);updateSave(h,item);Toast.makeText(owner,LibraryStore.saved(owner,item)?"Added to My list":"Removed from My list",Toast.LENGTH_SHORT).show();});}
    }
    private void updateSave(Holder h,JSONObject item){boolean saved=LibraryStore.saved(owner,item);h.save.setText(h.play!=null?(saved?"✓ My list":"+ My list"):(saved?"✓":"+"));h.save.setContentDescription((saved?"Remove from My list: ":"Add to My list: ")+item.optString("vod_name"));}
    public void onViewRecycled(Holder h){if(h.image!=null){h.image.setTag(null);h.image.setImageDrawable(null);}}
}
