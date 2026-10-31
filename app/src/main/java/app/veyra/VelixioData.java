package app.veyra;
import android.content.*;
import org.json.*;
import java.util.*;
/** Portable, allowlisted metadata. No sessions, server profiles or media URLs. */
final class VelixioData {
 static final int LIMIT=2*1024*1024;
 private static final String[] FAVORITE={"id","vod_name","vod_year","vod_douban_score","_provider","_kind"};
 private static final String[] HISTORY={"provider","kind","id","collection","audioType","title"};
 static JSONObject export(Context c)throws Exception{
  JSONObject out=new JSONObject().put("format","velixiodata").put("version",1).put("exportedAt",System.currentTimeMillis());
  JSONArray saved=new JSONArray(),history=new JSONArray();JSONObject resume=new JSONObject(),settings=new JSONObject();
  for(JSONObject o:LibraryStore.list(c))saved.put(cleanRecord(o,false));
  for(Object v:app.veyra.ProfileStore.prefs(c,"watch-history").getAll().values())history.put(cleanRecord(new JSONObject((String)v),true));
  for(Map.Entry<String,?> e:app.veyra.ProfileStore.prefs(c,"resume").getAll().entrySet())if(e.getValue() instanceof Long&&validResume(e.getKey()))resume.put(e.getKey(),e.getValue());
  SharedPreferences p=c.getSharedPreferences("settings",0);settings.put("theme",p.getInt("theme",0)).put("compactPosters",p.getBoolean("compactPosters",false)).put("autoplay",p.getBoolean("autoplay",false)).put("rememberSpeed",p.getBoolean("rememberSpeed",true)).put("speed",p.getFloat("speed",1)).put("qualityHeight",p.getInt("qualityHeight",0));
  out.put("personal",PersonalStore.export(c));
  out.put("favorites",saved).put("history",history).put("resume",resume).put("settings",settings);validate(out.toString());return out;
 }
 private static boolean validResume(String key){return key.length()<=512&&(key.startsWith("current:")||key.startsWith("classic:"))&&!key.contains("/")&&!key.contains("?");}
 private static String text(JSONObject o,String key,int max)throws Exception{Object value=o.opt(key);if(value==null)return "";if(value instanceof Number&&(key.equals("id")||key.equals("vod_year")||key.equals("vod_douban_score")))value=value.toString();if(!(value instanceof String)||((String)value).length()>max)throw new Exception("Invalid field");return (String)value;}
 private static long number(JSONObject o,String key,long max)throws Exception{Object value=o.opt(key);if(value==null)return 0;if(!(value instanceof Number))throw new Exception("Invalid number");double n=((Number)value).doubleValue();if(!Double.isFinite(n)||n<0||n>max||n!=Math.floor(n))throw new Exception("Invalid number");return (long)n;}
 static JSONObject cleanRecord(JSONObject raw,boolean history)throws Exception{
  JSONObject clean=new JSONObject();for(String key:history?HISTORY:FAVORITE)clean.put(key,text(raw,key,key.equals("title")||key.equals("vod_name")?1024:256));
  String provider=clean.optString(history?"provider":"_provider","current"),kind=clean.optString(history?"kind":"_kind","movie");if(provider.isEmpty())provider="current";if(kind.isEmpty())kind="movie";
  if(!(provider.equals("current")||provider.equals("classic"))||!(kind.equals("movie")||kind.equals("short"))||clean.optString("id").isEmpty())throw new Exception("Invalid title identity");
  clean.put(history?"provider":"_provider",provider).put(history?"kind":"_kind",kind);
  String image=text(raw,history?"poster":"vod_pic",4096);if(!image.isEmpty()){android.net.Uri uri=android.net.Uri.parse(image);if("https".equals(uri.getScheme())&&uri.getHost()!=null&&uri.getUserInfo()==null&&uri.getQuery()==null)clean.put(history?"poster":"vod_pic",image);}
  if(history){clean.put("position",number(raw,"position",2592000000L)).put("duration",number(raw,"duration",2592000000L)).put("updatedAt",number(raw,"updatedAt",4102444800000L));Object done=raw.opt("completed");if(done!=null&&!(done instanceof Boolean))throw new Exception("Invalid flag");clean.put("completed",Boolean.TRUE.equals(done));}
  return clean;
 }
 static JSONObject validate(String text)throws Exception{
  if(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>LIMIT)throw new Exception("File too large");
  int depth=0;boolean quoted=false,escaped=false;for(int i=0;i<text.length();i++){char ch=text.charAt(i);if(quoted){if(escaped)escaped=false;else if(ch==92)escaped=true;else if(ch==34)quoted=false;}else if(ch==34)quoted=true;else if(ch=='{'||ch=='['){if(++depth>12)throw new Exception("Backup nesting too deep");}else if(ch=='}'||ch==']')depth--;}
  JSONObject raw=new JSONObject(text);if(!("velixiodata".equals(raw.optString("format"))||"zunodata".equals(raw.optString("format")))||number(raw,"version",1)!=1)throw new Exception("Unsupported backup");
  JSONArray saved=raw.getJSONArray("favorites"),history=raw.getJSONArray("history");JSONObject positions=raw.getJSONObject("resume"),prefs=raw.getJSONObject("settings");
  if(saved.length()>1000||history.length()>100||positions.length()>200)throw new Exception("Too many records");
  JSONObject result=new JSONObject(),resume=new JSONObject(),settings=new JSONObject();JSONArray s=new JSONArray(),h=new JSONArray();for(int i=0;i<saved.length();i++)s.put(cleanRecord(saved.getJSONObject(i),false));for(int i=0;i<history.length();i++)h.put(cleanRecord(history.getJSONObject(i),true));
  Iterator<String> keys=positions.keys();while(keys.hasNext()){String key=keys.next();if(!validResume(key))throw new Exception("Invalid playback position");resume.put(key,number(positions,key,2592000000L));}
  for(String key:new String[]{"compactPosters","autoplay","rememberSpeed"})if(prefs.has(key)){if(!(prefs.get(key) instanceof Boolean))throw new Exception("Invalid setting");settings.put(key,prefs.getBoolean(key));}
  int theme=(int)number(prefs,"theme",ThemePalette.NAMES.length-1);settings.put("theme",theme);if(prefs.has("qualityHeight")&&!(prefs.get("qualityHeight") instanceof Number))throw new Exception("Invalid quality");int quality=prefs.optInt("qualityHeight",0);if(!(quality==-1||quality==0||quality==480||quality==720||quality==1080))throw new Exception("Invalid quality");settings.put("qualityHeight",quality);
  if(prefs.has("speed")&&!(prefs.get("speed") instanceof Number))throw new Exception("Invalid speed");double speed=prefs.optDouble("speed",1);if(!Double.isFinite(speed)||speed<0.25||speed>3)throw new Exception("Invalid speed");settings.put("speed",speed);
  if(raw.has("personal"))result.put("personal",PersonalStore.validate(raw.getJSONObject("personal")));
  return result.put("favorites",s).put("history",h).put("resume",resume).put("settings",settings);
 }
 static void restore(Context c,JSONObject safe)throws Exception{
  // Called only after full validation and the import summary. Merge title lists, retain newer history.
  JSONObject mergedPersonal=PersonalStore.export(c);if(safe.has("personal")){JSONObject imported=safe.getJSONObject("personal");Iterator<String> personalKeys=imported.keys();while(personalKeys.hasNext()){String key=personalKeys.next();mergedPersonal.put(key,imported.get(key));}}mergedPersonal=PersonalStore.validate(mergedPersonal);
  SharedPreferences fav=app.veyra.ProfileStore.prefs(c,"favorites"),hist=app.veyra.ProfileStore.prefs(c,"watch-history");SharedPreferences.Editor f=fav.edit(),h=hist.edit();JSONArray favorites=safe.getJSONArray("favorites"),history=safe.getJSONArray("history");
  Set<String> identities=new HashSet<>(fav.getAll().keySet());for(int i=0;i<favorites.length();i++)identities.add(LibraryStore.key(favorites.getJSONObject(i)));if(identities.size()>1000)throw new Exception("Merged list exceeds 1000 titles");
  for(int i=0;i<favorites.length();i++){JSONObject o=favorites.getJSONObject(i);f.putString(LibraryStore.key(o),o.toString());}
  Map<String,JSONObject> histories=new HashMap<>();for(Map.Entry<String,?> entry:hist.getAll().entrySet())try{histories.put(entry.getKey(),new JSONObject((String)entry.getValue()));}catch(Exception ignored){}
  for(int i=0;i<history.length();i++){JSONObject o=history.getJSONObject(i);String key=o.getString("provider")+":"+o.getString("kind")+":"+o.getString("id");JSONObject old=histories.get(key);if(old==null||o.optLong("updatedAt")>=old.optLong("updatedAt"))histories.put(key,o);}
  List<String> newest=new ArrayList<>(histories.keySet());newest.sort((x,y)->Long.compare(histories.get(y).optLong("updatedAt"),histories.get(x).optLong("updatedAt")));h.clear();for(int i=0;i<Math.min(100,newest.size());i++){String key=newest.get(i);h.putString(key,histories.get(key).toString());}
  SharedPreferences stored=app.veyra.ProfileStore.prefs(c,"resume");SharedPreferences.Editor positions=stored.edit(),prefs=c.getSharedPreferences("settings",0).edit();JSONObject resume=safe.getJSONObject("resume"),settings=safe.getJSONObject("settings");LinkedHashMap<String,Long> merged=new LinkedHashMap<>();for(Map.Entry<String,?> entry:stored.getAll().entrySet())if(entry.getValue() instanceof Long)merged.put(entry.getKey(),(Long)entry.getValue());Iterator<String> keys=resume.keys();while(keys.hasNext()){String key=keys.next();merged.remove(key);merged.put(key,resume.getLong(key));}while(merged.size()>200)merged.remove(merged.keySet().iterator().next());positions.clear();for(Map.Entry<String,Long> entry:merged.entrySet())positions.putLong(entry.getKey(),entry.getValue());
  keys=settings.keys();while(keys.hasNext()){String key=keys.next();Object value=settings.get(key);if(value instanceof Boolean)prefs.putBoolean(key,(Boolean)value);else if(key.equals("speed"))prefs.putFloat(key,(float)settings.getDouble(key));else prefs.putInt(key,settings.getInt(key));}
  PersonalStore.restore(c,mergedPersonal);f.apply();h.apply();positions.apply();prefs.apply();
 }
}
