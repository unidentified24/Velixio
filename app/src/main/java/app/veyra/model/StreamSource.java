package app.veyra.model;
import java.net.URI;
import java.util.*;
/** Backend labels are opaque: Sub is not assumed to mean a text track. */
public final class StreamSource {
    public final String id, label, url, mimeType;
    public final Map<String,String> headers;
    public StreamSource(String id, String label, String url, String mimeType, Map<String,String> headers) {
        this.id = required(id); this.label = required(label); this.url = required(url);
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getRawFragment() != null)
            throw new IllegalArgumentException("A valid HTTPS stream URL is required.");
        this.mimeType = required(mimeType);
        if (!mimeType.equals("application/x-mpegURL") && !mimeType.equals("application/dash+xml") && !mimeType.equals("video/mp4"))
            throw new IllegalArgumentException("Supported formats: HLS, DASH and MP4.");
        Map<String,String> copy = new LinkedHashMap<>();
        for (Map.Entry<String,String> e : headers.entrySet()) {
            String key = required(e.getKey()), value = required(e.getValue());
            if (!key.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+") || value.contains("\r") || value.contains("\n"))
                throw new IllegalArgumentException("Invalid stream header.");
            copy.put(key, value);
        }
        this.headers = Collections.unmodifiableMap(copy);
    }
    public static String required(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing value.");
        return value; // Preserve signed URL bytes and opaque identifiers.
    }
}
