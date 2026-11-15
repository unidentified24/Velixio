package app.veyra;

import org.json.JSONObject;
import java.util.Locale;

final class TitleMetadata {
    private TitleMetadata(){}
    static String originalTitle(JSONObject title){
        String original=title.optString("original_title").trim();
        String display=title.optString("vod_name").trim();
        return original.equalsIgnoreCase(display)?"":original;
    }
    static String duration(JSONObject episode){
        long minutes=episode.optLong("runtime_minutes");
        return minutes>0&&minutes<=1440?String.format(Locale.ROOT,"%d min",minutes):"";
    }
    static long[] skipRange(JSONObject episode,String kind){
        org.json.JSONObject marker=episode.optJSONObject(kind);
        if(marker==null)return null;
        long start=marker.optLong("start_ms",-1),end=marker.optLong("end_ms",-1);
        return start>=0&&end>start&&end<=24L*60*60*1000?new long[]{start,end}:null;
    }
}
