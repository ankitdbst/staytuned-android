package com.android.app.tvbuff;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.NetworkImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ProgrammesAdapter extends BaseAdapter {
    Context context;
    ArrayList<Programme> programmeArrayList;
    SharedPreferences settingsPrefs;
    String category;

    public ProgrammesAdapter(Context context, ArrayList<Programme> programmes, String category) {
        this.context = context;
        this.programmeArrayList = programmes;

        this.category = category;
        settingsPrefs = context
                .getSharedPreferences(SettingsDialogFragment.SETTINGS_PREF_FILE, 0);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        if (programmeArrayList.size() > 0) {
            return programmeArrayList.get(position);
        }
        return null;
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

        final ProgrammesFragment.CurlSingleton curlSingleton = ProgrammesFragment.CurlSingleton.getInstance(context);
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
                final Bitmap imageNotification;
                final Programme programme = (Programme) getItem(position);
                long longId = ProgrammesFragment.idToLong(programme.getId());

                //TODO move it out of on click listener
                final SharedPreferences notificationSubscribed = context
                        .getSharedPreferences(ProgrammesFragment.NOTIFICATION_PREF, 0);

                SharedPreferences.Editor editor = notificationSubscribed.edit();
                if (notificationSubscribed.contains(programme.getId())) {
                    //remove subscription from prefs data. Toggle operation
                    editor.remove(programme.getId());
                    editor.apply();

                    cancelNotification(view, longId, position);
                    Toast.makeText(context, "Reminder cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    //add to subscription pref data.
                    //add json string in SharedPref
                    Long reminderInterval = settingsPrefs.getLong(SettingsDialogFragment.REMINDER_INTERVAL,
                            15*60*1000);
                    long reminderArray[] = {reminderInterval};
                    JSONObject programmejson = new JSONObject();
                    try {
                        JSONArray reminderIntervalArray = new JSONArray(reminderArray);
                        programmejson.put("id",programme.getId());
                        programmejson.put("title",programme.getTitle());
                        programmejson.put("starttime",programme.getStart().getTime());
                        programmejson.put("stoptime",programme.getStop().getTime());
                        programmejson.put("channel",programme.getChannelName());
                        programmejson.put("remindertimearray",reminderIntervalArray);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    editor.putString(programme.getId(),programmejson.toString());
                    //editor.putLong(programme.getId(), programme.getStop().getTime());
                    //editor.putString(programme.getId()+"_title",programme.getTitle());
                    editor.apply();

                    //Retrieve thumbnail from url and store it in internal storage to be used later by notification
                    ImageRequest imageRequest = new ImageRequest(programme.getThumbnailUrl(),
                            new Response.Listener<Bitmap>() {
                            FileOutputStream fos;
                                @Override
                                public void onResponse(Bitmap bitmap) {
                                //store this bitmap to internal storage
                                    try {
                                        fos = context.openFileOutput(programme.getId(),Context.MODE_PRIVATE);
                                        bitmap.compress(Bitmap.CompressFormat.PNG,100,fos);
                                        fos.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            },0,0, ImageView.ScaleType.CENTER_INSIDE,null,
                            new Response.ErrorListener() {

                                @Override
                                public void onErrorResponse(VolleyError volleyError) {

                                }
                            });
                    curlSingleton.addToRequestQueue(imageRequest);
                    scheduleNotification(view, longId, programmejson);
                    Toast.makeText(context, "Reminder set", Toast.LENGTH_SHORT).show();
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
        if(isProgrammeRunning(programme.getStart())) {
            reminderBtn.setVisibility(View.GONE);
        }
        else {
            reminderBtn.setVisibility(View.VISIBLE);
        }

        // reset imdb info as there maybe some delay in fetching the current views rating
        TextView rating = (TextView) convertView.findViewById(R.id.rating);
        TextView plot = (TextView) convertView.findViewById(R.id.plot);
        TextView actors = (TextView) convertView.findViewById(R.id.actors);
        TextView directors = (TextView) convertView.findViewById(R.id.directors);
        plot.setText("");
        actors.setText("");
        directors.setText("");

        if (programme.getImdb() != null) {
            showIMDbInfo(programme.getImdb(), convertView);
        } else {
            rating.setVisibility(View.GONE);
        }


        // Return the completed view to render on screen
        return convertView;
    }

    //To hide notify me button for running programmes
    private boolean isProgrammeRunning(Date programmeDate)
    {
        Date currDate = Calendar.getInstance().getTime();
        if (currDate.after(programmeDate))
            return true;
        else
            return false;
    }

    private String getFriendlyDateTime(Date date) {
        Date currDate = Calendar.getInstance().getTime();

        if (currDate.after(date)) {
            long minsElapsed = (currDate.getTime() - date.getTime())/(1000*60);
            return "Started " + minsElapsed + " min(s) ago";
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

    private void scheduleNotification(View view, long id, JSONObject programmejson) {
        Log.v("scheduleNotification",""+ id);
        TextView title = (TextView) view.findViewById(R.id.title);
        Intent notificationIntent = new Intent(context, NotificationTriggerReceiver.class);
        //Intent notificationIntent2 = new Intent(context, PhoneBootReceiver.class);
        //context.sendBroadcast(notificationIntent2);
        //Log.v(TAG, "title :" + title.getText().toString() + " ,id :" + id);
        notificationIntent.putExtra(ProgrammesFragment.NOTIFICATION_INTENT_JSON,programmejson.toString());
        notificationIntent.putExtra(ProgrammesFragment.NOTIFICATION_INTENT_ID,id);
        //int currTime = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int)id, notificationIntent,PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // default 15 mins
//        Long reminderInterval = settingsPrefs.getLong(SettingsDialogFragment.REMINDER_INTERVAL,
//                15*60*1000);
        try {
            Long startTime = programmejson.getLong("starttime");
            JSONArray reminderInterval = programmejson.getJSONArray("remindertimearray");
            alarmManager.set(AlarmManager.RTC, startTime - reminderInterval.getLong(0), pendingIntent);
        } catch (JSONException e) {
            // do not set alarm
            Toast.makeText(context, R.string.notification_failure, Toast.LENGTH_SHORT).show();
        }
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

    public void showIMDbInfo(IMDb imdb, View convertView) {
        TextView rating = (TextView) convertView.findViewById(R.id.rating);
        TextView plot = (TextView) convertView.findViewById(R.id.plot);
        TextView actors = (TextView) convertView.findViewById(R.id.actors);
        TextView directors = (TextView) convertView.findViewById(R.id.directors);
        TextView title = (TextView) convertView.findViewById(R.id.title);

        rating.setText(imdb.rating);
        plot.setText(imdb.plot);
        actors.setText(imdb.actors);
        directors.setText(imdb.director);

        title.setText(Html.fromHtml("<strong>" + title.getText() + "</strong>" +
                " <small>" + imdb.year + "</small>"));
    }
}
