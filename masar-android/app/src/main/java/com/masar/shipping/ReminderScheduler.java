package com.masar.shipping;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
final class ReminderScheduler {
 private static final String PREFS="masar_reminders", KEY="appointments";
 static void sync(Context ctx,JSONArray input)throws Exception {
  JSONObject existing=load(ctx),next=new JSONObject();
  Set<String> seen=new HashSet<>();
  for(int i=0;i<input.length();i++){
   JSONObject item=input.optJSONObject(i);if(item==null)continue;
   int id=item.optInt("shipment_id",0);String when=item.optString("appointment","");
   if(id<=0||when.isEmpty())continue;
   long time=OffsetDateTime.parse(when).toInstant().toEpochMilli();
   if(time<=System.currentTimeMillis())continue;
   String key=String.valueOf(id);next.put(key,when);seen.add(key);
   if(!when.equals(existing.optString(key)))schedule(ctx,id,Math.max(System.currentTimeMillis()+2000,time-600000));
  }
  for(Iterator<String> it=existing.keys();it.hasNext();){
   String key=it.next();if(!seen.contains(key))try{cancel(ctx,Integer.parseInt(key));}catch(Exception ignored){}
  }
  ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,next.toString()).apply();
 }
 static void clear(Context ctx){
  JSONObject existing=load(ctx);
  for(Iterator<String> it=existing.keys();it.hasNext();){try{cancel(ctx,Integer.parseInt(it.next()));}catch(Exception ignored){}}
  ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(KEY).apply();
 }
 static void restore(Context ctx){
  JSONObject data=load(ctx);
  for(Iterator<String> it=data.keys();it.hasNext();){
   String id=it.next();try{
    long time=OffsetDateTime.parse(data.getString(id)).toInstant().toEpochMilli()-600000;
    if(time+600000>System.currentTimeMillis())schedule(ctx,Integer.parseInt(id),Math.max(System.currentTimeMillis()+2000,time));
   }catch(Exception ignored){}
  }
 }
 private static JSONObject load(Context ctx){try{return new JSONObject(ctx.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"{}"));}catch(Exception e){return new JSONObject();}}
 static void schedule(Context ctx,int id,long at){
  AlarmManager manager=(AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
  PendingIntent pi=alarmIntent(ctx,id);
  if(Build.VERSION.SDK_INT>=31&&manager.canScheduleExactAlarms())manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
  else if(Build.VERSION.SDK_INT>=23)manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi);
  else manager.set(AlarmManager.RTC_WAKEUP,at,pi);
 }
 static void cancel(Context ctx,int id){((AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE)).cancel(alarmIntent(ctx,id));}
 private static PendingIntent alarmIntent(Context ctx,int id){
  Intent i=new Intent(ctx,ReminderReceiver.class);i.putExtra("shipmentId",id);
  return PendingIntent.getBroadcast(ctx,id,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
 }
}
