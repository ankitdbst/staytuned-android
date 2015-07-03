package com.bhaijaan.bajrangi.moviesnow;

import android.app.DownloadManager;
import android.net.Uri;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class Programme {
    private String id;
    private String title;
    private Date start = null;
    private Date stop = null;
    private int duration;
    private String thumbnailUrl;
    private String genre;
    private String channelName;

    private IMDb imdb = new IMDb();

    public static class IMDb {
        private static final String DATA_SOURCE_URL = "omdb.com";

        // Request constants
        private static final String QUERY_TITLE = "t";
        private static final String QUERY_SEARCH = "s";
        private static final String QUERY_ID = "i";
        private static final String QUERY_TYPE = "type";
        private static final String QUERY_YEAR = "year";
        private static final String QUERY_PLOT = "plot";
        private static final String QUERY_RETURN = "r";

        // Response constants
        private static final String RESPONSE = "Response";
        private static final String TITLE = "Title";
        private static final String RATING = "imdbRating";
        private static final String YEAR = "Year";
        private static final String PLOT = "Plot";
        private static final String ID = "imdbID";

        private String id;
        private String title;
        private String rating;
        private String year;
        private String plot;
        private int retryCount;

        public void findByIdOrTitle(final MainActivity.CurlSingleton mInstance,
                                    String q_id, final String q_title) {
            Uri.Builder builder = new Uri.Builder()
                    .scheme("http")
                    .authority(DATA_SOURCE_URL)
                    .appendQueryParameter(QUERY_PLOT, "short")
                    .appendQueryParameter(QUERY_RETURN, "json")
                    .appendQueryParameter(QUERY_TYPE, "movie");

            if (q_id != null && !q_id.isEmpty()) {
                builder.appendQueryParameter(QUERY_ID, q_id);
            } else if (q_title != null && !q_title.isEmpty()) {
                builder.appendQueryParameter(QUERY_TITLE, q_title);
            } else {
                // do nothing for now, but exception should be thrown
                return;
            }

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.GET, builder.build().toString(), null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {
                            try {
                                String response = jsonObject.getString(RESPONSE);
                                if (response.equals("FALSE")) {
                                    // We fail once we encounter a fail post a search query
                                    // to imdb followed by findByIdOrTitle request
                                    if (retryCount > 0) {
                                        retryCount = 0;
                                        return;
                                    }
                                    // Remove `&`, `and` from q_title string
                                    search(mInstance, q_title);
                                    retryCount++;
                                    return;
                                }
                                title = jsonObject.getString(TITLE);
                                id = jsonObject.getString(ID);
                                year = jsonObject.getString(YEAR);
                                rating = jsonObject.getString(RATING);
                                plot = jsonObject.getString(PLOT);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            // do something about the error
                        }
                    }
            );

            mInstance.addToRequestQueue(jsonObjectRequest);
        }

        public void search(MainActivity.CurlSingleton mInstance, String q_title) {
            Uri.Builder builder = new Uri.Builder()
                    .scheme("http")
                    .authority(DATA_SOURCE_URL)
                    .appendQueryParameter(QUERY_RETURN, "json")
                    .appendQueryParameter(QUERY_TYPE, "movie");

            builder.appendQueryParameter(QUERY_SEARCH, q_title);
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                    JsonObjectRequest.Method.GET, builder.build().toString(), null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject jsonObject) {

                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            // do something about the error
                        }
                    }
            );

            mInstance.addToRequestQueue(jsonObjectRequest);
        }
    }

    public static Comparator<Programme> getCompByStartTime() {
        return new Comparator<Programme>(){
            @Override
            public int compare(Programme p1, Programme p2)
            {
                long t1 = p1.getStart().getTime();
                long t2 = p2.getStart().getTime();
                if (t1 == t2)
                    return 0;
                if (t1 < t2)
                    return -1;
                return 1;
            }
        };
    }

    String getTitle() {
        return title;
    }

    String getThumbnailUrl() {
        return thumbnailUrl;
    }

    Date getStart() {
        return start;
    }

    Date getStop() {
        return stop;
    }

    int getDuration() {
        return duration;
    }

    String getGenre() {
        return genre;
    }

    String getChannelName() {
        return channelName;
    }

    String getId() {
        return id;
    }

    IMDb getImdb() {
        return imdb;
    }

    void setTitle(String title) {
        this.title = title;
    }

    void setId(String id) {
        this.id = id;
    }

    void setStart(String date) {
        try {
            DateFormat format = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH);
            this.start = format.parse(date);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    void setStop(String date) {
        try {
            DateFormat format = new SimpleDateFormat("yyyyMMddHHmm", Locale.ENGLISH);
            this.stop = format.parse(date);
        }
        catch (ParseException e) {
            e.printStackTrace();
        }
    }

    void setDuration(int duration) {
        this.duration = duration;
    }

    void setThumbnailUrl(String url) {
        this.thumbnailUrl = url;
    }

    void setGenre(String genre) {
        this.genre = genre;
    }

    void setChannelName(String channelName) {
        this.channelName = channelName;
    }
}
