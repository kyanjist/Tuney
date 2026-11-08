package com.ale.tuney.model;

public class Activity {
    private final int id;
    private final User user;
    private final String type;
    private String referenceText;
    private final String createdAt;
    private Review review;
    private Comment comment;
    private Item item;

    public Activity(int id, User user, String type, String createdAt) {
        this.id = id;
        this.user = user;
        this.type = type;
        this.createdAt = createdAt;
    }

    public User getUser() {
        return user;
    }

    public String getType() {
        return type;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setReferenceText(String referenceText) {
        this.referenceText = referenceText;
    }

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }

    public void setComment(Comment comment) {
        this.comment = comment;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }
}
