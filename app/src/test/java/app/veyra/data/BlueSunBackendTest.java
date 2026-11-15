package app.veyra.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class BlueSunBackendTest {
    @Test public void scopedSearchUsesAdvertisedIdsAndSkipsBrowseOnlySections()throws Exception{
        List<String> searched=new ArrayList<>();
        BlueSunBackend backend=new BlueSunBackend((path,params)->{
            if(path.equals("/channels"))return new JSONObject().put("items",new JSONArray()
                .put(new JSONObject().put("id","b17").put("channel_name","Bollywood"))
                .put(new JSONObject().put("id","h29").put("channel_name","Hollywood"))
                .put(new JSONObject().put("id","s34").put("channel_name","Series"))
                .put(new JSONObject().put("id","live8").put("searchable",false)));
            assertEquals("/search",path);
            assertEquals("channel",params[0]);
            searched.add(params[1]);
            assertEquals("drishyam",params[3]);
            assertEquals("2",params[5]);
            return new JSONObject().put("items",new JSONArray());
        },true);
        for(CombinedCatalog.Route route:CombinedCatalog.searchRoutes("bluesun",backend)){
            backend.searchInChannel(route.id,"drishyam",2);
        }
        assertEquals(Arrays.asList("b17","h29","s34"),searched);
    }
    @Test public void authorizedGatewayProvidesExclusiveTitlesAndPreservesDubPlayback()throws Exception{
        JSONObject episode=new JSONObject().put("collection","blue-film-episode");
        JSONObject dub=new JSONObject().put("id","blue-film").put("vod_name","Example").put("vod_year","2025")
            .put("imdb_id","tt1234567").put("vod_lang","Hindi").put("audio_type","hindi")
            .put("vod_collection",new JSONArray().put(episode));
        JSONObject exclusive=new JSONObject().put("id","blue-exclusive").put("vod_name","Another film");
        String stream="https://example.invalid/movie.m3u8?sig=A%2BB%2FZ";
        JSONObject source=new JSONObject().put("id","hindi").put("label","Hindi").put("url",stream)
            .put("mimeType","application/x-mpegURL").put("headers",new JSONObject().put("Referer","https://example.invalid/"));
        List<List<String>> calls=new ArrayList<>();
        BlueSunBackend backend=new BlueSunBackend((path,parameters)->{
            List<String> call=new ArrayList<>();call.add(path);call.addAll(Arrays.asList(parameters));calls.add(call);
            switch(path){
                case "/channels": return new JSONObject().put("items",new JSONArray().put(new JSONObject().put("id","hindi")));
                case "/catalog":
                case "/search": return new JSONObject().put("items",new JSONArray().put(dub).put(exclusive));
                case "/detail": return dub;
                case "/playback": return new JSONObject().put("id","blue-film").put("episode","blue-film-episode")
                    .put("audioType","hindi").put("sources",new JSONArray().put(source));
                default: throw new IOException(path);
            }
        });
        assertEquals("hindi",backend.channels().getJSONObject(0).getString("id"));
        assertEquals(2,backend.catalog("hindi",2).length());
        JSONObject original=new JSONObject().put("id","current-film").put("_provider","current").put("vod_name","Example")
            .put("imdb_id","tt1234567").put("vod_lang","English");
        List<JSONObject> results=CatalogBackend.titles(backend.search("Example",3));
        for(JSONObject item:results)item.put("_provider","bluesun");
        results.add(0,original);
        List<JSONObject> merged=CombinedCatalog.group(results);
        assertEquals(2,merged.size());
        JSONObject choice=merged.get(0).getJSONArray("_editions").getJSONObject(1);
        assertEquals("Hindi",choice.getString("language"));
        assertEquals("blue-exclusive",merged.get(1).getString("id"));
        JSONObject detail=backend.detail(choice.getString("id"),choice.getString("audioType"),"movie");
        String playback=backend.playbackJson(detail,episode,"Hindi");
        assertEquals(stream,PlaybackJson.parse(playback).sources.get(0).url);
        assertEquals("https://example.invalid/",PlaybackJson.parse(playback).sources.get(0).headers.get("Referer"));
        assertEquals(Arrays.asList("/search","q","Example","page","3"),calls.get(2));
        assertEquals(Arrays.asList("/playback","id","blue-film","episode","blue-film-episode","audioType","hindi"),calls.get(4));
    }

    @Test public void rejectsPlaybackForAnUnexpectedTitleOrLanguage()throws Exception{
        BlueSunBackend backend=new BlueSunBackend((path,params)->new JSONObject().put("id","expected")
            .put("episode","episode-1").put("audioType","different"));
        JSONObject detail=new JSONObject().put("id","expected").put("audio_type","hindi").put("vod_name","Film");
        assertThrows(IOException.class,()->backend.playbackJson(detail,new JSONObject().put("collection","episode-1"),"Hindi"));
    }
}
