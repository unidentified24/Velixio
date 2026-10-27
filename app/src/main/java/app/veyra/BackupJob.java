package app.veyra;
import android.app.job.*;
public final class BackupJob extends JobService {
 private Thread work;
 public boolean onStartJob(JobParameters p){work=new Thread(()->{try{AutoBackup.write(this,false);}catch(Exception ignored){}jobFinished(p,false);},"scheduled-backup");work.start();return true;}
 public boolean onStopJob(JobParameters p){if(work!=null)work.interrupt();return true;}
}
