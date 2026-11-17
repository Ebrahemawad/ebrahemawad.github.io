package com.masar.shipping;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
public final class ReminderReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context ctx,Intent intent){int id=intent.getIntExtra("shipmentId",0);if(id>0)show(ctx,"مسار | تذكير توصيل","فاضل 10 دقايق على ميعاد توصيل الشحنة رقم "+id,id);}
 static void show(Context ctx,String title,String text,int id){
  if(Build.VERSION.SDK_INT>=33&&ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  Intent launch=ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
  if(launch==null)return;
  PendingIntent pi=PendingIntent.getActivity(ctx,0,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(ctx,MainActivity.CHANNEL_ID):new Notification.Builder(ctx);
  Notification n=b.setContentTitle(title).setContentText(text).setSmallIcon(android.R.drawable.ic_popup_reminder).setContentIntent(pi).setAutoCancel(true).build();
  ((NotificationManager)ctx.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id,n);
 }
}
