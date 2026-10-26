package app.veyra;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Veyra's geometric wordmark, drawn from paths rather than a device font. */
final class BrandView extends View {
    private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path letters=new Path();
    BrandView(Context context){super(context);setContentDescription("Veyra");setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);float scale=Math.min(getHeight()/44f,getWidth()/155f);canvas.save();canvas.translate(2,(getHeight()-30*scale)/2);canvas.scale(scale,scale);
        ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(3.2f);ink.setStrokeCap(Paint.Cap.ROUND);ink.setStrokeJoin(Paint.Join.ROUND);
        letters.reset();letters.moveTo(2,3);letters.lineTo(13,27);letters.lineTo(24,3);ink.setColor(Color.rgb(92,224,192));canvas.drawPath(letters,ink);
        letters.reset();letters.moveTo(52,4);letters.lineTo(34,4);letters.lineTo(34,26);letters.lineTo(52,26);letters.moveTo(34,15);letters.lineTo(49,15);
        letters.moveTo(62,4);letters.lineTo(72,15);letters.lineTo(82,4);letters.moveTo(72,15);letters.lineTo(72,26);
        letters.moveTo(94,26);letters.lineTo(94,4);letters.lineTo(104,4);letters.cubicTo(116,4,116,16,104,16);letters.lineTo(94,16);letters.moveTo(104,16);letters.lineTo(114,26);
        letters.moveTo(124,26);letters.lineTo(135,4);letters.lineTo(146,26);letters.moveTo(128,18);letters.lineTo(142,18);
        ink.setColor(Ui.INK);canvas.drawPath(letters,ink);canvas.restore();
    }
}
