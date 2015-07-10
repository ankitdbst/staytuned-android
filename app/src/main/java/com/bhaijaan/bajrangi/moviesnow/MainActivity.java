package com.bhaijaan.bajrangi.moviesnow;

import android.app.AlarmManager;
import android.app.ListActivity;
import android.app.ListFragment;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;


public class MainActivity extends ListActivity implements GestureDetector.OnGestureListener{

    public static final String DATA_SOURCE_URL = "timesofindia.indiatimes.com";
    public static final String CHANNEL_LIST_PATH = "tvschannellist.cms";
    public static final String SCHEDULE_LIST_PATH = "tvschedulejson.cms";

    public static final String PREFS_CHANNEL_LIST = "ChannelListFile";

    // Configuration param names
    public static final String USER_ID = "userid";
    public static final String CHANNEL_LIST = "channellist";
    public static final String CHANNEL_LIST_GENRE_NAME = "genrename";
    public static final String CHANNEL_LIST_LANGUAGE_NAME = "languagename";
    public static final String CHANNEL_LIST_FROM_DATE = "fromdatetime";
    public static final String CHANNEL_LIST_TO_DATE = "todatetime";

    public static final String TAG = "MoviesNowActivityMain";
    public static final String GESTUR_DEBUG_TAG = "Gestures";
    public static final String TAG_SCHEDULE = "ScheduleGrid";
    public static final String TAG_CHANNEL = "channel";
    public static final String TAG_CHANNEL_NAME = "channeldisplayname";
    public static final String TAG_PROGRAMME = "programme";
    public static final String TAG_PROGRAMME_ID = "programmeid";
    public static final String TAG_PROGRAMME_TITLE = "title";
    public static final String TAG_PROGRAMME_IMAGE_URL = "programmeurl";
    public static final String TAG_PROGRAMME_GENRE = "subgenre";
    public static final String TAG_PROGRAMME_START = "start";
    public static final String TAG_PROGRAMME_STOP = "stop";
    public static final String TAG_PROGRAMME_DURATION = "duration";

