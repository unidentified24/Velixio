package app.veyra.data;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;

/** Device-private title/episode metadata only; never stores stream URLs, keys or session tokens. */
public final class WatchHistory {
    private WatchHistory() {}
    public static synchronized void remember(Context context,String provider,String kind,String id,String collection,String audioType,String title,String poster,long position,long duration,boolean completed) {
        if(app.veyra.ProfileStore.prefs(context,"privacy").getBoolean("pauseHistory",false))return;
        if(id==null || id.isEmpty() || provider==null || kind==null)return;
        SharedPreferences prefs=app.veyra.ProfileStore.prefs(context,"watch-history");
        try {
            String key=provider+":"+kind+":"+id;
            JSONObject record=new JSONObject().put("provider",provider).put("kind",kind).put("id",id).put("collection",collection)
                .put("audioType",audioType).put("title",title).put("poster",poster==null?"":poster)
                .put("position",Math.max(0,position)).put("duration",Math.max(0,duration)).put("completed",completed).put("updatedAt",System.currentTimeMillis());
            SharedPreferences.Editor edit=prefs.edit().putString(key,record.toString());
            if(!prefs.contains(key) && prefs.getAll().size()>=100) {
                String oldest=null;long time=Long.MAX_VALUE;
                for(Map.Entry<String,?> entry:prefs.getAll().entrySet())try{long updated=new JSONObject((String)entry.getValue()).optLong("updatedAt");if(updated<time){time=updated;oldest=entry.getKey();}}catch(Exception ignored){edit.remove(entry.getKey());}
                if(oldest!=null)edit.remove(oldest);
            }
            edit.apply();app.veyra.ResumeWidget.refresh(context,false);
        }catch(JSONException ignored){}
    }
    public static List<JSONObject> list(Context context) {
        List<JSONObject> records=new ArrayList<>();
        for(Object value:app.veyra.ProfileStore.prefs(context,"watch-history").getAll().values())try{JSONObject record=new JSONObject((String)value);if(!record.optBoolean("completed") && record.optLong("position")>0)records.add(record);}catch(Exception ignored){}
        records.sort((a,b)->Long.compare(b.optLong("updatedAt"),a.optLong("updatedAt")));return records;
    }
    public static void clear(Context context){app.veyra.ProfileStore.prefs(context,"watch-history").edit().clear().apply();app.veyra.ProfileStore.prefs(context,"resume").edit().clear().apply();app.veyra.ResumeWidget.refresh(context,true);}
}
