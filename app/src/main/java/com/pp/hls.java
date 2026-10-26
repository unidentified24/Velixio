package com.pp;
/** JNI ABI declaration only. The correct owner-supplied engine is loaded inside its dedicated service process. */
public class hls {
    public native int exec(String command,String argument,String value);
    public native int load(String key,String packageName,String version,String storage,String cache,String config,String mode);
}
