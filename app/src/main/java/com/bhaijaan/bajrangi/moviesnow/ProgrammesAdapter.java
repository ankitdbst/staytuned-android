package com.bhaijaan.bajrangi.moviesnow;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import junit.framework.TestCase;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;

public class ProgrammesAdapter extends BaseAdapter {
    Context context;
    ArrayList<Programme> programmeArrayList;

    public ProgrammesAdapter(Context context, ArrayList<Programme> programmes) {
        this.context = context;
        this.programmeArrayList = programmes;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return programmeArrayList.get(position);
    }

    @Override
    public int getCount() {
        return programmeArrayList.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        //Log.v("receiver","position: "+position);
        Programme programme = (Programme) getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item, parent, false);
        }

        final View view = convertView;

        // Lookup view for data population
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView genre = (TextView) convertView.findViewById(R.id.genre);
        TextView channelName = (TextView) convertView.findViewById(R.id.channel_name);
        TextView startTime = (TextView) convertView.findViewById(R.id.start_time);
        TextView duration = (TextView) convertView.findViewById(R.id.duration);

        NetworkImageView mNetworkImageView = (NetworkImageView) convertView.findViewById(R.id.thumbnail);

        MainActivity.CurlSingleton curlSingleton = MainActivity.CurlSingleton.getInstance(context);
        // Get the ImageLoader through your singleton class.
        ImageLoader mImageLoader = curlSingleton.getImageLoader();
        //TODO Refresh on swipe down
        //TODO ActionBar
        //TODO Fling
        //TODO Bootup
        // Set the URL of the image that should be loaded into this view, and
        // specify the ImageLoader that will be used to make the request.
        mNetworkImageView.setImageUrl(programme.getThumbnailUrl(), mImageLoader);

        Button reminderBtn = (Button) convertView.findViewById(R.id.subscribe);

        if(programme.getSubscribed()) {
            reminderBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_alarm_on_white_18dp, 0, 0, 0);
            reminderBtn.setText(R.string.cancel);
        } else {
            reminderBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_alarm_add_white_18dp, 0, 0, 0);
            reminderBtn.setText(R.string.subscribe);
        }

        reminderBtn.setTag(position);
        reminderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = (Integer) v.getTag();
                Programme programme = (Programme) getItem(position);
                Long itemIdLong = Long.parseLong(programme.getId());
                TextView title = (TextView) view.findViewById(R.id.title);

                final SharedPreferences notificationSubscribed = context
                        .getSharedPreferences(EnglishMoviesFragment.NOTIFICATION_PREF, 0);
                //Log.v(TAG, "position :" + position + ", id: " + id + ",title  " + title.getText());
                SharedPreferences.Editor editor = notificationSubscribed.edit();
                if (notificationSubscribed.contains(programme.getId())) {
                    //remove subscription from prefs data. Toggle operation
                    editor.remove(programme.getId());
                    editor.apply();
                    //remove alarm also TO DO
                    //view.setBackgroundColor(Color.WHITE);
                    cancelNotification(view, itemIdLong, position);
                } else {
                    //add to subscription pref data.
                    editor.putLong(programme.getId(), programme.getStop().getTime());
                    editor.apply();
                    //Log.v(TAG, "Color:Green");
                    //view.setBackgroundColor(Color.GREEN);
                    scheduleNotification(view, itemIdLong);
                }
                programme.setSubscribed(!programme.getSubscribed());
                notifyDataSetChanged();
            }
        });

        // Populate the data into the template view using the data object
        title.setText(programme.getTitle());
        genre.setText(programme.getGenre());
        channelName.setText(programme.getChannelName());
        startTime.setText(programme.getStart().toLocaleString());
        duration.setText(Integer.toString(programme.getDuration()) + " min");

        // reset imdb info as there maybe some delay in fetching the current views rating
        TextView rating = (TextView) convertView.findViewById(R.id.rating);
        TextView plot = (TextView) convertView.findViewById(R.id.plot);
        TextView actors = (TextView) convertView.findViewById(R.id.actors);
        TextView directors = (TextView) convertView.findViewById(R.id.directors);
        rating.setText("N/A");
        plot.setText("");
        actors.setText("");
        directors.setText("");

        RelativeLayout imdbDetailView = (RelativeLayout) convertView.findViewById(R.id.imdb_detail);
        if (programme.getCollapsed()) {
            imdbDetailView.setVisibility(View.GONE);
        } else {
            imdbDetailView.setVisibility(View.VISIBLE);
        }

        // Queue IMDb query for fetching movie information
        IMDbDetail imDbDetail = programme.getImDbDetail();
        if (programme.isImDbNA()) {
            IMDb.queue(this, convertView, programme, curlSingleton);
        } else {
            showIMDbInfo(imDbDetail, convertView);
        }

        // Return the completed view to render on screen
        return convertView;
    }

    private void scheduleNotification(View view, long id) {
        //Log.v(TAG,"Schedule Notification");
        TextView title = (TextView) view.findViewById(R.id.title);
        Intent notificationIntent = new Intent(context, NotificationTriggerReceiver.class);
        //Log.v(TAG, "title :" + title.getText().toString() + " ,id :" + id);
        notificationIntent.putExtra(EnglishMoviesFragment.NOTIFICATION_INTENT_TITLE,title.getText().toString());
        notificationIntent.putExtra(EnglishMoviesFragment.NOTIFICATION_INTENT_ID,id);
        //int currTime = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int)id, notificationIntent,PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis()+10*1000, pendingIntent);

    }

    private void cancelNotification(View view, long id, int position) {

        //Log.v(TAG,"cancelling subscribed notification");
        Intent notificationIntent = new Intent(context, NotificationTriggerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                (int)id, notificationIntent,PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        //Log.v(TAG, "cancelled");
    }

    public void showIMDbInfo(IMDbDetail iMdbDetail, View convertView) {
        if (iMdbDetail != null && convertView.getVisibility() == View.VISIBLE) {
            TextView rating = (TextView) convertView.findViewById(R.id.rating);
            TextView plot = (TextView) convertView.findViewById(R.id.plot);
            TextView actors = (TextView) convertView.findViewById(R.id.actors);
            TextView directors = (TextView) convertView.findViewById(R.id.directors);

            rating.setText(iMdbDetail.getRating());
            plot.setText(iMdbDetail.getPlot());
            actors.setText(iMdbDetail.getActors());
            directors.setText(iMdbDetail.getDirectors());
        }
    }
}
