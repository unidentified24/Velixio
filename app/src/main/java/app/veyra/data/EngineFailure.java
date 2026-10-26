package app.veyra.data;

import java.io.FileNotFoundException;
import java.util.Locale;

/** Classifies native failures without exposing native exception text or private paths. */
public final class EngineFailure {
    private EngineFailure(){}
    public static String classify(Throwable failure){
        if(failure instanceof FileNotFoundException)return "MISSING_FILE";
        if(failure instanceof NoSuchMethodError)return "JNI_METHOD";
        if(failure instanceof NoClassDefFoundError || failure instanceof ClassNotFoundException)return "JNI_CLASS";
        if(failure instanceof SecurityException)return "PERMISSION";
        String message=failure.getMessage()==null?"":failure.getMessage().toLowerCase(Locale.ROOT);
        if(message.contains("permission denied"))return "PERMISSION";
        if(message.contains("namespace"))return "LINKER_NAMESPACE";
        if(message.contains("cannot locate symbol") || message.contains("undefined symbol"))return "MISSING_SYMBOL";
        if(message.contains("jni_err") || message.contains("jni_onload") || message.contains("register natives") || message.contains("registernatives"))return "JNI_REGISTRATION";
        if(message.contains("no implementation found"))return "JNI_METHOD";
        if(message.contains("elfclass") || message.contains("unexpected e_machine") || message.contains("bad elf"))return "ABI";
        if(message.contains("alignment") || message.contains("page size"))return "ALIGNMENT";
        if(failure instanceof UnsatisfiedLinkError)return "LINKER";
        if(failure instanceof java.io.IOException)return "STORAGE";
        if(failure instanceof LinkageError)return "JAVA_LINKAGE";
        return "INITIALIZATION";
    }
}
