package app.veyra;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import app.veyra.data.PlaybackJson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
public final class MainActivity extends Activity {
    private final ExecutorService io=Executors.newSingleThreadExecutor();
    private TextView status;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        LinearLayout root=Ui.column(this);
        root.addView(Ui.text(this,"V E Y R A",18,Ui.MUTED));
        root.addView(Ui.text(this,"A better place\nto press play.",38,Ui.INK));
        root.addView(Ui.text(this,"Your stories. A fresh perspective.",17,Ui.MUTED));
        root.addView(Ui.column(this),new LinearLayout.LayoutParams(-1,0,1));
        root.addView(Ui.text(this,"Player preview",23,Ui.INK));
        status=Ui.text(this,"Your library is not connected yet. Open a playback file to try the new player with your own streams.",16,Ui.MUTED); root.addView(status);
        Button open=Ui.button(this,"Open playback file");
        open.setOnClickListener(v->{
            Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*"); intent.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(intent,10);
        });
        root.addView(open,new LinearLayout.LayoutParams(-1,-2));
        root.addView(Ui.text(this,"Development preview · 0.1",12,Ui.MUTED));
        setContentView(root); Ui.insets(this,root,24);
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request!=10 || result!=RESULT_OK || data==null || data.getData()==null) return;
        status.setText("Opening playback file…");
        io.execute(()->{
            try(InputStream in=getContentResolver().openInputStream(data.getData())) {
                if(in==null) throw new IllegalArgumentException();
                ByteArrayOutputStream out=new ByteArrayOutputStream(); byte[] buffer=new byte[4096]; int n;
                while((n=in.read(buffer))!=-1) {
                    if(out.size()+n>65536) throw new IllegalArgumentException();
                    out.write(buffer,0,n);
                }
                String json=out.toString(StandardCharsets.UTF_8.name()); PlaybackJson.parse(json);
                runOnUiThread(()->{
                    if(isFinishing() || isDestroyed()) return;
                    status.setText("Ready. Open another playback file whenever you like.");
                    startActivity(new Intent(this,PlayerActivity.class).putExtra("playback",json));
                });
            } catch(Exception e) {
                runOnUiThread(()->{ if(!isDestroyed()) status.setText("Could not open this playback file. Check its format and HTTPS sources (maximum 64 KB)."); });
            }
        });
    }
    @Override protected void onDestroy() { io.shutdownNow(); super.onDestroy(); }
}
