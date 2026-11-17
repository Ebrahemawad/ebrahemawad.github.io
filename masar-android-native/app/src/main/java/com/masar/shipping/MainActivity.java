package com.masar.shipping;
import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.net.Uri;
import org.json.JSONArray;
public final class MainActivity extends Activity {
 private WebView web;
 static final String URL="https://ebrahemawad.github.io/masar/";
 static final String CHANNEL_ID="masar_delivery_reminders";
 private static final int NOTIFICATION_REQUEST=901;
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  NotificationManager nm=getSystemService(NotificationManager.class);
  if(Build.VERSION.SDK_INT>=26)nm.createNotificationChannel(new NotificationChannel(CHANNEL_ID,"تذكيرات توصيل مسار",NotificationManager.IMPORTANCE_HIGH));
  web=new WebView(this);setContentView(web);
  web.getSettings().setJavaScriptEnabled(true);
  web.getSettings().setDomStorageEnabled(true);
  web.getSettings().setAllowFileAccess(false);
  web.getSettings().setAllowContentAccess(false);
  web.addJavascriptInterface(new NativeBridge(),"MasarNative");
  web.setWebChromeClient(new WebChromeClient());
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
    Uri uri=request.getUrl();
    if("https".equals(uri.getScheme())&&"ebrahemawad.github.io".equals(uri.getHost())&&uri.getPath()!=null&&uri.getPath().startsWith("/masar/"))return false;
    try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(Exception ignored){}
    return true;
   }
  });
  web.loadUrl(URL);
 }
 private boolean trusted(){
  if(web==null)return false;
  try{Uri uri=Uri.parse(web.getUrl());return "https".equals(uri.getScheme())&&"ebrahemawad.github.io".equals(uri.getHost())&&uri.getPath()!=null&&uri.getPath().startsWith("/masar/");}catch(Exception e){return false;}
 }
 private class NativeBridge {
  @JavascriptInterface public void enableNotifications(){
   runOnUiThread(()->{
    if(!trusted())return;
    if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
     requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATION_REQUEST);
   });
  }
  @JavascriptInterface public void syncAppointments(String serialized){
   runOnUiThread(()->{
    if(!trusted()||serialized==null||serialized.length()>200000)return;
    try{ReminderScheduler.sync(MainActivity.this,new JSONArray(serialized));}catch(Exception e){android.util.Log.w("Masar","Reminder sync failed",e);}
   });
  }
  @JavascriptInterface public void clearReminders(){
   runOnUiThread(()->{if(trusted())ReminderScheduler.clear(MainActivity.this);});
  }
  @JavascriptInterface public void testNotification(){
   runOnUiThread(()->{
    if(!trusted())return;
    if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
     requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATION_REQUEST);return;
    }
    ReminderReceiver.show(MainActivity.this,"اختبار إشعارات مسار","إشعارات التطبيق الأصلية شغالة على الجهاز",900001);
   });
  }
 }
 @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onDestroy(){if(web!=null){web.removeJavascriptInterface("MasarNative");web.destroy();}super.onDestroy();}
}
