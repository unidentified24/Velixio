package app.veyra;
import org.junit.*;import org.json.*;import static org.junit.Assert.*;
public class AppUpdatesTest {
 @Test public void acceptsBrandedAndNumberedTags(){assertEquals("1.0.2",AppUpdates.normalized("VelixioStream1.0.2"));assertTrue(AppUpdates.compare("1.0.2-3","1.0.2-2")>0);assertTrue(AppUpdates.compare("1.0.3","1.0.2-3")>0);assertTrue(AppUpdates.compare("1.0.2","1.0.1-rc2")>0);}
 @Test public void fallsBackToReleaseTitle()throws Exception{AppUpdates.Release r=AppUpdates.select(new JSONArray().put(release("release",url,hash).put("name","Velixio 1.0.3")),"1.0.2");assertEquals("1.0.3",r.version);}
 private JSONObject release(String tag,String url,String digest)throws Exception{return new JSONObject().put("tag_name",tag).put("assets",new JSONArray().put(new JSONObject().put("name","Velixio.apk").put("browser_download_url",url).put("digest",digest).put("size",16000000)));}
 private String url="https://github.com/unidentified24/Velixio/releases/download/v1.0.0/Velixio.apk",hash="sha256:"+"a".repeat(64);
 @Test public void releaseOrderTreatsFinalAsNewerThanCandidate(){assertTrue(AppUpdates.compare("v1.0.0","1.0.0-rc2")>0);assertTrue(AppUpdates.compare("1.0.0-rc10","1.0.0-rc2")>0);assertTrue(AppUpdates.compare("v0.12.0","1.0.0-rc1")<0);assertTrue(AppUpdates.compare("garbage","1.0.0")<0);}
 @Test public void nextPatchFollowsPublishedStable(){assertTrue(AppUpdates.compare("1.0.1-rc1","1.0.0")>0);assertTrue(AppUpdates.compare("1.0.1","1.0.1-rc1")>0);}
 @Test public void selectsNewestVersionRatherThanUploadOrder()throws Exception{AppUpdates.Release r=AppUpdates.select(new JSONArray().put(release("v0.9.0",url,hash)).put(release("v1.1.0",url,hash)).put(release("v1.0.0",url,hash)),"1.0.1-rc1");assertEquals("1.1.0",r.version);}
 @Test public void rejectsMissingDigestForeignRepoAndDrafts()throws Exception{JSONArray list=new JSONArray().put(release("v2.0.0",url,"")).put(release("v2.0.0","https://github.com/other/app/releases/download/v2/a.apk",hash)).put(release("v2.0.0",url,hash).put("draft",true));assertNull(AppUpdates.select(list,"1.0.0"));}
 @Test public void stableInstallDoesNotReceivePrereleases()throws Exception{assertNull(AppUpdates.select(new JSONArray().put(release("v2.0.0-rc1",url,hash).put("prerelease",true)),"1.0.0"));}
}
