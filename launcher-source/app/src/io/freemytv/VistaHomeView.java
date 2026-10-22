package io.freemytv;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** Remote-friendly Vista Aero desktop. System switching and recovery remain in HomeMode. */
final class VistaHomeView extends android.widget.FrameLayout {
    private final Activity activity;
    private final Click click;
    private boolean navigationKey;
    private final android.view.ViewTreeObserver.OnGlobalFocusChangeListener focusSound = (oldFocus,newFocus)->{if(navigationKey && oldFocus!=null && newFocus!=null)clickMove();};
    private final float scale;
    private final Handler handler = new Handler();
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final LinearLayout desktop, windowBody;
    private final TextView clock, title;
    private final Button start;
    private final FrameLayout popup;
    private final ScrollView scroll;
    private final java.util.List<HomeApps.Item> apps = new ArrayList<>();
    private boolean alive = true;
    private View firstDesktop;
    private final Runnable tick = new Runnable() {
        public void run() { clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date())); handler.postDelayed(this, 30000); }
    };

    VistaHomeView(Activity a) {
        super(a); activity = a;click=new Click(a);getViewTreeObserver().addOnGlobalFocusChangeListener(focusSound);
        scale = a.getResources().getDisplayMetrics().widthPixels / 1280f;
        addView(new Wallpaper(a), new LayoutParams(-1,-1));
        desktop = new LinearLayout(a); desktop.setOrientation(LinearLayout.VERTICAL);
        LayoutParams dp = new LayoutParams(s(220), -2); dp.leftMargin=s(38); dp.topMargin=s(42); addView(desktop,dp);

        LinearLayout window = new LinearLayout(a); window.setOrientation(LinearLayout.VERTICAL);
        window.setPadding(s(4),s(4),s(4),s(4)); window.setBackground(bg(0xb84b737d,0xe51b303d,10));
        LayoutParams wp = new LayoutParams(s(720),s(450), Gravity.CENTER); wp.leftMargin=s(30); wp.bottomMargin=s(38); addView(window,wp);
        LinearLayout bar = new LinearLayout(a); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(s(12),0,s(8),0);
        bar.setBackground(bg(0x998eacb8,0xb51c3545,7));
        title = text("Windows Media Center",25,Color.WHITE,true); bar.addView(title,new LinearLayout.LayoutParams(0,s(48),1));
        TextView badge=text("AERO",22,Color.WHITE,true); bar.addView(badge); window.addView(bar);
        LinearLayout tools = new LinearLayout(a); tools.setPadding(s(14),s(9),s(14),s(9)); tools.setBackgroundColor(0xffdae6eb);
        tools.addView(text("Organize     Views     Media Library",16,0xff222222,false)); window.addView(tools);
        windowBody=new LinearLayout(a); windowBody.setOrientation(LinearLayout.VERTICAL); windowBody.setPadding(s(26),s(20),s(26),s(18));
        windowBody.setBackgroundColor(0xf2edf4f7); window.addView(windowBody,new LinearLayout.LayoutParams(-1,0,1));
        windowBody.addView(text("The view is beautiful.",34,0xff254e64,true));
        TextView sub=text("Windows Vista inspired • Xiaomi TV",19,0xff555555,false); sub.setPadding(0,s(5),0,s(20)); windowBody.addView(sub);
        LinearLayout actions=new LinearLayout(a); windowBody.addView(actions);
        addAction(actions,"All Programs",() -> showPrograms(false));
        addAction(actions,"HDMI Inputs",() -> showPrograms(true));
        addAction(actions,"Control Panel",() -> launch(HomeApps.settings()));
        TextView help=text("REMOTE CONTROL\n\nArrow keys  •  Select an icon\nOK  •  Open     Back  •  Start menu",17,0xff49627d,false);
        help.setPadding(0,s(22),0,0); windowBody.addView(help);

        LinearLayout taskbar=new LinearLayout(a); taskbar.setGravity(Gravity.CENTER_VERTICAL); taskbar.setBackground(bg(0xb5516e7e,0xf20b1823,0));
        LayoutParams tp=new LayoutParams(-1,s(56),Gravity.BOTTOM); addView(taskbar,tp);
        start=button("⊞",true); start.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(40)); start.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        start.setBackground(orb()); start.setOnFocusChangeListener((v,f)->v.setAlpha(f?1f:0.86f)); start.setOnClickListener(v->toggleStart());
        taskbar.addView(start,new LinearLayout.LayoutParams(s(72),s(54)));
        TextView task=text("  ▣  Windows Media Center",18,Color.WHITE,true); task.setBackground(bg(0x995d869c,0xc6274358,5));
        LinearLayout.LayoutParams taskp=new LinearLayout.LayoutParams(s(250),s(40)); taskp.leftMargin=s(14); taskbar.addView(task,taskp);
        View spacer=new View(a); taskbar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        clock=text("",19,Color.WHITE,false); clock.setGravity(Gravity.CENTER); clock.setBackground(bg(0x883f657a,0x99122b3b,0)); taskbar.addView(clock,new LinearLayout.LayoutParams(s(142),-1));

        popup=new FrameLayout(a); popup.setBackground(bg(0xf24d6878,0xfa142631,9)); popup.setPadding(s(5),s(5),s(5),s(5)); popup.setVisibility(GONE);
        LayoutParams pp=new LayoutParams(s(590),s(540),Gravity.BOTTOM|Gravity.LEFT); pp.leftMargin=s(12); pp.bottomMargin=s(56); addView(popup,pp);
        LinearLayout menu=new LinearLayout(a); menu.setOrientation(LinearLayout.VERTICAL); popup.addView(menu,new LayoutParams(-1,-1));
        TextView user=text("  ☺  Xiaomi TV • Vista Edition",25,Color.WHITE,true); user.setGravity(Gravity.CENTER_VERTICAL); menu.addView(user,new LinearLayout.LayoutParams(-1,s(62)));
        scroll=new ScrollView(a); scroll.setBackgroundColor(0xffeaf1f4); menu.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView footer=text("  Back: close menu     •     OK: open",16,Color.WHITE,false); footer.setGravity(Gravity.CENTER_VERTICAL); menu.addView(footer,new LinearLayout.LayoutParams(-1,s(40)));
        loader.execute(()->{
            java.util.List<HomeApps.Item> items=HomeApps.shelf(a,false); items.addAll(HomeApps.others(a));
            handler.post(()->{if(!alive)return; apps.addAll(items); populateDesktop();});
        });
        LinearLayout sidebar=new LinearLayout(a); sidebar.setOrientation(LinearLayout.VERTICAL); sidebar.setPadding(s(16),s(18),s(16),s(18)); sidebar.setBackground(bg(0x66557584,0xbb102536,10));
        LayoutParams sp=new LayoutParams(s(206),s(425),Gravity.RIGHT|Gravity.TOP);sp.rightMargin=s(26);sp.topMargin=s(65);addView(sidebar,sp);
        TextView gadget=text("Windows Sidebar",18,0xffd7efff,true);sidebar.addView(gadget);
        sidebar.addView(new AnalogClock(a),new LinearLayout.LayoutParams(-1,s(170)));
        TextView day=text(new SimpleDateFormat("EEEE\ndd MMMM",Locale.ENGLISH).format(new Date()),24,Color.WHITE,true);day.setGravity(Gravity.CENTER);day.setPadding(0,s(14),0,s(14));sidebar.addView(day);
        TextView note=text("XIAOMI TV\n\nAero desktop\nReady to play",18,0xffc5e5f5,false);note.setGravity(Gravity.CENTER);sidebar.addView(note);
        setFocusable(false);
    }
    private int s(float n){return Math.round(n*scale);}
    private GradientDrawable bg(int top,int bottom,int r){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});d.setCornerRadius(s(r));return d;}
    private TextView text(String t,int size,int color,boolean bold){TextView v=new TextView(activity);v.setText(t);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(size));v.setTextColor(color);v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return v;}
    private Button button(String label,boolean dark){Button b=new Button(activity); b.setText(label);b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(18));b.setTextColor(dark?Color.WHITE:0xff153c76);b.setAllCaps(false);b.setPadding(s(12),s(8),s(12),s(8));b.setFocusable(true);b.setSoundEffectsEnabled(false);b.setOnFocusChangeListener((v,f)->{v.setBackground(bg(f?0xffb9e9ff:0xfff7fbfd,f?0xff67bce4:0xffd4e1e8,4));b.setTextColor(0xff153c76);}); b.setBackground(bg(0xfff7fbfd,0xffd4e1e8,5));return b;}
    private void addAction(LinearLayout row,String label,Runnable action){Button b=button(label,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,s(66),1);p.setMargins(0,0,s(10),0);row.addView(b,p);b.setOnClickListener(v->action.run());}
    private void populateDesktop(){desktop.removeAllViews();int count=0;for(HomeApps.Item it:apps){if(it.kind!=HomeApps.APP)continue;if(count++>=5)break;
        LinearLayout icon=new LinearLayout(activity);icon.setOrientation(LinearLayout.VERTICAL);icon.setGravity(Gravity.CENTER);icon.setPadding(s(8),s(7),s(8),s(7));icon.setFocusable(true);icon.setSoundEffectsEnabled(false);icon.setContentDescription(it.label);
        ImageView image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.FIT_CENTER);if(it.logo!=null)image.setImageBitmap(it.logo);else if(it.tile!=null)image.setImageBitmap(it.tile); icon.addView(image,new LinearLayout.LayoutParams(s(92),s(57)));
        TextView name=text(it.label,17,Color.WHITE,true);name.setShadowLayer(s(2),s(1),s(2),0xff152d39);name.setGravity(Gravity.CENTER);icon.addView(name,new LinearLayout.LayoutParams(-1,s(31)));
        icon.setOnFocusChangeListener((v,f)->v.setBackgroundColor(f?0x886fbadd:Color.TRANSPARENT));icon.setOnClickListener(v->launch(it));desktop.addView(icon,new LinearLayout.LayoutParams(s(172),s(108)));if(firstDesktop==null)firstDesktop=icon;
    }if(firstDesktop!=null)firstDesktop.requestFocus();}
    private void launch(HomeApps.Item it){click.open();HomeApps.open(activity,it,this,0,0,getWidth(),getHeight());}
    private void showPrograms(boolean inputs){LinearLayout list=new LinearLayout(activity);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(s(12),s(8),s(12),s(8));scroll.removeAllViews();scroll.addView(list);
        java.util.List<HomeApps.Item> entries=new ArrayList<>();if(inputs)entries.addAll(HomeApps.allInputs(activity));else{entries.addAll(apps);entries.add(HomeApps.action(HomeApps.ANDROID_SETTINGS,"tv-settings","TV Settings","settings"));}
        for(HomeApps.Item it:entries){Button b=button(it.label,false);b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,s(53));p.bottomMargin=s(4);list.addView(b,p);b.setOnClickListener(v->launch(it));}
        if(entries.isEmpty())list.addView(text("No HDMI inputs available",20,0xff333333,false));popup.setVisibility(VISIBLE);if(list.getChildCount()>0)list.getChildAt(0).requestFocus();}
    private void toggleStart(){if(popup.getVisibility()==VISIBLE){popup.setVisibility(GONE);start.requestFocus();}else showPrograms(false);}
    void resume(){handler.removeCallbacks(tick);tick.run();}
    void pause(){handler.removeCallbacks(tick);}
    void destroy(){getViewTreeObserver().removeOnGlobalFocusChangeListener(focusSound);click.release();alive=false;handler.removeCallbacksAndMessages(null);loader.shutdownNow();}
    void reset(boolean focus){popup.setVisibility(GONE);if(firstDesktop!=null)firstDesktop.requestFocus();else start.requestFocus();}
    private void clickMove(){click.move();}
    boolean onKey(KeyEvent e){int key=e.getKeyCode();if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){navigationKey=e.getAction()==KeyEvent.ACTION_DOWN;return false;}navigationKey=false;if(e.getKeyCode()==KeyEvent.KEYCODE_BACK||e.getKeyCode()==KeyEvent.KEYCODE_MENU){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0)toggleStart();return true;}return false;}

    private GradientDrawable orb(){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xffa7e7ff,0xff2780aa,0xff063356});d.setShape(GradientDrawable.OVAL);d.setStroke(s(2),0xffb9e7f8);return d;}
    private final class AnalogClock extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        AnalogClock(Activity a){super(a);setContentDescription("Analog clock");}
        protected void onDraw(Canvas c){float x=getWidth()/2f,y=getHeight()/2f,r=Math.min(x,y)*0.84f;p.setColor(0xff172a36);c.drawCircle(x,y,r+s(4),p);p.setColor(0xfff0f4f5);c.drawCircle(x,y,r,p);p.setColor(0xff263b49);p.setStrokeWidth(s(2));for(int i=0;i<12;i++){double angle=i*Math.PI/6;c.drawLine(x+(float)Math.sin(angle)*r*.84f,y-(float)Math.cos(angle)*r*.84f,x+(float)Math.sin(angle)*r*.95f,y-(float)Math.cos(angle)*r*.95f,p);}Calendar now=Calendar.getInstance();double hour=(now.get(Calendar.HOUR)+now.get(Calendar.MINUTE)/60.0)*Math.PI/6,minute=now.get(Calendar.MINUTE)*Math.PI/30;p.setStrokeWidth(s(5));c.drawLine(x,y,x+(float)Math.sin(hour)*r*.49f,y-(float)Math.cos(hour)*r*.49f,p);p.setStrokeWidth(s(3));c.drawLine(x,y,x+(float)Math.sin(minute)*r*.72f,y-(float)Math.cos(minute)*r*.72f,p);p.setColor(0xff268cb0);c.drawCircle(x,y,s(5),p);postInvalidateDelayed(30000);}
    }
    /** Vector aurora wallpaper, independent of any external image. */
    private final class Wallpaper extends View {
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path ribbon=new Path();
        Wallpaper(Activity a){super(a);}
        protected void onDraw(Canvas c){c.save();c.scale(getWidth()/1280f,getHeight()/720f);
            p.setShader(new LinearGradient(0,0,1100,720,new int[]{0xff061226,0xff124959,0xff1c6380,0xff070e25},null,Shader.TileMode.CLAMP));c.drawRect(0,0,1280,720,p);
            p.setShader(new RadialGradient(950,365,650,new int[]{0x885dd1ba,0x3343a3bb,0},null,Shader.TileMode.CLAMP));c.drawRect(0,0,1280,720,p);
            ribbon.reset();ribbon.moveTo(-100,730);ribbon.cubicTo(350,650,460,180,1380,80);ribbon.cubicTo(890,230,720,550,1280,670);ribbon.lineTo(1280,820);ribbon.close();p.setShader(new LinearGradient(300,700,1000,150,new int[]{0x003fb9ee,0x7743b8e5,0x4478d5b1,0x0055cdaf},null,Shader.TileMode.CLAMP));c.drawPath(ribbon,p);
            ribbon.reset();ribbon.moveTo(-60,675);ribbon.cubicTo(350,640,650,245,1370,140);p.setShader(null);p.setColor(0x5587e6f9);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(2);c.drawPath(ribbon,p);
            ribbon.reset();ribbon.moveTo(220,780);ribbon.cubicTo(500,490,750,440,1320,570);p.setColor(0x335beab8);p.setStrokeWidth(3);c.drawPath(ribbon,p);p.setStyle(Paint.Style.FILL);c.restore();}
    }
}

