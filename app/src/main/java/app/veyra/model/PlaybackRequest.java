package app.veyra.model;
import java.util.*;
public final class PlaybackRequest {
    public final String contentId, title;
    public final List<StreamSource> sources;
    public PlaybackRequest(String contentId, String title, List<StreamSource> sources) {
        this.contentId = StreamSource.required(contentId); this.title = StreamSource.required(title);
        if (sources.isEmpty()) throw new IllegalArgumentException("At least one source is required.");
        Set<String> ids = new HashSet<>();
        for (StreamSource source : sources)
            if (!ids.add(source.id)) throw new IllegalArgumentException("Duplicate source ID.");
        this.sources = Collections.unmodifiableList(new ArrayList<>(sources));
    }
}
