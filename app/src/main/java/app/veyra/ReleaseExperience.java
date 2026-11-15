package app.veyra;
import android.app.Activity;import android.content.*;import android.view.*;import android.widget.*;
final class ReleaseExperience {
 static final String NOTES="Browser playback now has Fit, Fill and Stretch controls, fullscreen shortcuts and reconnect with position recovery.\n\nUse Viewing Overview to see your watch time and resume recent episodes.";
 static boolean showIfNeeded(Activity a){try{String version=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;SharedPreferences p=a.getSharedPreferences("release-experience",0);String previous=p.getString("seen","");if(version.equals(previous))return false;p.edit().putString("seen",version).apply();Intent i=new Intent(a,WhatsNewActivity.class);a.startActivity(i);return true;}catch(Exception e){return false;}}
}
