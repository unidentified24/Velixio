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
public class ZunoHomeTest {
 private List<JSONObject> titles()throws Exception{List<JSONObject> items=new ArrayList<>();int year=Calendar.getInstance().get(Calendar.YEAR);for(int i=0;i<90;i++)items.add(new JSONObject().put("id",""+i).put("_provider","current").put("_kind","movie").put("vod_name","Title "+i).put("vod_year",i%2==0?year:2001).put("vod_douban_score",8));return items;}
 @Test public void homeOffersDistinctSectionsAndAnotherFeature()throws Exception{try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){Activity a=controller.get();Ui.configure(a);PosterLoader p=new PosterLoader();try{HomeAdapter adapter=new HomeAdapter(a,p,titles(),title->{});assertTrue(adapter.getItemCount()>=6);HomeAdapter.Holder h=adapter.onCreateViewHolder(new FrameLayout(a),1);adapter.onBindViewHolder(h,0);String first=h.hero.title.getText().toString();LinearLayout heading=(LinearLayout)((LinearLayout)h.itemView).getChildAt(0);heading.getChildAt(1).performClick();adapter.onBindViewHolder(h,0);assertNotEquals(first,h.hero.title.getText().toString());}finally{p.close();}}}
 @Test public void posterSaveControlTogglesLibrary()throws Exception{try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){Activity a=controller.get();Ui.configure(a);a.getSharedPreferences("favorites",0).edit().clear().commit();PosterLoader p=new PosterLoader();try{JSONObject title=titles().get(0);CatalogAdapter adapter=new CatalogAdapter(a,p,Arrays.asList(title),false,t->{},null,null,true);CatalogAdapter.Holder h=adapter.onCreateViewHolder(new FrameLayout(a),0);adapter.onBindViewHolder(h,0);h.save.performClick();assertTrue(LibraryStore.saved(a,title));h.save.performClick();assertFalse(LibraryStore.saved(a,title));}finally{p.close();}}}
}
