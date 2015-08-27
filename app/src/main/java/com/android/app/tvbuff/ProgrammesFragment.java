package com.android.app.tvbuff;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;

public class ProgrammesFragment extends ListFragment {

    //public static final String DATA_SOURCE_URL = "timesofindia.indiatimes.com";
    public static final String DATA_SOURCE_URL = "stay-tunedapp.rhcloud.com";
    //public static final String DATA_SOURCE_URL = "10.0.2.2";
    //public static final String CHANNEL_LIST_PATH = "tvschannellist.cms";
    public static final String CHANNEL_LIST_PATH = "/api/channels";
    //public static final String SCHEDULE_LIST_PATH = "tvschedulejson.cms";
    public static final String SCHEDULE_LIST_PATH = "/api/listings";

    // Programme categories
    public static final String programmeCategories[] = {
            "movies",
            "entertainment",
            "sports",
            "kids",
            "lifestyle",
            "music",
            "news",
            "documentary",
            "religious"
    };

    // Programme languages
    public static final String programmeLanguages[] = {
            "english",
            "hindi"
    };

    // Configuration param names
    public static final String USER_ID = "userid";
    //public static final String CHANNEL_LIST = "channellist";
    public static final String CHANNEL_LIST = "channels";
    //public static final String CHANNEL_LIST_GENRE_NAME = "genrename";
    public static final String CHANNEL_LIST_GENRE_NAME = "category";
    public static final String CHANNEL_SORT_FIELD = "sortfield";
    //public static final String CHANNEL_LIST_LANGUAGE_NAME = "languagename";
    public static final String CHANNEL_LIST_LANGUAGE_NAME = "language";
    public static final String CHANNEL_LIST_FROM_DATE = "startime";
    //public static final String CHANNEL_LIST_FROM_DATE = "fromdatetime";
    public static final String CHANNEL_LIST_TO_DATE = "stoptime";
    //public static final String CHANNEL_LIST_TO_DATE = "todatetime";

    // Logging tag
    public static final String TAG = "ProgrammeFragment";

    public static final String TAG_SCHEDULE = "ScheduleGrid";
    public static final String TAG_CHANNEL = "channel";
    public static final String TAG_CHANNEL_NAME = "channel_name";
    public static final String TAG_PROGRAMME = "programme";
    public static final String TAG_PROGRAMME_ID = "_id";
    public static final String TAG_PROGRAMME_TITLE = "title";
    public static final String TAG_PROGRAMME_IMAGE_URL = "programmeurl";
    public static final String TAG_PROGRAMME_GENRE = "subgenre";
    public static final String TAG_PROGRAMME_START = "start";
    public static final String TAG_PROGRAMME_STOP = "stop";
    public static final String TAG_PROGRAMME_DURATION = "duration";

    //Notification Intent data sent
    public static final String NOTIFICATION_INTENT_JSON = "com.bajrangi.moviesnow.JSON";
    public static final String NOTIFICATION_INTENT_ID = "com.bajrangi.moviesnow.ID";
    public static final String NOTIFICATION_PREF = "notificationSubscription";

