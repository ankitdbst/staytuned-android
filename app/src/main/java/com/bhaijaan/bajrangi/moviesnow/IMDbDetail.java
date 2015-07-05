package com.bhaijaan.bajrangi.moviesnow;


public class IMDbDetail {
    private String id;
    private String title;
    private String rating;
    private String year;
    private String plot;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public void setPlot(String plot) {
        this.plot = plot;
    }

    public String getRating() {
        return rating;
    }

    public String getYear() {
        return year;
    }

    public String getPlot() {
        return plot;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }
}
