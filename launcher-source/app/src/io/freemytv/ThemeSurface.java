package io.freemytv;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

/** A single APK keeps preferences and recovery intact when changing the desktop. */
final class ThemeSurface extends FrameLayout {
    private final View surface;
    private final HomeQuickBar quickBar;
    ThemeSurface(Activity a) {
        super(a);
        String theme=PortableControl.theme(a);
        surface=theme.endsWith("-widgets")?new WidgetHomeView(a,theme):"xp".equals(theme)?new XpHomeView(a):"vista".equals(theme)?new VistaHomeView(a):"macos".equals(theme)?new MacHomeView(a):new HomeView(a);
        LayoutParams body=new LayoutParams(-1,-1);if(surface instanceof HomeView)body.topMargin=Math.round(64*a.getResources().getDisplayMetrics().widthPixels/1280f);addView(surface,body);
        quickBar=new HomeQuickBar(a);addView(quickBar,new LayoutParams(-1,Math.round(64*a.getResources().getDisplayMetrics().widthPixels/1280f)));
    }
    void resume(){quickBar.resume();if(surface instanceof HomeView)((HomeView)surface).resume();else if(surface instanceof XpHomeView)((XpHomeView)surface).resume();else if(surface instanceof VistaHomeView)((VistaHomeView)surface).resume();else if(surface instanceof WidgetHomeView)((WidgetHomeView)surface).resume();else ((MacHomeView)surface).resume();}
    void pause(){quickBar.pause();if(surface instanceof HomeView)((HomeView)surface).pause();else if(surface instanceof XpHomeView)((XpHomeView)surface).pause();else if(surface instanceof VistaHomeView)((VistaHomeView)surface).pause();else if(surface instanceof WidgetHomeView)((WidgetHomeView)surface).pause();else ((MacHomeView)surface).pause();}
    void destroy(){quickBar.destroy();if(surface instanceof HomeView)((HomeView)surface).destroy();else if(surface instanceof XpHomeView)((XpHomeView)surface).destroy();else if(surface instanceof VistaHomeView)((VistaHomeView)surface).destroy();else if(surface instanceof WidgetHomeView)((WidgetHomeView)surface).destroy();else ((MacHomeView)surface).destroy();}
    void reset(boolean focus){if(surface instanceof HomeView)((HomeView)surface).reset(focus);else if(surface instanceof XpHomeView)((XpHomeView)surface).reset(focus);else if(surface instanceof VistaHomeView)((VistaHomeView)surface).reset(focus);else if(surface instanceof WidgetHomeView)((WidgetHomeView)surface).reset(focus);else ((MacHomeView)surface).reset(focus);}
    boolean onKey(KeyEvent e){if(quickBar.hasFocus())return false;return surface instanceof HomeView?((HomeView)surface).onKey(e):surface instanceof XpHomeView?((XpHomeView)surface).onKey(e):surface instanceof VistaHomeView?((VistaHomeView)surface).onKey(e):surface instanceof WidgetHomeView?((WidgetHomeView)surface).onKey(e):((MacHomeView)surface).onKey(e);}
}
