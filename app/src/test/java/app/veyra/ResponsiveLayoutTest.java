package app.veyra;
import android.app.Activity;import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.android.controller.ActivityController;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ResponsiveLayoutTest {
 @Test public void searchBarHasAccessibleSubmitAndClearControls(){
  try(ActivityController<MainActivity> controller=Robolectric.buildActivity(MainActivity.class).create()){
   MainActivity activity=controller.get();
   java.util.ArrayList<android.view.View> views=new java.util.ArrayList<>();
   android.view.View root=activity.findViewById(android.R.id.content);
   root.findViewsWithText(views,"Search",android.view.View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
   assertEquals(1,views.size());
   views.get(0).performClick();
   views.clear();
   root.findViewsWithText(views,"Search titles",android.view.View.FIND_VIEWS_WITH_CONTENT_DESCRIPTION);
   assertEquals(1,views.size());
   assertEquals(android.view.View.VISIBLE,views.get(0).getVisibility());
   assertEquals(android.view.View.VISIBLE,((android.view.View)views.get(0).getParent()).getVisibility());
   android.view.ViewGroup bar=(android.view.ViewGroup)views.get(0).getParent();
   android.view.View clear=bar.getChildAt(bar.getChildCount()-1);
   assertEquals("Clear search",clear.getContentDescription().toString());
   assertEquals(android.view.View.INVISIBLE,clear.getVisibility());
  }
 }
 @Test public void downloadsOpensFromSearchWithoutReplacingItsSelectedTab() throws Exception {
  try(ActivityController<MainActivity> c=Robolectric.buildActivity(MainActivity.class).create()){
   MainActivity activity=c.get();
   java.lang.reflect.Field field=MainActivity.class.getDeclaredField("navigationItems");
   field.setAccessible(true);
   java.util.List<?> tabs=(java.util.List<?>)field.get(activity);
   NavItem search=(NavItem)tabs.get(1),downloads=(NavItem)tabs.get(4);
   search.performClick();
   assertTrue(search.isSelected());
   downloads.performClick();
   assertEquals(DownloadsActivity.class.getName(),Shadows.shadowOf(activity).getNextStartedActivity().getComponent().getClassName());
   assertTrue(search.isSelected());
   assertFalse(downloads.isSelected());
  }
 }
 @Test public void playerGlyphIsCenteredWithinSquareButton(){
  try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=c.get();
   android.widget.Button button=Ui.playerButton(activity,"settings","Playback settings",false);
   int size=Ui.dp(activity,48);
   button.layout(0,0,size,size);
   button.getBackground().setBounds(0,0,size,size);
   android.graphics.drawable.LayerDrawable layers=(android.graphics.drawable.LayerDrawable)button.getBackground();
   android.graphics.Rect bounds=layers.getDrawable(1).getBounds();
   assertEquals(size/2,bounds.centerX(),1);
   assertEquals(size/2,bounds.centerY(),1);
  }
 }
 @Test @Config(qualifiers="w320dp-h640dp") public void narrowPhoneKeepsTwoReadableColumns(){try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){c.get().getSharedPreferences("settings",0).edit().putBoolean("compactPosters",false).commit();assertEquals(2,Ui.columns(c.get()));}}
 @Test @Config(qualifiers="w320dp-h640dp") public void compactShowsThreeColumnsAndSmallerArtwork(){
  try(ActivityController<Activity> controller=Robolectric.buildActivity(Activity.class).setup()){
   Activity activity=controller.get();
   int comfortable=Ui.posterWidth(activity);
   activity.getSharedPreferences("settings",0).edit().putBoolean("compactPosters",true).commit();
   assertEquals(3,Ui.columns(activity));
   assertTrue(Ui.posterWidth(activity)<comfortable);
  }
 }
 @Test @Config(qualifiers="w840dp-h600dp-land") public void tabletUsesAvailableWidth(){try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){c.get().getSharedPreferences("settings",0).edit().putBoolean("compactPosters",false).commit();assertEquals(5,Ui.columns(c.get()));}}
 @Test @Config(qualifiers="w1280dp-h720dp-land") public void wideWindowCapsColumnCount(){try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){c.get().getSharedPreferences("settings",0).edit().putBoolean("compactPosters",true).commit();assertEquals(8,Ui.columns(c.get()));}}
}
