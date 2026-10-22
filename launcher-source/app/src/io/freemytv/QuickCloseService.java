package io.freemytv;
import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
/** Reads only the active window package. No text, screenshots or key filtering. */
public class QuickCloseService extends AccessibilityService {

    @Override public void onAccessibilityEvent(AccessibilityEvent e){
        if(e.getEventType()==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && e.getPackageName()!=null) {
            if(e.getPackageName().toString().equals(getPackageName()) && e.getClassName()!=null && (e.getClassName().toString().endsWith("SettingsActivity") || e.getClassName().toString().endsWith("QuickCloseSettings"))) QuickClose.clearTarget(this);
            else QuickClose.foreground(this,e.getPackageName().toString());
        }
    }
    @Override public void onInterrupt(){}
}


