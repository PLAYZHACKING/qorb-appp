package com.qorb.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.*;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    // ── Change this to your actual Workers URL ──────────────────
    private static final String APP_URL = "https://YOUR_WORKER.workers.dev";
    // ────────────────────────────────────────────────────────────

    private static final int REQ_PERMISSIONS = 1001;
    private static final int REQ_FILE_CHOOSER = 1002;
    private static final int REQ_CAMERA       = 1003;

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraImageUri;

    private static final String[] REQUIRED_PERMISSIONS = {
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Request all permissions upfront
        requestAllPermissions();

        webView = findViewById(R.id.webview);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Keep navigation inside app
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {

            // ── Mic / Camera permission for web ─────────────────
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                request.grant(request.getResources());
            }

            // ── File chooser (Photos, Files) ─────────────────────
            @Override
            public boolean onShowFileChooser(WebView wv,
                    ValueCallback<Uri[]> callback,
                    FileChooserParams params) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                fileCallback = callback;

                String[] acceptTypes = params.getAcceptTypes();
                boolean captureEnabled = params.isCaptureEnabled();
                boolean isImage = acceptTypes != null && acceptTypes.length > 0
                        && acceptTypes[0].startsWith("image");

                if (captureEnabled && isImage) {
                    // Camera capture
                    openCamera();
                } else {
                    // File / gallery chooser
                    Intent intent = params.createIntent();
                    try {
                        startActivityForResult(intent, REQ_FILE_CHOOSER);
                    } catch (Exception e) {
                        fileCallback = null;
                        Toast.makeText(MainActivity.this,
                                "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }
                return true;
            }

            // ── Console logs for debugging ────────────────────────
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                return true;
            }
        });

        webView.loadUrl(APP_URL);
    }

    // ── Camera ───────────────────────────────────────────────────
    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try { photoFile = createImageFile(); } catch (IOException ignored) {}
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".provider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                startActivityForResult(intent, REQ_CAMERA);
            }
        }
    }

    private File createImageFile() throws IOException {
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile("QORB_" + stamp, ".jpg", dir);
    }

    // ── Activity results ─────────────────────────────────────────
    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req == REQ_FILE_CHOOSER) {
            if (fileCallback == null) return;
            Uri[] results = null;
            if (res == RESULT_OK && data != null) {
                String dataStr = data.getDataString();
                if (dataStr != null) {
                    results = new Uri[]{ Uri.parse(dataStr) };
                } else if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                }
            }
            fileCallback.onReceiveValue(results);
            fileCallback = null;

        } else if (req == REQ_CAMERA) {
            if (fileCallback == null) return;
            if (res == RESULT_OK && cameraImageUri != null) {
                fileCallback.onReceiveValue(new Uri[]{ cameraImageUri });
            } else {
                fileCallback.onReceiveValue(null);
            }
            fileCallback = null;
        }
    }

    // ── Permissions ──────────────────────────────────────────────
    private void requestAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean allGranted = true;
            for (String p : REQUIRED_PERMISSIONS) {
                if (ContextCompat.checkSelfPermission(this, p)
                        != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (!allGranted) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQ_PERMISSIONS);
            }
        }
    }

    // ── Back button ──────────────────────────────────────────────
    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
