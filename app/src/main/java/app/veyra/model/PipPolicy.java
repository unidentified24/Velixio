package app.veyra.model;
/** Pure policy shared by playback and host-side lifecycle tests. */
public final class PipPolicy {
    private PipPolicy() {}
    public static boolean autoEnter(boolean ready,boolean error,boolean idle,boolean ended) { return ready && !error && !idle && !ended; }
    public static int ratioThousandths(int width,int height,float pixelRatio) {
        float ratio=width>0 && height>0 ? width*pixelRatio/height : 16f/9f;
        if(Float.isNaN(ratio) || Float.isInfinite(ratio) || ratio<=0)ratio=16f/9f;
        return Math.round(Math.max(.42f,Math.min(2.39f,ratio))*1000);
    }
}
