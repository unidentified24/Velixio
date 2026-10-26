package app.veyra;
import android.app.Activity;
import android.graphics.Bitmap;
import android.widget.ImageView;
import android.util.LruCache;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import java.lang.reflect.Field;
import java.util.concurrent.ThreadPoolExecutor;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ArtworkCacheTest {
 @Test public void imageCacheSurvivesLoaderCloseAndRepeatBindKeepsDrawable()throws Exception{try(ActivityController<Activity> c=Robolectric.buildActivity(Activity.class).setup()){Field f=PosterLoader.class.getDeclaredField("cache");f.setAccessible(true);@SuppressWarnings("unchecked") LruCache<String,Bitmap> cache=(LruCache<String,Bitmap>)f.get(null);String url="https://example.invalid/cached-test.png";Bitmap bitmap=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);cache.put(url,bitmap);PosterLoader first=new PosterLoader();ImageView view=new ImageView(c.get());first.load(c.get(),view,url);assertNotNull(view.getDrawable());Object drawable=view.getDrawable();first.load(c.get(),view,url);assertSame(drawable,view.getDrawable());first.close();PosterLoader second=new PosterLoader();try{ImageView next=new ImageView(c.get());second.load(c.get(),next,url);assertNotNull(next.getDrawable());assertSame(bitmap,cache.get(url));}finally{second.close();cache.remove(url);}}}
 @Test public void queuedArtworkWorkIsBounded()throws Exception{PosterLoader loader=new PosterLoader();try{Field field=PosterLoader.class.getDeclaredField("workers");field.setAccessible(true);ThreadPoolExecutor pool=(ThreadPoolExecutor)field.get(loader);assertEquals(96,pool.getQueue().remainingCapacity());assertEquals(2,pool.getMaximumPoolSize());}finally{loader.close();}}
}
