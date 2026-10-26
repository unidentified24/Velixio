package app.veyra;
import android.content.Context;
import android.graphics.*;
import android.view.View;
/** A custom geometric ZUNO wordmark with a folded-play Z. */
final class BrandView extends View {
 private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path p=new Path();
 BrandView(Context c){super(c);setContentDescription("Zuno");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
 protected void onDraw(Canvas c){super.onDraw(c);float scale=Math.min(getWidth()/138f,getHeight()/44f);c.save();c.translate(0,(getHeight()-30*scale)/2);c.scale(scale,scale);
 ink.setColor(Ui.ACCENT);ink.setStyle(Paint.Style.FILL);p.reset();p.moveTo(0,2);p.lineTo(29,2);p.lineTo(12,23);p.lineTo(29,23);p.lineTo(29,29);p.lineTo(0,29);p.lineTo(17,8);p.lineTo(0,8);p.close();c.drawPath(p,ink);
 ink.setColor(Ui.INK);ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(4);ink.setStrokeCap(Paint.Cap.ROUND);ink.setStrokeJoin(Paint.Join.ROUND);p.reset();p.moveTo(43,4);p.lineTo(43,19);p.cubicTo(43,32,65,32,65,19);p.lineTo(65,4);p.moveTo(79,27);p.lineTo(79,4);p.lineTo(101,27);p.lineTo(101,4);c.drawPath(p,ink);c.drawRoundRect(113,4,135,27,10,10,ink);c.restore();
 }
}
