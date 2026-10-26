package app.veyra;

import app.veyra.data.EngineFailure;
import java.lang.reflect.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class NativeContractTest {
    @Test public void engineDeclaresBothNativeEntrypoints() throws Exception {
        Method exec=com.pp.hls.class.getDeclaredMethod("exec",String.class,String.class,String.class);
        Method load=com.pp.hls.class.getDeclaredMethod("load",String.class,String.class,String.class,String.class,String.class,String.class,String.class);
        for(Method method:new Method[]{exec,load}){
            assertEquals(int.class,method.getReturnType());assertTrue(Modifier.isNative(method.getModifiers()));
            assertTrue(Modifier.isPublic(method.getModifiers()));assertFalse(Modifier.isStatic(method.getModifiers()));
        }
    }
    @Test public void protectionAbiMatchesFieldAndNativeMethod() throws Exception {
        Field map=com.jiagu.sdk.pp_hlsProtected.class.getDeclaredField("a");
        assertEquals(java.util.Map.class,map.getType());assertTrue(Modifier.isStatic(map.getModifiers()));assertFalse(Modifier.isFinal(map.getModifiers()));
        Method method=com.jiagu.sdk.pp_hlsProtected.class.getDeclaredMethod("interface14",int.class);
        assertEquals(String.class,method.getReturnType());assertTrue(Modifier.isNative(method.getModifiers()));assertTrue(Modifier.isStatic(method.getModifiers()));
    }
    @Test public void diagnosticsSeparateMissingMethodsAndLinkerFailures(){
        assertEquals("JNI_METHOD",EngineFailure.classify(new NoSuchMethodError("private method details")));
        assertEquals("MISSING_SYMBOL",EngineFailure.classify(new UnsatisfiedLinkError("cannot locate symbol secret-value")));
        assertEquals("MISSING_FILE",EngineFailure.classify(new java.io.FileNotFoundException("/private/path")));
        assertEquals("JNI_REGISTRATION",EngineFailure.classify(new UnsatisfiedLinkError("JNI_ERR returned from JNI_OnLoad")));
        assertEquals("ALIGNMENT",EngineFailure.classify(new UnsatisfiedLinkError("invalid page size")));
    }
    @Test public void unknownNativeMessagesNeverBecomeUserVisible(){
        String classified=EngineFailure.classify(new UnsatisfiedLinkError("https://private.example/?token=secret-value"));
        assertEquals("LINKER",classified);assertFalse(classified.contains("secret-value"));
    }
}
