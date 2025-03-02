package com.example.myapp.data;

import java.util.List;

public class Review {
    private String userId;
    private String userName; // Tên người dùng
    private String userAvatarUrl; // Ảnh đại diện
    private float rating;
    private String comment;
    private List<String> mediaUrls;

    public Review() {}

    public Review(String userId, String userName, String userAvatarUrl, float rating, String comment) {
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.rating = rating;
        this.comment = comment;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatarUrl() { return userAvatarUrl; }
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public List<String> getMediaUrls() { return mediaUrls; }
    public void setMediaUrls(List<String> mediaUrls) { this.mediaUrls = mediaUrls; }
}

