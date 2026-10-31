package app.veyra;
import android.app.*;import android.os.Bundle;import android.widget.*;
public final class WhatsNewActivity extends Activity {
 public void onCreate(Bundle saved){super.onCreate(saved);Ui.configure(this);LinearLayout root=Ui.column(this);root.addView(Ui.text(this,"A little more Velixio.",32,Ui.INK));root.addView(Ui.text(this,"WHAT’S NEW",12,Ui.ACCENT));root.addView(Ui.text(this,ReleaseExperience.NOTES,17,Ui.INK));Button done=Ui.primary(this,"Keep exploring");done.setOnClickListener(v->finish());root.addView(done);ScrollView scroll=new ScrollView(this);scroll.addView(root);setContentView(scroll);Ui.insets(this,scroll,24);}
}
