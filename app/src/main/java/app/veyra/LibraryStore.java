package app.veyra;
import android.content.Context;
import org.json.*;
import java.util.*;
final class LibraryStore {
 static String key(JSONObject o){return o.optString("_provider","current")+":"+o.optString("_kind","movie")+":"+o.optString("id");}
 static boolean saved(Context c,JSONObject o){return c.getSharedPreferences("favorites",0).contains(key(o));}
 static void toggle(Context c,JSONObject o){try{
  android.content.SharedPreferences prefs=c.getSharedPreferences("favorites",0);String key=key(o);
  if(prefs.contains(key))prefs.edit().remove(key).apply();else{JSONObject safe=new JSONObject();for(String field:new String[]{"id","vod_name","vod_pic","vod_year","vod_douban_score","_provider","_kind"})if(o.has(field))safe.put(field,o.optString(field));prefs.edit().putString(key,safe.toString()).apply();}
 }catch(JSONException ignored){} }
 static List<JSONObject> list(Context c){List<JSONObject> out=new ArrayList<>();for(Object v:c.getSharedPreferences("favorites",0).getAll().values())try{out.add(new JSONObject((String)v));}catch(Exception ignored){}out.sort(Comparator.comparing(o->o.optString("vod_name")));return out;}
}
