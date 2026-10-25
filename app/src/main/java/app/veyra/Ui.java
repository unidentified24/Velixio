package app.veyra;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
final class Ui {
    static final int BG=Color.rgb(12,12,19), INK=Color.rgb(244,242,255), MUTED=Color.rgb(168,165,187);
    static int dp(Activity a,int value) { return Math.round(value*a.getResources().getDisplayMetrics().density); }
    static LinearLayout column(Activity a) { LinearLayout v=new LinearLayout(a); v.setOrientation(LinearLayout.VERTICAL); v.setBackgroundColor(BG); return v; }
    static TextView text(Activity a,String value,int size,int color) {
        TextView v=new TextView(a); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setPadding(0,dp(a,8),0,dp(a,8)); return v;
    }
    static Button button(Activity a,String title) {
        Button b=new Button(a); b.setText(title); b.setAllCaps(false); b.setTextColor(BG); b.setTextSize(16); b.setTypeface(null,Typeface.BOLD);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(Color.rgb(187,171,255)); bg.setCornerRadius(dp(a,16));
        b.setBackground(bg); b.setMinHeight(dp(a,52)); b.setPadding(dp(a,20),dp(a,10),dp(a,20),dp(a,10)); return b;
    }
    static void insets(Activity a,View view,int padding) {
        int p=dp(a,padding);
        view.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(p+insets.getSystemWindowInsetLeft(),p+insets.getSystemWindowInsetTop(),p+insets.getSystemWindowInsetRight(),p+insets.getSystemWindowInsetBottom());
            return insets;
        }); view.requestApplyInsets();
    }
}
