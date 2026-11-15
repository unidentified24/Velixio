package app.veyra;
import java.net.URI;import org.json.JSONObject;
final class TrailerSource {
 static String url(JSONObject detail){String value=detail.optString("trailer_url");try{URI uri=URI.create(value);return "https".equalsIgnoreCase(uri.getScheme())&&uri.getHost()!=null&&uri.getRawUserInfo()==null&&uri.getRawFragment()==null?value:"";}catch(Exception ignored){return "";}}
}
