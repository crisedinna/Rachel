package cn.rachel.learningadventure;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.OutputStream;
import java.net.URI;

public class MainActivity extends Activity {
    private static final int PICK_JSON = 4101;
    private static final int SAVE_JSON = 4102;
    private static final String PREFS = "adventure_app";
    private static final String DEFAULT_URL = "http://192.168.22.230:8000/index.html";

    private WebView webView;
    private ProgressBar progress;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingBackup;
    private String serverUrl;
    private String serverOrigin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serverUrl = getPreferences(MODE_PRIVATE).getString("server_url", DEFAULT_URL);
        serverOrigin = originOf(serverUrl);
        buildScreen();
        configureWebView();
        webView.loadUrl(serverUrl);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        root.setFitsSystemWindows(true);

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(4), dp(10), dp(4));
        bar.setBackgroundColor(Color.rgb(255, 250, 252));
        TextView title = new TextView(this);
        title.setText("🌈 我的学习冒险岛");
        title.setTextColor(Color.rgb(67, 57, 82));
        title.setTextSize(18);
        title.setTypeface(null, 1);
        bar.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        Button settings = new Button(this);
        settings.setText("⚙");
        settings.setTextSize(18);
        settings.setAllCaps(false);
        settings.setMinWidth(dp(48));
        settings.setOnClickListener(v -> editServerAddress());
        bar.addView(settings, new LinearLayout.LayoutParams(dp(52), dp(48)));
        root.addView(bar);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(233, 112, 155)));
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    @SuppressWarnings("SetJavaScriptEnabled")
    private void configureWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true); // 学习记录使用 localStorage
        webView.getSettings().setDatabaseEnabled(true);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.addJavascriptInterface(new BackupBridge(), "AndroidBridge");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, PICK_JSON);
                return true;
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String candidate = uri.getScheme() + "://" + uri.getAuthority();
                if (candidate.equalsIgnoreCase(serverOrigin)) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (Exception ignored) {
                    Toast.makeText(MainActivity.this, "无法打开这个链接", Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
    }

    private void editServerAddress() {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(0x00000011); // text + URI
        input.setText(serverUrl);
        input.setSelection(input.length());
        new AlertDialog.Builder(this)
                .setTitle("Mac 网页地址")
                .setMessage("Mac 和学习机要连接同一个 Wi-Fi，且 Mac 上的网页服务需要保持运行。")
                .setView(input)
                .setNegativeButton("取消", null)
                .setNeutralButton("重新载入", (dialog, which) -> webView.reload())
                .setPositiveButton("保存并连接", (dialog, which) -> {
                    String candidate = input.getText().toString().trim();
                    if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
                        Toast.makeText(this, "地址需要以 http:// 或 https:// 开头", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String origin = originOf(candidate);
                    if (origin == null) {
                        Toast.makeText(this, "请检查网页地址", Toast.LENGTH_LONG).show();
                        return;
                    }
                    serverUrl = candidate;
                    serverOrigin = origin;
                    getPreferences(MODE_PRIVATE).edit().putString("server_url", serverUrl).apply();
                    webView.loadUrl(serverUrl);
                }).show();
    }

    private String originOf(String url) {
        try {
            URI uri = URI.create(url);
            if (uri.getScheme() == null || uri.getHost() == null) return null;
            int port = uri.getPort();
            return uri.getScheme() + "://" + uri.getHost() + (port < 0 ? "" : ":" + port);
        } catch (Exception e) {
            return null;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private class BackupBridge {
        // 仅此应用加载用户配置的 Mac 页面；备份写入由系统文件选择器完成。
        @JavascriptInterface
        public void saveBackup(String json) {
            runOnUiThread(() -> {
                pendingBackup = json;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "学习冒险岛备份.json");
                startActivityForResult(intent, SAVE_JSON);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_JSON && fileCallback != null) {
            Uri[] result = resultCode == RESULT_OK && data != null ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        } else if (requestCode == SAVE_JSON && resultCode == RESULT_OK && data != null) {
            try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                if (out != null && pendingBackup != null) {
                    out.write(pendingBackup.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    Toast.makeText(this, "备份文件已保存", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, "保存备份失败，请重试", Toast.LENGTH_LONG).show();
            } finally {
                pendingBackup = null;
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
