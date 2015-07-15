package com.android.app.tvbuff;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nitbhati on 7/5/15.
 */
public class NotificationTriggerReceiver extends BroadcastReceiver {


    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v("receiver", "Notification fire code shud be here.");
        sendNotification(context,intent);
    }

    private void sendNotification(Context context,Intent intent) {
        //create intent that will be fired when notification is clicked
        Log.v("receiver","FiringNotification");
        Intent notificationIntent = new Intent(context, MainActivity.class);
        JSONObject programmejson;
        String title="",channel="";
        long starttime,stoptime;
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        //TODO refresh on swipe up
        //TODO phone reboot notification
        //TODO navigation drawer beautify
        //TODO Settings: Channel filter and remind me before template
        //TODO Notification drawer icon
        //TODO Collapse issue/bug
        //TODO Scroll to top

        //using notification compat builder to set up notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);

        // Set the intent that will fire when the user taps the notification.
        //builder.setContentIntent(pendingIntent);

        builder.setAutoCancel(true);

        //Set the large icon, which appears on the left of the notification.
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
        builder.setVisibility(1);

        builder.setContentIntent(pendingIntent);
        Log.v("receiver", intent.getStringExtra(ProgrammesFragment.NOTIFICATION_INTENT_JSON));
        Log.v("receiver", "notification id: " + (int) intent.getLongExtra(ProgrammesFragment.NOTIFICATION_INTENT_ID, 0));
        String jsonString = intent.getStringExtra(ProgrammesFragment.NOTIFICATION_INTENT_JSON);
        try {
                programmejson = new JSONObject(jsonString);
                title = programmejson.getString("title");
                channel = programmejson.getString("channel");
                starttime = programmejson.getLong("starttime");
                stoptime = programmejson.getLong("stoptime");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        builder.setContentTitle(title);
        builder.setContentText("On "+channel);
        builder.setSubText("In 15 minutes");

        //Send the notification. This will immediately display the notification icon in the
        //         notification bar.

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) intent.getLongExtra(ProgrammesFragment.NOTIFICATION_INTENT_ID,0), builder.build());
    }
}
