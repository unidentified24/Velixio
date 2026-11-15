package app.veyra;

import android.app.Activity;
import android.content.Context;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class PrivateViewing {
    private static final Set<String> profiles=ConcurrentHashMap.newKeySet();
    private PrivateViewing(){}

    static boolean active(Context context){
        return context instanceof Activity && ((Activity)context).getIntent().getBooleanExtra("privateViewing",false)
            || profiles.contains(ProfileStore.id(context));
    }

    static boolean toggle(Context context){
        String profile=ProfileStore.id(context);
        if(profiles.remove(profile))return false;
        profiles.add(profile);
        return true;
    }
}
