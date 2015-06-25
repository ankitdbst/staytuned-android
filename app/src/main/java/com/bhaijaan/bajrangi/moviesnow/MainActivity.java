package com.bhaijaan.bajrangi.moviesnow;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;

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

import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private ProgressDialog pDialog;
    private static MySingleton mInstance;

    private static class MySingleton {
        private RequestQueue mRequestQueue;
        private ImageLoader mImageLoader;
        private static Context mCtx;

        private MySingleton(Context context) {
            mCtx = context;
            mRequestQueue = getRequestQueue();

            ImageLoader mImageLoader = new ImageLoader(mRequestQueue, new LruBitmapCache(
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
        setContentView(R.layout.activity_main);

        // Showing progress dialog
        pDialog = new ProgressDialog(MainActivity.this);
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();

        // Get a RequestQueue
        final RequestQueue queue = MySingleton.getInstance(this.getApplicationContext()).
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
                    String fromDate = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH)
                            .format(Calendar.getInstance().getTime());
                    String toDate = new SimpleDateFormat("yyyMMdd2359", Locale.ENGLISH)
                            .format(Calendar.getInstance().getTime());
                    // Do something with the response
                    String url = new Uri.Builder()
                            .scheme("http")
                            .authority(DATASOURCE_URL)
                            .path(SCHEDULE_LIST_PATH)
                            .appendQueryParameter(USER_ID, "0")
                            .appendQueryParameter(CHANNEL_LIST, response.replace(" ", "%20"))
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

                                ArrayList<HashMap<String, String>> programmeList =
                                        new ArrayList<HashMap<String, String>>();

                                for (int i = 0; i < channels.length(); ++i) {
                                    JSONObject channelObj = channels.getJSONObject(i);
                                    JSONArray programmes = channelObj.getJSONArray(TAG_PROGRAMME);

                                    for (int j = 0; j < programmes.length(); ++j) {
                                        HashMap<String, String> programme = new HashMap<>();

                                        String title =
                                                Html.fromHtml(programmes.getJSONObject(j)
                                                        .getString(TAG_PROGRAMME_TITLE)).toString();
                                        title = Html.fromHtml(title).toString();

                                        programme.put(TAG_PROGRAMME_TITLE,
                                                title);
                                        programme.put(TAG_PROGRAMME_GENRE,
                                                programmes.getJSONObject(j)
                                                        .getString(TAG_PROGRAMME_GENRE));
                                        programme.put(TAG_CHANNEL_NAME,
                                                channelObj.getString(TAG_CHANNEL_NAME));

                                        String startTime = programmes.getJSONObject(j)
                                                .getString(TAG_PROGRAMME_START);
                                        DateFormat format = new SimpleDateFormat("yyyyMMddHHmm",
                                                Locale.ENGLISH);
                                        Date date = format.parse(startTime);
                                        programme.put(TAG_PROGRAMME_START,
                                               date.toLocaleString());
                                        programmeList.add(programme);
                                    }
                                }

                                // Now create a new list adapter bound to the cursor.
                                // SimpleListAdapter is designed for binding to a Cursor.
                                ListAdapter adapter = new SimpleAdapter(
                                        MainActivity.this,
                                        programmeList,
                                        R.layout.list_item,
                                        new String[] {
                                                TAG_PROGRAMME_TITLE,
                                                TAG_PROGRAMME_GENRE,
                                                TAG_CHANNEL_NAME,
                                                TAG_PROGRAMME_START
                                        },
                                        new int[] {
                                                R.id.title,
                                                R.id.genre,
                                                R.id.channelname,
                                                R.id.starttime
                                        });

                                // Bind to our new adapter.
                                setListAdapter(adapter);
                            } catch (JSONException | ParseException e) {
                                e.printStackTrace();
                            }

                            if (pDialog.isShowing())
                                pDialog.dismiss();
                        }
                    }, new Response.ErrorListener() {

                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // TODO Auto-generated method stub
                            if (pDialog.isShowing())
                                pDialog.dismiss();
                        }
                    });

                    queue.add(jsObjRequest);
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

        queue.add(channelListRequest);
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
