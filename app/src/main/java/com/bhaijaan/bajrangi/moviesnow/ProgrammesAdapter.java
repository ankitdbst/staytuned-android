package com.bhaijaan.bajrangi.moviesnow;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;

import java.util.ArrayList;
import java.util.HashMap;

public class ProgrammesAdapter extends BaseAdapter {
    Context context;
    ArrayList<Programme> programmeArrayList;

    public ProgrammesAdapter(Context context, ArrayList<Programme> programmes) {
        this.context = context;
        this.programmeArrayList = programmes;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int position) {
        return programmeArrayList.get(position);
    }

    @Override
    public int getCount() {
        return programmeArrayList.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        Programme programme = (Programme) getItem(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.list_item, parent, false);
        }

        // Lookup view for data population
        TextView title = (TextView) convertView.findViewById(R.id.title);
        TextView genre = (TextView) convertView.findViewById(R.id.genre);
        TextView channelName = (TextView) convertView.findViewById(R.id.channelname);
        TextView startTime = (TextView) convertView.findViewById(R.id.starttime);

        NetworkImageView mNetworkImageView = (NetworkImageView) convertView.findViewById(R.id.thumbnail);

        // Get the ImageLoader through your singleton class.
        ImageLoader mImageLoader = MainActivity.CurlSingleton.getInstance(context).getImageLoader();

        // Set the URL of the image that should be loaded into this view, and
        // specify the ImageLoader that will be used to make the request.
        mNetworkImageView.setImageUrl(programme.getThumbnailUrl(), mImageLoader);

        // Populate the data into the template view using the data object
        title.setText(programme.getTitle());
        genre.setText(programme.getGenre());
        channelName.setText(programme.getChannelName());
        startTime.setText(programme.getStart().toLocaleString());

        // Return the completed view to render on screen
        return convertView;
    }
}
