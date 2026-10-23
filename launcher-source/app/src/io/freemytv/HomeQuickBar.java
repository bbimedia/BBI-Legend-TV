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
    private final boolean xp,vista,mac;
    private long rx=-1,tx=-1,at,lastName;
    private String name="Mạng";
    private boolean active,closed,querying;
    HomeQuickBar(Activity a){super(a);activity=a;String theme=PortableControl.theme(a);xp=theme.startsWith("xp");vista=theme.startsWith("vista");mac=theme.startsWith("macos");sound=new Click(a);scale=a.getResources().getDisplayMetrics().widthPixels/1280f;setGravity(Gravity.CENTER_VERTICAL);setPadding(s(18),s(6),s(18),s(6));setBackground(barBackground());setElevation(s(mac?10:vista?8:3));
        network=button("Mạng\n↓ —  ↑ —",()->openSettings(Settings.ACTION_WIFI_SETTINGS,Settings.ACTION_WIRELESS_SETTINGS));addView(network,new LayoutParams(0,-1,1));
        add("⇄  Đổi nguồn",170,()->AppCatalogActivity.open(a,true));
        add("▣  Video",145,()->openSettings(Settings.ACTION_DISPLAY_SETTINGS));
        add("▦  Ứng dụng",160,()->AppCatalogActivity.open(a,false));
        add("⚙  Cài đặt",145,()->a.startActivity(new Intent(a,SettingsActivity.class)));
    }
    private int s(float n){return Math.round(n*scale);}
    private GradientDrawable gradient(int top,int bottom,int radius,int stroke){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});d.setCornerRadius(s(radius));d.setStroke(s(1),stroke);return d;}
    private GradientDrawable barBackground(){
        if(xp)return gradient(0xff4e9eff,0xff0753cb,0,0xff8ec2ff);
        if(vista)return gradient(0xb385c6d1,0xda173445,10,0x999bdae7);
        if(mac)return gradient(0xebffffff,0xc9dce6f8,18,0xccffffff);
        return gradient(0xe6223751,0xe6102035,0,0x334a6483);
    }
    private void styleButton(Button b,boolean focused){
        int top,bottom,stroke,ink,radius;
        if(xp){top=focused?0xfffff4be:0xff4998ff;bottom=focused?0xffffc65b:0xff1461d1;stroke=focused?0xffffe59a:0xff86b9fb;ink=focused?0xff163a72:Color.WHITE;radius=4;}
        else if(vista){top=focused?0xffd2f8ff:0x557ad0e0;bottom=focused?0xff68bddc:0x55334f62;stroke=focused?0xffe3fcff:0x7795d0df;ink=focused?0xff123f58:0xfff3fbff;radius=7;}
        else if(mac){top=focused?0xff70a9f7:0x22ffffff;bottom=focused?0xff387bdc:0x11edf4ff;stroke=focused?0xffc3ddff:0x33596d90;ink=focused?Color.WHITE:0xff253c59;radius=12;}
        else{top=focused?0xffd7edff:0x553b5874;bottom=focused?0xffa8d4f4:0x55233a52;stroke=focused?0xffe7f5ff:0x33526e8d;ink=focused?0xff133c64:Color.WHITE;radius=9;}
        b.setBackground(gradient(top,bottom,radius,stroke));b.setTextColor(ink);b.setElevation(focused?s(mac?3:2):0);
    }
    private Button button(String label,Runnable action){Button b=new Button(activity);b.setText(label);b.setAllCaps(false);b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(15));b.setTypeface(android.graphics.Typeface.create("sans-serif-medium",android.graphics.Typeface.NORMAL));b.setPadding(s(10),0,s(10),0);b.setMinHeight(0);b.setMinimumHeight(0);b.setSoundEffectsEnabled(false);b.setMaxLines(2);b.setEllipsize(android.text.TextUtils.TruncateAt.END);b.setOnFocusChangeListener((v,f)->{styleButton(b,f);if(f)sound.move();});styleButton(b,false);b.setOnClickListener(v->{sound.open();action.run();});return b;}
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
