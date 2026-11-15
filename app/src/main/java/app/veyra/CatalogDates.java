package app.veyra;

import org.json.JSONObject;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

final class CatalogDates {
    private CatalogDates(){}
    static String date(JSONObject title,String key){
        String value=title.optString(key).trim();
        if(value.isEmpty())return null;
        SimpleDateFormat format=format();
        if(value.matches("[0-9]{10}|[0-9]{13}")){
            long epoch=Long.parseLong(value);
            return format.format(new Date(value.length()==10?epoch*1000:epoch));
        }
        if(!value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}(?:[T ].*)?"))return null;
        String day=value.substring(0,10);
        return format.parse(day,new ParsePosition(0))==null?null:day;
    }
    static String today(){return format().format(new Date());}
    static String added(JSONObject title){return date(title,"added_at");}
    static int releaseYear(JSONObject title){
        String date=date(title,"release_date");
        return date==null?title.optInt("vod_year",0):Integer.parseInt(date.substring(0,4));
    }
    private static SimpleDateFormat format(){
        SimpleDateFormat date=new SimpleDateFormat("yyyy-MM-dd",Locale.US);
        date.setLenient(false);
        date.setTimeZone(TimeZone.getTimeZone("UTC"));
        return date;
    }
}
