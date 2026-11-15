package app.veyra.data;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CombinedSearchTest {
    @Test public void verifiedDubUsesItsOwnProviderIdAndAudioType() throws Exception {
        JSONObject english=title("Example","English","HD").put("imdb_id","tt1234567");
        JSONObject hindi=title("Example","Hindi","HD").put("imdb_id","tt1234567")
            .put("_provider","classic").put("id","8").put("audio_type","2");
        JSONObject remake=title("Example","Malayalam","HD").put("imdb_id","tt1234568").put("_provider","classic");
        List<JSONObject> cards=CombinedCatalog.group(List.of(english,hindi,remake));
        assertEquals(2,cards.size());
        JSONArray editions=cards.get(0).getJSONArray("_editions");
        assertEquals(2,editions.length());
        JSONObject dub=editions.getJSONObject(1);
        assertEquals("Hindi",dub.getString("language"));
        assertEquals("8",dub.getString("id"));
        assertEquals("2",dub.getString("audioType"));
        assertEquals(2,CombinedCatalog.group(cards).get(0).getJSONArray("_editions").length());
    }
    @Test public void localizedAndFeaturedChannelsAreIndexed(){
        assertEquals("films",CombinedCatalog.sectionFor("Hindi Movies"));
        assertEquals("films",CombinedCatalog.sectionFor("English Films"));
        assertEquals("discover",CombinedCatalog.sectionFor("Latest"));
        assertEquals("series",CombinedCatalog.sectionFor("Tamil TV Shows"));
        assertEquals("animation",CombinedCatalog.sectionFor("Animation"));
    }
    @Test public void matchingCreditsLinkDubsButKeepSameNameFilmsSeparate() throws Exception {
        JSONObject original=title("Jailer","Tamil","HD").put("vod_director","Nelson Dilipkumar")
            .put("vod_actor","Rajinikanth").put("id","16");
        JSONObject dub=new JSONObject(original.toString()).put("_provider","classic").put("id","17").put("vod_lang","Hindi");
        JSONObject other=title("Jailer","Malayalam","HD").put("_provider","classic")
            .put("vod_director","Sakkir Madathil").put("vod_actor","Dhyan Sreenivasan");
        List<JSONObject> titles=CombinedCatalog.group(List.of(original,dub,other));
        assertEquals(2,titles.size());
        assertEquals(2,titles.get(0).getJSONArray("_editions").length());
    }
    @Test public void anotherSearchBatchIsAvailableAfterRepeatedPages() throws Exception {
        CombinedCatalog.SearchPage result=CombinedCatalog.collectSearch(1,true,page->
            new CombinedCatalog.SearchPage(List.of(title("Jailer","Tamil","HD")),page,true,""));
        assertEquals(4,result.page);
        assertTrue(result.more);
        assertEquals(1,result.titles.size());
    }
    @Test public void switchingFilmSourcesAcceptsProviderSpecificPlaybackIds() throws Exception {
        JSONArray alternate=new JSONArray().put(new JSONObject().put("collection","classic-99"));
        assertEquals("classic-99",EpisodeSelection.key(EpisodeSelection.switchSource(alternate,"current-17",true)));
        assertThrows(IOException.class,()->EpisodeSelection.switchSource(alternate,"current-17",false));
        alternate.put(new JSONObject().put("collection","classic-100"));
        assertThrows(IOException.class,()->EpisodeSelection.switchSource(alternate,"current-17",true));
    }
    @Test public void sourceStatusCountsBothProvidersInAGroupedResult() throws Exception {
        JSONObject current=title("Drishyam","Hindi","HD");
        JSONObject classic=new JSONObject(current.toString()).put("_provider","classic");
        CombinedCatalog.SearchPage result=CombinedCatalog.collectSearch(1,false,page->
            new CombinedCatalog.SearchPage(CombinedCatalog.group(List.of(current,classic)),page,true,
                "Partial response",java.util.Set.of("Current shorts")));
        assertEquals(1,result.titles.size());
        assertEquals(Integer.valueOf(1),result.sourceCounts().get("Current"));
        assertEquals(Integer.valueOf(1),result.sourceCounts().get("Classic"));
        assertTrue(result.failed.contains("Current shorts"));
    }
    @Test public void loadMoreKeepsEarlierTitlesAndFindsMatchesBeyondEmptyAndDuplicatePages() throws Exception {
        JSONObject original=title("Drishyam","Malayalam","HD");
        JSONObject remake=title("Drishyam","Hindi","HD");
        List<Integer> requests=new ArrayList<>();
        CombinedCatalog.SearchPage result=CombinedCatalog.collectSearch(1,true,page->{
            requests.add(page);
            List<JSONObject> titles=page==3?List.of():page==4?List.of(remake):List.of(original);
            return new CombinedCatalog.SearchPage(titles,page,true,"");
        });
        assertEquals(List.of(1,2,3,4),requests);
        assertEquals(2,result.titles.size());
        assertEquals(4,result.page);
    }

    @Test public void sourceFailureKeepsResultsAndAllowsRetry() throws Exception {
        CombinedCatalog.SearchPage result=CombinedCatalog.collectSearch(1,true,page->{
            if(page>1)throw new IOException("Unavailable");
            return new CombinedCatalog.SearchPage(List.of(title("Jailer","Tamil","HD")),page,true,"");
        });
        assertEquals(1,result.titles.size());
        assertTrue(result.more);
        assertFalse(result.warning.isEmpty());
    }

    @Test public void sameProviderIdDoesNotCollapseDifferentLanguageAndQualityEntries() throws Exception {
        JSONArray raw=new JSONArray().put(title("Jailer","Tamil","HD")).put(title("Jailer","Malayalam","HD"))
            .put(title("Jailer","Tamil","CAM")).put(title("Jailer","Tamil","HD"));
        assertEquals(3,CombinedCatalog.group(CatalogBackend.titles(raw)).size());
    }

    private static JSONObject title(String name,String language,String quality) throws Exception {
        return new JSONObject().put("id","1").put("vod_name",name).put("vod_year","2023")
            .put("vod_lang",language).put("quality",quality).put("_provider","current");
    }
}
