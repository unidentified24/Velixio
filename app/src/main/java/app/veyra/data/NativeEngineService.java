package app.veyra.data;

import android.app.*;
import android.content.Intent;
import android.os.*;
import java.io.*;
import org.json.JSONObject;

/** Non-exported process boundary for the owner-supplied native library. */
public abstract class NativeEngineService extends Service {
    static final int START=1, READY=2, FAILED=3;
    private HandlerThread worker;
    private Messenger receiver;
    private int port;
    protected abstract String provider();
    @Override public void onCreate() {
        super.onCreate();worker=new HandlerThread("streaming-engine");worker.start();
        receiver=new Messenger(new Handler(worker.getLooper(),message->{
            if(message.what!=START || message.replyTo==null)return true;
            Message response=Message.obtain();
            try {
                if(port==0)port=initialize(message.getData().getString("configuration",""));
                response.what=READY;response.arg1=port;
            }catch(Exception|LinkageError e){response.what=FAILED;}
            try{message.replyTo.send(response);}catch(RemoteException ignored){}
            return true;
        }));
    }
    private int initialize(String configuration) throws Exception {
        if(configuration.isEmpty() || configuration.length()>524288)throw new IOException("Invalid streaming configuration");
        String provider=provider();JSONObject profile;
        try(InputStream in=getAssets().open(provider.equals("classic")?"backend-classic.json":"backend.json")){profile=new JSONObject(LegacyBackend.read(in,65536));}
        // Both libraries retain their original ELF identity. Separate processes prevent symbol/state collisions.
        System.loadLibrary("veyra_"+provider+"_guard");
        System.loadLibrary("veyra_"+provider+"_engine");
        File storage=new File(getFilesDir(),"streaming/"+provider),cache=new File(getCacheDir(),"streaming/"+provider);
        if((!storage.isDirectory() && !storage.mkdirs()) || (!cache.isDirectory() && !cache.mkdirs()))throw new IOException("Cannot prepare streaming storage");
        int started=new com.pp.hls().load(profile.getString("nativeKey"),profile.getString("nativePackage"),profile.getString("nativeVersion"),storage.getAbsolutePath(),cache.getAbsolutePath(),configuration,"1");
        if(started<1 || started>65535)throw new IOException("Streaming initialization failed");
        return started;
    }
    @Override public IBinder onBind(Intent intent){return receiver.getBinder();}
    @Override public void onDestroy() {
        if(worker!=null)worker.quitSafely();super.onDestroy();
        // No trustworthy SDK shutdown ABI is available. Ending this dedicated process stops all native threads/sockets.
        String processName="";
        if(Build.VERSION.SDK_INT>=28)processName=Application.getProcessName();
        else try(FileInputStream in=new FileInputStream("/proc/self/cmdline")) {
            byte[] bytes=new byte[256];int count=in.read(bytes),length=0;
            while(length<count && bytes[length]!=0)length++;
            processName=new String(bytes,0,length,java.nio.charset.StandardCharsets.UTF_8);
        }catch(IOException ignored){}
        if(processName.endsWith(":stream_current") || processName.endsWith(":stream_classic"))android.os.Process.killProcess(android.os.Process.myPid());
    }
}
