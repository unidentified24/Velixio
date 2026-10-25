import app.veyra.model.*;
import java.util.*;
public final class ContractTest {
    static int checks;
    static void check(boolean value) { if(!value) throw new AssertionError(); checks++; }
    static void rejects(Runnable call) { try { call.run(); } catch(IllegalArgumentException e) { checks++; return; } throw new AssertionError("Expected rejection"); }
    public static void main(String[] args) {
        String url="https://media.example.invalid/a%2Fb/master.m3u8?token=A%2BB%3D&x=1&x=2";
        Map<String,String> headers=new LinkedHashMap<>(); headers.put("Referer","https://app.example.invalid/");
        StreamSource original=new StreamSource("opaque:01","Original",url,"application/x-mpegURL",headers);
        check(original.url.equals(url)); check(original.headers.get("Referer").equals(headers.get("Referer")));
        headers.clear(); check(original.headers.size()==1);
        StreamSource sub=new StreamSource("opaque:02","Sub",url,"application/x-mpegURL",Map.of());
        List<StreamSource> inputs=new ArrayList<>(List.of(original,sub));
        PlaybackRequest request=new PlaybackRequest("episode:23","Example",inputs); inputs.clear();
        check(request.sources.size()==2); check(request.sources.get(1).label.equals("Sub"));
        rejects(()->new PlaybackRequest("x","x",List.of(original,original)));
        rejects(()->new PlaybackRequest("x","x",List.of()));
        rejects(()->new StreamSource("x","x","http://media.example.invalid/a","video/mp4",Map.of()));
        rejects(()->new StreamSource("x","x","https://u:p@media.example.invalid/a","video/mp4",Map.of()));
        rejects(()->new StreamSource("x","x",url,"video/mp4",Map.of("X-Test","a\r\nInjected: b")));
        rejects(()->new StreamSource("x","x",url,"text/html",Map.of()));
        System.out.println(checks+" contract checks passed.");
    }
}
