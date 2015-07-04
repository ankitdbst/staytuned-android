package com.bhaijaan.bajrangi.moviesnow;

import android.content.ContentUris;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class IMDb {
    private static final String DATA_SOURCE_URL = "omdbapi.com";

    private MainActivity.CurlSingleton curlSingleton;

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

    private static IMDb mInstance;

    public static void queue(String title, TextView textView,
                        MainActivity.CurlSingleton curlSingleton) {
        if (mInstance == null) {
            mInstance = new IMDb(curlSingleton);
        }

        mInstance.findByIdOrTitle(null, title, textView);
    }

    public IMDb(MainActivity.CurlSingleton curlSingleton) {
        this.curlSingleton = curlSingleton;
    }

    public void findByIdOrTitle(String q_id, final String q_title, final TextView ratingView) {
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
                            if (response.equals("False")) {
                                // We fail once we encounter a fail post a search query
                                // to imdb followed by findByIdOrTitle request
                                if (retryCount > 0) {
                                    retryCount = 0;
                                    return;
                                }
                                // Remove `&`, `and` from q_title string
                                search(q_title);
                                retryCount++;
                                return;
                            }
                            title = jsonObject.getString(TITLE);
                            id = jsonObject.getString(ID);
                            year = jsonObject.getString(YEAR);
                            rating = jsonObject.getString(RATING);
                            plot = jsonObject.getString(PLOT);

                            // Set view text
                            if (ratingView.getVisibility() == View.VISIBLE) {
                                ratingView.setText(rating);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        // do something about the error
                        volleyError.printStackTrace();
                    }
                }
        );

        curlSingleton.addToRequestQueue(jsonObjectRequest);
    }

    public void search(String q_title) {
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

        curlSingleton.addToRequestQueue(jsonObjectRequest);
    }

    // Getters
    public String getRating() {
        return rating;
    }
}
