package com.ale.tuney.model;

public class Review {
    private final int id;
    private final Item target;
    private final User author;
    private int rating; // da 1 a 5 stelle
    private String text;
    private final String createdAt;

    public Review(int id, Item target, User author, int rating, String text, String createdAt) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Il voto dev'essere compreso tra 1 e 5.");
        }
        this.id = id;
        this.target = target;
        this.author = author;
        this.rating = rating;
        this.text = text;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public Item getTarget() { return target; }
    public User getAuthor() { return author; }
    public int getRating() { return rating; }
    public String getText() { return text; }
    public String getCreatedAt() { return createdAt; }

    public void setRating(int newRating) {
        if (newRating < 1 || newRating > 5) {
            throw new IllegalArgumentException("Il voto dev'essere compreso tra 1 e 5.");
        }
        rating = newRating;
    }

    public void setText(String newText) { text = newText; }

    @Override
    public String toString() {
        return author.toString() + " (" + rating + "/5): " + text;
    }
}
