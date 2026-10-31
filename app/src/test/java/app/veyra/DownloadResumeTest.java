package app.veyra;
import android.content.Context;import android.net.Uri;import androidx.media3.datasource.*;import androidx.media3.datasource.cache.*;import androidx.media3.exoplayer.offline.*;import org.json.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import java.util.*;import java.util.concurrent.atomic.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class DownloadResumeTest {
 @Test public void retryUsesCachedSegmentsAndTravelCheckRejectsMissingResources()throws Exception{
  Context c=RuntimeEnvironment.getApplication();String id=UUID.randomUUID().toString(),url="https://example.invalid/movie.m3u8";AtomicBoolean failSecond=new AtomicBoolean(true);AtomicInteger firstReads=new AtomicInteger();
  byte[] manifest=("#EXTM3U\n#EXT-X-VERSION:3\n#EXT-X-TARGETDURATION:1\n#EXTINF:1,\nfirst.ts\n#EXTINF:1,\nsecond.ts\n#EXT-X-ENDLIST\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
  DataSource.Factory upstream=()->new DataSource(){ByteArrayDataSource live;public void addTransferListener(TransferListener l){}public long open(DataSpec spec)throws java.io.IOException{String path=spec.uri.getPath();if(path.endsWith("second.ts")&&failSecond.get())throw new java.io.IOException("Interrupted fixture");if(path.endsWith("first.ts"))firstReads.incrementAndGet();live=new ByteArrayDataSource(path.endsWith(".m3u8")?manifest:new byte[]{1,2,3,4});return live.open(spec);}public int read(byte[] b,int o,int n)throws java.io.IOException{return live.read(b,o,n);}public Uri getUri(){return live==null?null:live.getUri();}public void close(){if(live!=null)live.close();}};
  CacheDataSource.Factory factory=new CacheDataSource.Factory().setCache(OfflineStore.cache(c,id)).setUpstreamDataSourceFactory(upstream);DownloadRequest request=new DownloadRequest.Builder(id,Uri.parse(url)).setMimeType("application/x-mpegURL").build();
  try{new DefaultDownloaderFactory(factory).createDownloader(request).download(null);fail();}catch(java.io.IOException expected){}assertTrue(OfflineStore.cache(c,id).getCacheSpace()>0);int readsBefore=firstReads.get();assertTrue(readsBefore>0);failSecond.set(false);new DefaultDownloaderFactory(factory).createDownloader(request).download(null);assertEquals(readsBefore,firstReads.get());
  OfflineStore.save(c,id,new JSONObject().put("state","complete").put("title","Fixture").put("url",url).put("mime","application/x-mpegURL"));TravelCheck.verify(c,id);
  OfflineStore.cache(c,id).removeResource("https://example.invalid/second.ts");try{TravelCheck.verify(c,id);fail("Missing segments must fail without a network source");}catch(java.io.IOException expected){}finally{OfflineStore.remove(c,id);}
 }
}
