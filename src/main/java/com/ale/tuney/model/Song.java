package com.ale.tuney.model;

import java.util.List;

public class Song extends Item {

    public Song(String id, String title, List<String> artists, String imageUrl) {
        super(id, title, artists, imageUrl);
    }

    @Override
    public String getType() { return "song"; }

    @Override
    public String toString() {
        String artistNames = String.join(", ", getArtists());
        return "\"" + getTitle() + "\" - " + artistNames;
    }
}
