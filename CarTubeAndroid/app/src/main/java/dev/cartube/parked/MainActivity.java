package dev.cartube.parked;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * CarTube: independently branded WebView client for YouTube's public mobile site.
 * Not an official YouTube app; video may be unavailable in embedded WebView.
 */
public final class MainActivity extends Activity {
    private static final String HOME = "https://m.youtube.com/";
    private static final int DARK = 0xff101010, RED = 0xffff0033;
    private WebView browser;
    private FrameLayout screen, videoLayer;
    private View videoView, drivingOverlay;
    private WebChromeClient.CustomViewCallback videoCallback;
    private SharedPreferences prefs;
    private CarSafetyController carSafety;
    private boolean automotive, restricted;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        automotive = getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
        restricted = automotive;
        prefs = getSharedPreferences("cartube", MODE_PRIVATE);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        createUi();
        if (automotive) {
            carSafety = new CarSafetyController(this, flag -> runOnUiThread(() -> setRestricted(flag)));
            carSafety.start();
        }
        String shared = getIntent() == null ? null : getIntent().getStringExtra(Intent.EXTRA_TEXT);
        String target = allowed(shared) ? shared : prefs.getString("last", HOME);
        browser.loadUrl(allowed(target) ? target : HOME);
    }

    private TextView button(String label, int width, Runnable action) {
        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(18);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(4), dp(8), dp(4));
        tv.setMinHeight(dp(52));
        tv.setOnClickListener(v -> { if (!restricted) action.run(); });
        tv.setContentDescription(label);
        tv.setFocusable(true);
        if (width > 0) tv.setMinWidth(dp(width));
        return tv;
    }
    private int dp(int p) { return Math.round(getResources().getDisplayMetrics().density * p); }
    private void toast(String msg) { Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); }

    private void createUi() {
        screen = new FrameLayout(this);
        screen.setBackgroundColor(DARK);
        setContentView(screen);
        LinearLayout base = new LinearLayout(this);
        base.setOrientation(LinearLayout.VERTICAL);
        screen.addView(base, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout top = new LinearLayout(this);
        top.setBackgroundColor(DARK);
        base.addView(top);
        TextView title = button("▶ CarTube", 0, () -> browse(HOME));
        title.setTextSize(20);
        title.setTextColor(RED);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1));
        top.addView(button("⌕", 52, this::search), new LinearLayout.LayoutParams(dp(60), dp(56)));
        top.addView(button("☆", 52, this::favorite), new LinearLayout.LayoutParams(dp(60), dp(56)));

        browser = new WebView(this);
        browser.setBackgroundColor(DARK);
        WebSettings ws = browser.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setMediaPlaybackRequiresUserGesture(true);
        ws.setAllowFileAccess(false);
        ws.setAllowContentAccess(false);
        ws.setJavaScriptCanOpenWindowsAutomatically(false);
        ws.setSupportMultipleWindows(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        browser.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri target = request.getUrl();
                if (allowed(target.toString())) return restricted;
                if (!automotive && "https".equals(target.getScheme())) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, target)); }
                    catch (Exception ignored) { toast("Ссылка недоступна"); }
                }
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (allowed(url)) prefs.edit().putString("last", url).apply();
                if (restricted) pauseMedia();
            }
        });
        browser.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View customView, CustomViewCallback callback) {
                if (restricted || videoView != null) { callback.onCustomViewHidden(); return; }
                videoView = customView;
                videoCallback = callback;
                videoLayer = new FrameLayout(MainActivity.this);
                videoLayer.setBackgroundColor(Color.BLACK);
                videoLayer.addView(customView, new FrameLayout.LayoutParams(-1, -1));
                TextView close = button("✕", 56, MainActivity.this::hideVideo);
                FrameLayout.LayoutParams pos = new FrameLayout.LayoutParams(dp(64), dp(64), Gravity.TOP | Gravity.RIGHT);
                videoLayer.addView(close, pos);
                screen.addView(videoLayer, new FrameLayout.LayoutParams(-1, -1));
            }
            @Override public void onHideCustomView() { hideVideo(); }
        });
        base.addView(browser, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout tabs = new LinearLayout(this);
        tabs.setBackgroundColor(DARK);
        base.addView(tabs, new LinearLayout.LayoutParams(-1, dp(64)));
        tab(tabs, "Главная", () -> browse(HOME));
        tab(tabs, "Shorts", () -> browse("https://m.youtube.com/shorts"));
        tab(tabs, "Подписки", () -> browse("https://m.youtube.com/feed/subscriptions"));
        tab(tabs, "Избранное", this::showFavorites);
        drivingOverlay = new TextView(this);
        TextView overlay = (TextView) drivingOverlay;
        overlay.setText("Видео доступно только на стоянке");
        overlay.setTextColor(Color.WHITE);
        overlay.setTextSize(22);
        overlay.setBackgroundColor(DARK);
        overlay.setGravity(Gravity.CENTER);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        screen.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        overlay.setVisibility(restricted ? View.VISIBLE : View.GONE);
    }
    private void tab(LinearLayout bar, String label, Runnable click) {
        bar.addView(button(label, 0, click), new LinearLayout.LayoutParams(0, -1, 1));
    }
    private void browse(String url) {
        if (!restricted && allowed(url)) browser.loadUrl(url);
    }
    private void search() {
        EditText text = new EditText(this);
        text.setSingleLine();
        text.setTextColor(Color.WHITE);
        text.setHint("Название или ссылка");
        new AlertDialog.Builder(this).setTitle("Поиск на YouTube")
            .setView(text).setPositiveButton("Искать", (d, w) -> {
                String s = text.getText().toString().trim();
                if (s.isEmpty()) return;
                if (allowed(s)) browse(s);
                else browse("https://m.youtube.com/results?search_query=" +
                    URLEncoder.encode(s, StandardCharsets.UTF_8));
            }).setNegativeButton("Отмена", null).show();
    }
    private JSONArray getFavorites() {
        try { return new JSONArray(prefs.getString("favorites", "[]")); }
        catch (Exception ignored) { return new JSONArray(); }
    }
    private void favorite() {
        String url = browser.getUrl();
        if (url == null || !(url.contains("/watch?") || url.contains("/shorts/"))) {
            toast("Сначала открой видео"); return;
        }
        JSONArray a = getFavorites();
        for (int i = 0; i < a.length(); i++) if (url.equals(a.optString(i))) {
            toast("Уже сохранено"); return;
        }
        a.put(url);
        prefs.edit().putString("favorites", a.toString()).apply();
        toast("Добавлено в избранное");
    }
    private void showFavorites() {
        JSONArray a = getFavorites();
        String[] urls = new String[a.length()];
        for (int i = 0; i < urls.length; i++) urls[i] = a.optString(i);
        new AlertDialog.Builder(this).setTitle("Избранное")
            .setItems(urls, (dialog, index) -> browse(urls[index]))
            .setNeutralButton("Очистить", (dialog, which) ->
                prefs.edit().remove("favorites").apply())
            .setNegativeButton("Закрыть", null).show();
    }
    private static boolean allowed(String value) {
        if (value == null) return false;
        try {
            Uri u = Uri.parse(value);
            if (!"https".equalsIgnoreCase(u.getScheme())) return false;
            String host = u.getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals("youtube.com") || host.endsWith(".youtube.com") || host.equals("youtu.be");
        } catch (Exception ignored) { return false; }
    }
    private void hideVideo() {
        if (videoLayer == null) return;
        FrameLayout frame = videoLayer;
        View v = videoView;
        WebChromeClient.CustomViewCallback cb = videoCallback;
        videoLayer = null;
        videoView = null;
        videoCallback = null;
        if (v != null) frame.removeView(v);
        screen.removeView(frame);
        if (cb != null) cb.onCustomViewHidden();
    }
    private void pauseMedia() {
        if (browser != null) {
            browser.evaluateJavascript("(function(){document.querySelectorAll('video,audio').forEach(v=>v.pause())})()", null);
            browser.onPause();
            browser.pauseTimers();
        }
    }
    private void setRestricted(boolean r) {
        restricted = r;
        if (r) { hideVideo(); pauseMedia(); }
        else if (browser != null) { browser.onResume(); browser.resumeTimers(); }
        if (drivingOverlay != null) {
            drivingOverlay.setVisibility(r ? View.VISIBLE : View.GONE);
            if (r) drivingOverlay.bringToFront();
        }
    }
    @Override protected void onResume() {
        super.onResume();
        if (!restricted && browser != null) { browser.onResume(); browser.resumeTimers(); }
    }
    @Override protected void onPause() { pauseMedia(); super.onPause(); }
    @Override protected void onDestroy() {
        if (carSafety != null) carSafety.stop();
        hideVideo();
        if (browser != null) { browser.destroy(); browser = null; }
        super.onDestroy();
    }
    @Override public void onBackPressed() {
        if (videoView != null) hideVideo();
        else if (!restricted && browser != null && browser.canGoBack()) browser.goBack();
        else super.onBackPressed();
    }
}
