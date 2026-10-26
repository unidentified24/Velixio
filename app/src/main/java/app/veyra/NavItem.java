package app.veyra;

import android.app.Activity;
import android.graphics.*;
import android.view.*;
import android.widget.*;

/** Consistent navigation symbols with an explicit selected state. */
final class NavItem extends LinearLayout {
    private final TextView label;
    private final View icon;
    private int color=Ui.MUTED;
    NavItem(Activity activity,String name,int index){
        super(activity);setOrientation(VERTICAL);setGravity(Gravity.CENTER);setPadding(0,Ui.dp(activity,8),0,Ui.dp(activity,5));setContentDescription(name);setFocusable(true);
        icon=new View(activity){
            private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Path p=new Path();
            protected void onDraw(Canvas c){super.onDraw(c);c.save();c.scale(getWidth()/28f,getHeight()/28f);paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.8f);paint.setStrokeJoin(Paint.Join.ROUND);paint.setStrokeCap(Paint.Cap.ROUND);p.reset();
                if(index==0){p.moveTo(3,13);p.lineTo(14,4);p.lineTo(25,13);p.moveTo(6,11);p.lineTo(6,24);p.lineTo(11,24);p.lineTo(11,17);p.lineTo(17,17);p.lineTo(17,24);p.lineTo(22,24);p.lineTo(22,11);c.drawPath(p,paint);}
                else if(index==1){c.drawCircle(12,12,8,paint);c.drawLine(18,18,25,25,paint);}
                else{p.moveTo(6,4);p.lineTo(22,4);p.lineTo(22,25);p.lineTo(14,19);p.lineTo(6,25);p.close();c.drawPath(p,paint);}c.restore();
            }
        };addView(icon,new LayoutParams(Ui.dp(activity,24),Ui.dp(activity,24)));
        label=Ui.text(activity,name,11,color);label.setPadding(0,Ui.dp(activity,4),0,0);label.setGravity(Gravity.CENTER);addView(label);setActive(index==0);
    }
    void setActive(boolean selected){setSelected(selected);color=selected?Color.rgb(92,224,192):Ui.MUTED;label.setTextColor(color);icon.invalidate();}
}
