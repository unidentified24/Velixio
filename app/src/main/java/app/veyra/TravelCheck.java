package app.veyra;
import android.content.Context;import android.net.Uri;import androidx.media3.exoplayer.offline.*;import org.json.*;
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
final class TravelCheck {
 /** Re-walk the downloaded resource graph with no network upstream. This does not validate codecs or checksums. */
 static void verify(Context c,String id)throws Exception{JSONObject record=OfflineStore.get(c,id);if(!record.optString("state").equals("complete"))throw new java.io.IOException("Download is not complete");OfflineStore.pin(id);try{DownloadRequest request=new DownloadRequest.Builder(id,Uri.parse(record.getString("url"))).setMimeType(record.getString("mime")).build();new DefaultDownloaderFactory(OfflineStore.readOnly(c,id)).createDownloader(request).download(null);}finally{OfflineStore.unpin(id);OfflineStore.releaseIdle(id);}}
}
