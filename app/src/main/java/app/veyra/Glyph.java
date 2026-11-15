package app.veyra;

import android.graphics.*;
import android.graphics.drawable.Drawable;

final class Glyph extends Drawable {
    private final String kind;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF surface=new RectF();
    private final Path path=new Path();

    Glyph(String kind,int color){
        this.kind=kind;
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.7f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }

    @Override public int getIntrinsicWidth(){return 24;}
    @Override public int getIntrinsicHeight(){return 24;}

    @Override public void draw(Canvas canvas){
        Rect bounds=getBounds();
        canvas.save();
        canvas.translate(bounds.left,bounds.top);
        canvas.scale(bounds.width()/24f,bounds.height()/24f);
        switch(kind){
            case "menu": lines(canvas,4,6,20,6, 4,12,20,12, 4,18,20,18);break;
            case "close": lines(canvas,6,6,18,18, 18,6,6,18);break;
            case "search": canvas.drawCircle(10.5f,10.5f,6.5f,paint);lines(canvas,15,15,21,21);break;
            case "back": trace(canvas,15,5,8,12,15,19);break;
            case "forward": trace(canvas,9,5,16,12,9,19);break;
            case "pip": box(canvas,2,4,22,20);box(canvas,12,12,19,17);break;
            case "rotate":
                box(canvas,4.5f,8,13.5f,21.5f);
                surface.set(8,3,20,15);canvas.drawArc(surface,-110,103,false,paint);
                trace(canvas,16.5f,8,20,12,22,7);break;
            case "video": box(canvas,2,5,22,19);lines(canvas,8,10,16,10, 8,14,16,14);break;
            case "cast":
                trace(canvas,3,9,3,4,21,4,21,20,15,20);
                surface.set(-5,12,11,28);canvas.drawArc(surface,-90,90,false,paint);
                surface.set(-1,16,7,24);canvas.drawArc(surface,-90,90,false,paint);
                canvas.drawCircle(3,20,.8f,paint);break;
            case "screen":
                box(canvas,3,5,21,19);trace(canvas,6,10,6,8,9,8);trace(canvas,15,16,18,16,18,14);break;
            case "sources":
                box(canvas,3,5,21,10);box(canvas,3,14,21,19);lines(canvas,7,7.5f,8,7.5f, 7,16.5f,8,16.5f);break;
            case "audio": lines(canvas,4,10,4,14, 8,7,8,17, 12,4,12,20, 16,7,16,17, 20,10,20,14);break;
            case "captions": box(canvas,2,5,22,19);lines(canvas,6,11,10,11, 14,11,18,11, 6,15,18,15);break;
            case "download": lines(canvas,12,3,12,15);trace(canvas,7,10,12,15,17,10);trace(canvas,4,16,4,21,20,21,20,16);break;
            case "profile":
                canvas.drawCircle(12,7.5f,3.5f,paint);
                surface.set(4,13,20,25);canvas.drawArc(surface,180,180,false,paint);break;
            case "appearance":
                canvas.drawCircle(12,12,9,paint);trace(canvas,12,3,12,21);lines(canvas,7,8,9,6, 6,13,9,10, 7,17,9,15);break;
            case "settings":
                for(int y=5;y<=19;y+=7)lines(canvas,4,y,20,y);
                for(int y=5;y<=19;y+=7){int x=y==12?15:9;lines(canvas,x,y-2,x,y+2);}break;
            case "more":
                for(int x=5;x<=19;x+=7)canvas.drawCircle(x,12,1.3f,paint);
                break;
            case "history":
                canvas.drawCircle(12,12,9,paint);trace(canvas,12,6,12,12,16,14);break;
            case "play": trace(canvas,7,4,20,12,7,20,7,4);break;
            case "bookmark": trace(canvas,6,20,6,4,18,4,18,20,12,16,6,20);break;
            case "add": lines(canvas,5,12,19,12, 12,5,12,19);break;
            case "check": trace(canvas,5,12,10,17,19,7);break;
            case "collections": box(canvas,4,7,20,20);lines(canvas,6,4,18,4, 8,12,16,12);break;
            case "privacy": trace(canvas,12,3,20,6,19,14,16,18,12,21,8,18,5,14,4,6,12,3);break;
            case "layout":
                for(int x=3;x<=13;x+=10)for(int y=3;y<=13;y+=10)box(canvas,x,y,x+8,y+8);
                break;
            case "updates":
                surface.set(4,4,20,20);canvas.drawArc(surface,35,280,false,paint);
                trace(canvas,15,6,20,6,20,2);break;
            case "insights": lines(canvas,4,20,21,20, 7,16,7,11, 12,16,12,7, 17,16,17,4);break;
            case "message": trace(canvas,4,5,20,5,20,16,11,16,6,20,6,16,4,16,4,5);lines(canvas,8,10,16,10);break;
            case "sparkle": trace(canvas,12,3,14.5f,9.5f,21,12,14.5f,14.5f,12,21,9.5f,14.5f,3,12,9.5f,9.5f,12,3);break;
            default: for(int x=5;x<=19;x+=7)canvas.drawCircle(x,12,1,paint);
        }
        canvas.restore();
    }

    private void box(Canvas canvas,float left,float top,float right,float bottom){
        canvas.drawRoundRect(left,top,right,bottom,2,2,paint);
    }
    private void lines(Canvas canvas,float... points){canvas.drawLines(points,paint);}
    private void trace(Canvas canvas,float... points){
        path.reset();
        path.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);
        canvas.drawPath(path,paint);
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
