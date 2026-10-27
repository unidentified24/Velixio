package app.veyra;
import android.graphics.*;import android.graphics.drawable.Drawable;
final class Glyph extends Drawable {
 private final String kind;private final Paint p=new Paint(3);Glyph(String kind){this.kind=kind;}
 public void draw(Canvas c){c.save();Rect b=getBounds();c.translate(b.left,b.top);c.scale(b.width()/24f,b.height()/24f);p.setColor(Ui.INK);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.7f);p.setStrokeCap(Paint.Cap.ROUND);if(kind.equals("menu")){for(int y=6;y<=18;y+=6)c.drawLine(4,y,20,y,p);}else if(kind.equals("pip")){c.drawRoundRect(2,4,22,20,2,2,p);p.setStyle(Paint.Style.FILL);c.drawRoundRect(12,12,19,17,1,1,p);}else if(kind.equals("rotate")){c.drawRoundRect(7,4,17,20,2,2,p);c.drawArc(1,2,23,22,200,110,false,p);c.drawLine(20,3,21,8,p);c.drawLine(21,8,16,7,p);}else if(kind.equals("download")){c.drawLine(12,3,12,15,p);c.drawLine(7,10,12,15,p);c.drawLine(17,10,12,15,p);c.drawLine(4,20,20,20,p);}else{for(int x=5;x<=19;x+=7)c.drawCircle(x,12,1,p);}c.restore();}
 public void setAlpha(int a){p.setAlpha(a);}public void setColorFilter(ColorFilter f){p.setColorFilter(f);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
