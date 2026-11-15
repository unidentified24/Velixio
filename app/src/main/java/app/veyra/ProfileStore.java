package app.veyra;
import android.content.*;import org.json.*;import java.util.*;
/** Metadata profiles. Default retains the original preference files without copying or deleting them. */
public final class ProfileStore {
 private ProfileStore(){}
 private static final class Fixed extends ContextWrapper {final String profile;Fixed(Context c,String id){super(c);profile=id;}}
 public static Context fixed(Context c,String id){if(!valid(id))throw new IllegalArgumentException("Invalid profile");return new Fixed(c,id);}
 public static String id(Context c){if(c instanceof Fixed)return ((Fixed)c).profile;if(c instanceof android.app.Activity){String fixed=((android.app.Activity)c).getIntent().getStringExtra("_profile");if(fixed!=null&&valid(fixed))return fixed;}String value=c.getSharedPreferences("profiles",0).getString("active","default");return valid(value)?value:"default";}
 private static boolean valid(String id){return id.equals("default")||id.matches("[a-f0-9-]{36}");}
 public static SharedPreferences prefs(Context c,String name){String id=id(c);return c.getSharedPreferences(id.equals("default")?name:"profile-"+id+"-"+name,0);}
 public static LinkedHashMap<String,String> list(Context c){LinkedHashMap<String,String> names=new LinkedHashMap<>();names.put("default","My profile");List<String> ids=new ArrayList<>(c.getSharedPreferences("profiles",0).getAll().keySet());Collections.sort(ids);for(String key:ids)if(key.startsWith("name:")&&valid(key.substring(5)))names.put(key.substring(5),c.getSharedPreferences("profiles",0).getString(key,"Profile"));return names;}
 public static String create(Context c,String name){name=name.trim();if(name.isEmpty()||name.length()>32||list(c).size()>=8)throw new IllegalArgumentException("Use 1–32 characters; up to eight profiles");String id=UUID.randomUUID().toString();c.getSharedPreferences("profiles",0).edit().putString("name:"+id,name).apply();return id;}
 public static void select(Context c,String id){if(!list(c).containsKey(id))throw new IllegalArgumentException("Unknown profile");c.getSharedPreferences("profiles",0).edit().putString("active",id).apply();ResumeWidget.refresh(c,true);}
 public static String name(Context c){return list(c).get(id(c));}
}
