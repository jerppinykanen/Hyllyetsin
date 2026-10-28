package fi.vastine.etsin;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.InputStream;

/**
 * Loads the bundled index.html via https address
 * (https://appassets.androidplatform.net/) so WebView treats the page
 * as a secure context and the live camera works.
 */
public class MainActivity extends Activity {
    private static final String HOST = "appassets.androidplatform.net";
    private static final int REQ_CAM = 1;
    private WebView web;
    private PermissionRequest pending;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " VastineApp/1.0");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
                Uri u = r.getUrl();
                if (!HOST.equals(u.getHost())) return null;
                String path = u.getPath();
                if (path == null || path.equals("/") || path.isEmpty()) path = "/index.html";
                try {
                    InputStream in = getAssets().open(path.substring(1));
                    String mime = path.endsWith(".js") ? "application/javascript"
                            : path.endsWith(".css") ? "text/css" : "text/html";
                    return new WebResourceResponse(mime, "utf-8", in);
                } catch (Exception e) {
                    return new WebResourceResponse("text/plain", "utf-8", 404, "Not found",
                            null, null);
                }
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final PermissionRequest req) {
                runOnUiThread(() -> {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        req.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                    } else {
                        pending = req;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM);
                    }
                });
            }
        });

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM);
        }
        web.loadUrl("https://" + HOST + "/index.html");
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code != REQ_CAM || pending == null) return;
        if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) {
            pending.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
        } else {
            pending.deny();
        }
        pending = null;
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
