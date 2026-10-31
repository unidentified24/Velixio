package app.veyra;
import android.content.*;import org.json.*;import java.util.*;
final class DiscoveryPreferences {
 static final class Model {final Set<String> genres=new LinkedHashSet<>(),seen=new HashSet<>();final boolean avoid;
  Model(Context c){SharedPreferences p=ProfileStore.prefs(c,"discovery-prefs");avoid=p.getBoolean("avoidWatched",true);for(String genre:p.getString("genres","").split(",")){genre=genre.trim().toLowerCase(Locale.ROOT);if(!genre.isEmpty()&&genre.length()<=40&&genres.size()<12)genres.add(genre);}for(JSONObject saved:LibraryStore.list(c)){String text=SearchFilters.genres(saved);for(String genre:text.split("[,/|;、]"))if(!genre.trim().isEmpty()&&genres.size()<24)genres.add(genre.trim());}for(String key:ProfileStore.prefs(c,"watch-history").getAll().keySet())seen.add(key);for(Map.Entry<String,?> entry:PersonalStore.prefs(c).getAll().entrySet())if(entry.getKey().startsWith("watched:")&&Boolean.TRUE.equals(entry.getValue()))seen.add(entry.getKey().substring(8));}
  double score(JSONObject t){double score=0;String text=SearchFilters.genres(t);for(String genre:genres)if(text.contains(genre))score+=3;if(avoid&&seen.contains(LibraryStore.key(t)))score-=5;return score;}
 }
}
