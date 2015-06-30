package com.bhaijaan.bajrangi.moviesnow;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ListView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends ListActivity {

    public static final String DATASOURCE_URL = "timesofindia.indiatimes.com";
    public static final String CHANNEL_LIST_PATH = "tvschannellist.cms";
    public static final String SCHEDULE_LIST_PATH = "tvschedulejson.cms";

    // Configuration param names
    public static final String USER_ID = "userid";
    public static final String CHANNEL_LIST = "channellist";
    public static final String CHANNEL_LIST_GENRE_NAME = "genrename";
    public static final String CHANNEL_LIST_LANGUAGE_NAME = "languagename";
    public static final String CHANNEL_LIST_FROM_DATE = "fromdatetime";
    public static final String CHANNEL_LIST_TO_DATE = "todatetime";

    public static final String TAG_SCHEDULE = "ScheduleGrid";
    public static final String TAG_CHANNEL = "channel";
    public static final String TAG_CHANNEL_NAME = "channeldisplayname";
    public static final String TAG_PROGRAMME = "programme";
    public static final String TAG_PROGRAMME_TITLE = "title";
    public static final String TAG_PROGRAMME_IMAGE_URL = "programmeurl";
    public static final String TAG_PROGRAMME_GENRE = "subgenre";
    public static final String TAG_PROGRAMME_START = "start";
    public static final String TAG_PROGRAMME_STOP = "stop";
    public static final String TAG_PROGRAMME_DURATION = "duration";

    private final Calendar start = Calendar.getInstance();
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH);

    private static MySingleton mInstance;

    private ProgressDialog pDialog;
    private ArrayList<Programme> programmeList;
    private ProgrammesAdapter adapter;
    private String channelList;
    private RequestQueue requestQueue;
    private boolean loading = false;


    public static class MySingleton {
        private RequestQueue mRequestQueue;
        private ImageLoader mImageLoader;
        private static Context mCtx;

        private MySingleton(Context context) {
            mCtx = context;
            mRequestQueue = getRequestQueue();

            mImageLoader = new ImageLoader(mRequestQueue, new LruBitmapCache(
                    LruBitmapCache.getCacheSize(context)));
        }

        public static synchronized MySingleton getInstance(Context context) {
            if (mInstance == null) {
                mInstance = new MySingleton(context);
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

        // Create a Programmes Adapter to retrieve the list of programmes
        programmeList = new ArrayList<>();
        adapter = new ProgrammesAdapter(MainActivity.this, programmeList);

        ListView listView = getListView();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem + visibleItemCount == totalItemCount && totalItemCount != 0) {
                    if (!loading) {
                        loading = true;
                        loadData();
                    }
                }
            }
        });

        // Bind to our new adapter.
        setListAdapter(adapter);

        // Get a RequestQueue
        requestQueue = MySingleton.getInstance(this.getApplicationContext()).
                getRequestQueue();

        // Formulate the request and handle the response.
        String url = new Uri.Builder()
                .scheme("http")
                .authority(DATASOURCE_URL)
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

        requestQueue.add(channelListRequest);
    }

    private void loadData() {
        final Date thresholdDate = start.getTime();
        String fromDate = dateFormat.format(start.getTime());
        // Retrieve all the listings from t to t+3*pageCount hours
        start.add(Calendar.HOUR_OF_DAY, 3);
        String toDate = dateFormat.format(start.getTime());

        String url = new Uri.Builder()
                .scheme("http")
                .authority(DATASOURCE_URL)
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
                            // Check if the start date for the programme is after the threshold
                            // for current load request, as the API returns programs which are currently
                            // running and not just those that started after `start`
                            if (programmeObj.getString(TAG_PROGRAMME_START)
                                    .compareTo(dateFormat.format(thresholdDate)) < 0) {
                                continue;
                            }
                            String title =
                                    Html.fromHtml(programmeObj
                                            .getString(TAG_PROGRAMME_TITLE)).toString();
                            title = Html.fromHtml(title).toString();

                            Programme p = new Programme();
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
                // TODO Auto-generated method stub
                if (pDialog.isShowing())
                    pDialog.dismiss();
                loading = false;
            }
        });

        requestQueue.add(jsObjRequest);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
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
