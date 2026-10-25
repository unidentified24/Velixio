package app.veyra.data;
import app.veyra.model.PlaybackRequest;
/** Implement after tracing authenticated catalog/episode/source responses from the existing backend. */
public interface StreamingBackend {
    PlaybackRequest resolve(String contentId, String episodeId) throws Exception;
}
