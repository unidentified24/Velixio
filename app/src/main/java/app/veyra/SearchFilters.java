package app.veyra;
import android.content.*;import org.json.*;import java.util.*;
final class SearchFilters {
 static final class Criteria {final int from,to,rating;final String genre,language,kind;Criteria(int f,int t,int r,String g,String l,String k){if(f<0||t<0||(t>0&&f>t)||r<0||r>10||g.length()>80||l.length()>80||!Arrays.asList("all","movie","short").contains(k))throw new IllegalArgumentException("Check the year range and rating");from=f;to=t;rating=r;genre=g.trim().toLowerCase(Locale.ROOT);language=l.trim().toLowerCase(Locale.ROOT);kind=k;}}
 static Criteria read(Context c){SharedPreferences p=ProfileStore.prefs(c,"search-filters");try{return new Criteria(p.getInt("from",0),p.getInt("to",0),p.getInt("rating",0),p.getString("genre",""),p.getString("language",""),p.getString("kind","all"));}catch(Exception e){return new Criteria(0,0,0,"","","all");}}
 static void save(Context c,Criteria f){ProfileStore.prefs(c,"search-filters").edit().putInt("from",f.from).putInt("to",f.to).putInt("rating",f.rating).putString("genre",f.genre).putString("language",f.language).putString("kind",f.kind).apply();}
 static boolean active(Context c){Criteria f=read(c);return qualityMode(c)!=0||f.from>0||f.to>0||f.rating>0||!f.genre.isEmpty()||!f.language.isEmpty()||!f.kind.equals("all");}
 static int qualityMode(Context c){
  SharedPreferences prefs=ProfileStore.prefs(c,"search-filters");
  int mode=prefs.getInt("qualityMode",prefs.getBoolean("hdOnly",false)?1:0);
  return mode>=0&&mode<=2?mode:0;
 }
 static void qualityMode(Context c,int mode){ProfileStore.prefs(c,"search-filters").edit().remove("hdOnly").putInt("qualityMode",mode).apply();}
 static boolean isCam(String quality){
  return quality.toUpperCase(Locale.ROOT).matches(".*(?:CAM|TELESYNC|TELECINE|\\b(?:HD[\\s-]*)?(?:TS|TC)\\b).*");
 }
 static boolean isHd(String quality){
  String value=quality.trim().toUpperCase(Locale.ROOT);
  return !isCam(value)
      && value.matches(".*(?:720P|1080[PI]|1440P|2160P|4K|UHD|FHD|\\bHD\\b).*");
 }
 static boolean matchesVersion(JSONObject item,String language,boolean hd){
  return matchesVersion(item,language,hd?1:0);
 }
 private static boolean qualityMatches(String quality,int mode){
  return mode==0||mode==1&&isHd(quality)||mode==2&&isCam(quality);
 }
 static boolean matchesVersion(JSONObject item,String language,int mode){
  if(qualityMatches(app.veyra.data.CombinedCatalog.quality(item),mode)
      &&app.veyra.data.CombinedCatalog.language(item).toLowerCase(Locale.ROOT).contains(language))return true;
  JSONArray editions=item.optJSONArray("_editions");
  if(editions!=null)for(int i=0;i<editions.length();i++){
   JSONObject edition=editions.optJSONObject(i);
   if(edition!=null&&qualityMatches(edition.optString("quality"),mode)
       &&edition.optString("language").toLowerCase(Locale.ROOT).contains(language))return true;
  }
  return false;
 }
 static String genres(JSONObject t){return (t.optString("vod_class")+" "+t.optString("vod_genre")+" "+t.optString("genre")).toLowerCase(Locale.ROOT);}
 static List<JSONObject> apply(Context c,List<JSONObject> titles){Criteria f=read(c);int mode=qualityMode(c);List<JSONObject> out=new ArrayList<>();for(JSONObject t:titles){if(!matchesVersion(t,f.language,mode))continue;int year=CatalogDates.releaseYear(t);double rating=t.optDouble("vod_douban_score",0);if(f.from>0&&year<f.from||f.to>0&&(year==0||year>f.to)||f.rating>0&&(!Double.isFinite(rating)||rating<f.rating||rating>10))continue;if(!f.genre.isEmpty()&&!genres(t).contains(f.genre))continue;if(!f.kind.equals("all")&&!f.kind.equals(t.optString("_kind","movie")))continue;out.add(t);}return out;}
}
