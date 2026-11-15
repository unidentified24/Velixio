package app.veyra;

import android.content.Intent;
import app.veyra.data.EpisodeSelection;
import org.json.JSONArray;
import org.json.JSONObject;

final class EpisodeAvailability {
    private EpisodeAvailability() {}

    static Intent apply(Intent intent, JSONObject detail, JSONObject episode) {
        return intent.putExtra("hasPreviousEpisode", hasNeighbor(detail, episode, -1))
            .putExtra("hasNextEpisode", hasNeighbor(detail, episode, 1));
    }

    static boolean hasNeighbor(JSONObject detail, JSONObject episode, int offset) {
        try {
            JSONArray episodes = detail.optJSONArray("vod_collection");
            JSONObject neighbor = EpisodeSelection.select(episodes, EpisodeSelection.key(episode), offset);
            EpisodeSelection.select(episodes, EpisodeSelection.key(neighbor), 0);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
