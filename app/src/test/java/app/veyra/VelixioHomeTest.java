package app.veyra;
import android.app.Activity;
import android.widget.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class VelixioHomeTest {
 private List<JSONObject> titles()throws Exception{List<JSONObject> items=new ArrayList<>();int year=Calendar.getInstance().get(Calendar.YEAR);for(int i=0;i<90;i++)items.add(new JSONObject().put("id",""+i).put("_provider","current").put("_kind","movie").put("vod_name","Title "+i).put("vod_year",i%2==0?year:2001).put("vod_douban_score",8));return items;}
 @Test public void homeOffersDistinctSectionsAndAnotherFeature()throws Exception{try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){Activity a=controller.get();Ui.configure(a);PosterLoader p=new PosterLoader();try{HomeAdapter adapter=new HomeAdapter(a,p,titles(),title->{});assertTrue(adapter.getItemCount()>=6);HomeAdapter.Holder h=adapter.onCreateViewHolder(new FrameLayout(a),1);adapter.onBindViewHolder(h,0);String first=h.hero.title.getText().toString();LinearLayout heading=(LinearLayout)((LinearLayout)h.itemView).getChildAt(0);heading.getChildAt(1).performClick();adapter.onBindViewHolder(h,0);assertNotEquals(first,h.hero.title.getText().toString());}finally{p.close();}}}
 @Test public void posterSaveControlTogglesLibrary()throws Exception{try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){Activity a=controller.get();Ui.configure(a);a.getSharedPreferences("favorites",0).edit().clear().commit();PosterLoader p=new PosterLoader();try{JSONObject title=titles().get(0);CatalogAdapter adapter=new CatalogAdapter(a,p,Arrays.asList(title),false,t->{},null,null,true);CatalogAdapter.Holder h=adapter.onCreateViewHolder(new FrameLayout(a),0);adapter.onBindViewHolder(h,0);h.save.performClick();assertTrue(LibraryStore.saved(a,title));h.save.performClick();assertFalse(LibraryStore.saved(a,title));}finally{p.close();}}}
 @Test public void recyclingShelfReusesItsAdapter()throws Exception{try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){Activity a=c.get();Ui.configure(a);PosterLoader p=new PosterLoader();try{HomeAdapter adapter=new HomeAdapter(a,p,titles(),t->{});HomeAdapter.Holder h=adapter.onCreateViewHolder(new FrameLayout(a),0);adapter.onBindViewHolder(h,1);Object original=h.rail.getAdapter();adapter.onViewRecycled(h);adapter.onBindViewHolder(h,1);assertSame(original,h.rail.getAdapter());}finally{p.close();}}}
 @Test public void excludedCatalogDoesNotReappearAsFallback()throws Exception{try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){PosterLoader p=new PosterLoader();try{HomeAdapter adapter=new HomeAdapter(c.get(),p,Arrays.asList(new JSONObject().put("id","1").put("vod_name","Baddies USA")),t->{});assertEquals(2,adapter.getItemViewType(0));HomeAdapter.Holder h=adapter.onCreateViewHolder(new FrameLayout(c.get()),2);adapter.onBindViewHolder(h,0);assertTrue(((TextView)h.itemView).getText().toString().contains("No recommendations"));}finally{p.close();}}}
 @Test public void continueWatchingFitsPortraitAndResumesExactRecord()throws Exception{
  try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){
   Activity a=c.get();Ui.configure(a);PosterLoader loader=new PosterLoader();
   try{for(boolean compact:new boolean[]{false,true}){
    a.getSharedPreferences("settings",0).edit().putBoolean("compactPosters",compact).commit();
    app.veyra.data.WatchHistory.clear(a);app.veyra.data.WatchHistory.remember(a,"current","movie","resume-film","episode-2","0","A portrait film","",600000,1800000,false);
    JSONObject[] opened={null};HomeAdapter home=new HomeAdapter(a,loader,titles(),item->opened[0]=item);
    HomeAdapter.Holder section=home.onCreateViewHolder(new FrameLayout(a),0);home.onBindViewHolder(section,1);assertEquals("Continue watching",section.heading.getText().toString());
    CatalogAdapter rail=(CatalogAdapter)section.rail.getAdapter();CatalogAdapter.Holder card=rail.onCreateViewHolder(section.rail,rail.getItemViewType(0));rail.onBindViewHolder(card,0);
    int width=card.itemView.getLayoutParams().width;card.itemView.measure(android.view.View.MeasureSpec.makeMeasureSpec(width,android.view.View.MeasureSpec.EXACTLY),android.view.View.MeasureSpec.makeMeasureSpec(0,android.view.View.MeasureSpec.UNSPECIFIED));card.itemView.layout(0,0,width,card.itemView.getMeasuredHeight());
    assertEquals(ImageView.ScaleType.FIT_CENTER,card.image.getScaleType());assertEquals(1.5,card.image.getMeasuredHeight()/(double)card.image.getMeasuredWidth(),.02);assertEquals(333,card.progress.getProgress());assertEquals("20 min left",card.meta.getText().toString());card.resume.performClick();assertEquals("episode-2",opened[0].getJSONObject("_resume").getString("collection"));assertTrue("Card "+card.itemView.getMeasuredHeight()+" exceeds rail "+section.rail.getLayoutParams().height+" compact="+compact,card.itemView.getMeasuredHeight()<=section.rail.getLayoutParams().height);
   }}finally{loader.close();app.veyra.data.WatchHistory.clear(a);a.getSharedPreferences("settings",0).edit().remove("compactPosters").commit();}
  }
 }
}
