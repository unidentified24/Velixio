package app.veyra;
import android.content.Context;import java.io.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class StorageSafetyTest {
 @Test public void cleanupLeavesFreshUpdatesAndCompletedDownloads()throws Exception{Context c=RuntimeEnvironment.getApplication();File updates=new File(c.getCacheDir(),"updates");updates.mkdirs();File old=new File(updates,"old.apk"),fresh=new File(updates,"fresh.apk"),video=new File(c.getFilesDir(),"downloads/test/video.bin");video.getParentFile().mkdirs();try(FileOutputStream out=new FileOutputStream(old)){out.write(new byte[17]);}fresh.createNewFile();video.createNewFile();assertTrue(old.setLastModified(System.currentTimeMillis()-172800000L));assertEquals(17,StorageTools.cleanUpdates(c));assertTrue(fresh.exists());assertTrue(video.exists());}
}
