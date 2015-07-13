package com.android.app.tvbuff;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
        Programme programme = (Programme) getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item, parent, false);
        }

        final View view = convertView;

        // Collapse/Expand for IMDb detail view
        RelativeLayout imdbDetailView = (RelativeLayout) convertView.findViewById(R.id.imdb_detail);
        if (programme.getCollapsed()) {
            imdbDetailView.setVisibility(View.GONE);
        } else {
            imdbDetailView.setVisibility(View.VISIBLE);
        }

        // Lookup view for data population
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView genre = (TextView) convertView.findViewById(R.id.genre);
        TextView channelName = (TextView) convertView.findViewById(R.id.channel_name);
        TextView startTime = (TextView) convertView.findViewById(R.id.start_time);
        TextView duration = (TextView) convertView.findViewById(R.id.duration);

        NetworkImageView mNetworkImageView = (NetworkImageView) convertView.findViewById(R.id.thumbnail);

        ProgrammesFragment.CurlSingleton curlSingleton = ProgrammesFragment.CurlSingleton.getInstance(context);
        // Get the ImageLoader through your singleton class.
        ImageLoader mImageLoader = curlSingleton.getImageLoader();

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

                final SharedPreferences notificationSubscribed = context
                        .getSharedPreferences(ProgrammesFragment.NOTIFICATION_PREF, 0);

                SharedPreferences.Editor editor = notificationSubscribed.edit();
                if (notificationSubscribed.contains(programme.getId())) {
                    //remove subscription from prefs data. Toggle operation
                    editor.remove(programme.getId());
                    editor.apply();

                    Toast.makeText(context, "Reminder cancelled", Toast.LENGTH_SHORT).show();
                    cancelNotification(view, itemIdLong, position);
                } else {
                    //add to subscription pref data.
                    editor.putLong(programme.getId(), programme.getStop().getTime());
                    editor.apply();

                    Toast.makeText(context, "Reminder set", Toast.LENGTH_SHORT).show();
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
        startTime.setText(getFriendlyDateTime(programme.getStart()));
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

    private String getFriendlyDateTime(Date date) {
        Date currDate = Calendar.getInstance().getTime();

        if (currDate.after(date)) {
            long minsElapsed = (currDate.getTime() - date.getTime())/(1000*60);
            return "Running since: " + minsElapsed + " min(s)";
        } else {
            DateFormat dateFormatter = new SimpleDateFormat("h:mm a", Locale.ENGLISH);
            DateFormat dateFormatter2 = new SimpleDateFormat("EEE, MMM d, h:mm a", Locale.ENGLISH);

            long diff = date.getDate() - currDate.getDate();
            if (diff == 0) {
                return "Today, " + dateFormatter.format(date);
            } else if (diff == 1) {
                return "Tomorrow, " + dateFormatter.format(date);
            } else {
                return dateFormatter2.format(date);
            }
        }
    }

    private void scheduleNotification(View view, long id) {
        //Log.v(TAG,"Schedule Notification");
        TextView title = (TextView) view.findViewById(R.id.title);
        Intent notificationIntent = new Intent(context, NotificationTriggerReceiver.class);
        //Log.v(TAG, "title :" + title.getText().toString() + " ,id :" + id);
        notificationIntent.putExtra(ProgrammesFragment.NOTIFICATION_INTENT_TITLE,title.getText().toString());
        notificationIntent.putExtra(ProgrammesFragment.NOTIFICATION_INTENT_ID,id);
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
            TextView title = (TextView) convertView.findViewById(R.id.title);

            rating.setText(iMdbDetail.getRating());
            plot.setText(iMdbDetail.getPlot());
            actors.setText(iMdbDetail.getActors());
            directors.setText(iMdbDetail.getDirectors());

            title.setText(Html.fromHtml("<strong>" + title.getText() + "</strong>" +
                    " <small>" + iMdbDetail.getYear() + "</small>"));
        }
    }
}