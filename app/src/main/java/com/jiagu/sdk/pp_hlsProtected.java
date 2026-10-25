package com.jiagu.sdk;
/** Original protection-library JNI names; the binary remains a private build input. */
public final class pp_hlsProtected {
    private pp_hlsProtected() {}
    public static final java.util.Map<Integer,String> a=new java.util.concurrent.ConcurrentHashMap<>();
    public static native String interface14(int index);
    public static String a(int index){String value=a.get(index);if(value==null){value=interface14(index);if(value!=null)a.put(index,value);}return value;}
    public static void b(android.app.Application application){}
}
