package com.bhaijaan.bajrangi.moviesnow;

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
