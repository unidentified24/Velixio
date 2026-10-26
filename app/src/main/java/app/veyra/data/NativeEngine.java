package app.veyra.data;

import android.content.*;
import android.os.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/** Each streaming SDK lives in its own bound process and owns its own loopback port. */
public final class NativeEngine {
    @android.annotation.SuppressLint("StaticFieldLeak") // Application contexts only.
    private static final Map<String,NativeEngine> engines=new HashMap<>();
    private final Context context;
    private final String provider;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Object lock=new Object();
    private Connection connection;
    private CompletableFuture<Integer> pending;
    private volatile int port;
    private NativeEngine(Context context,String provider){this.context=context.getApplicationContext();this.provider=provider;}
    public static synchronized NativeEngine get(Context context,String provider) {
        if(!provider.equals("current") && !provider.equals("classic"))throw new IllegalArgumentException("Unknown engine");
        NativeEngine engine=engines.get(provider);
        if(engine==null){engine=new NativeEngine(context,provider);engines.put(provider,engine);}return engine;
    }
    public int port(){return port;}
    public int start(String configuration) throws IOException {
        if(Looper.myLooper()==Looper.getMainLooper())throw new IOException("Streaming setup must run off the UI thread");
        CompletableFuture<Integer> attempt;
        synchronized(lock) {
            if(port>0)return port;
            if(pending==null || pending.isDone()) {
                pending=new CompletableFuture<>();
                Connection next=new Connection(configuration,pending);connection=next;
                main.post(()->bind(next));
            }
            attempt=pending;
        }
        try{return attempt.get(45,TimeUnit.SECONDS);}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Streaming setup interrupted");}
        catch(TimeoutException e){close();throw new IOException("The streaming service timed out (S08). Please retry.");}
        catch(ExecutionException e){close();throw new IOException(e.getCause() instanceof IOException?e.getCause().getMessage():"The streaming service stopped (S09). Please retry.");}
    }
    private void bind(Connection next) {
        synchronized(lock){if(connection!=next)return;}
        Intent intent=new Intent(context,provider.equals("classic")?ClassicEngineService.class:CurrentEngineService.class);
        try {
            next.bound=context.bindService(intent,next,Context.BIND_AUTO_CREATE);
            if(!next.bound)next.fail();
        }catch(RuntimeException e){next.fail();}
    }
    private final class Connection implements ServiceConnection {
        final String configuration;
        final CompletableFuture<Integer> result;
        boolean bound;
        final Messenger replies;
        Connection(String configuration,CompletableFuture<Integer> result) {
            this.configuration=configuration;this.result=result;
            replies=new Messenger(new Handler(Looper.getMainLooper(),message->{
                synchronized(lock) {
                    if(connection!=this)return true;
                    if(message.what==NativeEngineService.READY && message.arg1>0 && message.arg1<=65535){port=message.arg1;result.complete(port);}
                    else {String reason=message.getData().getString("reason","");if(!reason.matches("[A-Z_]{0,32}"))reason="UNKNOWN";fail("S0"+Math.max(1,Math.min(7,message.arg1))+(reason.isEmpty()?"":"/"+reason)+(message.arg1==7?"; result "+message.arg2:""));}
                }
                return true;
            }));
        }
        public void onServiceConnected(ComponentName name,IBinder service) {
            synchronized(lock){if(connection!=this)return;}
            try {
                Message message=Message.obtain(null,NativeEngineService.START);Bundle data=new Bundle();data.putString("configuration",configuration);message.setData(data);message.replyTo=replies;
                new Messenger(service).send(message);
            }catch(RemoteException e){fail();}
        }
        public void onServiceDisconnected(ComponentName name){fail();}
        public void onBindingDied(ComponentName name){fail();}
        public void onNullBinding(ComponentName name){fail();}
        void fail(){fail("S09");}
        void fail(String code){invalidate(this,code);}
    }
    private void invalidate(Connection failed,String code) {
        synchronized(lock){if(connection!=failed)return;connection=null;port=0;pending=null;failed.result.completeExceptionally(new IOException("The streaming service could not start ("+code+"). Please retry."));}
        main.post(()->{if(failed.bound){try{context.unbindService(failed);}catch(IllegalArgumentException ignored){}failed.bound=false;}});
    }
    public void close() {
        Connection old;
        synchronized(lock){old=connection;connection=null;port=0;if(pending!=null && !pending.isDone())pending.completeExceptionally(new IOException("Streaming component stopped"));pending=null;}
        if(old!=null)main.post(()->{if(old.bound){try{context.unbindService(old);}catch(IllegalArgumentException ignored){}old.bound=false;}});
    }
    public static synchronized void closeAll(){for(NativeEngine engine:engines.values())engine.close();}
}
