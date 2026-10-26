package app.veyra;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.*;
final class Ui {
    static final int BG=Color.rgb(9,13,18), INK=Color.rgb(243,246,249), MUTED=Color.rgb(153,165,180);
    static int dp(Activity a,int value) { return Math.round(value*a.getResources().getDisplayMetrics().density); }
    static LinearLayout column(Activity a) { LinearLayout v=new LinearLayout(a); v.setOrientation(LinearLayout.VERTICAL); v.setBackgroundColor(BG); return v; }
    static TextView text(Activity a,String value,int size,int color) {
        TextView v=new TextView(a); v.setText(value); v.setTextSize(size); v.setTextColor(color);
        v.setPadding(0,dp(a,8),0,dp(a,8)); return v;
    }
    static Button button(Activity a,String title) {
        Button b=new Button(a); b.setText(title); b.setAllCaps(false); b.setTextColor(BG); b.setTextSize(16); b.setTypeface(null,Typeface.BOLD);
        GradientDrawable bg=new GradientDrawable(); bg.setColor(Color.rgb(92,224,192)); bg.setCornerRadius(dp(a,10));
        b.setBackground(bg); b.setMinHeight(dp(a,52)); b.setPadding(dp(a,20),dp(a,10),dp(a,20),dp(a,10)); return b;
    }
    static LinearLayout card(Activity a) {
        LinearLayout card=column(a);GradientDrawable background=new GradientDrawable();background.setColor(Color.rgb(21,28,37));background.setCornerRadius(dp(a,10));
        card.setBackground(background);card.setClipToOutline(true);int p=dp(a,12);card.setPadding(p,p,p,p);return card;
    }
    static Button chip(Activity a,String title) {
        Button chip=button(a,title);GradientDrawable surface=new GradientDrawable();surface.setColor(Color.rgb(27,36,47));surface.setCornerRadius(dp(a,22));chip.setBackground(surface);chip.setTextColor(INK);chip.setTextSize(13);chip.setMinHeight(dp(a,44));chip.setPadding(dp(a,14),dp(a,8),dp(a,14),dp(a,8));return chip;
    }
    static void insets(Activity a,View view,int padding) {
        int p=dp(a,padding);
        view.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(p+insets.getSystemWindowInsetLeft(),p+insets.getSystemWindowInsetTop(),p+insets.getSystemWindowInsetRight(),p+insets.getSystemWindowInsetBottom());
            return insets;
        }); view.requestApplyInsets();
    }
}
