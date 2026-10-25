package app.veyra.model;
import java.net.URI;

public final class SubtitleTrack {
    public final String id,label,url,mimeType,language;
    public SubtitleTrack(String id,String label,String url,String mimeType,String language) {
        this.id=StreamSource.required(id);this.label=StreamSource.required(label);this.url=StreamSource.required(url);this.mimeType=StreamSource.required(mimeType);
        URI uri=URI.create(url);
        if(!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost()==null || uri.getRawUserInfo()!=null || uri.getRawFragment()!=null)throw new IllegalArgumentException("A valid HTTPS caption URL is required");
        if(!mimeType.equals("application/x-subrip") && !mimeType.equals("text/vtt") && !mimeType.equals("application/ttml+xml"))throw new IllegalArgumentException("Unsupported caption format");
        this.language=language==null?"":language;
    }
}
