package com.example.projectmanager.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class User {
    public enum Role {
        ADMIN("admin"),
        MANAGER("manager"),
        MEMBER("member");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Role fromString(String value) {
            for (Role role : Role.values()) {
                if (role.value.equals(value)) {
                    return role;
                }
            }
            return MEMBER; // Default role
        }
    }

    private String id;
    private String email;
    private String displayName;
    private Role role;
    private String fcmToken;
    private Date createdAt;
    private Date lastLogin;
    private boolean isActive;

    public User() {
        // Required for Firestore
    }

    public User(String id, String email, String displayName) {
        this.id = id;
        this.email = email;
        this.displayName = displayName;
        this.role = Role.MEMBER; // Default role
        this.isActive = true;
        this.createdAt = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getLastLogin() { return lastLogin; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    // Permission checks
    public boolean canManageTasks() {
        return role == Role.ADMIN || role == Role.MANAGER;
    }

    public boolean canApproveBudget() {
        return role == Role.ADMIN || role == Role.MANAGER;
    }

    public boolean canManageUsers() {
        return role == Role.ADMIN;
    }

    public boolean canDeleteTasks() {
        return role == Role.ADMIN;
    }

    public boolean canViewReports() {
        return role == Role.ADMIN || role == Role.MANAGER;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("role", role.getValue());
        map.put("fcmToken", fcmToken);
        map.put("createdAt", createdAt);
        map.put("lastLogin", lastLogin);
        map.put("isActive", isActive);
        return map;
    }

    public static User fromMap(Map<String, Object> map) {
        User user = new User();
        user.setId((String) map.get("id"));
        user.setEmail((String) map.get("email"));
        user.setDisplayName((String) map.get("displayName"));
        user.setRole(Role.fromString((String) map.get("role")));
        user.setFcmToken((String) map.get("fcmToken"));

        // Handle Firestore Timestamp for createdAt
        Object createdAtObj = map.get("createdAt");
        if (createdAtObj instanceof com.google.firebase.Timestamp) {
            user.setCreatedAt(((com.google.firebase.Timestamp) createdAtObj).toDate());
        } else if (createdAtObj instanceof Date) {
            user.setCreatedAt((Date) createdAtObj);
        }

        // Handle Firestore Timestamp for lastLogin
        Object lastLoginObj = map.get("lastLogin");
        if (lastLoginObj instanceof com.google.firebase.Timestamp) {
            user.setLastLogin(((com.google.firebase.Timestamp) lastLoginObj).toDate());
        } else if (lastLoginObj instanceof Date) {
            user.setLastLogin((Date) lastLoginObj);
        }

        user.setActive((Boolean) map.getOrDefault("isActive", true));
        return user;
    }
}