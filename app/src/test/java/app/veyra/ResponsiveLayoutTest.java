package app.veyra;
import android.app.Activity;import org.junit.Test;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.android.controller.ActivityController;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ResponsiveLayoutTest {
 @Test @Config(qualifiers="w320dp-h640dp") public void narrowPhoneKeepsTwoReadableColumns(){try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){c.get().getSharedPreferences("settings",0).edit().putBoolean("compactPosters",false).commit();assertEquals(2,Ui.columns(c.get()));}}
 @Test @Config(qualifiers="w840dp-h600dp-land") public void tabletUsesAvailableWidth(){try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){c.get().getSharedPreferences("settings",0).edit().putBoolean("compactPosters",false).commit();assertEquals(5,Ui.columns(c.get()));}}
 @Test @Config(qualifiers="w1280dp-h720dp-land") public void wideWindowCapsColumnCount(){try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){c.get().getSharedPreferences("settings",0).edit().putBoolean("compactPosters",true).commit();assertEquals(8,Ui.columns(c.get()));}}
}
