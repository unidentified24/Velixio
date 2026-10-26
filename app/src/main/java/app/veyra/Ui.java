package app.veyra;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
final class Ui {
    static int BG=0xff100e19, INK=0xfff5f2ff, MUTED=0xffaaa5bc, ACCENT=0xffbc9cff, SURFACE=0xff211c30;
    static void configure(Activity a){
        int theme=a.getSharedPreferences("settings",0).getInt("theme",0);
        int[][] colors={{0xff100e19,0xffbc9cff,0xff211c30},{0xff08151d,0xff64d9f4,0xff142a37},{0xff19110d,0xffffb582,0xff302117},{0xff000000,0xffeeeeee,0xff1b1b1b}};
        if(theme<0||theme>=colors.length)theme=0;BG=colors[theme][0];ACCENT=colors[theme][1];SURFACE=colors[theme][2];
        a.setTheme(new int[]{R.style.Theme_Zuno,R.style.Theme_Zuno_Ocean,R.style.Theme_Zuno_Ember,R.style.Theme_Zuno_Oled}[theme]);
        a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(BG);a.getWindow().setBackgroundDrawable(new ColorDrawable(BG));
    }
    static int columns(Activity a){return a.getResources().getConfiguration().screenWidthDp>=600?5:a.getSharedPreferences("settings",0).getBoolean("compactPosters",false)?3:2;}
    static int posterWidth(Activity a){return a.getSharedPreferences("settings",0).getBoolean("compactPosters",false)?112:136;}
    static int dp(Activity a,int value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
    static LinearLayout column(Activity a){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.VERTICAL);v.setBackgroundColor(BG);return v;}
    static TextView text(Activity a,String value,int size,int color){
        TextView v=new TextView(a);v.setText(value);v.setTextSize(size);v.setTextColor(color);v.setIncludeFontPadding(false);
        v.setPadding(0,dp(a,6),0,dp(a,6));return v;
    }
    private static GradientDrawable shape(Activity a,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(a,radius));return d;}
    private static Button control(Activity a,String title){
        Button b=new Button(a);b.setText(title);b.setAllCaps(false);b.setTextSize(13);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setIncludeFontPadding(false);
        // Reset both TextView and View theme minimums. Keep a 48dp touch target around a smaller visible surface.
        b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(a,48));b.setMinimumHeight(dp(a,48));
        b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));b.setSingleLine(true);b.setEllipsize(android.text.TextUtils.TruncateAt.END);b.setStateListAnimator(null);b.setElevation(0);return b;
    }
    static Button button(Activity a,String title){return chip(a,title);}
    static Button primary(Activity a,String title){
        Button b=control(a,title);b.setTextColor(BG);Drawable face=new InsetDrawable(shape(a,ACCENT,12),0,dp(a,5),0,dp(a,5));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000),face,null));b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));return b;
    }
    static Button chip(Activity a,String title){
        Button b=control(a,title);b.setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_selected},new int[]{}},new int[]{ACCENT,INK}));
        StateListDrawable states=new StateListDrawable();GradientDrawable selected=shape(a,SURFACE,12);selected.setStroke(dp(a,1),ACCENT);
        states.addState(new int[]{android.R.attr.state_selected},selected);states.addState(new int[]{},shape(a,SURFACE,12));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22ffffff),new InsetDrawable(states,0,dp(a,6),0,dp(a,6)),null));b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));return b;
    }
    static Button quiet(Activity a,String title){
        Button b=control(a,title);b.setTextColor(INK);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22ffffff),null,shape(a,Color.WHITE,8)));b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));return b;
    }
    static LinearLayout card(Activity a){LinearLayout card=column(a);card.setBackground(shape(a,SURFACE,14));card.setClipToOutline(true);int p=dp(a,12);card.setPadding(p,p,p,p);return card;}
    static void insets(Activity a,View view,int padding){int p=dp(a,padding);view.setOnApplyWindowInsetsListener((v,insets)->{
        v.setPadding(p+insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),p+insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
    });view.requestApplyInsets();}
}
