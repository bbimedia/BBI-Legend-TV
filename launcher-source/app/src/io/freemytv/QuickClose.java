package io.freemytv;

import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.util.AtomicFile;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;

/** Explicit, opt-in force stop. Never disables packages or changes app data. */
final class QuickClose {
    private static final ExecutorService WORK = Executors.newSingleThreadExecutor();
    private static final Set<String> SYSTEM_APPS = new HashSet<>(Arrays.asList(
        "com.google.android.youtube.tv", "com.android.vending", "com.netflix.ninja",
        "com.amazon.amazonvideo.livingroom", "com.google.android.videos", "com.spotify.tv.android"));
    private static int presses;
    private static long firstPress;
    private static String candidate;
    private static boolean busy;
    static boolean enabled(Context c) { return "1".equals(read(c,"quick-close-enabled")); }
    static void enabled(Context c, boolean on) { write(c,"quick-close-enabled",on?"1":"0"); clearTarget(c); reset(); }
    static boolean eligible(Context c,String pkg) {
        if(pkg==null || !pkg.matches("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+") || pkg.equals(c.getPackageName())) return false;
        try {
            ApplicationInfo info=c.getPackageManager().getApplicationInfo(pkg,0);
            if((info.flags & ApplicationInfo.FLAG_PERSISTENT)!=0) return false;
            if((info.flags & (ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0 && !SYSTEM_APPS.contains(pkg)) return false;
            if(HomeApps.launchIntent(c.getPackageManager(),pkg)==null) return false;
            Intent homes=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            for(ResolveInfo ri:c.getPackageManager().queryIntentActivities(homes,0)) if(pkg.equals(ri.activityInfo.packageName)) return false;
            Intent keyboards=new Intent("android.view.InputMethod");
            for(ResolveInfo ri:c.getPackageManager().queryIntentServices(keyboards,0)) if(pkg.equals(ri.serviceInfo.packageName)) return false;
            return true;
        } catch(PackageManager.NameNotFoundException e) { return false; }
    }
    static void foreground(Context c,String pkg) {
        if(!enabled(c) || pkg==null || pkg.equals(c.getPackageName()) || pkg.equals("com.android.systemui")) return;
        
        if(eligible(c,pkg)) {
            try { JSONObject state=new JSONObject();state.put("package",pkg);state.put("boot",android.provider.Settings.Global.getInt(c.getContentResolver(),"boot_count",-1));state.put("elapsed",SystemClock.elapsedRealtime());write(c,"quick-close-target",state.toString()); } catch(Exception ignored) {}
        } else clearTarget(c);
    }
    static void clearTarget(Context c) {  write(c,"quick-close-target",""); }
    private static String target(Context c) {
        try {JSONObject j=new JSONObject(read(c,"quick-close-target"));long age=SystemClock.elapsedRealtime()-j.getLong("elapsed");return j.optInt("boot",-2)==android.provider.Settings.Global.getInt(c.getContentResolver(),"boot_count",-1) && age>=0 && age<86400000 ? j.getString("package") : null;}catch(Exception e){return null;}
    }
    static void home(Activity a,Intent intent) {
        if(!enabled(a) || busy || intent==null || !Intent.ACTION_MAIN.equals(intent.getAction()) || !intent.hasCategory(Intent.CATEGORY_HOME)) { reset();return; }
        
        long now=SystemClock.elapsedRealtime();
        if(presses==0 || now-firstPress>1800) {firstPress=now;presses=1;candidate=target(a);return;}
        if(++presses<3)return;
        final String pkg=candidate;reset();
        if(pkg==null || !pkg.equals(target(a)) || !eligible(a,pkg)) {Toast.makeText(a,"Không có ứng dụng có thể đóng",Toast.LENGTH_SHORT).show();return;}
        busy=true;
        Context app=a.getApplicationContext();
        WORK.execute(()->{
            String message;
            try(Adb adb=Adb.connect(app,5000,null)) {
                // Re-check immediately before the only package-changing command.
                if(!enabled(app)||!pkg.equals(target(app))||!eligible(app,pkg)) throw new IOException("Thao tác đã hủy");
                String result=adb.shell("am force-stop --user 0 "+pkg).trim();
                if(!result.isEmpty())throw new IOException(result);
                String label=String.valueOf(app.getPackageManager().getApplicationLabel(app.getPackageManager().getApplicationInfo(pkg,0)));
                clearTarget(app);message="Đã đóng hẳn "+label;android.util.Log.i("FreeMyTV","QuickClose stopped "+pkg);
            }catch(Exception e){message="Không đóng được ứng dụng: "+e.getMessage();AppLog.w("QuickClose failed: "+e.getMessage());}
            final String toast=message;new Handler(Looper.getMainLooper()).post(()->{busy=false;Toast.makeText(app,toast,Toast.LENGTH_LONG).show();});
        });
    }
    private static void reset(){presses=0;candidate=null;firstPress=0;}
    private static String read(Context c,String name){try{return new String(new AtomicFile(new File(c.getFilesDir(),name)).readFully(),StandardCharsets.UTF_8).trim();}catch(IOException e){return "";}}
    private static void write(Context c,String name,String value){AtomicFile f=new AtomicFile(new File(c.getFilesDir(),name));FileOutputStream out=null;try{out=f.startWrite();out.write(value.getBytes(StandardCharsets.UTF_8));f.finishWrite(out);}catch(IOException e){if(out!=null)f.failWrite(out);AppLog.w("QuickClose state: "+e.getMessage());}}
}





