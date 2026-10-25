package app.veyra.data;

import java.io.IOException;
import org.json.*;

/** Resolve an episode by server identity, never by an assumed list position. */
public final class EpisodeSelection {
    private EpisodeSelection() {}
    public static String key(JSONObject episode) throws IOException {
        String collection=episode.optString("collection", "");
        if(!collection.trim().isEmpty()) return collection;
        String id=episode.optString("id", "");
        if(!id.trim().isEmpty()) return "id:"+id;
        throw new IOException("The episode has no stable identity");
    }
    public static JSONObject select(JSONArray episodes,String selected,int offset) throws Exception {
        if(episodes==null || selected==null || selected.isEmpty()) throw new IOException("Episode unavailable");
        int match=-1;
        for(int i=0;i<episodes.length();i++) {
            if(key(episodes.getJSONObject(i)).equals(selected)) {
                if(match!=-1) throw new IOException("The episode identity is ambiguous");
                match=i;
            }
        }
        if(match<0) throw new IOException("The episode is no longer available for this source");
        long target=(long)match+offset;
        if(target<0 || target>=episodes.length()) throw new IOException(offset<0?"This is the first episode":"This is the last episode");
        return episodes.getJSONObject((int)target);
    }
}
