package com.android.app.tvbuff;

import android.net.Uri;
import android.util.Log;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.StringTokenizer;

public class IMDb {
    private static final String DATA_SOURCE_URL = "omdbapi.com";

    private ProgrammesFragment.CurlSingleton curlSingleton;
    private int retryCount = 0;

    // Words, Symbols to be filtered from the search text before query to IMDb
    private final String filterWords[] = { "and", "of", "the", "an", "a" };
    private final String filterSymbols[] = { "&", ":", ";" };

    private static final String TAG = "IMDb";

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
    private static final String TYPE = "Type";
    private static final String ACTORS = "Actors";
    private static final String DIRECTOR = "Director";
    private static final String SEARCH = "Search";


    private static IMDb mInstance;

    public static void queue(ProgrammesAdapter adapter, View convertView,
                             Programme programme, ProgrammesFragment.CurlSingleton curlSingleton) {
        if (mInstance == null) {
            mInstance = new IMDb(curlSingleton);
        }

        mInstance.findByIdOrTitle(null, programme, adapter, convertView);
    }

    public IMDb(ProgrammesFragment.CurlSingleton curlSingleton) {
        this.curlSingleton = curlSingleton;
    }

    public void findByIdOrTitle(String q_id, final Programme programme,
                                final ProgrammesAdapter adapter, final View convertView) {
        final String q_title = programme.getTitle();

        Uri.Builder builder = new Uri.Builder()
                .scheme("http")
                .authority(DATA_SOURCE_URL)
                .appendQueryParameter(QUERY_PLOT, "short")
                .appendQueryParameter(QUERY_RETURN, "json");

        if (q_id != null && !q_id.isEmpty()) {
            builder.appendQueryParameter(QUERY_ID, q_id);
        } else if (q_title != null && !q_title.isEmpty()) {
            builder.appendQueryParameter(QUERY_TITLE, q_title);
        } else {
            Log.e(TAG, "Required parameter q_title and q_id are both null/empty.");
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
                                    programme.setImDbNA(false);
                                    return;
                                }
                                search(q_title, programme, adapter, convertView);
                                retryCount++;
                                return;
                            }

                            IMDbDetail imDbDetail = new IMDbDetail();

                            imDbDetail.setId(jsonObject.getString(ID));
                            imDbDetail.setPlot(jsonObject.getString(PLOT));
                            imDbDetail.setTitle(jsonObject.getString(TITLE));
                            imDbDetail.setYear(jsonObject.getString(YEAR));
                            imDbDetail.setRating(jsonObject.getString(RATING));
                            imDbDetail.setActors(jsonObject.getString(ACTORS));
                            imDbDetail.setDirectors(jsonObject.getString(DIRECTOR));

                            programme.setImDbDetail(imDbDetail);
                            if (convertView != null) {
                                adapter.showIMDbInfo(imDbDetail, convertView);
                            }
                        } catch (JSONException e) {
                            Log.d(TAG, "Error parsing response: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.e(TAG, "Volley Error: " + volleyError.getMessage());
                    }
                }
        );

        curlSingleton.addToRequestQueue(jsonObjectRequest);
    }

    private String filter(String title) {
        StringTokenizer tokenizer = new StringTokenizer(title);
        StringBuilder stringBuilder = new StringBuilder();

        while (tokenizer.hasMoreTokens()) {
            boolean flag = true;
            String token = tokenizer.nextToken();
            for (String word: filterWords) {
                if (token.equalsIgnoreCase(word)) {
                    flag = false;
                }
            }

            for (String symbol: filterSymbols) {
                if (token.equalsIgnoreCase(symbol)) {
                    flag = false;
                }
            }

            if (flag) {
                stringBuilder.append(token);
                if (tokenizer.hasMoreTokens()) {
                    stringBuilder.append(" ");
                }
            }
        }

        return stringBuilder.toString();
    }

    private String computeClosestMatch(String str, JSONArray results) throws JSONException {
        int strLen = str.length();
        int minEditDistance = 100, minEditDistanceIndex = -1;

        for (int i = 0; i < results.length(); ++i) {
            String result = results.getJSONObject(i).getString(TITLE);
            int len = result.length();

            int dp[][] = new int[strLen+1][len+1];

            for (int j = 0; j < strLen+1; ++j)
                dp[j][0] = j;

            for (int j = 0; j < len+1; ++j)
                dp[0][j] = j;

            for (int j = 1; j < strLen+1; ++j) {
                for (int k = 1; k < len+1; ++k) {
                    dp[j][k] = Math.min(Math.min(dp[j-1][k]+1, dp[j][k-1]+1),
                                dp[j-1][k-1] + (str.charAt(j-1) == result.charAt(k-1) ? 1 : 0));
                }
            }

            if (dp[strLen][len] < minEditDistance) {
                minEditDistance = dp[strLen][len];
                minEditDistanceIndex = i;
            }
        }

        return results.getJSONObject(minEditDistanceIndex).getString(ID);
    }

    public void search(final String title, final Programme programme,
                       final ProgrammesAdapter adapter, final View convertView) {
        final String q_title = filter(title);

        Uri.Builder builder = new Uri.Builder()
                .scheme("http")
                .authority(DATA_SOURCE_URL)
                .appendQueryParameter(QUERY_RETURN, "json");

        builder.appendQueryParameter(QUERY_SEARCH, q_title);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                JsonObjectRequest.Method.GET, builder.build().toString(), null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        try {
                            String response = jsonObject.getString(RESPONSE);
                            if (response.equals("False")) {
                                programme.setImDbNA(false);
                            }
                        } catch (JSONException e) {
                            Log.d(TAG, "No results found for search: "
                                    + q_title + " " + e.getMessage());
                        }

                        try {
                            JSONArray results = jsonObject.getJSONArray(SEARCH);
                            String imdbId = computeClosestMatch(title, results);
                            findByIdOrTitle(imdbId, programme, adapter, convertView);
                        } catch (JSONException e) {
                            Log.d(TAG, "Error parsing response for search: "
                                    + q_title + " " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        Log.e(TAG, "Volley Error: " + volleyError.getMessage());
                    }
                }
        );

        curlSingleton.addToRequestQueue(jsonObjectRequest);
    }
}
