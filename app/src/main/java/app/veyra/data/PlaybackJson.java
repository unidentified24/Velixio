package app.veyra.data;
import app.veyra.model.*;
import org.json.*;
import java.util.*;
public final class PlaybackJson {
    private PlaybackJson() {}
    public static PlaybackRequest parse(String text) throws Exception {
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
            sources.add(new StreamSource(s.getString("id"), s.getString("label"), s.getString("url"), s.getString("mimeType"), headers));
        }
        return new PlaybackRequest(root.getString("contentId"), root.getString("title"), sources);
    }
}
