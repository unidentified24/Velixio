package app.veyra.data;
import org.json.JSONArray;
import org.json.JSONObject;
/** UI-independent catalog and playback boundary; implementations preserve provider identities. */
public interface StreamingBackend {
    JSONArray channels() throws Exception;
    JSONArray catalog(String channel,int page) throws Exception;
    Object search(String text,int page) throws Exception;
    default boolean channelScopedSearch(){return false;}
    default Object searchInChannel(String channel,String text,int page)throws Exception{
        throw new UnsupportedOperationException("No channel-scoped search");
    }
    default Object shortCatalog(String query,int page) throws Exception {
        throw new UnsupportedOperationException("No separate shorts catalog");
    }
    JSONObject detail(String id,String audioType,String kind) throws Exception;
    String playbackJson(JSONObject detail,JSONObject episode,String sourceLabel) throws Exception;
}
