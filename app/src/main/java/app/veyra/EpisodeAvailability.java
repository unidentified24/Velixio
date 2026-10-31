package app.veyra;
import org.json.*;import app.veyra.data.EpisodeSelection;
final class EpisodeAvailability {static boolean hasNext(JSONObject detail,JSONObject episode){try{EpisodeSelection.select(detail.optJSONArray("vod_collection"),EpisodeSelection.key(episode),1);return true;}catch(Exception ignored){return false;}}}
