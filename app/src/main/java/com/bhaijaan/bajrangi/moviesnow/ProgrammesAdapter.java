package com.bhaijaan.bajrangi.moviesnow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;
import java.util.HashMap;

public class ProgrammesAdapter extends ArrayAdapter<HashMap<String, String>> {
    public ProgrammesAdapter(Context context, ArrayList<HashMap<String, String>> users) {
        super(context, 0, users);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        HashMap<String, String> programme = getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.list_item, parent, false);
        }

        // Lookup view for data population
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView genre = (TextView) convertView.findViewById(R.id.genre);
        TextView channelName = (TextView) convertView.findViewById(R.id.channelname);
        TextView startTime = (TextView) convertView.findViewById(R.id.starttime);

        NetworkImageView mNetworkImageView = (NetworkImageView) convertView.findViewById(R.id.thumbnail);

        // Get the ImageLoader through your singleton class.
        ImageLoader mImageLoader = MainActivity.MySingleton.getInstance(getContext()).getImageLoader();

        // Set the URL of the image that should be loaded into this view, and
        // specify the ImageLoader that will be used to make the request.
        mNetworkImageView.setImageUrl(programme.get(MainActivity.TAG_PROGRAMME_IMAGE_URL), mImageLoader);

        // Populate the data into the template view using the data object
        title.setText(programme.get(MainActivity.TAG_PROGRAMME_TITLE));
        genre.setText(programme.get(MainActivity.TAG_PROGRAMME_GENRE));
        channelName.setText(programme.get(MainActivity.TAG_CHANNEL_NAME));
        startTime.setText(programme.get(MainActivity.TAG_PROGRAMME_START));

        // Return the completed view to render on screen
        return convertView;
    }
}
