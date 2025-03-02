package com.example.myapp.data;

import androidx.annotation.NonNull;

public class User {

    public User(String id, String fullName, String email, String avatarUrl, String phoneNumber, String gender, Role role) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.phoneNumber = phoneNumber != null ? phoneNumber : "";
        this.gender = gender != null ? gender : "";
        this.role = role != null ? String.valueOf(role) : Role.USER.toString();
    }

    public enum Role {
        USER, ADMIN, EDITOR, VIEWER;

        @NonNull
        @Override
        public String toString() {
            return name();
        }

        public static Role fromString(String role) {
            try {
                return Role.valueOf(role);
            } catch (IllegalArgumentException e) {
                return USER;
            }
        }
    }

    private String id; // ID tài liệu từ Firestore
    private String fullName;
    private String email;
    private String phoneNumber;
    private String gender;
    private String avatarUrl;
    private String role;
    private String status;
    private int points;

    // Constructor mặc định cần thiết cho Firestore
    public User() {}

    public User(String id, String fullName, String email, String avatarUrl, String phoneNumber) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.phoneNumber = phoneNumber != null ? phoneNumber : "";
        this.gender = gender != null ? gender : "";
        this.role = role != null ? role : Role.USER.toString();
    }

    // Getters
    public String getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getAvatarUrl() { return avatarUrl; }
    public String getPhoneNumber() { return phoneNumber; }
    public String getGender() { return gender; }
    public Role getRole() { return Role.fromString(role); }
    public String getStatus() { return status; }
    public int getPoints() { return points; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setGender(String gender) { this.gender = gender; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setRole(Role role) { this.role = role.toString(); }
    public void setStatus(String status) { this.status = status; }
    public void setPoints(int points) { this.points = points; }
}
