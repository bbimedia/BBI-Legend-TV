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
final class MacHomeView extends android.widget.FrameLayout {
    private final Activity activity;
    private final Click click;
    private boolean navigationKey;
    private final android.view.ViewTreeObserver.OnGlobalFocusChangeListener focusSound = (oldFocus, newFocus) -> { if (navigationKey && oldFocus != null && newFocus != null) clickMove(); };
    private final float scale;
    private final Handler handler = new Handler();
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final LinearLayout desktop, windowBody;
    private final TextView clock, title;
    private final Button start;
    private final FrameLayout popup;
    private final ScrollView scroll;
    private final AppSuggestionPanel suggestions;
    private final java.util.List<HomeApps.Item> apps = new ArrayList<>();
    private boolean alive = true;
    private View firstDesktop;
    private final Runnable tick = new Runnable() {
        public void run() { clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date())); handler.postDelayed(this, 30000); }
    };

    MacHomeView(Activity a) {
        super(a);activity=a;click=new Click(a);getViewTreeObserver().addOnGlobalFocusChangeListener(focusSound);scale=a.getResources().getDisplayMetrics().widthPixels/1280f;
        addView(new Wallpaper(a),new LayoutParams(-1,-1));
        LinearLayout menuBar=new LinearLayout(a);menuBar.setGravity(Gravity.CENTER_VERTICAL);menuBar.setPadding(s(30),0,s(26),0);menuBar.setBackground(bg(0x99ffffff,0x66e5efff,0));
        addView(menuBar,new LayoutParams(-1,s(38),Gravity.TOP));menuBar.setVisibility(GONE);
        TextView menu=text("●    Finder     File     Edit     View     Go     Window     Help",16,0xff14283e,true);menuBar.addView(menu,new LinearLayout.LayoutParams(0,-2,1));
        clock=text("",16,0xff14283e,true);menuBar.addView(clock);
        LinearLayout window=new LinearLayout(a);window.setOrientation(LinearLayout.VERTICAL);window.setPadding(s(2),s(2),s(2),s(2));window.setBackground(bg(0xdfffffff,0xccdce7fa,24));window.setElevation(s(18));
        LayoutParams wp=new LayoutParams(s(1020),s(402),Gravity.CENTER);wp.bottomMargin=s(38);addView(window,wp);window.setVisibility(GONE);
        LinearLayout header=new LinearLayout(a);header.setPadding(s(24),s(16),s(24),s(12));header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout lights=new LinearLayout(a);int[] colors={0xffff6058,0xffffbd2e,0xff28c840};for(int color:colors){View dot=new View(a);GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(color);dot.setBackground(circle);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(s(14),s(14));p.rightMargin=s(9);lights.addView(dot,p);}header.addView(lights);
        title=text("  Xiaomi TV",20,0xff25374d,true);header.addView(title,new LinearLayout.LayoutParams(0,-2,1));header.addView(text("⌕   Applications",16,0xff5d6d84,false));window.addView(header,new LinearLayout.LayoutParams(-1,s(62)));
        LinearLayout content=new LinearLayout(a);window.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout sidebar=new LinearLayout(a);sidebar.setOrientation(LinearLayout.VERTICAL);sidebar.setPadding(s(24),s(18),s(20),s(18));sidebar.setBackground(bg(0x557a9fd1,0x44779bc2,18));sidebar.addView(text("FAVORITES",12,0xff586a83,true));TextView folders=text("Applications\n\nTelevision\n\nHDMI Inputs\n\nSettings",18,0xff293f5b,false);folders.setPadding(0,s(18),0,0);sidebar.addView(folders);content.addView(sidebar,new LinearLayout.LayoutParams(s(204),-1));
        windowBody=new LinearLayout(a);windowBody.setOrientation(LinearLayout.VERTICAL);windowBody.setPadding(s(34),s(24),s(30),s(24));content.addView(windowBody,new LinearLayout.LayoutParams(0,-1,1));windowBody.addView(text("Hello, big screen.",38,0xff14263e,true));TextView subtitle=text("Your favorite apps. A fresh perspective.",19,0xff5e6e85,false);subtitle.setPadding(0,s(8),0,s(28));windowBody.addView(subtitle);
        LinearLayout actions=new LinearLayout(a);windowBody.addView(actions);addAction(actions,"Applications",()->showPrograms(false));addAction(actions,"HDMI Inputs",()->showPrograms(true));addAction(actions,"Settings",()->launch(HomeApps.settings()));TextView help=text("↑ ↓ ← →  Select     •     OK  Open     •     Back  Applications",15,0xff62748e,false);help.setPadding(0,s(24),0,0);windowBody.addView(help);
        TextView edition=text("macOS 27 inspired\nLiquid Glass • TV Edition",15,0xffeef4ff,false);edition.setGravity(Gravity.RIGHT);edition.setShadowLayer(s(2),0,s(1),0x99503b5d);LayoutParams ep=new LayoutParams(-2,-2,Gravity.RIGHT|Gravity.TOP);ep.rightMargin=s(36);ep.topMargin=s(68);addView(edition,ep);edition.setVisibility(GONE);
        LinearLayout dock=new LinearLayout(a);dock.setPadding(s(15),s(8),s(15),s(8));dock.setGravity(Gravity.CENTER);dock.setBackground(bg(0xbbffffff,0x99d1e3fa,28));dock.setElevation(s(14));LayoutParams dp=new LayoutParams(-2,s(122),Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);dp.bottomMargin=s(20);addView(dock,dp);
        suggestions = new AppSuggestionPanel(a, "macos");
        LayoutParams sp = new LayoutParams(s(1200), s(500), Gravity.CENTER_HORIZONTAL);
        sp.topMargin = s(66); addView(suggestions, sp);
        start=button("▦\nApps",false);start.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(23));start.setOnClickListener(v->toggleStart());dock.addView(start,new LinearLayout.LayoutParams(s(98),s(96)));View separator=new View(a);separator.setBackgroundColor(0x44536c8d);LinearLayout.LayoutParams sep=new LinearLayout.LayoutParams(s(1),s(70));sep.setMargins(s(14),0,s(8),0);dock.addView(separator,sep);desktop=new LinearLayout(a);desktop.setOrientation(LinearLayout.HORIZONTAL);dock.addView(desktop,new LinearLayout.LayoutParams(-2,-1));
        popup=new FrameLayout(a);popup.setBackground(bg(0xf2f9fbff,0xf2dce8f8,26));popup.setPadding(s(18),s(12),s(18),s(12));popup.setElevation(s(28));popup.setVisibility(GONE);LayoutParams pp=new LayoutParams(s(690),s(490),Gravity.CENTER);pp.bottomMargin=s(30);addView(popup,pp);LinearLayout panel=new LinearLayout(a);panel.setOrientation(LinearLayout.VERTICAL);popup.addView(panel,new LayoutParams(-1,-1));TextView heading=text("Applications",27,0xff1a2e49,true);heading.setPadding(s(10),s(8),0,s(10));panel.addView(heading);scroll=new ScrollView(a);panel.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));TextView footer=text("Back to close  •  OK to open",14,0xff61748d,false);footer.setGravity(Gravity.CENTER);panel.addView(footer,new LinearLayout.LayoutParams(-1,s(32)));
        loader.execute(()->{java.util.List<HomeApps.Item> items=HomeApps.shelf(a,false);items.addAll(HomeApps.others(a));handler.post(()->{if(!alive)return;apps.addAll(items);populateDesktop();});});setFocusable(false);
    }
    private int s(float n){return Math.round(n*scale);}
    private GradientDrawable bg(int top,int bottom,int r){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{top,bottom});d.setCornerRadius(s(r));return d;}
    private TextView text(String t,int size,int color,boolean bold){TextView v=new TextView(activity);v.setText(t);v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(size));v.setTextColor(color);v.setTypeface(Typeface.create("sans-serif",bold?Typeface.BOLD:Typeface.NORMAL));return v;}
    private Button button(String label,boolean dark){Button b=new Button(activity); b.setText(label);b.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,s(18));b.setTextColor(dark?Color.WHITE:0xff153c76);b.setAllCaps(false);b.setPadding(s(12),s(8),s(12),s(8));b.setFocusable(true);b.setSoundEffectsEnabled(false);b.setClipToOutline(true);b.setOnFocusChangeListener((v,f)->{v.setBackground(bg(f?0xffe5f5ff:0xddffffff,f?0xff91d0ff:0xcce3edf8,20));b.setTextColor(0xff153c76);}); b.setBackground(bg(0xddffffff,0xccdce7f5,20));return b;}
    private void addAction(LinearLayout row,String label,Runnable action){Button b=button(label,false);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,s(66),1);p.setMargins(0,0,s(10),0);row.addView(b,p);b.setOnClickListener(v->action.run());}
    private void populateDesktop(){desktop.removeAllViews();int count=0;for(HomeApps.Item it:apps){if(it.kind!=HomeApps.APP)continue;if(count++>=7)break;
        LinearLayout icon=new LinearLayout(activity);icon.setOrientation(LinearLayout.VERTICAL);icon.setGravity(Gravity.CENTER);icon.setPadding(s(8),s(7),s(8),s(7));icon.setFocusable(true);icon.setSoundEffectsEnabled(false);icon.setContentDescription(it.label);
        ImageView image=new RoundedAppIcon(activity);image.setScaleType(ImageView.ScaleType.FIT_CENTER);if(it.logo!=null)image.setImageBitmap(it.logo);else if(it.tile!=null)image.setImageBitmap(it.tile); icon.addView(image,new LinearLayout.LayoutParams(s(82),s(54)));
        TextView name=text(it.label,13,0xff203448,true);name.setShadowLayer(s(2),s(1),s(2),0x33556c82);name.setGravity(Gravity.CENTER);icon.addView(name,new LinearLayout.LayoutParams(-1,s(31)));
        icon.setOnFocusChangeListener((v,f)->{v.setBackground(bg(f?0x99ffffff:0x00ffffff,f?0x66e5f0ff:0x00ffffff,18)); if(f) suggestions.showItem(it);});icon.setOnClickListener(v->launch(it));desktop.addView(icon,new LinearLayout.LayoutParams(s(116),s(102)));if(firstDesktop==null)firstDesktop=icon;
    }if(firstDesktop!=null)firstDesktop.requestFocus(); if(!apps.isEmpty())suggestions.showItem(apps.get(0));}
    private void launch(HomeApps.Item it){click.open();HomeApps.open(activity,it,this,0,0,getWidth(),getHeight());}
    private void showPrograms(boolean inputs){click.open();AppCatalogActivity.open(activity,inputs);}
    private void toggleStart(){showPrograms(false);}
    void resume(){handler.removeCallbacks(tick);tick.run();}
    void pause(){handler.removeCallbacks(tick);}
    void destroy(){suggestions.destroy();getViewTreeObserver().removeOnGlobalFocusChangeListener(focusSound);click.release();alive=false;handler.removeCallbacksAndMessages(null);loader.shutdownNow();}
    void reset(boolean focus){popup.setVisibility(GONE);if(firstDesktop!=null)firstDesktop.requestFocus();else start.requestFocus();}
    private void clickMove(){click.move();}
    boolean onKey(KeyEvent e){int key=e.getKeyCode();if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT){navigationKey=e.getAction()==KeyEvent.ACTION_DOWN;return false;}navigationKey=false;if(e.getKeyCode()==KeyEvent.KEYCODE_BACK||e.getKeyCode()==KeyEvent.KEYCODE_MENU){if(e.getAction()==KeyEvent.ACTION_DOWN&&e.getRepeatCount()==0)toggleStart();return true;}return false;}

    /** Clip the fitted image itself so letterboxed app banners also get rounded corners. */
    private final class RoundedAppIcon extends ImageView {
        private final Path rounded = new Path();
        private final RectF imageBounds = new RectF();
        RoundedAppIcon(Activity a) { super(a); }
        @Override protected void onDraw(Canvas canvas) {
            android.graphics.drawable.Drawable drawable = getDrawable();
            if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                super.onDraw(canvas); return;
            }
            float fit = Math.min((float)getWidth()/drawable.getIntrinsicWidth(), (float)getHeight()/drawable.getIntrinsicHeight());
            float w = drawable.getIntrinsicWidth()*fit, h = drawable.getIntrinsicHeight()*fit;
            imageBounds.set((getWidth()-w)/2f,(getHeight()-h)/2f,(getWidth()+w)/2f,(getHeight()+h)/2f);
            float radius = Math.min(s(13),h*0.24f);
            rounded.reset(); rounded.addRoundRect(imageBounds,radius,radius,Path.Direction.CW);
            int save=canvas.save(); canvas.clipPath(rounded); super.onDraw(canvas); canvas.restoreToCount(save);
        }
    }
    private final class Wallpaper extends View {private final ThemeWallpaper shared=new ThemeWallpaper();
        private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final Path curve=new Path();
        Wallpaper(Activity a){super(a);}
        protected void onDraw(Canvas c){shared.draw(c,"macos",getWidth(),getHeight());}
    }
}


