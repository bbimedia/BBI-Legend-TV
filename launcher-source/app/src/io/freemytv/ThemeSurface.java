package io.freemytv;

import android.app.Activity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;

/** A single APK keeps preferences and recovery intact when changing the desktop. */
final class ThemeSurface extends FrameLayout {
    private final View surface;
    ThemeSurface(Activity a) {
        super(a);
        String theme=PortableControl.theme(a);
        surface="xp-widgets".equals(theme)?new XpWidgetsView(a):"xp".equals(theme)?new XpHomeView(a):"vista".equals(theme)?new VistaHomeView(a):"macos".equals(theme)?new MacHomeView(a):new HomeView(a);
        addView(surface,new LayoutParams(-1,-1));
    }
    void resume(){if(surface instanceof HomeView)((HomeView)surface).resume();else if(surface instanceof XpHomeView)((XpHomeView)surface).resume();else if(surface instanceof VistaHomeView)((VistaHomeView)surface).resume();else if(surface instanceof XpWidgetsView)((XpWidgetsView)surface).resume();else ((MacHomeView)surface).resume();}
    void pause(){if(surface instanceof HomeView)((HomeView)surface).pause();else if(surface instanceof XpHomeView)((XpHomeView)surface).pause();else if(surface instanceof VistaHomeView)((VistaHomeView)surface).pause();else if(surface instanceof XpWidgetsView)((XpWidgetsView)surface).pause();else ((MacHomeView)surface).pause();}
    void destroy(){if(surface instanceof HomeView)((HomeView)surface).destroy();else if(surface instanceof XpHomeView)((XpHomeView)surface).destroy();else if(surface instanceof VistaHomeView)((VistaHomeView)surface).destroy();else if(surface instanceof XpWidgetsView)((XpWidgetsView)surface).destroy();else ((MacHomeView)surface).destroy();}
    void reset(boolean focus){if(surface instanceof HomeView)((HomeView)surface).reset(focus);else if(surface instanceof XpHomeView)((XpHomeView)surface).reset(focus);else if(surface instanceof VistaHomeView)((VistaHomeView)surface).reset(focus);else if(surface instanceof XpWidgetsView)((XpWidgetsView)surface).reset(focus);else ((MacHomeView)surface).reset(focus);}
    boolean onKey(KeyEvent e){return surface instanceof HomeView?((HomeView)surface).onKey(e):surface instanceof XpHomeView?((XpHomeView)surface).onKey(e):surface instanceof VistaHomeView?((VistaHomeView)surface).onKey(e):surface instanceof XpWidgetsView?((XpWidgetsView)surface).onKey(e):((MacHomeView)surface).onKey(e);}
}
