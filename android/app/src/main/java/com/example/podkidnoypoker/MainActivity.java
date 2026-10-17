package com.example.podkidnoypoker;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** WebView client for first-party multiplayer server; Android app never handles game scores. */
public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(7, 30, 26);
    private FrameLayout body;
    private WebView webView;
    private String allowedScheme;
    private String allowedHost;
    private int allowedPort;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackgroundColor(Color.rgb(10, 47, 39));
        top.setPadding(dp(12), 0, dp(8), 0);
        TextView title = new TextView(this);
        title.setText("Подкидной покер Online");
        title.setTextColor(Color.WHITE);
        title.setTextSize(15);
        title.setSingleLine(true);
        title.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button change = new Button(this);
        change.setText("Сервер");
        change.setTextSize(12);
        change.setAllCaps(false);
        change.setOnClickListener(v -> askForServer());
        top.addView(change, new LinearLayout.LayoutParams(dp(90), dp(44)));
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(48)));
        body = new FrameLayout(this);
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        String saved = getPreferences(MODE_PRIVATE).getString("serverUrl", "");
        if (saved.isEmpty()) saved = BuildConfig.GAME_BASE_URL;
        if (isValidServerUrl(saved)) openServer(saved);
        else { showPlaceholder(); askForServer(); }
    }

    private int dp(int v) {
        return (int) (getResources().getDisplayMetrics().density * v + .5f);
    }

    private void showPlaceholder() {
        TextView hint = new TextView(this);
        hint.setText("Для игры нужен работающий сервер.\n\nНажмите «Сервер» и укажите его HTTPS-адрес.");
        hint.setTextColor(Color.WHITE);
        hint.setTextSize(17);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(dp(24), dp(24), dp(24), dp(24));
        body.removeAllViews();
        body.addView(hint, new FrameLayout.LayoutParams(-1, -1));
    }

    private boolean isLocalHost(String host) {
        String h = host.toLowerCase(java.util.Locale.ROOT);
        if (h.equals("localhost") || h.equals("127.0.0.1") ||
            h.equals("10.0.2.2") || h.endsWith(".local")) return true;
        String[] p = h.split("\\.");
        if (p.length != 4) return false;
        int[] o = new int[4];
        try {
            for (int i=0;i<4;i++) {
                o[i] = Integer.parseInt(p[i]);
                if (o[i] < 0 || o[i] > 255) return false;
            }
        } catch (NumberFormatException ex) { return false; }
        return o[0]==10 || (o[0]==192 && o[1]==168) ||
               (o[0]==172 && o[1]>=16 && o[1]<=31) || o[0]==127;
    }

    private boolean isValidServerUrl(String url) {
        try {
            Uri u = Uri.parse(url);
            String scheme = u.getScheme();
            String host = u.getHost();
            if (scheme==null || host==null || host.isEmpty() || u.getUserInfo()!=null ||
                u.getQuery()!=null || u.getFragment()!=null ||
                !(u.getPath()==null || u.getPath().isEmpty() || "/".equals(u.getPath())) ||
                u.getPort()>65535 || u.getPort()==0) return false;
            if ("https".equalsIgnoreCase(scheme)) return true;
            return BuildConfig.DEBUG && "http".equalsIgnoreCase(scheme) && isLocalHost(host);
        } catch (Exception ex) { return false; }
    }

    private void askForServer() {
        EditText field = new EditText(this);
        String saved = getPreferences(MODE_PRIVATE).getString("serverUrl", "");
        field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        field.setText(saved.isEmpty() ? BuildConfig.GAME_BASE_URL : saved);
        field.setHint("https://play.example.com");
        LinearLayout area = new LinearLayout(this);
        area.setPadding(dp(20), dp(8), dp(20), 0);
        area.addView(field, new LinearLayout.LayoutParams(-1, -2));
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Адрес игрового сервера")
            .setMessage("Для интернета используйте HTTPS. Debug APK разрешает HTTP только к локальному серверу.")
            .setView(area)
            .setPositiveButton("Подключиться", null)
            .setNegativeButton("Отмена", (d,w)->{})
            .create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String url = field.getText().toString().trim().replaceAll("/+$", "");
            if (!isValidServerUrl(url)) {
                field.setError("Нужен HTTPS-адрес (либо локальный HTTP для debug)");
                return;
            }
            getPreferences(MODE_PRIVATE).edit().putString("serverUrl", url).apply();
            dialog.dismiss();
            openServer(url);
        }));
        dialog.show();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void openServer(String url) {
        if (!isValidServerUrl(url)) { showPlaceholder(); return; }
        Uri uri = Uri.parse(url);
        allowedScheme = uri.getScheme().toLowerCase(java.util.Locale.ROOT);
        allowedHost = uri.getHost().toLowerCase(java.util.Locale.ROOT);
        allowedPort = uri.getPort()==-1 ? ("https".equals(allowedScheme)?443:80) : uri.getPort();
        if (webView!=null) { body.removeView(webView); webView.destroy(); }
        body.removeAllViews();
        webView = new WebView(this);
        webView.setBackgroundColor(BACKGROUND);
        body.addView(webView, new FrameLayout.LayoutParams(-1,-1));
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setSupportMultipleWindows(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSafeBrowsingEnabled(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri target = request.getUrl();
                String targetScheme = target.getScheme(), targetHost = target.getHost();
                if (targetScheme==null || targetHost==null) return true;
                int port = target.getPort()==-1 ?
                    ("https".equalsIgnoreCase(targetScheme)?443:80) : target.getPort();
                return !allowedScheme.equalsIgnoreCase(targetScheme) ||
                       !allowedHost.equalsIgnoreCase(targetHost) || allowedPort!=port;
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler h, SslError e) {
                h.cancel();
            }
        });
        webView.loadUrl(url + "/");
    }

    @Override protected void onDestroy() {
        if (webView!=null) { webView.stopLoading(); webView.destroy(); }
        super.onDestroy();
    }
}
