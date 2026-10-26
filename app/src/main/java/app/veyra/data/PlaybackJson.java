package app.veyra.data;
import app.veyra.model.*;
import org.json.*;
import java.util.*;
public final class PlaybackJson {
    private PlaybackJson() {}
    public static PlaybackRequest parse(String text) throws Exception { return parse(text,false); }
    public static PlaybackRequest parseInternal(String text) throws Exception { return parse(text,true); }
    private static PlaybackRequest parse(String text,boolean internal) throws Exception {
        if(text==null || text.length()>65536)throw new IllegalArgumentException("Playback data exceeds size limit.");
        JSONObject root = new JSONObject(text);
        if (root.getInt("schemaVersion") != 1) throw new IllegalArgumentException("Unsupported schema.");
        JSONArray entries = root.getJSONArray("sources");
        if (entries.length() > 30) throw new IllegalArgumentException("Too many sources.");
        List<StreamSource> sources = new ArrayList<>();
        for (int i=0; i<entries.length(); i++) {
            JSONObject s = entries.getJSONObject(i);
            Map<String,String> headers = new LinkedHashMap<>();
            JSONObject values = s.optJSONObject("headers");
            if (values != null) {
                Iterator<String> keys = values.keys();
                while (keys.hasNext()) { String key = keys.next(); headers.put(key, values.getString(key)); }
            }
            List<SubtitleTrack> subtitles=new ArrayList<>();JSONArray captions=s.optJSONArray("subtitles");
            if(captions!=null) {
                if(captions.length()>20)throw new IllegalArgumentException("Too many caption tracks");
                for(int n=0;n<captions.length();n++){JSONObject caption=captions.getJSONObject(n);subtitles.add(new SubtitleTrack(caption.getString("id"),caption.getString("label"),caption.getString("url"),caption.getString("mimeType"),caption.optString("language")));}
            }
            if (s.optBoolean("localProxy")) {
                if (!internal || !CatalogBackend.isActiveProxy(s.getString("url"))) throw new IllegalArgumentException("Inactive local streaming adapter");
                sources.add(StreamSource.localProxy(s.getString("id"),s.getString("label"),s.getString("url"),s.getString("mimeType"),subtitles));
            } else sources.add(new StreamSource(s.getString("id"), s.getString("label"), s.getString("url"), s.getString("mimeType"), headers,subtitles));
        }
        return new PlaybackRequest(root.getString("contentId"), root.getString("title"), sources);
    }
}
