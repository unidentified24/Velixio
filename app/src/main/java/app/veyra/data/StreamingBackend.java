package app.veyra.data;
import org.json.JSONArray;
import org.json.JSONObject;
/** UI-independent catalog and playback boundary; implementations preserve provider identities. */
public interface StreamingBackend {
    JSONArray channels() throws Exception;
    JSONArray catalog(String channel,int page) throws Exception;
    Object search(String text,int page) throws Exception;
    JSONObject detail(String id,String audioType,String kind) throws Exception;
    String playbackJson(JSONObject detail,JSONObject episode,String sourceLabel) throws Exception;
}
