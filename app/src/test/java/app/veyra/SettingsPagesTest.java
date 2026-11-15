package app.veyra;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="w320dp-h640dp")
public class SettingsPagesTest {
 private View text(View root,String label){if(root instanceof TextView&&((TextView)root).getText().toString().equals(label))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View found=text(g.getChildAt(i),label);if(found!=null)return found;}}return null;}
 private View root(SettingsActivity a){return ((ViewGroup)a.findViewById(android.R.id.content)).getChildAt(0);}
 private void click(SettingsActivity a,String label){View v=text(root(a),label);assertNotNull(label,v);while(!v.hasOnClickListeners()&&v.getParent() instanceof View)v=(View)v.getParent();assertTrue(v.performClick());}
 @Test public void viewingOverviewShowsPeriodsAndCanResumeTheExactEpisode() throws Exception {
  android.content.Context context=RuntimeEnvironment.getApplication();
  ViewingActivityLog.record(context,123000,System.currentTimeMillis());
  app.veyra.data.WatchHistory.remember(context,"current","movie","show-1","episode-2","0","Recent series","",60000,600000,false);
  Intent intent=new Intent(context,SettingsActivity.class).putExtra("page","stats");
  try(ActivityController<SettingsActivity> controller=Robolectric.buildActivity(SettingsActivity.class,intent).setup()){
   SettingsActivity activity=controller.get();
   assertNotNull(text(root(activity),"1 active day in the last 7 days"));
   click(activity,"30 days");
   assertNotNull(text(root(activity),"1 active day in the last 30 days"));
   click(activity,"Resume");
   Intent result=Shadows.shadowOf(activity).getResultIntent();
   assertEquals("episode-2",new org.json.JSONObject(result.getStringExtra("resume")).getString("collection"));
  }
 }
 @Test public void homeViewingOverviewShortcutReturnsDirectlyToHome(){
  Intent intent=new Intent(RuntimeEnvironment.getApplication(),SettingsActivity.class).putExtra("page","stats");
  try(ActivityController<SettingsActivity> controller=Robolectric.buildActivity(SettingsActivity.class,intent).setup()){
   controller.get().onBackPressed();
   assertTrue(controller.get().isFinishing());
  }
 }
 @Test public void settingsNavigationUsesPagesAndBackReturnsToOverview(){try(ActivityController<SettingsActivity> c=Robolectric.buildActivity(SettingsActivity.class).setup()){SettingsActivity a=c.get();click(a,"Appearance");assertNotNull(text(root(a),"Set the mood"));assertNull(ShadowDialog.getLatestDialog());a.onBackPressed();assertNotNull(text(root(a),"Settings"));click(a,"Playback");assertNotNull(text(root(a),"Playback settings"));a.onBackPressed();click(a,"Your data");assertNotNull(text(root(a),"Your stories, saved."));assertNull(ShadowDialog.getLatestDialog());}}
 @Test public void themeAndDensityApplyWithoutLeavingAppearance(){try(ActivityController<SettingsActivity> c=Robolectric.buildActivity(SettingsActivity.class).setup()){SettingsActivity a=c.get();click(a,"Appearance");click(a,"Pacific");assertEquals(1,a.getSharedPreferences("settings",0).getInt("theme",0));assertNotNull(text(root(a),"Set the mood"));click(a,"Compact");assertTrue(a.getSharedPreferences("settings",0).getBoolean("compactPosters",false));assertNull(ShadowDialog.getLatestDialog());}}
 @Test public void historyClearingRequiresDedicatedConfirmation(){try(ActivityController<SettingsActivity> c=Robolectric.buildActivity(SettingsActivity.class).setup()){SettingsActivity a=c.get();a.getSharedPreferences("resume",0).edit().putLong("current:movie:1:id:1",500).commit();click(a,"Your data");click(a,"Clear watch history");assertNotNull(text(root(a),"A fresh start?"));click(a,"Keep my history");assertEquals(500,a.getSharedPreferences("resume",0).getLong("current:movie:1:id:1",0));click(a,"Clear watch history");click(a,"Clear watch history");assertEquals(0,a.getSharedPreferences("resume",0).getAll().size());assertNotNull(text(root(a),"Your stories, saved."));}}
 @Test public void exportOpensAndroidDocumentPicker(){try(ActivityController<SettingsActivity> c=Robolectric.buildActivity(SettingsActivity.class).setup()){SettingsActivity a=c.get();click(a,"Your data");click(a,"Export your collection");Intent intent=Shadows.shadowOf(a).getNextStartedActivity();assertNotNull(intent);assertEquals(Intent.ACTION_CREATE_DOCUMENT,intent.getAction());assertTrue(intent.getStringExtra(Intent.EXTRA_TITLE).endsWith(".velixiodata"));}}
 @Test public void appearanceSurvivesRecreationAndFitsNarrowViewport(){try(ActivityController<SettingsActivity> c=Robolectric.buildActivity(SettingsActivity.class).setup()){click(c.get(),"Appearance");c.recreate();SettingsActivity a=c.get();assertNotNull(text(root(a),"Set the mood"));View v=root(a);int width=Ui.dp(a,320),height=Ui.dp(a,640);v.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));v.layout(0,0,width,height);assertEquals(width,v.getWidth());assertNotNull(text(v,"OLED black"));assertNull(ShadowDialog.getLatestDialog());}}
}
