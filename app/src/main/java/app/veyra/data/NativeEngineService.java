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
    private int stage=1;
    private int nativeResult;
    protected abstract String provider();
    @Override public void onCreate() {
        super.onCreate();worker=new HandlerThread("streaming-engine");worker.start();
        receiver=new Messenger(new Handler(worker.getLooper(),message->{
            if(message.what!=START || message.replyTo==null)return true;
            Message response=Message.obtain();
            try {
                if(port==0)port=initialize(message.getData().getString("configuration",""));
                response.what=READY;response.arg1=port;
            }catch(Exception|LinkageError e){response.what=FAILED;response.arg1=stage;response.arg2=nativeResult; Bundle safe=new Bundle();safe.putString("reason",stage==7?"INVALID_PORT":EngineFailure.classify(e));response.setData(safe); /* Raw native messages never cross this boundary. */ }
            try{message.replyTo.send(response);}catch(RemoteException ignored){}
            return true;
        }));
    }
    private int initialize(String configuration) throws Exception {
        if(configuration.isEmpty() || configuration.length()>524288)throw new IOException("Invalid streaming configuration");
        stage=2;String provider=provider();JSONObject profile;
        try(InputStream in=getAssets().open(provider.equals("classic")?"backend-classic.json":"backend.json")){profile=new JSONObject(CatalogBackend.read(in,65536));}
        // Both libraries retain their original ELF identity. Separate processes prevent symbol/state collisions.
        stage=3;
        if(provider.equals("classic"))loadLibrary(provider,"guard","libjiagu_sdk_pp_hlsProtected.so");
        stage=4;
        loadLibrary(provider,"engine","libpp_hls.so");
        stage=5;
        // The SDK's fourth argument is the shared-storage root, not its cache directory.
        // Its fifth argument is the app-scoped external data path; no broad storage permission is requested.
        File data=getExternalFilesDir(null);
        if(data==null || (!data.isDirectory()&&!data.mkdirs()))throw new IOException("Streaming storage unavailable");
        File storage=Environment.getExternalStorageDirectory();
        String root=storage==null?data.getAbsolutePath():storage.getAbsolutePath();
        stage=6;int started=new com.pp.hls().load(profile.getString("nativeKey"),profile.getString("nativePackage"),profile.getString("nativeVersion"),root,data.getAbsolutePath(),configuration,"1");
        nativeResult=started;
        stage=7;
        if(started<1 || started>65535)throw new IOException("Streaming initialization failed");
        return started;
    }
    private void loadLibrary(String provider,String component,String filename) throws IOException {
        // Preserve the library basename used by the SDK, with separate process and filesystem namespaces.
        File source=new File(getApplicationInfo().nativeLibraryDir,"libveyra_"+provider+"_"+component+".so");
        File directory=new File(getCodeCacheDir(),"runtime-v4/"+provider);
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("Cannot prepare native runtime");
        File target=new File(directory,filename);
        if(!target.isFile()){
            try(InputStream in=new FileInputStream(source);OutputStream out=new FileOutputStream(target)){
                byte[] bytes=new byte[16384];int count;while((count=in.read(bytes))!=-1)out.write(bytes,0,count);
            }catch(IOException e){target.delete();throw e;}
            if(!target.setReadOnly()){target.delete();throw new IOException("Cannot protect native runtime");}
        }
        System.load(target.getAbsolutePath());
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
