package app.veyra;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import android.content.*;import android.app.job.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class BackupScheduleTest {
 private Context c;
 @Before public void setup(){c=RuntimeEnvironment.getApplication();c.getSharedPreferences("auto-backup",0).edit().clear().commit();c.getSystemService(JobScheduler.class).cancelAll();}
 @Test public void noFolderMeansNoAutomaticAccessOrJob(){AutoBackup.schedule(c);assertNull(c.getSystemService(JobScheduler.class).getPendingJob(AutoBackup.JOB));assertFalse(AutoBackup.due(c,System.currentTimeMillis()));}
 @Test public void dailyBackupKeepsStableScheduleAndStopCancels(){c.getSharedPreferences("auto-backup",0).edit().putString("folder","content://documents/tree/test").putLong("lastSuccess",1000).commit();AutoBackup.schedule(c);JobInfo job=c.getSystemService(JobScheduler.class).getPendingJob(AutoBackup.JOB);assertNotNull(job);assertTrue(job.isPersisted());assertEquals(AutoBackup.DAY,job.getIntervalMillis());assertFalse(AutoBackup.due(c,2000));assertTrue(AutoBackup.due(c,1000+AutoBackup.DAY));AutoBackup.stop(c);assertNull(c.getSystemService(JobScheduler.class).getPendingJob(AutoBackup.JOB));}
 @Test public void allThemesRoundTripThroughBackup()throws Exception{for(int i=0;i<ThemePalette.NAMES.length;i++){c.getSharedPreferences("settings",0).edit().putInt("theme",i).commit();assertEquals(i,VelixioData.validate(VelixioData.export(c).toString()).getJSONObject("settings").getInt("theme"));}}
}
