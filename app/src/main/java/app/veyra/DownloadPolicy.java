package app.veyra;
import android.content.*;import android.net.*;import org.json.*;
final class DownloadPolicy {
 static boolean wifi(Context c){ConnectivityManager m=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);if(m==null)return false;NetworkCapabilities caps=m.getNetworkCapabilities(m.getActiveNetwork());return caps!=null&&caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);}
 static String blocked(Context c,long bytes,String current){if(c.getSharedPreferences("settings",0).getBoolean("wifiDownloads",false)&&!wifi(c))return "Wi-Fi is required by your download preferences";long used=0;for(JSONObject record:OfflineStore.list(c))if(!record.optString("downloadId").equals(current))used+=record.optLong("bytes");int gb=c.getSharedPreferences("settings",0).getInt("downloadLimitGb",8);gb=Math.max(2,Math.min(32,gb));return used+bytes>=gb*1073741824L?"Your offline storage limit has been reached":"";}
}
