package app.veyra.model;
import java.net.URI;
import java.util.*;
/** Backend labels are opaque: Sub is not assumed to mean a text track. */
public final class StreamSource {
    public final String id, label, url, mimeType;
    public final Map<String,String> headers;
    public final List<SubtitleTrack> subtitles;
    public StreamSource(String id, String label, String url, String mimeType, Map<String,String> headers) {
        this(id,label,url,mimeType,headers,Collections.emptyList(),false);
    }
    public StreamSource(String id,String label,String url,String mimeType,Map<String,String> headers,List<SubtitleTrack> subtitles) {
        this(id,label,url,mimeType,headers,subtitles,false);
    }
    public static StreamSource localProxy(String id,String label,String url,String mimeType,List<SubtitleTrack> subtitles) {
        return new StreamSource(id,label,url,mimeType,Collections.emptyMap(),subtitles,true);
    }
    public static StreamSource localProxy(String id,String label,String url,String mimeType) {
        return new StreamSource(id,label,url,mimeType,Collections.emptyMap(),Collections.emptyList(),true);
    }
    private StreamSource(String id,String label,String url,String mimeType,Map<String,String> headers,List<SubtitleTrack> subtitles,boolean local) {
        this.id = required(id); this.label = required(label); this.url = required(url);
        URI uri = URI.create(url);
        if (!("https".equalsIgnoreCase(uri.getScheme()) || (local && "http".equals(uri.getScheme()) && "127.0.0.1".equals(uri.getHost()) && uri.getPort()>0 && ("/resource.m3u8".equals(uri.getPath()) || "/resource.mp4".equals(uri.getPath())))) || uri.getHost() == null
                || uri.getRawUserInfo() != null || uri.getRawFragment() != null)
            throw new IllegalArgumentException("A valid HTTPS stream URL is required.");
        this.mimeType = required(mimeType);
        if (!mimeType.equals("application/x-mpegURL") && !mimeType.equals("application/dash+xml") && !mimeType.equals("video/mp4"))
            throw new IllegalArgumentException("Supported formats: HLS, DASH and MP4.");
        Map<String,String> copy = new LinkedHashMap<>();
        if(headers==null || headers.size()>32)throw new IllegalArgumentException("Too many stream headers.");
        int headerBytes=0;
        for (Map.Entry<String,String> e : headers.entrySet()) {
            String key = required(e.getKey()), value = required(e.getValue());
            if (!key.matches("[!#$%&'*+.^_`|~0-9A-Za-z-]+") || value.contains("\r") || value.contains("\n"))
                throw new IllegalArgumentException("Invalid stream header.");
            if(Arrays.asList("host","content-length","transfer-encoding","connection","proxy-authorization").contains(key.toLowerCase(Locale.ROOT)))throw new IllegalArgumentException("Unsafe stream header.");
            headerBytes+=key.length()+value.length();
            if(headerBytes>16384 || value.chars().anyMatch(c->c<32 || c>126))throw new IllegalArgumentException("Invalid stream header value.");
            copy.put(key, value);
        }
        this.headers = Collections.unmodifiableMap(copy);
        if(subtitles==null || subtitles.size()>20)throw new IllegalArgumentException("Too many caption tracks");
        Set<String> subtitleIds=new HashSet<>();for(SubtitleTrack subtitle:subtitles)if(subtitle==null || !subtitleIds.add(subtitle.id))throw new IllegalArgumentException("Invalid caption tracks");
        this.subtitles=Collections.unmodifiableList(new ArrayList<>(subtitles));
    }
    public static String required(String value) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException("Missing value.");
        return value; // Preserve signed URL bytes and opaque identifiers.
    }
}
