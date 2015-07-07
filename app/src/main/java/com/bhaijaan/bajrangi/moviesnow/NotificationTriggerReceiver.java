package com.bhaijaan.bajrangi.moviesnow;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;

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
        Intent notificationIntent = new Intent(context,com.bhaijaan.bajrangi.moviesnow.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

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
        Log.v("receiver", intent.getStringExtra("com.bajrangi.moviesnow.TITLE"));
        Log.v("receiver","notification id: "+(int) intent.getLongExtra("com.bajrangi.moviesnow.ID",0));
        builder.setContentTitle(intent.getStringExtra("com.bajrangi.moviesnow.TITLE"));
        builder.setContentText("Time to learn about notifications!");
        builder.setSubText("Tap to view documentation about notifications.");

        //Send the notification. This will immediately display the notification icon in the
        //         notification bar.

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) intent.getLongExtra("com.bajrangi.moviesnow.ID",0), builder.build());
    }
}
