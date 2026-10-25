package app.veyra;
import android.app.Activity;
import android.graphics.*;
import android.widget.ImageView;
import android.util.LruCache;
import java.net.*;
import java.io.*;
import java.lang.ref.WeakReference;
import java.util.concurrent.*;

/** Small bounded HTTPS-only poster loader; no cookies, tracking or disk cache. */
final class PosterLoader {
    private final ThreadPoolExecutor workers=(ThreadPoolExecutor)Executors.newFixedThreadPool(2);
    private final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(8*1024*1024){
        protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}
    };
    private volatile boolean closed;
    void load(Activity activity,ImageView target,String url) {
        if(url.isEmpty() || closed)return;target.setTag(url);
        Bitmap cached=cache.get(url);if(cached!=null){target.setImageBitmap(cached);return;}
        WeakReference<ImageView> image=new WeakReference<>(target);WeakReference<Activity> owner=new WeakReference<>(activity);
        workers.execute(()->{
            HttpURLConnection c=null;
            try {
                URI uri=URI.create(url);if(!"https".equals(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null)return;
                c=(HttpURLConnection)uri.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(8000);c.setReadTimeout(8000);c.setUseCaches(false);
                if(c.getResponseCode()!=200)return;
                ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()) {byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(closed || out.size()+n>2*1024*1024)return;out.write(b,0,n);}}
                byte[] bytes=out.toByteArray();BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
                if(options.outWidth<1 || options.outHeight<1 || options.outWidth>16000 || options.outHeight>16000)return;
                options.inSampleSize=1;while(options.outWidth/options.inSampleSize>400 || options.outHeight/options.inSampleSize>600)options.inSampleSize*=2;
                options.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);if(bitmap==null || closed)return;
                cache.put(url,bitmap);Activity a=owner.get();if(a!=null)a.runOnUiThread(()->{Activity live=owner.get();ImageView v=image.get();if(!closed && live!=null && !live.isDestroyed() && v!=null && url.equals(v.getTag()))v.setImageBitmap(bitmap);});
            }catch(Exception ignored){ /* Title and metadata remain usable when artwork is unavailable. */ }
            finally{if(c!=null)c.disconnect();}
        });
    }
    void clearPending(){workers.getQueue().clear();}
    void close(){closed=true;workers.shutdownNow();cache.evictAll();}
}
