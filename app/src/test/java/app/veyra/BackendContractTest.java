package app.veyra;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
import app.veyra.data.*;
import app.veyra.model.*;
import java.util.*;

public class BackendContractTest {
    @Test public void catalogFlattensSectionsAndDeduplicatesIdsWithoutTreatingAdsAsTitles() throws Exception {
        JSONObject first=new JSONObject().put("id",23).put("vod_name","First");
        JSONObject second=new JSONObject().put("id",24).put("vod_name","Second");
        JSONArray sections=new JSONArray().put(new JSONObject().put("block_list",new JSONArray().put(new JSONObject().put("vod_info",first)).put(new JSONObject().put("jump_url","https://ads.example.invalid/")))).put(new JSONObject().put("vod_list",new JSONArray().put(second).put(first)));
        List<JSONObject> titles=LegacyBackend.titles(sections);
        assertEquals(2,titles.size());assertEquals(23,titles.get(0).getInt("id"));assertEquals(24,titles.get(1).getInt("id"));
    }
    @Test public void combinedCatalogRetainsUniqueTitlesAndBothEditions() throws Exception {
        JSONObject a=new JSONObject().put("id",23).put("vod_name","Shared").put("vod_year","2024").put("_provider","current");
        JSONObject b=new JSONObject(a.toString()).put("_provider","classic");
        JSONObject c=new JSONObject().put("id",24).put("vod_name","Classic only").put("_provider","classic");
        List<JSONObject> grouped=CombinedCatalog.group(Arrays.asList(a,b,c));
        assertEquals(2,grouped.size());assertEquals(2,grouped.get(0).getJSONArray("_editions").length());
        assertEquals("classic",grouped.get(1).getJSONArray("_editions").getJSONObject(0).getString("provider"));
    }
    @Test public void sameIdWithDifferentTitleIsNotCollapsed() throws Exception {
        JSONObject a=new JSONObject().put("id",23).put("vod_name","First").put("_provider","current");
        JSONObject b=new JSONObject().put("id",23).put("vod_name","Second").put("_provider","classic");
        assertEquals(2,CombinedCatalog.group(Arrays.asList(a,b)).size());
    }
    @Test public void shortDramaIdsDoNotCollideWithMovieIds() throws Exception {
        JSONObject movie=new JSONObject().put("id",23).put("vod_name","Title").put("_provider","current");
        JSONObject shortDrama=LegacyBackend.normalizeShort(new JSONObject().put("id",23).put("title","Title").put("cover","https://example.invalid/cover.jpg"));shortDrama.put("_provider","current");
        assertEquals(2,CombinedCatalog.group(Arrays.asList(movie,shortDrama)).size());assertEquals("short",shortDrama.getString("_kind"));
    }
    @Test public void deeplyNestedInputIsBounded() throws Exception {
        JSONObject value=new JSONObject().put("id",1).put("vod_name","Too deep");
        for(int i=0;i<20;i++)value=new JSONObject().put("nested",value);
        assertTrue(LegacyBackend.titles(value).isEmpty());
    }
    @Test public void importedPlaybackPreservesSignedUrlAndOpaqueSourceLabels() throws Exception {
        String url="https://cdn.example.invalid/a%2Fb.m3u8?x=A%2BB&x=2";
        JSONObject entry=new JSONObject().put("id","a").put("label","Sub").put("url",url).put("mimeType","application/x-mpegURL").put("headers",new JSONObject().put("Referer","https://example.invalid/"));
        JSONObject root=new JSONObject().put("schemaVersion",1).put("contentId","23:4").put("title","Example").put("sources",new JSONArray().put(entry));
        PlaybackRequest request=PlaybackJson.parse(root.toString());assertEquals(url,request.sources.get(0).url);assertEquals("Sub",request.sources.get(0).label);
    }
    @Test public void importedPlaybackCannotOptIntoLocalProxyEvenWithFlag() throws Exception {
        JSONObject entry=new JSONObject().put("id","a").put("label","Original").put("url","http://127.0.0.1:12345/resource.m3u8?src=x").put("mimeType","application/x-mpegURL").put("localProxy",true);
        JSONObject root=new JSONObject().put("schemaVersion",1).put("contentId","23").put("title","Example").put("sources",new JSONArray().put(entry));
        assertThrows(IllegalArgumentException.class,()->PlaybackJson.parse(root.toString()));
    }
    @Test public void oversizedInputIsRejectedBeforeParsing() {assertThrows(IllegalArgumentException.class,()->PlaybackJson.parse("x".repeat(65537)));}
}
