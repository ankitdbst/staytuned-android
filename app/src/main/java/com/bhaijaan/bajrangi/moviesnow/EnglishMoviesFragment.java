package com.bhaijaan.bajrangi.moviesnow;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.app.ListFragment;
import android.support.v4.view.GestureDetectorCompat;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by nitbhati on 7/9/15.
 */
public class EnglishMoviesFragment extends ListFragment {


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
    private String movieLanguageQuery;


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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Showing progress dialog
        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();


        mInstance = CurlSingleton.getInstance(getActivity().getApplicationContext());

        // Create a Programmes Adapter to retrieve the list of programmes
        programmeList = new ArrayList<>();
        adapter = new ProgrammesAdapter(getActivity(), programmeList);


        //mDetector = new GestureDetectorCompat(this,this);


/*

        ArrayList<Map<String,String>>data = new ArrayList<>();
        for(int i=0;i<10;i++)
        {
            Map<String,String>m = new HashMap<>();
            m.put("ListKey","ListValue");
            data.add(m);
        }
        String to[] = {"ListKey"};
        int toint[] = {R.id.section_label};
        SimpleAdapter adaptor = new SimpleAdapter(getActivity(),data,R.layout.fragment_main_activity3_navigation,to,toint);
        setListAdapter(adaptor);
        pDialog.dismiss();
*/

    }

    public void toggleReminder() {
        //
        Log.v(TAG, "test");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SharedPreferences notificationSubscribed = getActivity().getSharedPreferences(NOTIFICATION_PREF, 0);
        Bundle args = getArguments();
        movieLanguageQuery = args.getString(getString(R.string.movies_language));

        ListView listView = getListView();
        listView.setOnScrollListener(new AbsListView.OnScrollListener(){

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
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

                RelativeLayout imdbDetailView = (RelativeLayout) view.findViewById(R.id.imdb_detail);

                //ResizeAnimation a = new ResizeAnimation(networkImageView);
                //a.setDuration(500);

                //int newHeight, newWidth;
                if (programmeCollapsed) {
                    imdbDetailView.setVisibility(View.VISIBLE);
                    //newHeight = dpToPx(140);
                    //newWidth = dpToPx(140);
                    programme.setCollapsed(false);
                } else {
                    imdbDetailView.setVisibility(View.GONE);
                    //newHeight = dpToPx(100);
                    //newWidth = dpToPx(100);
                    programme.setCollapsed(true);
                }

                // set the starting height (the current height) and the new height that the view should have after the animation
                //a.setParams(networkImageView.getHeight(), newHeight,
                //        networkImageView.getWidth(), newWidth);

                //networkImageView.startAnimation(a);
            }
        });

        // Bind to our new adapter.
        setListAdapter(adapter);

        // Avoid network call, if we have the channel list already fetched from the previous time.
        final SharedPreferences channelListPref = getActivity().getSharedPreferences(PREFS_CHANNEL_LIST, 0);
        if (!channelListPref.contains(CHANNEL_LIST)) {
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
                .appendQueryParameter(CHANNEL_LIST_LANGUAGE_NAME, movieLanguageQuery)
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

    public int dpToPx(int value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics());
    }

    public class ResizeAnimation extends Animation {

        private int startHeight;
        private int deltaHeight; // distance between start and end height

        private int startWidth;
        private int deltaWidth;
        private View view;

        /**
         * constructor, do not forget to use the setParams(int, int) method before
         * starting the animation
         * @param v
         */
        public ResizeAnimation (View v) {
            this.view = v;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {

            view.getLayoutParams().height = (int) (startHeight +
                    deltaHeight * interpolatedTime);
            view.getLayoutParams().width = (int) (startWidth +
                    deltaWidth * interpolatedTime);
            view.requestLayout();
        }

        /**
         * set the starting and ending height for the resize animation
         * starting height is usually the views current height, the end height is the height
         * we want to reach after the animation is completed
         * @param startHeight height in pixels
         * @param endHeight height in pixels
         */
        public void setParams(int startHeight, int endHeight, int startWidth, int endWidth) {

            this.startHeight = startHeight;
            deltaHeight = endHeight - startHeight;

            this.startWidth = startWidth;
            deltaWidth = endWidth - startWidth;
        }

        /**
         * set the duration for the hideshowanimation
         */
        @Override
        public void setDuration(long durationMillis) {
            super.setDuration(durationMillis);
        }

        @Override
        public boolean willChangeBounds() {
            return true;
        }
    }

    public EnglishMoviesFragment() {
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
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }
}
