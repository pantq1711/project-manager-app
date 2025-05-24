package com.example.projectmanager.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class for Budget with Firestore Timestamp handling
 */
public class Budget {
    private String id;
    private String title;
    private String description;
    private double amount;
    private String category;
    private String userId;
    private boolean approved;
    private Date createdAt;
    private Date updatedAt;

    public Budget() {
        // Required for Firestore
        this.approved = false;
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    public Budget(String title, double amount, String description, String category, String userId, boolean approved) {
        this();
        this.title = title;
        this.amount = amount;
        this.description = description;
        this.category = category;
        this.userId = userId;
        this.approved = approved;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    // Convert to Map for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("title", title);
        result.put("description", description);
        result.put("amount", amount);
        result.put("category", category);
        result.put("userId", userId);
        result.put("approved", approved);
        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);
        return result;
    }

    // Create from Map with Timestamp handling
    public static Budget fromMap(Map<String, Object> map) {
        Budget budget = new Budget();
        budget.setId((String) map.get("id"));
        budget.setTitle((String) map.get("title"));
        budget.setDescription((String) map.get("description"));
        budget.setCategory((String) map.get("category"));
        budget.setUserId((String) map.get("userId"));

        // Handle amount
        Object amountObj = map.get("amount");
        if (amountObj instanceof Number) {
            budget.setAmount(((Number) amountObj).doubleValue());
        }

        // Handle approved
        Object approvedObj = map.get("approved");
        if (approvedObj instanceof Boolean) {
            budget.setApproved((Boolean) approvedObj);
        }

        // Handle Firestore Timestamp for createdAt
        Object createdAtObj = map.get("createdAt");
        if (createdAtObj instanceof com.google.firebase.Timestamp) {
            budget.setCreatedAt(((com.google.firebase.Timestamp) createdAtObj).toDate());
        } else if (createdAtObj instanceof Date) {
            budget.setCreatedAt((Date) createdAtObj);
        }

        // Handle Firestore Timestamp for updatedAt
        Object updatedAtObj = map.get("updatedAt");
        if (updatedAtObj instanceof com.google.firebase.Timestamp) {
            budget.setUpdatedAt(((com.google.firebase.Timestamp) updatedAtObj).toDate());
        } else if (updatedAtObj instanceof Date) {
            budget.setUpdatedAt((Date) updatedAtObj);
        }

        return budget;
    }
}