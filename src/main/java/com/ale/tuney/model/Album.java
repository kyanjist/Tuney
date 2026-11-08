package com.ale.tuney.model;

import java.util.List;

public class Album extends Item {
    private final int year;

    public Album(String id, String title, List<String> artists, int year, String imageUrl) {
        super(id, title, artists, imageUrl);
        this.year = year;
    }

    public int getYear() { return year; }

    @Override
    public String getType() { return "album"; }

    @Override
    public String toString() {
        String artistNames = String.join(", ", getArtists());
        return "\"" + getTitle() + "\" - " + artistNames + " (" + year + ")";
    }
}
