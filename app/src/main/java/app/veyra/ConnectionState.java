package app.veyra;
import android.content.*;import android.net.*;
final class ConnectionState {
 static boolean online(Context c){ConnectivityManager m=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);if(m==null)return false;NetworkCapabilities n=m.getNetworkCapabilities(m.getActiveNetwork());return n!=null&&n.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);}
}
