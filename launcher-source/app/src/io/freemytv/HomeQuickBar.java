package io.freemytv;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.*;
import android.net.wifi.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Actual traffic deltas for the whole TV; never a claimed Internet capacity/speed test. */
final class HomeQuickBar extends LinearLayout {
    private final Activity activity;
    private final Handler handler=new Handler();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Click sound;
    private final Button network;
    private final float scale;
    private long rx=-1,tx=-1,at,lastName;
    private String name="Mạng";
    private boolean active,closed,querying;
    HomeQuickBar(Activity a){super(a);activity=a;sound=new Click(a);scale=a.getResources().getDisplayMetrics().widthPixels/1280f;setGravity(Gravity.CENTER_VERTICAL);setPadding(s(18),s(6),s(18),s(6));setBackgroundColor(0xe6102035);
        network=button("Mạng\n↓ —  ↑ —",()->openSettings(Settings.ACTION_WIFI_SETTINGS,Settings.ACTION_WIRELESS_SETTINGS));addView(network,new LayoutParams(0,-1,1));
        add("⇄  Đổi nguồn",170,()->AppCatalogActivity.open(a,true));
        add("▣  Video",145,()->openSettings(Settings.ACTION_DISPLAY_SETTINGS));
        add("▦  Ứng dụng",160,()->AppCatalogActivity.open(a,false));
        add("⚙  Cài đặt",145,()->a.startActivity(new Intent(a,SettingsActivity.class)));
    }
    private int s(float n){return Math.round(n*scale);}
    private Button button(String label,Runnable action){Button b=new Button(activity);b.setText(label);b.setAllCaps(false);b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(15));b.setTextColor(Color.WHITE);b.setPadding(s(10),0,s(10),0);b.setMinHeight(0);b.setMinimumHeight(0);b.setSoundEffectsEnabled(false);b.setMaxLines(2);b.setEllipsize(android.text.TextUtils.TruncateAt.END);b.setOnFocusChangeListener((v,f)->{GradientDrawable d=new GradientDrawable();d.setCornerRadius(s(9));d.setColor(f?0xffcce9ff:0x553b5874);b.setBackground(d);b.setTextColor(f?0xff133c64:Color.WHITE);if(f)sound.move();});GradientDrawable d=new GradientDrawable();d.setCornerRadius(s(9));d.setColor(0x553b5874);b.setBackground(d);b.setOnClickListener(v->{sound.open();action.run();});return b;}
    private void add(String label,int width,Runnable action){Button b=button(label,action);LayoutParams p=new LayoutParams(s(width),-1);p.leftMargin=s(10);addView(b,p);}
    private void openSettings(String... actions){for(String action:actions)try{activity.startActivity(new Intent(action));return;}catch(ActivityNotFoundException|SecurityException ignored){}Toast.makeText(activity,"TV chưa có màn hình cài đặt này; mở cài đặt hệ thống.",Toast.LENGTH_LONG).show();try{activity.startActivity(new Intent(Settings.ACTION_SETTINGS));}catch(Exception ignored){}}
    private final Runnable tick=new Runnable(){public void run(){if(!active)return;long now=SystemClock.elapsedRealtime(),r=TrafficStats.getTotalRxBytes(),t=TrafficStats.getTotalTxBytes();ConnectivityManager cm=(ConnectivityManager)activity.getSystemService(Context.CONNECTIVITY_SERVICE);NetworkInfo info=cm.getActiveNetworkInfo();boolean connected=info!=null&&info.isConnected();String down="—",up="—";if(connected&&at>0&&rx>=0&&tx>=0&&r>=rx&&t>=tx&&now>at){down=AppMetrics.bytes(AppMetrics.rate(rx,r,now-at))+"/s";up=AppMetrics.bytes(AppMetrics.rate(tx,t,now-at))+"/s";}rx=r;tx=t;at=now;
        if(!connected)name="Chưa kết nối mạng";else if(info.getType()==ConnectivityManager.TYPE_ETHERNET)name="Ethernet / LAN";else if(info.getType()==ConnectivityManager.TYPE_WIFI){if(name.equals("Chưa kết nối mạng")||name.equals("Mạng")||name.equals("Ethernet / LAN"))name="Wi-Fi";if(now-lastName>15000&&!querying){lastName=now;readName();}}else name=info.getTypeName();network.setText(name+"\nTV  ↓ "+down+"   ↑ "+up);handler.postDelayed(this,1000);
    }};
    private void readName(){querying=true;worker.execute(()->{String found=null;try{WifiManager wifi=(WifiManager)activity.getApplicationContext().getSystemService(Context.WIFI_SERVICE);String ssid=wifi.getConnectionInfo().getSSID();if(ssid!=null&&!ssid.equals("<unknown ssid>"))found=ssid.replaceAll("^\"|\"$","");}catch(Exception ignored){}if(found==null)try(Adb adb=Adb.connect(activity,3000,null)){Matcher m=Pattern.compile("type=WIFI[^\\n]*?networkId=\"([^\"]+)\"").matcher(adb.shell("dumpsys netstats"));if(m.find())found=m.group(1);}catch(Exception ignored){}final String value=found;handler.post(()->{querying=false;if(!closed&&value!=null)name=value;});});}
    void resume(){if(active)return;active=true;rx=tx=-1;at=lastName=0;handler.removeCallbacks(tick);tick.run();}
    void pause(){active=false;handler.removeCallbacks(tick);rx=tx=-1;at=0;}
    void destroy(){closed=true;pause();handler.removeCallbacksAndMessages(null);worker.shutdownNow();sound.release();}
}
