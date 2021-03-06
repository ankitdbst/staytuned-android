package com.android.app.tvbuff;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
        JSONObject programmejson;
        JSONArray reminderInterval = new JSONArray();
        long stoptime = 0, starttime = 0, reminderIntervalLong = 0;
        final Map<String,?> storedData = notificationSubscribed.getAll();
        long currTime = System.currentTimeMillis();
        for(String key:storedData.keySet())
        {
           // Toast.makeText(context,"Reading preference file",Toast.LENGTH_SHORT);
            //Log.v("bootrestart", key + " value: " + Long.parseLong(storedData.get(key).toString()));
            //long id = Long.parseLong(key);
            long id = ProgrammesFragment.idToLong(key);
            //iterating through each item
            try {
                programmejson = new JSONObject(storedData.get(key).toString());
                stoptime = programmejson.getLong("stoptime");
                starttime = programmejson.getLong("starttime");
                reminderInterval = programmejson.getJSONArray("remindertimearray");
                reminderIntervalLong = reminderInterval.getLong(0);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(stoptime<currTime)
            {
                //Reset the alarm for this
                Intent notificationIntent = new Intent(context, NotificationTriggerReceiver.class);
                //Log.v(TAG, "title :" + title.getText().toString() + " ,id :" + id);
                //notificationIntent.putExtra(ProgrammesFragment.NOTIFICATION_INTENT_TITLE,title.getText().toString());
                notificationIntent.putExtra(ProgrammesFragment.NOTIFICATION_INTENT_ID,id);
                notificationIntent.putExtra(ProgrammesFragment.NOTIFICATION_INTENT_JSON,storedData.get(key).toString());
                int currentTime = (int) System.currentTimeMillis();
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int)id, notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.set(AlarmManager.RTC, starttime - reminderIntervalLong, pendingIntent);
            }
        }

    }
}
