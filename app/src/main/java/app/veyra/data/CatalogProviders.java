package app.veyra.data;

import android.content.Context;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Select the protocol by provider while leaving playback requests provider-owned. */
public final class CatalogProviders {
    private static final String BLUESUN_CONFIG="backend-bluesun.json";

    private CatalogProviders(){}

    public static boolean known(String provider){
        return "current".equals(provider)||"classic".equals(provider)||"bluesun".equals(provider);
    }

    public static boolean enabled(Context context,String provider){
        if(!"bluesun".equals(provider))return known(provider);
        try(java.io.InputStream input=context.getAssets().open(BLUESUN_CONFIG)){
            return true;
        }catch(IOException unavailable){return false;}
    }

    static List<String> configured(Context context){
        List<String> providers=new ArrayList<>(Arrays.asList("current","classic"));
        if(enabled(context,"bluesun"))providers.add("bluesun");
        return providers;
    }

    public static StreamingBackend get(Context context,String provider)throws Exception {
        if("bluesun".equals(provider)){
            if(!enabled(context,provider))throw new FileNotFoundException("BlueSun provider configuration is unavailable");
            return new BlueSunBackend(context,BLUESUN_CONFIG);
        }
        return CatalogBackend.get(context,provider);
    }
}
