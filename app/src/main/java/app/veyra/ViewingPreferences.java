package app.veyra;

import android.content.Context;
import android.content.SharedPreferences;

final class ViewingPreferences {
    private ViewingPreferences(){}
    static SharedPreferences prefs(Context context){return ProfileStore.prefs(context,"viewing-preferences");}
    static String language(Context context,String title,String kind){
        SharedPreferences preferences=prefs(context);
        return preferences.getString(kind+":"+title,preferences.getString(kind,""));
    }
    static int quality(Context context,String title){
        return prefs(context).getInt("quality:"+title,prefs(context).getInt("quality",
            context.getSharedPreferences("settings",0).getInt("qualityHeight",0)));
    }
}
