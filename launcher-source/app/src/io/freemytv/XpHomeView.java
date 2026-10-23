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

/** Remote-friendly XP desktop. System switching and recovery remain in HomeMode. */
final class XpHomeView extends android.widget.FrameLayout {
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

    XpHomeView(Activity a) {
        super(a); activity = a;click=new Click(a);getViewTreeObserver().addOnGlobalFocusChangeListener(focusSound);
        scale = a.getResources().getDisplayMetrics().widthPixels / 1280f;
        addView(new Wallpaper(a), new LayoutParams(-1,-1));
        desktop = new LinearLayout(a); desktop.setOrientation(LinearLayout.VERTICAL);
        LayoutParams dp = new LayoutParams(s(220), -2); dp.leftMargin=s(38); dp.topMargin=s(85); addView(desktop,dp);

        LinearLayout window = new LinearLayout(a); window.setOrientation(LinearLayout.VERTICAL);
        window.setPadding(s(4),s(4),s(4),s(4)); window.setBackground(bg(0xff0755df,0xff003bb5,8));
        LayoutParams wp = new LayoutParams(s(820),s(480), Gravity.CENTER); wp.leftMargin=s(150); wp.bottomMargin=s(38); addView(window,wp);window.setVisibility(GONE);
        LinearLayout bar = new LinearLayout(a); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(s(12),0,s(8),0);
        bar.setBackground(bg(0xff4097ff,0xff0051da,5));
        title = text("My Television",25,Color.WHITE,true); bar.addView(title,new LinearLayout.LayoutParams(0,s(48),1));
        TextView badge=text("XP",22,Color.WHITE,true); bar.addView(badge); window.addView(bar);
        LinearLayout tools = new LinearLayout(a); tools.setPadding(s(14),s(9),s(14),s(9)); tools.setBackgroundColor(0xffece9d8);
        tools.addView(text("File     View     Favorites     Tools     Help",16,0xff222222,false)); window.addView(tools);
        windowBody=new LinearLayout(a); windowBody.setOrientation(LinearLayout.VERTICAL); windowBody.setPadding(s(26),s(20),s(26),s(18));
        windowBody.setBackgroundColor(0xfffffff7); window.addView(windowBody,new LinearLayout.LayoutParams(-1,0,1));
        windowBody.addView(text("Welcome home.",34,0xff16449c,true));
        TextView sub=text("Your TV, with a little nostalgia.",19,0xff555555,false); sub.setPadding(0,s(5),0,s(20)); windowBody.addView(sub);
        LinearLayout actions=new LinearLayout(a); windowBody.addView(actions);
        addAction(actions,"All Programs",() -> showPrograms(false));
        addAction(actions,"HDMI Inputs",() -> showPrograms(true));
        addAction(actions,"Control Panel",() -> launch(HomeApps.settings()));
        TextView help=text("REMOTE CONTROL\n\nArrow keys  •  Select an icon\nOK  •  Open     Back  •  Start menu",17,0xff49627d,false);
        help.setPadding(0,s(22),0,0); windowBody.addView(help);

        LinearLayout taskbar=new LinearLayout(a); taskbar.setGravity(Gravity.CENTER_VERTICAL); taskbar.setBackground(bg(0xff3889ff,0xff1357d4,0));
        LayoutParams tp=new LayoutParams(-1,s(56),Gravity.BOTTOM); addView(taskbar,tp);
        start=button("▦  start",true); start.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(25)); start.setTypeface(Typeface.DEFAULT,Typeface.BOLD_ITALIC);
        start.setBackground(bg(0xff68b75b,0xff23772c,9)); start.setOnFocusChangeListener((v,f)->v.setAlpha(f?1f:0.86f)); start.setOnClickListener(v->toggleStart());
        taskbar.addView(start,new LinearLayout.LayoutParams(s(156),-1));
        TextView task=text("  ▣  My Television",18,Color.WHITE,true); task.setBackground(bg(0xff5299f6,0xff2670db,3));
        LinearLayout.LayoutParams taskp=new LinearLayout.LayoutParams(s(250),s(40)); taskp.leftMargin=s(14); taskbar.addView(task,taskp);task.setVisibility(GONE);
        View spacer=new View(a); taskbar.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        clock=text("",19,Color.WHITE,false); clock.setGravity(Gravity.CENTER); clock.setBackground(bg(0xff22bafa,0xff078de3,0)); taskbar.addView(clock,new LinearLayout.LayoutParams(s(142),-1));

