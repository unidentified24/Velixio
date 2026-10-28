package app.veyra;
import android.content.Context;import java.io.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class UpdateCacheTest {
 @Test public void prunesOnlyExpiredUpdatePackages()throws Exception{Context c=RuntimeEnvironment.getApplication();File dir=new File(c.getCacheDir(),"updates");dir.mkdirs();File old=new File(dir,"old.apk"),recent=new File(dir,"recent.apk"),other=new File(dir,"keep.txt");old.createNewFile();recent.createNewFile();other.createNewFile();assertTrue(old.setLastModified(System.currentTimeMillis()-2*24*60*60*1000L));AppUpdates.pruneUpdates(c);assertFalse(old.exists());assertTrue(recent.exists());assertTrue(other.exists());}
}
