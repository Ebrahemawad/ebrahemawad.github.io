package com.masar.shipping;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.util.Base64;
import java.io.OutputStream;
import android.webkit.DownloadListener;
import android.webkit.GeolocationPermissions;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String START_URL = "https://ebrahemawad.github.io/masar/";
    private static final String APP_HOST = "ebrahemawad.github.io";
    private static final int LOCATION_REQ = 707;
    private static final int FILE_REQ = 708;
    private static final int PDF_SAVE_REQ = 709;
    private byte[] pdfPending;
    private String pdfSuggestedName;
    private WebView web;
    private GeolocationPermissions.Callback gpsCallback;
    private String gpsOrigin;
    private ValueCallback<Uri[]> fileCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        setContentView(web);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        // Saving PDFs requires the user to approve the target document in Android's file picker.
        // JS interface is intentionally limited to prompting a save; it cannot read local files.
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void savePdf(String dataUri, String fileName) {
                if (dataUri == null || !dataUri.startsWith("data:application/pdf;") ||
                    dataUri.length() > 16 * 1024 * 1024) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "ملف PDF غير صالح أو حجمه كبير", Toast.LENGTH_LONG).show());
                    return;
                }
                try {
                    int comma = dataUri.indexOf(',');
                    if (comma < 0) throw new IllegalArgumentException("Invalid PDF");
                    byte[] data = Base64.decode(dataUri.substring(comma + 1), Base64.DEFAULT);
                    if (data.length < 5 || data[0] != '%' || data[1] != 'P' || data[2] != 'D' || data[3] != 'F') throw new IllegalArgumentException("Not a PDF");
                    String clean = fileName == null ? "Masar-Orders.pdf" : fileName.replaceAll("[^A-Za-z0-9._-]", "_");
                    if (!clean.toLowerCase(Locale.ROOT).endsWith(".pdf")) clean += ".pdf";
                    final String target = clean;
                    runOnUiThread(() -> {
                        if (pdfPending != null) {
                            Toast.makeText(MainActivity.this, "احفظ الملف الحالي أولًا", Toast.LENGTH_LONG).show();
                            return;
                        }
                        pdfPending = data;
                        pdfSuggestedName = target;
                        try {
                            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                            intent.addCategory(Intent.CATEGORY_OPENABLE);
                            intent.setType("application/pdf");
                            intent.putExtra(Intent.EXTRA_TITLE, pdfSuggestedName);
                            startActivityForResult(intent, PDF_SAVE_REQ);
                        } catch(Exception e) {
                            pdfPending = null;
                            Toast.makeText(MainActivity.this, "تعذر فتح نافذة حفظ PDF", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch(Exception e) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "تعذر تجهيز ملف PDF", Toast.LENGTH_LONG).show());
                }
            }
        }, "MasarAndroid");
        CookieManager.getInstance().setAcceptCookie(true);
        // The site and the login service need persistent browser cookies.
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri uri = req.getUrl();
                if ("https".equals(uri.getScheme()) && APP_HOST.equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null && uri.getPath().startsWith("/masar/")) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); }
                catch (Exception e) { Toast.makeText(MainActivity.this, "تعذر فتح الرابط", Toast.LENGTH_LONG).show(); }
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (!origin.equals("https://" + APP_HOST)) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (hasLocation()) { callback.invoke(origin, true, false); return; }
                gpsCallback = callback;
                gpsOrigin = origin;
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQ);
            }
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> cb, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = cb;
                try {
                    Intent chooser = params.createIntent();
                    startActivityForResult(chooser, FILE_REQ);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "تعذر فتح الملفات", Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        });
        web.setDownloadListener((url, userAgent, contentDisposition, mimeType, length) -> {
            // Excel exports generated by client-side JS typically trigger an HTML blob download;
            // Android WebView cannot save blob: links with DownloadManager directly.
            if (url.startsWith("blob:")) {
                Toast.makeText(this, "تصدير Excel من داخل التطبيق يحتاج دعم حفظ الملفات في إصدار لاحق. استخدم الموقع مؤقتًا.", Toast.LENGTH_LONG).show();
                return;
            }
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch(Exception e) { Toast.makeText(this, "تعذر تنزيل الملف", Toast.LENGTH_LONG).show(); }
        });
        web.loadUrl(START_URL);
    }
    private boolean hasLocation() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED
           || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if(requestCode==LOCATION_REQ && gpsCallback!=null) {
            gpsCallback.invoke(gpsOrigin, hasLocation(), false);
            gpsCallback = null; gpsOrigin = null;
        }
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PDF_SAVE_REQ) {
            if(resultCode == RESULT_OK && data != null && data.getData() != null && pdfPending != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                    if(out == null) throw new IllegalStateException("No output stream");
                    out.write(pdfPending);
                    out.flush();
                    Toast.makeText(this, "تم حفظ ملف PDF", Toast.LENGTH_LONG).show();
                } catch(Exception e) {
                    Toast.makeText(this, "فشل حفظ ملف PDF", Toast.LENGTH_LONG).show();
                }
            }
            pdfPending = null;
            pdfSuggestedName = null;
            return;
        }
        if(requestCode==FILE_REQ && fileCallback!=null) {
            Uri[] result = WebChromeClient.FileChooserParams.parseResult(resultCode,data);
            fileCallback.onReceiveValue(result);
            fileCallback=null;
        }
    }
    @Override public void onBackPressed() {
        if(web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if(fileCallback!=null){fileCallback.onReceiveValue(null);fileCallback=null;}
        pdfPending = null;
        if(web!=null) web.destroy();
        super.onDestroy();
    }
}
