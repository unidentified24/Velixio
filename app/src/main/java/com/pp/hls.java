package com.pp;
/** JNI ABI declaration for the owner-supplied optional streaming library. No SDK binary is stored in Git. */
public final class hls {
    static { System.loadLibrary("pp_hls"); }
    public native int load(String key,String packageName,String version,String storage,String cache,String config,String mode);
}
