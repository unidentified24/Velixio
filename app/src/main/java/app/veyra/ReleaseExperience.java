package app.veyra;
import android.app.Activity;import android.content.*;import android.view.*;import android.widget.*;
final class ReleaseExperience {
 static final String NOTES="• Personal profiles for separate lists and viewing history\n• Richer update notices and a reminder option\n• Storage breakdown and safe update-file cleanup\n• Sleep timer, touch lock and next-episode countdown\n• Download data controls and private viewing";
 static boolean showIfNeeded(Activity a){try{String version=a.getPackageManager().getPackageInfo(a.getPackageName(),0).versionName;SharedPreferences p=a.getSharedPreferences("release-experience",0);String previous=p.getString("seen","");if(version.equals(previous))return false;p.edit().putString("seen",version).apply();Intent i=new Intent(a,WhatsNewActivity.class);a.startActivity(i);return true;}catch(Exception e){return false;}}
}
