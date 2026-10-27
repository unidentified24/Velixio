package app.veyra;
import android.content.Context;import android.graphics.*;import android.view.View;
/** Generated ribbon mark, paired with a restrained wordmark. */
final class BrandView extends View {
 private final Paint p=new Paint(3);private final Bitmap logo;private final RectF bounds=new RectF();
 BrandView(Context c){super(c);logo=BitmapFactory.decodeResource(c.getResources(),R.drawable.velixio_logo);setContentDescription("Velixio");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
 protected void onDraw(Canvas c){super.onDraw(c);float s=Math.min(getWidth()/176f,getHeight()/48f);c.save();c.scale(s,s);bounds.set(0,0,48,48);c.drawBitmap(logo,null,bounds,p);p.setColor(Ui.INK);p.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));p.setTextSize(28);c.drawText("velixio",53,32,p);c.restore();}
}
