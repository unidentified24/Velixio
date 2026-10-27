package app.veyra;
import android.content.Context;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class VelixioDataTest {
 private Context c;
 @Before public void setup(){c=RuntimeEnvironment.getApplication();for(String name:new String[]{"favorites","watch-history","resume","settings","session"})c.getSharedPreferences(name,0).edit().clear().commit();}
 @Test public void importsEarlierBackupFormat()throws Exception{LibraryStore.toggle(c,title());JSONObject old=VelixioData.export(c).put("format","zunodata");assertEquals(1,VelixioData.validate(old.toString()).getJSONArray("favorites").length());assertEquals("velixiodata",VelixioData.export(c).getString("format"));}
 private JSONObject title()throws Exception{return new JSONObject().put("id","42").put("vod_name","Example").put("_provider","current").put("_kind","movie").put("vod_year",2024).put("vod_douban_score",8.2);}
 @Test public void backupRoundTripPreservesListHistoryAndPreferences()throws Exception{
  LibraryStore.toggle(c,title());app.veyra.data.WatchHistory.remember(c,"current","movie","42","id:1","0","Example","",12000,90000,false);
  c.getSharedPreferences("resume",0).edit().putLong("current:movie:42:id:1",12000).commit();c.getSharedPreferences("settings",0).edit().putInt("theme",2).putBoolean("compactPosters",true).commit();
  JSONObject backup=VelixioData.validate(VelixioData.export(c).toString());setup();VelixioData.restore(c,backup);assertTrue(LibraryStore.saved(c,title()));assertEquals(1,app.veyra.data.WatchHistory.list(c).size());assertEquals(12000,c.getSharedPreferences("resume",0).getLong("current:movie:42:id:1",0));assertEquals(2,c.getSharedPreferences("settings",0).getInt("theme",0));
 }
 @Test public void exportsExcludeUnknownFieldsAndCredentials()throws Exception{JSONObject t=title().put("token","secret-123").put("url","https://media.invalid/private");LibraryStore.toggle(c,t);c.getSharedPreferences("settings",0).edit().putString("password","secret-123").commit();c.getSharedPreferences("session",0).edit().putString("token","secret-123").commit();String out=VelixioData.export(c).toString();assertFalse(out.contains("secret-123"));assertFalse(out.contains("media.invalid"));}
 @Test public void malformedBackupCannotModifyExistingData()throws Exception{LibraryStore.toggle(c,title());JSONObject raw=VelixioData.export(c);raw.getJSONArray("favorites").put(new JSONObject().put("id","x").put("_provider","invalid"));try{VelixioData.validate(raw.toString());fail();}catch(Exception expected){}assertTrue(LibraryStore.saved(c,title()));assertEquals(1,LibraryStore.list(c).size());}
 @Test public void rejectsFutureVersion()throws Exception{JSONObject raw=VelixioData.export(c).put("version",99);try{VelixioData.validate(raw.toString());fail();}catch(Exception expected){}}
 @Test public void rejectsInvalidThemeAndResumeUrls()throws Exception{JSONObject raw=VelixioData.export(c);raw.getJSONObject("settings").put("theme",55);try{VelixioData.validate(raw.toString());fail();}catch(Exception expected){}raw.getJSONObject("settings").put("theme",0);raw.getJSONObject("resume").put("current:https://bad",20);try{VelixioData.validate(raw.toString());fail();}catch(Exception expected){}}
 @Test public void importMergesLists()throws Exception{LibraryStore.toggle(c,title());JSONObject raw=VelixioData.export(c);LibraryStore.toggle(c,title().put("id","43"));VelixioData.restore(c,VelixioData.validate(raw.toString()));assertEquals(2,LibraryStore.list(c).size());}
 @Test public void signedPosterQueryIsNotExported()throws Exception{LibraryStore.toggle(c,title().put("vod_pic","https://images.invalid/a?token=secret"));assertFalse(VelixioData.export(c).toString().contains("token="));}
 @Test public void importKeepsNewerHistory()throws Exception{app.veyra.data.WatchHistory.remember(c,"current","movie","42","id:1","0","Example","",20000,90000,false);JSONObject raw=VelixioData.export(c);raw.getJSONArray("history").getJSONObject(0).put("updatedAt",1).put("position",100);VelixioData.restore(c,VelixioData.validate(raw.toString()));assertEquals(20000,app.veyra.data.WatchHistory.list(c).get(0).getLong("position"));}
 @Test public void deeplyNestedInputIsRejectedBeforeParsing()throws Exception{String deep="0";for(int i=0;i<50;i++)deep="["+deep+"]";try{VelixioData.validate(deep);fail();}catch(Exception expected){assertEquals("Backup nesting too deep",expected.getMessage());}}
}
