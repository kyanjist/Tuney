package com.ale.tuney.model;

import java.util.List;

public abstract class Item {
    private final String id;
    private final String title;
    private final List<String> artists; // lista perché potrebbero esserci featuring
    private final String imageUrl;

    public Item(String id, String title, List<String> artists, String imageUrl) {
        this.id = id;
        this.title = title;
        this.artists = artists;
        this.imageUrl = imageUrl;
    }

    public String getId() { return id; }
    public String getTitle() { return title; }
    public List<String> getArtists() { return artists; }
    public String getImageUrl() { return imageUrl; }
    public abstract String getType();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return id.equals(item.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
