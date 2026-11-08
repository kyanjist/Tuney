package com.ale.tuney.model;

public class User {
    // dati principali account
    private final int id;
    private final String email;
    private String password;
    private final String username;
    private String name;
    private String bio;
    private String profileImageUrl;

    // costruttore
    public User(int id, String email, String password, String username, String name, String bio, String profileImageUrl) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.username = username;
        this.name = name;
        this.bio = bio;
        this.profileImageUrl = profileImageUrl;
    }

    // getters
    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getBio() { return bio; }
    public String getProfileImageUrl() { return profileImageUrl; }

    // setters
    public void setPassword(String newPassword) { password = newPassword; }
    public void setName(String newName) { name = newName; }
    public void setBio(String newBio) { bio = newBio; }
    public void setProfileImageUrl(String newUrl) { profileImageUrl = newUrl; }

    @Override
    public String toString() {
        return "@" + username;
    }
}