    //Notification Intent data sent
    public static final String NOTIFICATION_INTENT_TITLE = "com.bajrangi.moviesnow.TITLE";
    public static final String NOTIFICATION_INTENT_ID = "com.bajrangi.moviesnow.ID";
    public static final String NOTIFICATION_PREF = "notificationSubscription";
    // # of items left when API should prefetch data
    int NOTIFICATION_ID = 1;
    private final int PREFETCH_LIMIT = 5;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH);
    private GestureDetectorCompat mDetector;

    private static CurlSingleton mInstance;

    /* Calendar instance */
    private final Calendar calendar = Calendar.getInstance();

    private ProgressDialog pDialog;
    /* Store programme items */
    private ArrayList<Programme> programmeList;
    /* Store programmes currently running to invalidate duplicates during
    * successive fetches from the API */
    private HashMap<String, Boolean> programmeMap = new LinkedHashMap<>();
    /* Programme Model */
    private ProgrammesAdapter adapter;
    /* Indicate data being fetched */
    private boolean loading = false;
    /* List of channels to retrieve programme listings from */
    private String channelList;
    /* Track the count of pages retrieved from the API */
    private int pageCount = 0;

    @Override
    public boolean onDown(MotionEvent e) {
        Log.v(GESTUR_DEBUG_TAG,"onDown: "+e.toString());
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.v(GESTUR_DEBUG_TAG,"onShowPress: "+e.toString());
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.v(GESTUR_DEBUG_TAG,"onSingleTapUp: "+e.toString());
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.v(GESTUR_DEBUG_TAG,"onScroll: "+e1.toString()+e2.toString()+"distanceX: "+distanceX+" distanceY: "+distanceY);
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.v(GESTUR_DEBUG_TAG,"onLongPress: "+e.toString());
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.v(GESTUR_DEBUG_TAG, "onFling: " + e1.toString() + e2.toString() + "velocityX: " + velocityX + " velocityY: " + velocityY);
        return true;
    }

    public static class CurlSingleton {
        private RequestQueue mRequestQueue;
        private ImageLoader mImageLoader;
        private static Context mCtx;

        private CurlSingleton(Context context) {
            mCtx = context;
            mRequestQueue = getRequestQueue();

            mImageLoader = new ImageLoader(mRequestQueue, new LruBitmapCache(
                    LruBitmapCache.getCacheSize(context)));
        }

        public static synchronized CurlSingleton getInstance(Context context) {
            if (mInstance == null) {
                mInstance = new CurlSingleton(context);
            }
            return mInstance;
        }

        public RequestQueue getRequestQueue() {
            if (mRequestQueue == null) {
                // getApplicationContext() is key, it keeps you from leaking the
                // Activity or BroadcastReceiver if someone passes one in.
                mRequestQueue = Volley.newRequestQueue(mCtx.getApplicationContext());
            }
            return mRequestQueue;
        }

        public <T> void addToRequestQueue(Request<T> req) {
            getRequestQueue().add(req);
        }

        public ImageLoader getImageLoader() {
            return mImageLoader;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Showing progress dialog
        pDialog = new ProgressDialog(MainActivity.this);
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();

        setContentView(R.layout.activity_main);

        mInstance = CurlSingleton.getInstance(getApplicationContext());

        // Create a Programmes Adapter to retrieve the list of programmes
        programmeList = new ArrayList<>();
        adapter = new ProgrammesAdapter(MainActivity.this, programmeList);
        final SharedPreferences notificationSubscribed = getSharedPreferences(NOTIFICATION_PREF, 0);

        mDetector = new GestureDetectorCompat(this,this);
        ListView listView = getListView();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem + visibleItemCount + PREFETCH_LIMIT == totalItemCount &&
                        totalItemCount != 0) {
                    if (!loading) {
                        loading = true;
                        loadData();
                    }
                }
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Programme programme = programmeList.get(position);
                Boolean programmeCollapsed = programme.getCollapsed();

                NetworkImageView networkImageView = (NetworkImageView) findViewById(R.id.thumbnail);
                ViewGroup.LayoutParams layoutParams = networkImageView.getLayoutParams();
                if (programmeCollapsed) {
                    layoutParams.height = (int) TypedValue
                            .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 140,
                                    getResources().getDisplayMetrics());
                    layoutParams.width = (int) TypedValue
                            .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 140,
                                    getResources().getDisplayMetrics());
                    programme.setCollapsed(false);
                } else {
                    layoutParams.height = (int) TypedValue
                            .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80,
                                    getResources().getDisplayMetrics());
                    layoutParams.width = (int) TypedValue
                            .applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80,
                                    getResources().getDisplayMetrics());
                    programme.setCollapsed(true);
                }
                networkImageView.requestLayout();

                /*
                Long itemIdLong = Long.parseLong(subscribeToggle.getId());
                TextView title = (TextView) view.findViewById(R.id.title);
                Log.v(TAG, "position :" + position + ", id: " + id + ",title  " + title.getText());
                SharedPreferences.Editor editor = notificationSubscribed.edit();
                if (notificationSubscribed.contains(subscribeToggle.getId())) {
                    //remove subscription from prefs data. Toggle operation
                    editor.remove(subscribeToggle.getId());
                    editor.apply();
                    //remove alarm also TO DO
                    //view.setBackgroundColor(Color.WHITE);
                    cancelNotification(view, itemIdLong, position);
                } else {
                    //add to subscription pref data.
                    editor.putLong(subscribeToggle.getId(), subscribeToggle.getStop().getTime());
                    editor.apply();
                    Log.v(TAG, "Color:Green");
                    //view.setBackgroundColor(Color.GREEN);
                    scheduleNotification(view, itemIdLong);
                }
                subscribeToggle.setSubscribed(!subscribeToggle.getSubscribed());
                adapter.notifyDataSetChanged();
                //sendNotification(view);
                */
            }
        });
        /*
        listView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.v(GESTUR_DEBUG_TAG,"listView onTouch: "+event.toString());
                mDetector.onTouchEvent(event);
                return true;
            }
        });
    */
        // Bind to our new adapter.
        setListAdapter(adapter);

        // Avoid network call, if we have the channel list already fetched from the previous time.
        final SharedPreferences channelListPref = getSharedPreferences(PREFS_CHANNEL_LIST, 0);
        if (channelListPref.contains(CHANNEL_LIST)) {
            channelList = channelListPref.getString(CHANNEL_LIST, "");
            if (channelList != null && !channelList.isEmpty()) {
                loadData();
                return;
            }
        }

        // Formulate the request and handle the response.
        String url = new Uri.Builder()
                .scheme("http")
                .authority(DATA_SOURCE_URL)
                .path(CHANNEL_LIST_PATH)
                .appendQueryParameter(USER_ID, "0")
                .appendQueryParameter(CHANNEL_LIST_GENRE_NAME, "movies")
                .appendQueryParameter(CHANNEL_LIST_LANGUAGE_NAME, "english")
                .build()
                .toString();

        StringRequest channelListRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        channelList = response;

                        SharedPreferences.Editor editor = channelListPref.edit();
                        editor.putString(CHANNEL_LIST, channelList);
                        // commit changes async
                        editor.apply();

                        loading = true;
                        loadData();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Handle error
                        if (pDialog.isShowing())
                            pDialog.dismiss();
                    }
                });

        mInstance.addToRequestQueue(channelListRequest);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.v(GESTUR_DEBUG_TAG,"MotionEvent: "+event.toString());
        this.mDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    public void sendNotification(View view) {
        //create intent that will be fired when notification is clicked

        Log.v(TAG, "inNotification");
        Intent intent = new Intent(getApplicationContext(),com.bhaijaan.bajrangi.moviesnow.MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);

        //using notification compat builder to set up notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Set the intent that will fire when the user taps the notification.
        //builder.setContentIntent(pendingIntent);

        builder.setAutoCancel(true);

        //Set the large icon, which appears on the left of the notification.
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher));
        builder.setVisibility(1);

        builder.setContentIntent(pendingIntent);

        builder.setContentTitle("BasicNotifications Sample");
        builder.setContentText("Time to learn about notifications!");
        builder.setSubText("Tap to view documentation about notifications.");

        //Send the notification. This will immediately display the notification icon in the
        //         notification bar.

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID++, builder.build());
    }

    private void scheduleNotification(View view, long id) {
        Log.v(TAG,"Schedule Notification");
        TextView title = (TextView) view.findViewById(R.id.title);
        Intent notificationIntent = new Intent(this,NotificationTriggerReceiver.class);
        Log.v(TAG, "title :" + title.getText().toString() + " ,id :" + id);
        notificationIntent.putExtra(NOTIFICATION_INTENT_TITLE,title.getText().toString());
        notificationIntent.putExtra(NOTIFICATION_INTENT_ID,id);
        //int currTime = (int) System.currentTimeMillis();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, (int)id, notificationIntent,PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, System.currentTimeMillis()+10*1000, pendingIntent);

    }

    private void cancelNotification(View view, long id, int position) {

        Log.v(TAG,"cancelling subscribed notification");
        Intent notificationIntent = new Intent(this,NotificationTriggerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, (int)id, notificationIntent,PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        Log.v(TAG, "cancelled");
    }


    private void loadData() {
        String fromDate = dateFormat.format(calendar.getTime());
        // Retrieve all the listings from t to t+3*pageCount hours
        calendar.add(Calendar.HOUR_OF_DAY, 3);
        String toDate = dateFormat.format(calendar.getTime());

        String url = new Uri.Builder()
                .scheme("http")
                .authority(DATA_SOURCE_URL)
                .path(SCHEDULE_LIST_PATH)
                .appendQueryParameter(USER_ID, "0")
                .appendQueryParameter(CHANNEL_LIST, channelList)
                .appendQueryParameter(CHANNEL_LIST_FROM_DATE, fromDate)
                .appendQueryParameter(CHANNEL_LIST_TO_DATE, toDate)
                .build()
                .toString();

        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray channels = response.getJSONObject(TAG_SCHEDULE).
                                    getJSONArray(TAG_CHANNEL);

                            ArrayList<Programme> list =
                                    new ArrayList<Programme>();

                            for (int i = 0; i < channels.length(); ++i) {
                                JSONObject channelObj = channels.getJSONObject(i);
                                JSONArray programmes = channelObj.getJSONArray(TAG_PROGRAMME);

                                for (int j = 0; j < programmes.length(); ++j) {
                                    JSONObject programmeObj = programmes.getJSONObject(j);
                                    String programmeId = programmeObj.getString(TAG_PROGRAMME_ID);
                                    if (programmeMap.containsKey(programmeId)) {
                                        continue;
                                    }
                                    // Add currently added item to map, to filter these in the next
                                    // iteration, if running.
                                    programmeMap.put(programmeId, true);

                                    String title =
                                            Html.fromHtml(programmeObj
                                                    .getString(TAG_PROGRAMME_TITLE)).toString();
                                    title = Html.fromHtml(title).toString();

                                    Programme p = new Programme();
                                    //Log.v(TAG,"id: "+programmeId+" title: "+title);
                                    p.setId(programmeId);
                                    p.setTitle(title);
                                    p.setStart(programmeObj.getString(TAG_PROGRAMME_START));
                                    p.setStop(programmeObj.getString(TAG_PROGRAMME_STOP));
                                    p.setDuration(programmeObj.getInt(TAG_PROGRAMME_DURATION));
                                    p.setGenre(programmeObj.getString(TAG_PROGRAMME_GENRE));
                                    p.setThumbnailUrl(programmeObj.
                                            getString(TAG_PROGRAMME_IMAGE_URL));
                                    p.setChannelName(channelObj.getString(TAG_CHANNEL_NAME));
                                    list.add(p);
                                }
                            }

                            pageCount++;
                            // We can remove the currently running programmes keys from the map
                            // because no movie can last > 6 hours No?
                            // i.e after 2 iterations
                            if (pageCount > 0 && pageCount%2 == 0) {
                                programmeMap.clear();
                                for (Programme p: list) {
                                    programmeMap.put(p.getId(), true);
                                }
                            }

                            Collections.sort(list, Programme.getCompByStartTime());
                            programmeList.addAll(list);
                            adapter.notifyDataSetChanged();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (pDialog.isShowing())
                            pDialog.dismiss();
                        loading = false;
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (pDialog.isShowing())
                            pDialog.dismiss();
                        loading = false;
                    }
                });

        mInstance.addToRequestQueue(jsObjRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