    // # of items left when API should prefetch data
    private final int PREFETCH_LIMIT = 8;
    private final int MIN_PROGLIST_ITEMS = 10;
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH);

    private static CurlSingleton mInstance;

    /* Calendar instance */
    private Calendar calendar = Calendar.getInstance();

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
    private List<String> channelList = new ArrayList<>();
    private SharedPreferences channelListPref;
    /* Track the count of pages retrieved from the API */
    private int pageCount = 0;
    private int firstVisibleItemCount = 0;

    /* Category with which the fragment is instantiated */
    private String mCategory;
    /* Language with which the fragment is instantiated */
    private String mLanguage;
    /* Pref key for current selection */
    private String currentPrefKey;

	private SwipeRefreshLayout mSwipeRefreshLayout;
    private TextView scrollToTop;

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

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static ProgrammesFragment newInstance(String category, String language) {
        ProgrammesFragment fragment = new ProgrammesFragment();
        Bundle args = new Bundle();

        args.putString(NavigationDrawerFragment.ITEM_CATEGORY, category);
        args.putString(NavigationDrawerFragment.ITEM_LANGUAGE, language);

        fragment.setArguments(args);
        return fragment;
    }

    private void getChannelListFromPref() {
        for (Map.Entry<String, ?> entry : channelListPref.getAll().entrySet()) {
            if ((Boolean)entry.getValue()) {
                channelList.add(entry.getKey());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Create the list fragment's content view by calling the super method
        //final View listFragmentView = super.onCreateView(inflater, container, savedInstanceState);
        final View listFragmentView = inflater.inflate(R.layout.fragment_programmes, container , false);
        scrollToTop = (TextView) listFragmentView.findViewById(R.id.scrollToTop);
        //tv.setText("scrolling");

        // Now create a SwipeRefreshLayout to wrap the fragment's content view
        mSwipeRefreshLayout = new ListFragmentSwipeRefreshLayout(container.getContext());
        // Add the list fragment's content view to the SwipeRefreshLayout, making sure that it fills
        // the SwipeRefreshLayout
        mSwipeRefreshLayout.addView(listFragmentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        // Make sure that the SwipeRefreshLayout will fill the fragment
        mSwipeRefreshLayout.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

        // Now return the SwipeRefreshLayout as this fragment's content view
        return mSwipeRefreshLayout;
    }

    public void setOnRefreshListener(SwipeRefreshLayout.OnRefreshListener listener) {
        mSwipeRefreshLayout.setOnRefreshListener(listener);
    }

    public boolean isRefreshing() {
        return mSwipeRefreshLayout.isRefreshing();
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
    }

    public void setColorScheme(int colorRes1, int colorRes2, int colorRes3, int colorRes4) {
        mSwipeRefreshLayout.setColorSchemeResources(colorRes1, colorRes2, colorRes3, colorRes4);
    }

    public SwipeRefreshLayout getSwipeRefreshLayout() {
        return mSwipeRefreshLayout;
    }

    private class ListFragmentSwipeRefreshLayout extends SwipeRefreshLayout {

        public ListFragmentSwipeRefreshLayout(Context context) {
            super(context);
        }

        /**
         * As mentioned above, we need to override this method to properly signal when a
         * 'swipe-to-refresh' is possible.
         *
         * @return true if the {@link android.widget.ListView} is visible and can scroll up.
         */
        @Override
        public boolean canChildScrollUp() {
            final ListView listView = getListView();
            if (listView.getVisibility() == View.VISIBLE) {
                return canListViewScrollUp(listView);
            } else {
                return false;
            }
        }

    }
    /**
     * Utility method to check whether a {@link ListView} can scroll up from it's current position.
     * Handles platform version differences, providing backwards compatible functionality where
     * needed.
     */
    private static boolean canListViewScrollUp(ListView listView) {
        if (android.os.Build.VERSION.SDK_INT >= 14) {
            // For ICS and above we can call canScrollVertically() to determine this
            return ViewCompat.canScrollVertically(listView, -1);
        } else {
            // Pre-ICS we need to manually check the first visible item and the child view's top
            // value
            return listView.getChildCount() > 0 &&
                    (listView.getFirstVisiblePosition() > 0
                            || listView.getChildAt(0).getTop() < listView.getPaddingTop());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        onRefreshHandler();
        //loadData(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        // test
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        Bundle args = getArguments();

        mCategory = args.getString(NavigationDrawerFragment.ITEM_CATEGORY);
        mLanguage = args.getString(NavigationDrawerFragment.ITEM_LANGUAGE);

        currentPrefKey = getActivity().getPackageName() + "." + mCategory + "_" + mLanguage;

        // Showing progress dialog
        pDialog = new ProgressDialog(getActivity());
        pDialog.setMessage("Please wait...");
        pDialog.setCancelable(false);
        pDialog.show();

        mInstance = CurlSingleton.getInstance(getActivity().getApplicationContext());

        // Create a Programmes Adapter to retrieve the list of programmes
        programmeList = new ArrayList<>();
        adapter = new ProgrammesAdapter(getActivity(), programmeList, mCategory);
        // Local storage :: Retrieve channel listings prefs
        /** Shared Preference list file key for Channel Lists
         Pref file for each category
         Can store:
         1. Channel list to query
         2. Genre to filter
         3. Imdb rating to fetch YES/NO etc.
         */
        channelListPref = getActivity().getSharedPreferences(currentPrefKey, 0);

        final ListView listView = getListView();

        scrollToTop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listView.smoothScrollToPosition(0);
                //listView.setFastScrollEnabled(true);
                //listView.fling(100);
            }
        });
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

                if (firstVisibleItem > 30 && firstVisibleItem < firstVisibleItemCount) {
                    //Toast.makeText(getActivity(),"Scroll to top button for 3 seconds", Toast.LENGTH_LONG).show();
                    // view.smoothScrollToPosition(0);
                    //view.fling(1000);
                    scrollToTop.setVisibility(View.VISIBLE);
                } else if (firstVisibleItem < 30) {
                    scrollToTop.setVisibility(View.GONE);
                } else if (firstVisibleItem > firstVisibleItemCount) {
                    scrollToTop.setVisibility(View.GONE);
                }

                if ((totalItemCount - firstVisibleItem < PREFETCH_LIMIT && visibleItemCount > 0) ||
                        (firstVisibleItem + visibleItemCount == totalItemCount &&
                                totalItemCount != 0) ||
                        (firstVisibleItem + visibleItemCount + PREFETCH_LIMIT == totalItemCount)) {
                    if (!loading) {
                        loading = true;
                        loadData(false);
                    }
                }
                firstVisibleItemCount = firstVisibleItem;
            }
        });

        // No detail view for other categories?!
        if (mCategory.equals("movies")) {
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Programme programme = programmeList.get(position);
                    Boolean programmeCollapsed = programme.getCollapsed();

                    RelativeLayout imdbDetailView = (RelativeLayout) view.findViewById(R.id.imdb_detail);

                    if (programmeCollapsed) {
                        imdbDetailView.setVisibility(View.VISIBLE);
                    } else {
                        imdbDetailView.setVisibility(View.GONE);
                    }
                    programme.setCollapsed(!programmeCollapsed);
                }
            });
        }

        setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                onRefreshHandler();
            }
        });

        // Bind to our new adapter.
        setListAdapter(adapter);

        if (channelListPref.getAll().size() > 0) {
            getChannelListFromPref();
            // Always load channel listing from the prefs, if present
            if (channelList.size() == 0) {
                Toast.makeText(getActivity(), R.string.channel_list_empty ,
                        Toast.LENGTH_LONG).show();
                return;
            }
            loadData(false);
            return;
        }

        // Retrieve channel listings fresh
        String url = new Uri.Builder()
                .scheme("http")
                .authority(DATA_SOURCE_URL)
                .path(CHANNEL_LIST_PATH)
                .appendQueryParameter(CHANNEL_LIST_GENRE_NAME, mCategory)
                .appendQueryParameter(CHANNEL_LIST_LANGUAGE_NAME, mLanguage)
                .build()
                .toString();


        JsonRequest channelListRequest = new JsonArrayRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        SharedPreferences.Editor editor = channelListPref.edit();
                        for (int i = 0; i < response.length(); ++i) {
                            try {
                                JSONObject channelObj = (JSONObject) response.get(i);
                                String channelName = channelObj.getString("name");
                                channelList.add(channelName);
                                editor.putBoolean(channelName, true);
                            } catch (JSONException e) {
                                Log.d(TAG, e.getMessage());
                                //e.printStackTrace();
                            }
                        }
                        // commit changes async
                        editor.apply();

                        loading = true;
                        loadData(false);
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

    private void onRefreshHandler() {
        if (programmeList.size() == 0) {
            loadData(true);
            return;
        }

        //traverse through programme list and remove which have been completed
        int index = 0;
        List<Integer> oldProgrammes = new ArrayList<>();

        for(Programme p:programmeList) {
            if(p.getStop().getTime()-System.currentTimeMillis()<0) {
                oldProgrammes.add(index);
            }
            index++;
        }

        for(int i=0;i<oldProgrammes.size();i++) {
            programmeList.remove(oldProgrammes.get(i).intValue());
        }

        Programme lastProgramme = programmeList.get(programmeList.size() - 1);

        //setting calendar to the timestamp of last programmelist element
        // Edge case: programme list empty
        calendar.setTime(lastProgramme.getStart());

        //Checking if only 1 hour data is left or only 10 items left in
        if(programmeList.size()<MIN_PROGLIST_ITEMS ||
                lastProgramme.getStart().getTime()-System.currentTimeMillis()< 60*60*1000) {
            //call loadData
            loadData(false);
        } else {
            adapter.notifyDataSetChanged();
        }
        setRefreshing(false);
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

    public ProgrammesFragment() {
    }

    public void loadData(boolean reset) {
        if (reset) {
            calendar = Calendar.getInstance();
            // Fetch current channel list
            channelList.clear();
            getChannelListFromPref();
            // Reset adapter
            programmeList.clear();
            programmeMap.clear();
            // loading
            loading = true;
            pDialog.show();
        }

        String fromDate = dateFormat.format(calendar.getTime());
        // Retrieve all the listings from t to t+3*pageCount hours
        calendar.add(Calendar.HOUR_OF_DAY, 3);
        String toDate = dateFormat.format(calendar.getTime());

        int idx = 0, size = channelList.size();
        StringBuilder channelListStr = new StringBuilder();
        for (String channel : channelList) {
            channelListStr.append(channel);
            if (idx < size-1) {
                channelListStr.append(",");
            }
            idx++;
        }

        String url = new Uri.Builder()
                .scheme("http")
                .authority(DATA_SOURCE_URL)
                .path(SCHEDULE_LIST_PATH)
                .appendQueryParameter(CHANNEL_LIST, channelListStr.toString())
                .appendQueryParameter(CHANNEL_LIST_FROM_DATE, fromDate)
                .appendQueryParameter(CHANNEL_LIST_TO_DATE, toDate)
                .build()
                .toString();

        JsonRequest jsObjRequest = new JsonArrayRequest
                (Request.Method.GET, url, null,  new Response.Listener<JSONArray>() {

                    @Override
                    public void onResponse(JSONArray programmes) {
                        try {
                            ArrayList<Programme> list =
                                    new ArrayList<Programme>();

                            for (int j = 0; j < programmes.length(); ++j) {
                                JSONObject programmeObj = programmes.getJSONObject(j);
                                String channelName = Html.fromHtml(programmeObj
                                        .getString(TAG_CHANNEL_NAME)).toString();
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
                                p.setChannelName(channelName);
                                p.setSubscribed(getActivity()
                                        .getSharedPreferences(ProgrammesFragment.NOTIFICATION_PREF, 0)
                                        .contains(programmeId));

                                if (!programmeObj.isNull("imdb")) {
                                    JSONObject imdbObj = programmeObj.getJSONObject("imdb");

                                    IMDb imdb = new IMDb();
                                    imdb.id = imdbObj.getString(IMDb.ID);
                                    imdb.plot = imdbObj.getString(IMDb.PLOT);
                                    imdb.title = imdbObj.getString(IMDb.TITLE);
                                    imdb.year = imdbObj.getString(IMDb.YEAR);
                                    imdb.rating = imdbObj.getString(IMDb.RATING);
                                    imdb.actors = imdbObj.getString(IMDb.ACTORS);
                                    imdb.director = imdbObj.getString(IMDb.DIRECTOR);

                                    p.setIMDb(imdb);
                                }

                                list.add(p);
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
                        Log.v(TAG, "Failed to load data: " + error.getLocalizedMessage());
                        if (getActivity() != null && error instanceof NetworkError) {
                            Toast.makeText(getActivity(), R.string.connection_failure,
                                    Toast.LENGTH_LONG).show();
                        }
                    }
                });

        int socketTimeout = 5000;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
        jsObjRequest.setRetryPolicy(policy);
        mInstance.addToRequestQueue(jsObjRequest);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.channel_filter) {
            DialogFragment fragment = new FiltersDialogFragment();

            Bundle args = new Bundle();
            args.putString(NavigationDrawerFragment.ITEM_CATEGORY, mCategory);
            args.putString(NavigationDrawerFragment.ITEM_LANGUAGE, mLanguage);

            fragment.setArguments(args);
            fragment.show(getActivity().getSupportFragmentManager(), "FiltersDialogFragment");
            return true;
        } else if (id == R.id.action_settings) {
            DialogFragment fragment = new SettingsDialogFragment();
            fragment.show(getActivity().getSupportFragmentManager(), "SettingsDialogFragment");
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
