package com.ale.tuney.model;

public class Comment {
    private final int id;
    private final User author;
    private final int reviewId;
    private final String text;
    private final String createdAt;

    public Comment(int id, User author, int reviewId, String text, String createdAt) {
        this.id = id;
        this.author = author;
        this.reviewId = reviewId;
        this.text = text;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public User getAuthor() { return author; }
    public int getReviewId() { return reviewId; }
    public String getText() { return text; }
    public String getCreatedAt() { return createdAt; }

}
