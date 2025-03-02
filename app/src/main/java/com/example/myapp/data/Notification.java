package com.example.myapp.data;


import com.google.firebase.Timestamp;

public class Notification {
    private String id;
    private String title;
    private String message;
    private String type;
    private String orderId;
    private Timestamp timestamp;
    private boolean isRead;
    private boolean forAdmin;

    public Notification() {} // Required for Firestore

    public Notification(String title, String message, String type, String orderId,
                        Timestamp timestamp, boolean isRead, boolean forAdmin) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.orderId = orderId;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.forAdmin = forAdmin;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public boolean isForAdmin() { return forAdmin; }
    public void setForAdmin(boolean forAdmin) { this.forAdmin = forAdmin; }
}