        popup=new FrameLayout(a); popup.setBackground(bg(0xff247df0,0xff0751c9,6)); popup.setPadding(s(5),s(5),s(5),s(5)); popup.setVisibility(GONE);
        LayoutParams pp=new LayoutParams(s(590),s(540),Gravity.BOTTOM|Gravity.LEFT); pp.leftMargin=s(12); pp.bottomMargin=s(56); addView(popup,pp);
        LinearLayout menu=new LinearLayout(a); menu.setOrientation(LinearLayout.VERTICAL); popup.addView(menu,new LayoutParams(-1,-1));
        TextView user=text("  ☺  Xiaomi TV • XP Edition",25,Color.WHITE,true); user.setGravity(Gravity.CENTER_VERTICAL); menu.addView(user,new LinearLayout.LayoutParams(-1,s(62)));
        scroll=new ScrollView(a); scroll.setBackgroundColor(0xfff8f8ef); menu.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        TextView footer=text("  Back: close menu     •     OK: open",16,Color.WHITE,false); footer.setGravity(Gravity.CENTER_VERTICAL); menu.addView(footer,new LinearLayout.LayoutParams(-1,s(40)));
        loader.execute(()->{
            java.util.List<HomeApps.Item> items=HomeApps.shelf(a,false); items.addAll(HomeApps.others(a));
            handler.post(()->{if(!alive)return; apps.addAll(items); populateDesktop();});
        });
        setFocusable(false);
    }
    private int s(float n){return Math.round(n*scale);}
    private GradientDrawable bg(int top,int bottom,int r){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});d.setCornerRadius(s(r));return d;}
    private TextView text(String t,int size,int color,boolean bold){TextView v=new TextView(activity);v.setText(t);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(size));v.setTextColor(color);v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return v;}
    private Button button(String label,boolean dark){Button b=new Button(activity); b.setText(label);b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(18));b.setTextColor(dark?Color.WHITE:0xff153c76);b.setAllCaps(false);b.setPadding(s(12),s(8),s(12),s(8));b.setFocusable(true);b.setSoundEffectsEnabled(false);b.setOnFocusChangeListener((v,f)->{v.setBackground(bg(f?0xffffd369:0xfffaf9f0,f?0xffffb52d:0xffdedbd0,4));b.setTextColor(0xff153c76);}); b.setBackground(bg(0xfffaf9f0,0xffdedbd0,4));return b;}
    private void addAction(LinearLayout row,String label,Runnable action){Button b=button(label,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,s(66),1);p.setMargins(0,0,s(10),0);row.addView(b,p);b.setOnClickListener(v->action.run());}
    private void populateDesktop(){desktop.removeAllViews();int count=0;for(HomeApps.Item it:apps){if(it.kind!=HomeApps.APP)continue;if(count++>=5)break;
        LinearLayout icon=new LinearLayout(activity);icon.setOrientation(LinearLayout.VERTICAL);icon.setGravity(Gravity.CENTER);icon.setPadding(s(8),s(7),s(8),s(7));icon.setFocusable(true);icon.setSoundEffectsEnabled(false);icon.setContentDescription(it.label);
        ImageView image=new ImageView(activity);image.setScaleType(ImageView.ScaleType.FIT_CENTER);if(it.logo!=null)image.setImageBitmap(it.logo);else if(it.tile!=null)image.setImageBitmap(it.tile); icon.addView(image,new LinearLayout.LayoutParams(s(92),s(57)));
        TextView name=text(it.label,17,Color.WHITE,true);name.setShadowLayer(s(2),s(1),s(2),0xff152d39);name.setGravity(Gravity.CENTER);icon.addView(name,new LinearLayout.LayoutParams(-1,s(31)));
        icon.setOnFocusChangeListener((v,f)->v.setBackgroundColor(f?0x99316ac5:Color.TRANSPARENT));icon.setOnClickListener(v->launch(it));desktop.addView(icon,new LinearLayout.LayoutParams(s(172),s(108)));if(firstDesktop==null)firstDesktop=icon;
    }if(firstDesktop!=null)firstDesktop.requestFocus();}
    private void launch(HomeApps.Item it){click.open();HomeApps.open(activity,it,this,0,0,getWidth(),getHeight());}
    private void showPrograms(boolean inputs){click.open();AppCatalogActivity.open(activity,inputs);}
    private void toggleStart(){showPrograms(false);}
    void resume(){handler.removeCallbacks(tick);tick.run();}
    void pause(){handler.removeCallbacks(tick);}
    void destroy(){getViewTreeObserver().removeOnGlobalFocusChangeListener(focusSound);click.release();alive=false;handler.removeCallbacksAndMessages(null);loader.shutdownNow();}
    void reset(boolean focus){popup.setVisibility(GONE);if(firstDesktop!=null)firstDesktop.requestFocus();else start.requestFocus();}
    private void clickMove(){click.move();}
    boolean onKey(KeyEvent e){int key=e.getKeyCode();if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){navigationKey=e.getAction()==KeyEvent.ACTION_DOWN;return false;}navigationKey=false;if(e.getKeyCode()==KeyEvent.KEYCODE_BACK||e.getKeyCode()==KeyEvent.KEYCODE_MENU){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0)toggleStart();return true;}return false;}

    /** Original vector landscape: cached by Android's display list, no per-frame allocations. */
    private final class Wallpaper extends View {private final ThemeWallpaper shared=new ThemeWallpaper();
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path hill=new Path();
        Wallpaper(Activity a){super(a);}
        protected void onDraw(Canvas c){shared.draw(c,"xp",getWidth(),getHeight());}
    }
}

