package app.veyra;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
final class Ui {
    static int BG=0xff090d16, INK=0xfff4f6f7, MUTED=0xffa5adb5, ACCENT=0xffff795f, SURFACE=0xff171e2b;
    static void configure(Activity a){
        int theme=a.getSharedPreferences("settings",0).getInt("theme",0);
        int[][] colors=ThemePalette.COLORS;theme=ThemePalette.index(theme);BG=colors[theme][0];ACCENT=colors[theme][1];SURFACE=colors[theme][2];
        a.setTheme(new int[]{R.style.Theme_Velixio,R.style.Theme_Velixio_Ocean,R.style.Theme_Velixio_Ember,R.style.Theme_Velixio_Oled,R.style.Theme_Velixio_Amethyst,R.style.Theme_Velixio_Forest,R.style.Theme_Velixio_Rosewood,R.style.Theme_Velixio_Glacier,R.style.Theme_Velixio_Sunset,R.style.Theme_Velixio_MidnightGold}[theme]);
        MUTED=a.getSharedPreferences("settings",0).getBoolean("highContrast",false)?0xffe0e5ec:0xffa5adb5;
        a.getWindow().setStatusBarColor(BG);a.getWindow().setNavigationBarColor(BG);a.getWindow().setBackgroundDrawable(new ColorDrawable(BG));
    }
    static boolean tv(Activity a){return (a.getResources().getConfiguration().uiMode&android.content.res.Configuration.UI_MODE_TYPE_MASK)==android.content.res.Configuration.UI_MODE_TYPE_TELEVISION;}
    static void focus(Activity a,View v){v.setFocusable(true);v.setOnFocusChangeListener((item,on)->{if(tv(a)){GradientDrawable outline=new GradientDrawable();outline.setColor(Color.TRANSPARENT);outline.setCornerRadius(dp(a,12));outline.setStroke(dp(a,3),on?ACCENT:Color.TRANSPARENT);item.setForeground(outline);}});}
    static int columns(Activity a){int width=a.getResources().getConfiguration().screenWidthDp;int card=tv(a)?190:a.getSharedPreferences("settings",0).getBoolean("compactPosters",false)?110:145;return Math.max(2,Math.min(8,(width-32)/card));}
    static int posterWidth(Activity a){return tv(a)?180:a.getSharedPreferences("settings",0).getBoolean("compactPosters",false)?112:136;}
    static Button icon(Activity a,String kind,String description){Button b=quiet(a,"");Glyph icon=new Glyph(kind);icon.setBounds(0,0,dp(a,24),dp(a,24));b.setCompoundDrawables(null,icon,null,null);b.setGravity(Gravity.CENTER);b.setPadding(dp(a,12),dp(a,12),dp(a,12),dp(a,12));b.setContentDescription(description);if(android.os.Build.VERSION.SDK_INT>=26)b.setTooltipText(description);return b;}
    static int dp(Activity a,int value){return Math.round(value*a.getResources().getDisplayMetrics().density);}
    static LinearLayout column(Activity a){LinearLayout v=new LinearLayout(a);v.setOrientation(LinearLayout.VERTICAL);v.setBackgroundColor(BG);return v;}
    static TextView text(Activity a,String value,int size,int color){
        TextView v=new TextView(a);v.setText(value);v.setTextSize(size*(a.getSharedPreferences("settings",0).getBoolean("largerText",false)?1.15f:1f));v.setTextColor(color);v.setIncludeFontPadding(false);
        v.setPadding(0,dp(a,6),0,dp(a,6));return v;
    }
    private static GradientDrawable shape(Activity a,int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(a,radius));return d;}
    private static Button control(Activity a,String title){
        Button b=new Button(a);b.setText(title);b.setAllCaps(false);b.setTextSize(13);b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));b.setIncludeFontPadding(false);
        // Reset both TextView and View theme minimums. Keep a 48dp touch target around a smaller visible surface.
        b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(a,48));b.setMinimumHeight(dp(a,48));
        b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));b.setSingleLine(true);b.setEllipsize(android.text.TextUtils.TruncateAt.END);focus(a,b);b.setStateListAnimator(null);b.setElevation(0);return b;
    }
    static Button button(Activity a,String title){return chip(a,title);}
    static Button primary(Activity a,String title){
        Button b=control(a,title);b.setTextColor(BG);Drawable face=new InsetDrawable(shape(a,ACCENT,24),0,dp(a,5),0,dp(a,5));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000),face,null));b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));return b;
    }
    static Button chip(Activity a,String title){
        Button b=control(a,title);b.setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_selected},new int[]{}},new int[]{ACCENT,INK}));
        StateListDrawable states=new StateListDrawable();GradientDrawable selected=shape(a,SURFACE,24);selected.setStroke(dp(a,1),ACCENT);
        states.addState(new int[]{android.R.attr.state_selected},selected);states.addState(new int[]{},shape(a,SURFACE,24));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22ffffff),new InsetDrawable(states,0,dp(a,6),0,dp(a,6)),null));b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));return b;
    }
    static Button tab(Activity a,String title){Button b=quiet(a,title);b.setTextSize(14);b.setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_selected},new int[]{}},new int[]{INK,MUTED}));return b;}
    static Button quiet(Activity a,String title){
        Button b=control(a,title);b.setTextColor(INK);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22ffffff),null,shape(a,Color.WHITE,8)));b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));return b;
    }
    static LinearLayout card(Activity a){LinearLayout card=column(a);card.setBackground(shape(a,SURFACE,14));card.setClipToOutline(true);int p=dp(a,12);card.setPadding(p,p,p,p);return card;}
    static void insets(Activity a,View view,int padding){int maxWidth=(a instanceof SettingsActivity||a instanceof UpdatesActivity)?760:1100;int p=Math.max(dp(a,tv(a)?48:padding),(a.getResources().getDisplayMetrics().widthPixels-dp(a,maxWidth))/2);view.setOnApplyWindowInsetsListener((v,insets)->{
        v.setPadding(p+insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),p+insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
    });view.requestApplyInsets();}
}
