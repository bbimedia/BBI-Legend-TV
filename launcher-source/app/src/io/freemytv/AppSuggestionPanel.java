package io.freemytv;

import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

/**
 * 6-Card Real Content Showcase displayed in the empty screen space of classic themes
 * (XP, Vista, macOS, Ubuntu) whenever an app is focused or selected.
 * Renders a 2x3 grid of genuine 16:9 posters, titles, quality badges, and instant launch actions.
 */
final class AppSuggestionPanel extends LinearLayout {
    private final Activity activity;
    private final float scale;
    private final String theme;
    private final ImageView iconView;
    private final TextView headerTitle, headerSubtitle, headerBadge, launchHint;
    private final LinearLayout gridContainer;
    private final ExecutorService worker = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String, Bitmap> memCache = new HashMap<>();
    private HomeApps.Item currentItem;
    private boolean alive = true;

    static final class ContentItem {
        final String title;
        final String meta;
        final String badge;
        final String category;
        final String imageUrl;
        final int fallbackColorTop;
        final int fallbackColorBottom;

        ContentItem(String title, String meta, String badge, String category, String imageUrl, int colTop, int colBot) {
            this.title = title;
            this.meta = meta;
            this.badge = badge;
            this.category = category;
            this.imageUrl = imageUrl;
            this.fallbackColorTop = colTop;
            this.fallbackColorBottom = colBot;
        }
    }

    private static final class RoundedAppIcon extends ImageView {
        private final Path rounded = new Path();
        private final RectF imageBounds = new RectF();
        private final float scale;
        RoundedAppIcon(Activity a) {
            super(a);
            scale = a.getResources().getDisplayMetrics().widthPixels / 1280f;
        }
        @Override protected void onDraw(Canvas canvas) {
            android.graphics.drawable.Drawable drawable = getDrawable();
            if (drawable == null || drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
                super.onDraw(canvas);
                return;
            }
            float fit = Math.min((float)getWidth()/drawable.getIntrinsicWidth(), (float)getHeight()/drawable.getIntrinsicHeight());
            float w = drawable.getIntrinsicWidth()*fit, h = drawable.getIntrinsicHeight()*fit;
            imageBounds.set((getWidth()-w)/2f,(getHeight()-h)/2f,(getWidth()+w)/2f,(getHeight()+h)/2f);
            float radius = Math.min(13 * scale, h * 0.24f);
            rounded.reset();
            rounded.addRoundRect(imageBounds, radius, radius, Path.Direction.CW);
            int save = canvas.save();
            canvas.clipPath(rounded);
            super.onDraw(canvas);
            canvas.restoreToCount(save);
        }
    }

    AppSuggestionPanel(Activity a, String currentTheme) {
        super(a);
        activity = a;
        theme = currentTheme;
        scale = a.getResources().getDisplayMetrics().widthPixels / 1280f;
        setOrientation(VERTICAL);
        setClickable(false);
        setFocusable(false);

        boolean xp = theme.startsWith("xp");
        boolean vista = theme.startsWith("vista");
        boolean mac = theme.startsWith("macos");
        boolean ubuntu = theme.startsWith("ubuntu");

        int padX = s(mac ? 20 : 16), padY = s(mac ? 14 : 12);
        setPadding(padX, padY, padX, padY);

        if (xp) {
            setBackground(gradient(0xf8fcfdff, 0xeee4effb, 10, 0xaa7ba4e0));
            setElevation(s(6));
        } else if (vista) {
            setBackground(gradient(0xd9162e3d, 0xee0b1c26, 12, 0x887ecfe0));
            setElevation(s(8));
        } else if (mac) {
            setBackground(gradient(0xeeffffff, 0xd8e4f0fa, 20, 0xb0ffffff));
            setElevation(s(10));
        } else if (ubuntu) {
            setBackground(gradient(0xf4282828, 0xf4181818, 14, 0x33ffffff));
            setElevation(s(8));
        } else {
            setBackground(gradient(0xee203348, 0xee142435, 12, 0x444b6785));
            setElevation(s(6));
        }

        // ================= Header =================
        LinearLayout header = new LinearLayout(a);
        header.setGravity(Gravity.CENTER_VERTICAL);
        addView(header, new LayoutParams(-1, -2));

        iconView = new RoundedAppIcon(a);
        iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(iconView, new LayoutParams(s(40), s(28)));

        LinearLayout titleCol = new LinearLayout(a);
        titleCol.setOrientation(VERTICAL);
        titleCol.setPadding(s(10), 0, 0, 0);
        header.addView(titleCol, new LayoutParams(0, -2, 1));

        headerTitle = text("GỢI Ý NỘI DUNG", 15, fontColor(true), true);
        headerTitle.setSingleLine(true);
        titleCol.addView(headerTitle);

        headerSubtitle = text("6 nội dung nổi bật & được xem nhiều nhất hôm nay", 11, fontColor(false), false);
        titleCol.addView(headerSubtitle);

        headerBadge = text("THỊNH HÀNH", 10, Color.WHITE, true);
        headerBadge.setPadding(s(7), s(3), s(7), s(3));
        headerBadge.setBackground(round(accentColor(), 7));
        header.addView(headerBadge);

        // ================= 2x3 Grid Container (2 rows, each with 3 cards) =================
        gridContainer = new LinearLayout(a);
        gridContainer.setOrientation(VERTICAL);
        LayoutParams gp = new LayoutParams(-1, 0, 1.0f);
        gp.topMargin = s(10);
        addView(gridContainer, gp);

        // ================= Footer Hint =================
        launchHint = text("Chọn thẻ và nhấn OK trên remote để xem ngay", 11, fontColor(false), false);
        LayoutParams hp = new LayoutParams(-1, -2);
        hp.topMargin = s(8);
        addView(launchHint, hp);
    }

