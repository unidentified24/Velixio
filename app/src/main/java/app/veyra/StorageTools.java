package app.veyra;
import android.content.Context;import java.io.*;import java.util.*;
/** Cleanup is deliberately restricted to stale updater files; native runtime and downloads are excluded. */
final class StorageTools {
 static long size(File f){if(f==null||!f.exists())return 0;if(f.isFile())return f.length();long total=0;File[] files=f.listFiles();if(files!=null)for(File child:files)try{if(child.getCanonicalFile().getParentFile().equals(f.getCanonicalFile()))total+=size(child);}catch(IOException ignored){}return total;}
 static long updates(Context c){return size(new File(c.getCacheDir(),"updates"));}
 static long cleanUpdates(Context c){File dir=new File(c.getCacheDir(),"updates");long removed=0;File[] files=dir.listFiles();if(files!=null)for(File f:files)if(f.isFile()&&f.getName().endsWith(".apk")&&f.lastModified()<System.currentTimeMillis()-24*60*60*1000L){long bytes=f.length();if(f.delete())removed+=bytes;}return removed;}
 static String format(long bytes){return String.format(Locale.ROOT,bytes>=1073741824L?"%.1f GB":"%.1f MB",bytes/(bytes>=1073741824L?1073741824.0:1048576.0));}
}
