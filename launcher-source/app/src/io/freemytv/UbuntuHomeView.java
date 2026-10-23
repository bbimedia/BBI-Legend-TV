package io.freemytv;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Remote-friendly desktop inspired by the current Ubuntu GNOME session: dark top bar (the shared quick bar),
 * an extended Ubuntu Dock on the left with an orange focus accent and a "Show Applications" grid button.
 * Original vectors only; no Canonical logo or wallpaper assets.
 */
final class UbuntuHomeView extends FrameLayout {
    static final int ACCENT = 0xffe95420;
    private final Activity activity;
    private final Click click;
    private final float scale;
    private final Handler handler = new Handler();
    private final ExecutorService loader = Executors.newSingleThreadExecutor();
    private final LinearLayout dockApps;
    private final View showApps;
    private final TextView tip;
    private final AppSuggestionPanel suggestions;
    private final List<HomeApps.Item> apps = new ArrayList<>();
    private boolean alive = true, navigationKey;
    private View firstDock;
    private final ViewTreeObserver.OnGlobalFocusChangeListener focusSound = (oldFocus, newFocus) -> { if (navigationKey && oldFocus != null && newFocus != null) clickMove(); };

    UbuntuHomeView(Activity a) {
        super(a); activity = a; click = new Click(a); scale = a.getResources().getDisplayMetrics().widthPixels / 1280f;
        getViewTreeObserver().addOnGlobalFocusChangeListener(focusSound);
        addView(new Wallpaper(a), new LayoutParams(-1, -1));
        // Extended Ubuntu Dock: attached to the left edge, below the top bar.
        LinearLayout dock = new LinearLayout(a); dock.setOrientation(LinearLayout.VERTICAL); dock.setGravity(Gravity.CENTER_HORIZONTAL);
        dock.setPadding(s(10), s(12), s(10), s(12));
        GradientDrawable dockBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0xeb1b1b1b, 0xe0242424}); dock.setBackground(dockBg); dock.setElevation(s(6));
        LayoutParams dp = new LayoutParams(s(124), -1, Gravity.LEFT); dp.topMargin = s(64); addView(dock, dp);
        dockApps = new LinearLayout(a); dockApps.setOrientation(LinearLayout.VERTICAL); dockApps.setGravity(Gravity.CENTER_HORIZONTAL);
        dock.addView(dockApps, new LinearLayout.LayoutParams(-1, 0, 1));
        View line = new View(a); line.setBackgroundColor(0x33ffffff); LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s(72), s(1)); lp.bottomMargin = s(8); dock.addView(line, lp);
        showApps = dockItem(null, "Hiện ứng dụng", () -> programs(false)); dock.addView(showApps, new LinearLayout.LayoutParams(s(104), s(76)));
        tip = new TextView(a); tip.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, s(16)); tip.setTextColor(0xfff7f7f7); tip.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        tip.setGravity(Gravity.CENTER_VERTICAL); tip.setPadding(s(14), 0, s(14), 0); tip.setSingleLine(true); tip.setBackground(round(0xf2303030, 10)); tip.setElevation(s(10)); tip.setVisibility(GONE);
        LayoutParams tp = new LayoutParams(-2, s(38)); tp.leftMargin = s(134); addView(tip, tp);
        suggestions = new AppSuggestionPanel(a, "ubuntu");
        LayoutParams sp = new LayoutParams(s(1140), s(595)); sp.leftMargin = s(125); sp.topMargin = s(66); addView(suggestions, sp);
        loader.execute(() -> { List<HomeApps.Item> items = HomeApps.shelf(a, false); items.addAll(HomeApps.others(a)); handler.post(() -> { if (!alive) return; apps.addAll(items); populateDock(); }); });
        setFocusable(false);
    }

    private int s(float n) { return Math.round(n * scale); }
    private GradientDrawable round(int color, int r) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(s(r)); return d; }

    /** One dock slot: rounded hover plate, orange running-style pill on focus and a GNOME tooltip to the right. */
    private View dockItem(HomeApps.Item it, String label, Runnable action) {
        FrameLayout slot = new FrameLayout(activity); slot.setFocusable(true); slot.setSoundEffectsEnabled(false); slot.setContentDescription(label);
        View plate = new View(activity); slot.addView(plate, new LayoutParams(-1, -1));
        if (it != null) {
            ImageView image = new RoundedAppIcon(activity); image.setScaleType(ImageView.ScaleType.FIT_CENTER);
            if (it.logo != null) image.setImageBitmap(it.logo); else if (it.tile != null) image.setImageBitmap(it.tile);
            slot.addView(image, new LayoutParams(s(84), s(50), Gravity.CENTER));
        } else slot.addView(new AppGrid(activity), new LayoutParams(s(44), s(44), Gravity.CENTER));
        View pill = new View(activity); pill.setBackground(round(ACCENT, 3)); pill.setVisibility(INVISIBLE);
        LayoutParams pp = new LayoutParams(s(5), s(22), Gravity.LEFT | Gravity.CENTER_VERTICAL); pp.leftMargin = -s(1); slot.addView(pill, pp);
        slot.setOnFocusChangeListener((v, f) -> {
            plate.setBackground(f ? round(0x33ffffff, 14) : null); pill.setVisibility(f ? VISIBLE : INVISIBLE);
            if (f) { showTip(v, label); if (it != null) suggestions.showItem(it); } else if (tip.getTag() == v) tip.setVisibility(GONE);
        });
        slot.setOnClickListener(v -> action.run());
        return slot;
    }

    private void showTip(View anchor, String label) {
        tip.setText(label); tip.setTag(anchor);
        anchor.post(() -> {
            if (!alive || !anchor.hasFocus()) return;
            int[] at = new int[2], me = new int[2]; anchor.getLocationInWindow(at); getLocationInWindow(me);
            LayoutParams p = (LayoutParams) tip.getLayoutParams(); p.topMargin = at[1] - me[1] + (anchor.getHeight() - s(38)) / 2;
            tip.setLayoutParams(p); tip.setVisibility(VISIBLE);
        });
    }

    private void populateDock() {
        dockApps.removeAllViews(); firstDock = null; Set<String> seen = new HashSet<>();
        for (HomeApps.Item it : apps) {
            if (it.kind != HomeApps.APP || !seen.add(it.key)) continue;
            if (dockApps.getChildCount() >= 6) break;
            View item = dockItem(it, it.label, () -> launch(it));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(s(104), s(76)); p.bottomMargin = s(6); dockApps.addView(item, p);
            if (firstDock == null) firstDock = item;
        }
        (firstDock != null ? firstDock : showApps).requestFocus(); if(!apps.isEmpty())suggestions.showItem(apps.get(0));
    }

    private void launch(HomeApps.Item it) { click.open(); HomeApps.open(activity, it, this, 0, 0, getWidth(), getHeight()); }
    private void programs(boolean inputs) { click.open(); AppCatalogActivity.open(activity, inputs); }
    private void clickMove() { click.move(); }
    void resume() {}
    void pause() { tip.setVisibility(GONE); }
    void destroy() { suggestions.destroy(); getViewTreeObserver().removeOnGlobalFocusChangeListener(focusSound); click.release(); alive = false; handler.removeCallbacksAndMessages(null); loader.shutdownNow(); }
    void reset(boolean focus) { if (firstDock != null) firstDock.requestFocus(); else showApps.requestFocus(); }
    boolean onKey(KeyEvent e) {
        int key = e.getKeyCode();
        if (key >= KeyEvent.KEYCODE_DPAD_UP && key <= KeyEvent.KEYCODE_DPAD_RIGHT) { navigationKey = e.getAction() == KeyEvent.ACTION_DOWN; return false; }
        navigationKey = false;
        if (key == KeyEvent.KEYCODE_BACK || key == KeyEvent.KEYCODE_MENU) { if (e.getAction() == KeyEvent.ACTION_DOWN && e.getRepeatCount() == 0) programs(false); return true; }
        return false;
    }

    /** GNOME "Show Applications": a 3×3 grid of rounded dots. */
    private final class AppGrid extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        AppGrid(Activity a) { super(a); p.setColor(0xfff2f2f2); }
        @Override protected void onDraw(Canvas c) {
            float step = getWidth() / 3f, r = step * 0.2f;
            for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) c.drawCircle(step * (col + .5f), step * (row + .5f), r, p);
        }
    }

    /** Clip the fitted image itself so letterboxed app banners also get rounded corners. */
    private final class RoundedAppIcon extends ImageView {
        private final Path rounded = new Path();
        private final RectF imageBounds = new RectF();
        RoundedAppIcon(Activity a) { super(a); }
        @Override protected void onDraw(Canvas canvas) {
            android.graphics.drawable.Drawable drawable = getDrawable();
            if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) { super.onDraw(canvas); return; }
            float fit = Math.min((float) getWidth() / drawable.getIntrinsicWidth(), (float) getHeight() / drawable.getIntrinsicHeight());
            float w = drawable.getIntrinsicWidth() * fit, h = drawable.getIntrinsicHeight() * fit;
            imageBounds.set((getWidth() - w) / 2f, (getHeight() - h) / 2f, (getWidth() + w) / 2f, (getHeight() + h) / 2f);
            float radius = Math.min(s(12), h * 0.24f);
            rounded.reset(); rounded.addRoundRect(imageBounds, radius, radius, Path.Direction.CW);
            int save = canvas.save(); canvas.clipPath(rounded); super.onDraw(canvas); canvas.restoreToCount(save);
        }
    }

    private final class Wallpaper extends View {
        private final ThemeWallpaper shared = new ThemeWallpaper();
        Wallpaper(Activity a) { super(a); }
        @Override protected void onDraw(Canvas c) { shared.draw(c, "ubuntu", getWidth(), getHeight()); }
    }
}
