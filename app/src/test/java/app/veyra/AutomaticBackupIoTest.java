package app.veyra;
import android.content.*;import android.content.pm.ProviderInfo;import android.database.*;import android.net.Uri;import android.os.*;import android.provider.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.*;import org.robolectric.annotation.Config;import org.robolectric.shadows.ShadowContentResolver;import java.io.*;import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class AutomaticBackupIoTest {
 public static class Folder extends DocumentsProvider {
  File root;boolean fail;
  public boolean onCreate(){root=new File(getContext().getCacheDir(),"backup-fixture-"+java.util.UUID.randomUUID());root.mkdirs();return true;}
  public boolean isChildDocument(String parent,String child){return parent.equals("root")&&new File(root,child).isFile();}
  public Cursor queryRoots(String[] p){return new MatrixCursor(new String[]{DocumentsContract.Root.COLUMN_ROOT_ID});}
  public Cursor queryDocument(String id,String[] p){return new MatrixCursor(new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID});}
  public Cursor queryChildDocuments(String id,String[] p,String sort){return new MatrixCursor(new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID});}
  public String createDocument(String parent,String mime,String name)throws FileNotFoundException{try{File f=new File(root,name);if(!f.createNewFile())throw new IOException();return name;}catch(IOException e){throw new FileNotFoundException();}}
  public void deleteDocument(String id){new File(root,id).delete();}
  public ParcelFileDescriptor openDocument(String id,String mode,CancellationSignal signal)throws FileNotFoundException{if(fail&&mode.contains("w"))throw new FileNotFoundException();return ParcelFileDescriptor.open(new File(root,id),ParcelFileDescriptor.parseMode(mode));}
 }
 private Context c;private Folder provider;
 @Before public void setup(){c=RuntimeEnvironment.getApplication();c.getSharedPreferences("auto-backup",0).edit().clear().commit();provider=new Folder();ProviderInfo info=new ProviderInfo();info.authority="velixio.test.documents";info.exported=true;info.grantUriPermissions=true;info.readPermission="android.permission.MANAGE_DOCUMENTS";info.writePermission="android.permission.MANAGE_DOCUMENTS";provider.attachInfo(c,info);ShadowContentResolver.registerProviderInternal(info.authority,provider);c.getSharedPreferences("auto-backup",0).edit().putString("folder",DocumentsContract.buildTreeDocumentUri(info.authority,"root").toString()).commit();}
 @Test public void writesValidBackupsAndRetainsSeven()throws Exception{for(int i=0;i<8;i++)AutoBackup.write(c,true);assertEquals(7,provider.root.listFiles().length);File f=provider.root.listFiles()[0];try(InputStream in=new FileInputStream(f)){String json=app.veyra.data.CatalogBackend.read(in,VelixioData.LIMIT);assertEquals("velixiodata",new org.json.JSONObject(json).getString("format"));assertNotNull(VelixioData.validate(json).getJSONArray("favorites"));}assertTrue(c.getSharedPreferences("auto-backup",0).getLong("lastSuccess",0)>0);}
 @Test public void failedWriteKeepsPreviousCopyAndReportsFailure()throws Exception{AutoBackup.write(c,true);long last=c.getSharedPreferences("auto-backup",0).getLong("lastSuccess",0);provider.fail=true;try{AutoBackup.write(c,true);fail();}catch(IOException expected){}assertEquals(1,provider.root.listFiles().length);assertEquals(last,c.getSharedPreferences("auto-backup",0).getLong("lastSuccess",0));assertTrue(c.getSharedPreferences("auto-backup",0).contains("error"));}
}
