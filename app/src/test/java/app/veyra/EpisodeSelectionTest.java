package app.veyra;
import app.veyra.data.EpisodeSelection;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.IOException;

public class EpisodeSelectionTest {
    private JSONObject episode(int id,String collection) throws Exception {
        JSONObject episode=new JSONObject().put("id",id);
        if(collection!=null)episode.put("collection",collection);
        return episode;
    }
    @Test public void refreshFindsCollectionAfterServerReordersEpisodes() throws Exception {
        JSONArray episodes=new JSONArray().put(episode(22,"2")).put(episode(11,"1"));
        assertEquals(11,EpisodeSelection.select(episodes,"1",0).getInt("id"));
    }
    @Test public void nextAndPreviousUseServerOrderWithoutChangingIdentity() throws Exception {
        JSONArray episodes=new JSONArray().put(episode(11,"1")).put(episode(22,"Special")).put(episode(33,"2"));
        assertEquals(22,EpisodeSelection.select(episodes,"1",1).getInt("id"));
        assertEquals(11,EpisodeSelection.select(episodes,"Special",-1).getInt("id"));
    }
    @Test public void shortEpisodesWithoutCollectionRemainDistinct() throws Exception {
        JSONArray episodes=new JSONArray().put(episode(91,null)).put(episode(92,null));
        assertEquals("id:91",EpisodeSelection.key(episodes.getJSONObject(0)));
        assertEquals(92,EpisodeSelection.select(episodes,"id:91",1).getInt("id"));
    }
    @Test public void sourceChangeMayChangeIdButKeepCollection() throws Exception {
        JSONArray episodes=new JSONArray().put(episode(999,"01"));
        assertEquals(999,EpisodeSelection.select(episodes,"01",0).getInt("id"));
        assertThrows(IOException.class,()->EpisodeSelection.select(episodes,"1",0));
    }
    @Test public void missingAndDuplicateIdentitiesNeverSelectAnArbitraryEpisode() throws Exception {
        JSONArray episodes=new JSONArray().put(episode(11,"1")).put(episode(22,"1"));
        assertThrows(IOException.class,()->EpisodeSelection.select(episodes,"1",0));
        assertThrows(IOException.class,()->EpisodeSelection.select(episodes,"7",0));
        assertThrows(IOException.class,()->EpisodeSelection.key(new JSONObject()));
    }
    @Test public void navigationDoesNotWrapOrOverflow() throws Exception {
        JSONArray episodes=new JSONArray().put(episode(11,"1")).put(episode(22,"2"));
        assertThrows(IOException.class,()->EpisodeSelection.select(episodes,"1",-1));
        assertThrows(IOException.class,()->EpisodeSelection.select(episodes,"2",1));
        assertThrows(IOException.class,()->EpisodeSelection.select(episodes,"2",Integer.MAX_VALUE));
    }
}