    private int s(float n) { return Math.round(n * scale); }

    private int fontColor(boolean primary) {
        boolean darkText = theme.startsWith("xp") || theme.startsWith("macos");
        if (darkText) return primary ? 0xff14263e : 0xff4b617a;
        return primary ? 0xffffffff : 0xffa9bccd;
    }

    private int accentColor() {
        if (theme.startsWith("xp")) return 0xff0d4fb5;
        if (theme.startsWith("vista")) return 0xff008eb5;
        if (theme.startsWith("ubuntu")) return 0xffe95420;
        return 0xff007aff;
    }

    private GradientDrawable gradient(int top, int bottom, int r, int stroke) {
        GradientDrawable d = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{top, bottom});
        d.setCornerRadius(s(r));
        if (stroke != 0) d.setStroke(s(1), stroke);
        return d;
    }

    private GradientDrawable round(int color, int r) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(s(r));
        return d;
    }

    private TextView text(String t, int size, int color, boolean bold) {
        TextView v = new TextView(activity);
        v.setText(t);
        v.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, s(size));
        v.setTextColor(color);
        v.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return v;
    }

    void showItem(HomeApps.Item it) {
        if (it == null) return;
        currentItem = it;

        if (it.logo != null) iconView.setImageBitmap(it.logo);
        else if (it.tile != null) iconView.setImageBitmap(it.tile);
        else iconView.setImageDrawable(null);

        String appName = it.label != null ? it.label.toUpperCase(Locale.ROOT) : "ỨNG DỤNG";
        headerTitle.setText("GỢI Ý NỘI DUNG TỪ " + appName);

        worker.execute(() -> {
            if (!alive) return;
            List<ContentItem> items = getRecommendationsForApp(it);
            handler.post(() -> {
                if (!alive || currentItem != it) return;
                renderGrid(items, it);
            });
        });
    }

    private void renderGrid(List<ContentItem> items, HomeApps.Item app) {
        gridContainer.removeAllViews();
        if (items == null || items.isEmpty()) return;

        int total = Math.min(items.size(), 6);
        boolean darkTheme = theme.startsWith("vista") || theme.startsWith("ubuntu");
        int cardBg = darkTheme ? 0x44000000 : 0x24ffffff;

        // Create 2 rows of 3 columns
        int itemsPerRow = 3;
        for (int r = 0; r < 2; r++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            LayoutParams rp = new LayoutParams(-1, 0, 1.0f);
            if (r > 0) rp.topMargin = s(8);
            gridContainer.addView(row, rp);

            for (int c = 0; c < itemsPerRow; c++) {
                int index = r * itemsPerRow + c;
                if (index >= total) break;
                ContentItem ci = items.get(index);

                LinearLayout card = new LinearLayout(activity);
                card.setOrientation(VERTICAL);
                card.setFocusable(true);
                card.setClickable(true);
                card.setPadding(s(4), s(4), s(4), s(5));
                card.setBackground(round(cardBg, 8));

                // 16:9 Image Frame
                FrameLayout frame = new FrameLayout(activity);
                ImageView imgView = new ImageView(activity);
                imgView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                frame.addView(imgView, new FrameLayout.LayoutParams(-1, -1));

                // Load real photo asynchronously
                loadPosterAsync(ci, imgView);

                // Quality Badge (e.g. 4K, LIVE, HOT, ★ 8.9)
                if (ci.badge != null && !ci.badge.isEmpty()) {
                    TextView badgeView = text(ci.badge, 9, Color.WHITE, true);
                    badgeView.setPadding(s(5), s(2), s(5), s(2));
                    int bCol = ci.badge.contains("LIVE") ? 0xffe50914 : (ci.badge.contains("★") ? 0xffe68a00 : 0xd0000000);
                    badgeView.setBackground(round(bCol, 5));
                    FrameLayout.LayoutParams bp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.RIGHT);
                    bp.topMargin = s(4);
                    bp.rightMargin = s(4);
                    frame.addView(badgeView, bp);
                }

                // Scrim overlay at bottom of poster
                View scrim = new View(activity);
                scrim.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                        new int[]{0x00000000, 0x88000000, 0xf0000000}));
                FrameLayout.LayoutParams scp = new FrameLayout.LayoutParams(-1, s(48), Gravity.BOTTOM);
                frame.addView(scrim, scp);

                // Title overlaid on photo bottom
                TextView titleView = text(ci.title, 13, Color.WHITE, true);
                titleView.setSingleLine(true);
                titleView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                titleView.setShadowLayer(s(2), s(1), s(1), 0xff000000);
                FrameLayout.LayoutParams tp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
                tp.leftMargin = s(6);
                tp.rightMargin = s(6);
                tp.bottomMargin = s(4);
                frame.addView(titleView, tp);

                card.addView(frame, new LayoutParams(-1, 0, 1.0f));

                // Subtitle / Meta below poster
                TextView metaView = text(ci.meta, 11, fontColor(false), false);
                metaView.setSingleLine(true);
                metaView.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LayoutParams mp = new LayoutParams(-1, -2);
                mp.topMargin = s(4);
                mp.leftMargin = s(4);
                mp.rightMargin = s(4);
                card.addView(metaView, mp);

                // Focus & Click Handlers
                card.setOnFocusChangeListener((v, hasFocus) -> {
                    if (hasFocus) {
                        v.setBackground(round(accentColor(), 8));
                        v.setElevation(s(10));
                        v.setScaleX(1.04f);
                        v.setScaleY(1.04f);
                    } else {
                        v.setBackground(round(cardBg, 8));
                        v.setElevation(s(0));
                        v.setScaleX(1.0f);
                        v.setScaleY(1.0f);
                    }
                });

                card.setOnClickListener(v -> HomeApps.open(activity, app, card, 0, 0, getWidth(), getHeight()));

                LayoutParams cp = new LayoutParams(0, -1, 1.0f);
                if (c > 0) cp.leftMargin = s(10);
                row.addView(card, cp);
            }
        }
    }

    private void loadPosterAsync(ContentItem ci, ImageView targetView) {
        String url = ci.imageUrl;
        if (url == null || url.isEmpty()) {
            targetView.setImageBitmap(createFallbackPoster(ci));
            return;
        }

        Bitmap mem = memCache.get(url);
        if (mem != null) {
            targetView.setImageBitmap(mem);
            return;
        }

        // Show fast placeholder fallback immediately
        targetView.setImageBitmap(createFallbackPoster(ci));

        worker.execute(() -> {
            if (!alive) return;
            Bitmap loaded = fetchOrLoadDisk(url);
            if (loaded != null) {
                memCache.put(url, loaded);
                handler.post(() -> {
                    if (!alive) return;
                    targetView.setImageBitmap(loaded);
                });
            }
        });
    }

    private Bitmap fetchOrLoadDisk(String urlStr) {
        try {
            File cacheDir = new File(activity.getCacheDir(), "content_posters");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            String hash = md5(urlStr);
            File diskFile = new File(cacheDir, hash + ".jpg");

            // 1. Load from disk cache
            if (diskFile.exists() && diskFile.length() > 0) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inSampleSize = 1;
                return BitmapFactory.decodeFile(diskFile.getPath(), opts);
            }

            // 2. Download from network
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(12000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android TV)");

            File tempFile = new File(cacheDir, hash + ".part");
            try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            }
            tempFile.renameTo(diskFile);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            return BitmapFactory.decodeFile(diskFile.getPath(), opts);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String md5(String s) {
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            byte[] d = m.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }

    private Bitmap createFallbackPoster(ContentItem ci) {
        int w = s(240);
        int h = Math.round(w * 9f / 16f);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        Canvas c = new Canvas(bmp);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, w, h,
                new int[]{ci.fallbackColorTop, ci.fallbackColorBottom, 0xff0e1318},
                new float[]{0f, 0.65f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);

        // Subtle watermark
        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(0x22ffffff);
        tp.setTextSize(s(18));
        tp.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        tp.setTextAlign(Paint.Align.CENTER);
        c.drawText(ci.category.toUpperCase(Locale.ROOT), w / 2f, h / 2f + s(6), tp);

        return bmp;
    }

    private List<ContentItem> getRecommendationsForApp(HomeApps.Item it) {
        List<ContentItem> list = new ArrayList<>();
        String label = (it.label != null ? it.label.toLowerCase(Locale.ROOT) : "");
        String pkg = (it.pkg != null ? it.pkg.toLowerCase(Locale.ROOT) : "");

        if (label.contains("youtube") || label.contains("smarttube") || pkg.contains("youtube") || pkg.contains("smarttube")) {
            list.add(new ContentItem(
                    "Sơn Tùng M-TP - Đừng Làm Trái Tim Anh Đau",
                    "Âm nhạc • 4K • 120M lượt xem",
                    "TOP 1", "YouTube",
                    "https://i.ytimg.com/vi/abPmFUFQ4qE/hqdefault.jpg",
                    0xff8b0000, 0xff3b0000
            ));
            list.add(new ContentItem(
                    "Độ Mixi: Những Khoảnh Khắc Hài Hước Nhất",
                    "Gaming • 1080p 60fps • 3.2M views",
                    "HOT", "Gaming",
                    "https://i.ytimg.com/vi/07d2dXHYb94/hqdefault.jpg",
                    0xff004d40, 0xff00251a
            ));
            list.add(new ContentItem(
                    "Khám Phá Vũ Trụ: Bí Ẩn Hố Đen Không Gian",
                    "Khoa học • 4K Ultra HD • 1.8M views",
                    "4K", "Khoa học",
                    "https://i.ytimg.com/vi/1La4QzGeaaQ/hqdefault.jpg",
                    0xff1a237e, 0xff0d133a
            ));
            list.add(new ContentItem(
                    "Rap Việt 2026: Vòng Bứt Phá Đỉnh Cao",
                    "Show truyền hình • Full HD • 4.5M views",
                    "TRENDING", "Show",
                    "https://i.ytimg.com/vi/8ZhwSt6j12Y/hqdefault.jpg",
                    0xff4a148c, 0xff1e0638
            ));
            list.add(new ContentItem(
                    "Đánh Giá Siêu Phẩm Công Nghệ Mới Nhất",
                    "Công nghệ TV & Mobile • 4K 60fps",
                    "MỚI", "Review",
                    "https://i.ytimg.com/vi/q7v3rL6yR9Y/hqdefault.jpg",
                    0xffe65100, 0xff3e1600
            ));
            list.add(new ContentItem(
                    "Táo Quân Đặc Biệt: Gặp Nhau Cuối Năm",
                    "Hài kịch truyền hình • Full HD • 15M views",
                    "ĐẶC SẮC", "Hài Kịch",
                    "https://i.ytimg.com/vi/kJQP7kiw5Fk/hqdefault.jpg",
                    0xffb71c1c, 0xff3e0707
            ));
        } else if (label.contains("netflix") || pkg.contains("netflix")) {
            list.add(new ContentItem(
                    "Stranger Things: Trận Chiến Cuối Cùng",
                    "Viễn tưởng, Bí ẩn • 4K Dolby Vision • ★ 8.9",
                    "MỚI", "Netflix",
                    "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                    0xffb71c1c, 0xff3e0707
            ));
            list.add(new ContentItem(
                    "Squid Game: Trò Chơi Con Mực 2",
                    "Giật gân, Sinh tồn • 4K HDR • ★ 8.5",
                    "HOT", "Series",
                    "https://image.tmdb.org/t/p/w780/dKQA8scg6Kp49AEaeNptqSp1b5y.jpg",
                    0xff004d40, 0xff09231f
            ));
            list.add(new ContentItem(
                    "One Piece: Hành Trình Đại Hải Trình",
                    "Phiêu lưu, Giả tưởng • 4K • ★ 8.6",
                    "4K", "Live Action",
                    "https://image.tmdb.org/t/p/w780/2rmK7mnchw9Xr3XdiTFSxTT0Yqi.jpg",
                    0xffe65100, 0xff3e1600
            ));
            list.add(new ContentItem(
                    "Wednesday: Bí Mật Học Viện Nevermore",
                    "Bí ẩn, Hài đen • 4K Dolby Atmos • ★ 8.4",
                    "POPULAR", "Series",
                    "https://image.tmdb.org/t/p/w780/9PFonBhy4cQy7Jz20NpMygczOkv.jpg",
                    0xff311b92, 0xff12093a
            ));
            list.add(new ContentItem(
                    "The Witcher: Thợ Săn Quái Vật",
                    "Hành động, Huyền bí • 4K HDR • ★ 8.2",
                    "TOP 10", "Fantasy",
                    "https://image.tmdb.org/t/p/w780/jBJWaqoSCiARWtfV0Glq6Y0QIiq.jpg",
                    0xff263238, 0xff0c1012
            ));
            list.add(new ContentItem(
                    "Red Notice: Lệnh Truy Nã Đỏ",
                    "Hành động, Hài hước • 4K • ★ 7.9",
                    "BOM TẤN", "Điện Ảnh",
                    "https://image.tmdb.org/t/p/w780/dK12GMoivI9G2S7Y9vH5kQ82jH4.jpg",
                    0xffbf360c, 0xff3e1103
            ));
        } else if (label.contains("fpt") || label.contains("vtv") || label.contains("tv360") || label.contains("vieon")
                || label.contains("monster") || label.contains("k+") || label.contains("mytv")) {
            list.add(new ContentItem(
                    "Ngoại Hạng Anh: Man City vs Arsenal",
                    "Trực tiếp 22:30 • Full HD 50fps • Bình luận TV",
                    "TRỰC TIẾP", "Bóng Đá",
                    "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800&q=80",
                    0xff00695c, 0xff00332c
            ));
            list.add(new ContentItem(
                    "UEFA Champions League: Đêm Định Mệnh C1",
                    "Trực tiếp 02:00 • 4K HDR 60fps",
                    "C1 LIVE", "Bóng Đá",
                    "https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=800&q=80",
                    0xff0d47a1, 0xff051e44
            ));
            list.add(new ContentItem(
                    "Thời Sự VTV1 19:00: Tin Tức Toàn Cảnh",
                    "Tin tức trong nước & thế giới hôm nay",
                    "TIN NÓNG", "Thời Sự",
                    "https://images.unsplash.com/photo-1585829365295-ab7cd400c167?w=800&q=80",
                    0xff1565c0, 0xff09284d
            ));
            list.add(new ContentItem(
                    "Rap Việt 2026: Chung Kết & Trao Giải",
                    "Show truyền hình âm nhạc bùng nổ • Full HD",
                    "HOT", "Giải Trí",
                    "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800&q=80",
                    0xff4a148c, 0xff1e0638
            ));
            list.add(new ContentItem(
                    "Giải Đua Xe Công Thức 1 (F1 Grand Prix)",
                    "Vòng đua tốc độ 300km/h đỉnh cao thế giới",
                    "TỐC ĐỘ", "Thể Thao",
                    "https://images.unsplash.com/photo-1568605117036-5fe5e7bab0b7?w=800&q=80",
                    0xffd84315, 0xff3e1103
            ));
            list.add(new ContentItem(
                    "Phim Truyền Hình Giờ Vàng VTV Đặc Sắc",
                    "Tập mới phát sóng tối nay • HD",
                    "MỚI", "Phim Truyện",
                    "https://image.tmdb.org/t/p/w780/7RyHsO4yDXtBv1zUU3mTpHeQ0d5.jpg",
                    0xff2e7d32, 0xff0c230e
            ));
        } else if (label.contains("spotify") || label.contains("zing") || label.contains("nhac") || label.contains("music")) {
            list.add(new ContentItem(
                    "Top 50 Bài Hát Thịnh Hành Việt Nam",
                    "Cập nhật hàng ngày • Lossless Audio",
                    "TOP 1", "Nhạc Việt",
                    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&q=80",
                    0xff1b5e20, 0xff08230c
            ));
            list.add(new ContentItem(
                    "Acoustic Chill Cafe: Thư Giãn Cuối Tuần",
                    "Giai điệu nhẹ nhàng sâu lắng • 320kbps",
                    "CHILL", "Acoustic",
                    "https://images.unsplash.com/photo-1445964047600-cdadb8736418?w=800&q=80",
                    0xffe65100, 0xff421700
            ));
            list.add(new ContentItem(
                    "EDM Festival: Đại Nhạc Hội Điện Tử",
                    "Bass cực căng, âm thanh vòm sống động",
                    "BASS", "EDM",
                    "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&q=80",
                    0xff4a148c, 0xff1e0638
            ));
            list.add(new ContentItem(
                    "Lofi Hip Hop: Giai Điệu Cho Đêm Muộn",
                    "Thư giãn tâm trí & làm việc hiệu quả",
                    "LOFI", "Tuyển Tập",
                    "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=800&q=80",
                    0xff006064, 0xff002426
            ));
            list.add(new ContentItem(
                    "Rock Classics: Bất Hủ Mọi Thời Đại",
                    "Những huyền thoại guitar & rock thế giới",
                    "ROCK", "Huyền Thoại",
                    "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=800&q=80",
                    0xff263238, 0xff0c1012
            ));
            list.add(new ContentItem(
                    "Piano Không Lời: Khúc Nhạc Bình Yên",
                    "Tuyển chọn những bản piano sâu lắng nhất",
                    "PIANO", "Thư Giãn",
                    "https://images.unsplash.com/photo-1520523839898-50712140d9fe?w=800&q=80",
                    0xff1565c0, 0xff09284d
            ));
        } else if (label.contains("kodi") || label.contains("plex") || label.contains("vlc") || label.contains("stremio") || label.contains("mx")) {
            list.add(new ContentItem(
                    "Dune: Hành Tinh Cát 2 (4K Remux)",
                    "Khoa học viễn tưởng • IMAX 4K Atmos • ★ 8.8",
                    "4K REMUX", "Rạp Phim",
                    "https://image.tmdb.org/t/p/w780/xOMo8BRK7PfcJv9JCnx7s520QIq.jpg",
                    0xffbf360c, 0xff3e1103
            ));
            list.add(new ContentItem(
                    "Oppenheimer: Bom Nguyên Tử",
                    "Lịch sử, Kịch tính • 4K HDR • ★ 8.9",
                    "IMAX", "Điện Ảnh",
                    "https://image.tmdb.org/t/p/w780/fm6KqXpk3M2HVveHwCrBSSBaO0V.jpg",
                    0xff263238, 0xff0c1012
            ));
            list.add(new ContentItem(
                    "Spider-Man: Across the Spider-Verse",
                    "Hoạt hình hành động đỉnh cao • 4K HDR • ★ 8.7",
                    "HDR", "Marvel",
                    "https://image.tmdb.org/t/p/w780/4HodYYKEIsGOdinkGi2Ucz6X9i0.jpg",
                    0xff880e4f, 0xff31041b
            ));
            list.add(new ContentItem(
                    "Avatar: Dòng Chảy Của Nước (3D 4K)",
                    "Hành động, Thám hiểm • 4K HFR • ★ 8.6",
                    "3D 4K", "Bom Tấn",
                    "https://image.tmdb.org/t/p/w780/ovM06PdF36CfeK90hYx2a62T8Y2.jpg",
                    0xff0277bd, 0xff012d47
            ));
            list.add(new ContentItem(
                    "Interstellar: Hố Đen Vũ Trụ",
                    "Khoa học viễn tưởng không gian • ★ 8.9",
                    "KINH ĐIỂN", "Không Gian",
                    "https://image.tmdb.org/t/p/w780/xJHokMbljvjADYdit5fK5VQsXEG.jpg",
                    0xff004d40, 0xff00231d
            ));
            list.add(new ContentItem(
                    "The Dark Knight: Hiệp Sĩ Bóng Đêm",
                    "Hành động, Tội phạm • ★ 9.0",
                    "TOP 1", "DC",
                    "https://image.tmdb.org/t/p/w780/dqK9Hag1054tghRQSqLSfrkvQnA.jpg",
                    0xff1a237e, 0xff0d133a
            ));
        } else if (label.contains("chrome") || label.contains("web") || label.contains("browser") || label.contains("cốc")) {
            list.add(new ContentItem(
                    "VnExpress: Tin Nhanh Thời Sự & Kinh Tế",
                    "Báo điện tử nhiều người đọc nhất Việt Nam",
                    "TIN MỚI", "Báo Chí",
                    "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800&q=80",
                    0xff006064, 0xff002426
            ));
            list.add(new ContentItem(
                    "Trí Tuệ Nhân Tạo & Công Nghệ Mới 2026",
                    "Những đột phá mới nhất về AI và thế giới số",
                    "XU HƯỚNG", "Công Nghệ",
                    "https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&q=80",
                    0xff1a237e, 0xff070c36
            ));
            list.add(new ContentItem(
                    "Thị Trường Tài Chính & Kinh Doanh Toàn Cầu",
                    "Chỉ số chứng khoán, giá vàng và đầu tư",
                    "KINH TẾ", "Tài Chính",
                    "https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?w=800&q=80",
                    0xff1b5e20, 0xff09280e
            ));
            list.add(new ContentItem(
                    "Khám Phá Thế Giới Số & Phần Mềm TV",
                    "Các ứng dụng và tiện ích giải trí hay nhất",
                    "ỨNG DỤNG", "Khám Phá",
                    "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&q=80",
                    0xff4a148c, 0xff1e0638
            ));
            list.add(new ContentItem(
                    "Thể Thao 24h: Toàn Cảnh Các Giải Đấu",
                    "Bóng đá, quần vợt, bóng rổ & điền kinh",
                    "THỂ THAO", "Tin Nóng",
                    "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?w=800&q=80",
                    0xffe65100, 0xff3e1600
            ));
            list.add(new ContentItem(
                    "Xe & Công Nghệ Di Chuyển Tương Lai",
                    "Siêu xe điện, ô tô thông minh và tự hành",
                    "XE", "Đời Sống",
                    "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800&q=80",
                    0xff263238, 0xff0c1012
            ));
        } else {
            // General Fallback (6 real posters)
            list.add(new ContentItem(
                    "Phim Chiếu Rạp 4K Nổi Bật: " + it.label,
                    "Trải nghiệm phim ảnh chất lượng cao trên màn hình lớn",
                    "4K UHD", "Đặc Sắc",
                    "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
                    0xff1565c0, 0xff09284d
            ));
            list.add(new ContentItem(
                    "Video Giải Trí & Âm Nhạc Được Xem Nhiều",
                    "Tuyển tập nội dung giải trí thịnh hành",
                    "TOP 1", "Giải Trí",
                    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&q=80",
                    0xff6a1b9a, 0xff260838
            ));
            list.add(new ContentItem(
                    "Thể Thao Trực Tiếp: Đỉnh Cao Cạnh Tranh",
                    "Theo dõi những trận cầu và sự kiện hấp dẫn",
                    "TRỰC TIẾP", "Thể Thao",
                    "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=800&q=80",
                    0xff00695c, 0xff002823
            ));
            list.add(new ContentItem(
                    "Bản Tin Thời Sự & Khám Phá Công Nghệ",
                    "Cập nhật những chuyển động công nghệ mới",
                    "TIN TỨC", "Đời Sống",
                    "https://images.unsplash.com/photo-1504711434969-e33886168f5c?w=800&q=80",
                    0xffe65100, 0xff431700
            ));
            list.add(new ContentItem(
                    "Series Phim Bộ Hấp Dẫn Nhất Tuần",
                    "Xem trọn bộ chất lượng cao trên TV",
                    "HOT", "Series",
                    "https://image.tmdb.org/t/p/w780/dKQA8scg6Kp49AEaeNptqSp1b5y.jpg",
                    0xff004d40, 0xff00231d
            ));
            list.add(new ContentItem(
                    "Khám Phá & Mở Ngay Ứng Dụng Này",
                    "Bấm để trải nghiệm ngay trên chiếc TV của bạn",
                    "SẴN SÀNG", "Xem Ngay",
                    "https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&q=80",
                    0xff263238, 0xff0c1012
            ));
        }

        return list;
    }

    void destroy() {
        alive = false;
        memCache.clear();
        handler.removeCallbacksAndMessages(null);
        worker.shutdownNow();
    }
}
