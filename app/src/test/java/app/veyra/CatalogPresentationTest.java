package app.veyra;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.*;

public class CatalogPresentationTest {
    @Test public void languageAndHdMustMatchTheSameVersion() throws Exception {
        JSONObject title=new JSONObject().put("vod_lang","English").put("quality","HD")
            .put("_editions",new org.json.JSONArray().put(new JSONObject().put("language","Hindi").put("quality","CAM")));
        assertTrue(SearchFilters.matchesVersion(title,"hindi",false));
        assertFalse(SearchFilters.matchesVersion(title,"hindi",true));
        assertTrue(SearchFilters.matchesVersion(title,"english",true));
        assertTrue(SearchFilters.matchesVersion(title,"hindi",2));
        assertFalse(SearchFilters.matchesVersion(title,"english",2));
        assertTrue(SearchFilters.matchesVersion(title,"hindi",0));
    }
    @Test public void hdFilterExcludesCameraRecordingsAndUnknownQuality(){
        assertTrue(SearchFilters.isHd("HD"));
        assertTrue(SearchFilters.isHd("1080p WEB-DL"));
        assertFalse(SearchFilters.isHd("1080p HDCAM"));
        assertFalse(SearchFilters.isHd("1080p HDTS"));
        assertFalse(SearchFilters.isHd("HD-TC"));
        assertFalse(SearchFilters.isHd("1080p HD TC"));
        assertFalse(SearchFilters.isHd(""));
    }
    @Test public void additionDateDoesNotUseReleaseYear() throws Exception {
        JSONObject title=new JSONObject().put("vod_year",2001).put("added_at","2026-11-13T23:00:00Z");
        assertEquals(2001,CatalogDates.releaseYear(title));
        assertEquals("2026-11-13",CatalogDates.added(title));
        title.remove("added_at");
        assertNull(CatalogDates.added(title));
    }
    @Test public void episodeMarkersRequireExplicitOrderedTimestamps() throws Exception {
        JSONObject episode=new JSONObject().put("intro",new JSONObject().put("start_ms",1000).put("end_ms",30000))
            .put("recap",new JSONObject().put("start_ms",5000).put("end_ms",2000));
        assertArrayEquals(new long[]{1000,30000},TitleMetadata.skipRange(episode,"intro"));
        assertNull(TitleMetadata.skipRange(episode,"recap"));
        assertNull(TitleMetadata.skipRange(episode,"credits"));
    }
}
