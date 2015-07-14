package com.android.app.tvbuff;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Map;

/**
 * Created by nitbhati on 7/14/15.
 */
public class PhoneBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {

        //Accesing preferences where alarm details are stored
        //Toast.makeText(context,"Resetting alarms lost",Toast.LENGTH_SHORT);
        Log.v("bootrestart","reading preferences");
        final SharedPreferences notificationSubscribed = context.getSharedPreferences(ProgrammesFragment.NOTIFICATION_PREF, 0);
        final Map<String,?> storedData = notificationSubscribed.getAll();
        long currTime = Calendar.getInstance().getTimeInMillis();
        for(String key:storedData.keySet())
        {
           // Toast.makeText(context,"Reading preference file",Toast.LENGTH_SHORT);
            Log.v("bootrestart", key + " value: " + Long.parseLong(storedData.get(key).toString()));
            long id = Long.parseLong(key);
            //iterating through each item
            if(Long.parseLong(storedData.get(key).toString())<currTime)
            {
                //Reset the alarm for this
                Intent notificationIntent = new Intent(context, NotificationTriggerReceiver.class);
                //Log.v(TAG, "title :" + title.getText().toString() + " ,id :" + id);
                //notificationIntent.putExtra(ProgrammesFragment.NOTIFICATION_INTENT_TITLE,title.getText().toString());
                notificationIntent.putExtra(ProgrammesFragment.NOTIFICATION_INTENT_ID,id);
                notificationIntent.putExtra(ProgrammesFragment.NOTIFICATION_INTENT_TITLE,"After Restart");
                int currentTime = (int) System.currentTimeMillis();
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int)id, notificationIntent,PendingIntent.FLAG_ONE_SHOT);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC,currentTime+10*1000, pendingIntent);
            }
        }

    }
}
