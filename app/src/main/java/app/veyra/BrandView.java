package app.veyra;
import android.content.Context;import android.graphics.*;import android.view.View;
/** Generated ribbon mark, paired with a restrained wordmark. */
final class BrandView extends View {
 private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
 private final Bitmap logo;
 private final RectF bounds=new RectF();
 private final Paint.FontMetrics font=new Paint.FontMetrics();
 BrandView(Context c){
     super(c);
     logo=BitmapFactory.decodeResource(c.getResources(),R.drawable.velixio_logo);
     p.setTypeface(Ui.type(c,R.font.oswald_medium));
     setContentDescription("Velixio");
     setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
 }
 protected void onDraw(Canvas c){
     super.onDraw(c);
     float scale=Math.min(getWidth()/132f,getHeight()/48f);
     c.save();
     c.translate(0,(getHeight()-48*scale)/2);
     c.scale(scale,scale);
     bounds.set(0,6,36,42);
     c.drawBitmap(logo,null,bounds,p);
     p.setColor(Ui.INK);
     p.setTextSize(26);
     p.getFontMetrics(font);
     c.drawText("Velixio",42,24-(font.ascent+font.descent)/2,p);
     c.restore();
 }
}
