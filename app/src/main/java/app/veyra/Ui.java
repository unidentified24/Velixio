package app.veyra;
import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
final class Ui {
    static final int BG=Color.rgb(9,13,18), INK=Color.rgb(243,246,249), MUTED=Color.rgb(153,165,180), ACCENT=Color.rgb(92,224,192);
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
        Button b=control(a,title);b.setTextColor(BG);Drawable face=new InsetDrawable(shape(a,ACCENT,8),0,dp(a,5),0,dp(a,5));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33000000),face,null));b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));return b;
    }
    static Button chip(Activity a,String title){
        Button b=control(a,title);b.setTextColor(new ColorStateList(new int[][]{new int[]{android.R.attr.state_selected},new int[]{}},new int[]{ACCENT,INK}));
        StateListDrawable states=new StateListDrawable();GradientDrawable selected=shape(a,0xff17352f,8);selected.setStroke(dp(a,1),0xff357b69);
        states.addState(new int[]{android.R.attr.state_selected},selected);states.addState(new int[]{},shape(a,0xff18212b,8));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22ffffff),new InsetDrawable(states,0,dp(a,6),0,dp(a,6)),null));b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));return b;
    }
    static Button quiet(Activity a,String title){
        Button b=control(a,title);b.setTextColor(INK);b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22ffffff),null,shape(a,Color.WHITE,8)));b.setPadding(dp(a,14),dp(a,6),dp(a,14),dp(a,6));return b;
    }
    static LinearLayout card(Activity a){LinearLayout card=column(a);card.setBackground(shape(a,0xff151c25,10));card.setClipToOutline(true);int p=dp(a,12);card.setPadding(p,p,p,p);return card;}
    static void insets(Activity a,View view,int padding){int p=dp(a,padding);view.setOnApplyWindowInsetsListener((v,insets)->{
        v.setPadding(p+insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),p+insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;
    });view.requestApplyInsets();}
}
