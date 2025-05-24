package com.example.projectmanager.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Task model class with all required methods for TaskRepository
 */
public class Task {
    private String id;
    private String title;
    private String description;
    private String assignedToUserId;
    private String assignedToName;
    private String assignerUserId;
    private String assignerName;
    private String status; // pending, in_progress, completed
    private String priority; // low, medium, high
    private Date dueDate;
    private Date createdAt;
    private Date updatedAt;
    private String attachmentUrl;
    private String attachmentName;

    // Default constructor
    public Task() {
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.status = "pending";
        this.priority = "medium";
    }

    // Constructor with basic fields
    public Task(String title, String description, String assignedToUserId, String assignedToName,
                Date dueDate, String priority) {
        this();
        this.title = title;
        this.description = description;
        this.assignedToUserId = assignedToUserId;
        this.assignedToName = assignedToName;
        this.dueDate = dueDate;
        this.priority = priority;
    }

    // Constructor with all fields
    public Task(String title, String description, String assignedToUserId, String assignedToName,
                String assignerUserId, String assignerName, String status, String priority,
                Date dueDate, Date createdAt, Date updatedAt) {
        this.title = title;
        this.description = description;
        this.assignedToUserId = assignedToUserId;
        this.assignedToName = assignedToName;
        this.assignerUserId = assignerUserId;
        this.assignerName = assignerName;
        this.status = status != null ? status : "pending";
        this.priority = priority != null ? priority : "medium";
        this.dueDate = dueDate;
        this.createdAt = createdAt != null ? createdAt : new Date();
        this.updatedAt = updatedAt != null ? updatedAt : new Date();
    }

    // Convert Task to Map for Firestore
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("description", description);
        map.put("assignedToUserId", assignedToUserId);
        map.put("assignedToName", assignedToName);
        map.put("assignerUserId", assignerUserId);
        map.put("assignerName", assignerName);
        map.put("status", status);
        map.put("priority", priority);
        map.put("dueDate", dueDate);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        map.put("attachmentUrl", attachmentUrl);
        map.put("attachmentName", attachmentName);
        return map;
    }

    // Create Task from Map (Firestore document)
    public static Task fromMap(Map<String, Object> map) {
        Task task = new Task();

        if (map.containsKey("title")) task.setTitle((String) map.get("title"));
        if (map.containsKey("description")) task.setDescription((String) map.get("description"));
        if (map.containsKey("assignedToUserId")) task.setAssignedToUserId((String) map.get("assignedToUserId"));
        if (map.containsKey("assignedToName")) task.setAssignedToName((String) map.get("assignedToName"));
        if (map.containsKey("assignerUserId")) task.setAssignerUserId((String) map.get("assignerUserId"));
        if (map.containsKey("assignerName")) task.setAssignerName((String) map.get("assignerName"));
        if (map.containsKey("status")) task.setStatus((String) map.get("status"));
        if (map.containsKey("priority")) task.setPriority((String) map.get("priority"));
        if (map.containsKey("attachmentUrl")) task.setAttachmentUrl((String) map.get("attachmentUrl"));
        if (map.containsKey("attachmentName")) task.setAttachmentName((String) map.get("attachmentName"));

        // Handle Date fields
        if (map.containsKey("dueDate")) {
            Object dueDateObj = map.get("dueDate");
            if (dueDateObj instanceof Date) {
                task.setDueDate((Date) dueDateObj);
            } else if (dueDateObj instanceof com.google.firebase.Timestamp) {
                task.setDueDate(((com.google.firebase.Timestamp) dueDateObj).toDate());
            }
        }

        if (map.containsKey("createdAt")) {
            Object createdAtObj = map.get("createdAt");
            if (createdAtObj instanceof Date) {
                task.setCreatedAt((Date) createdAtObj);
            } else if (createdAtObj instanceof com.google.firebase.Timestamp) {
                task.setCreatedAt(((com.google.firebase.Timestamp) createdAtObj).toDate());
            }
        }

        if (map.containsKey("updatedAt")) {
            Object updatedAtObj = map.get("updatedAt");
            if (updatedAtObj instanceof Date) {
                task.setUpdatedAt((Date) updatedAtObj);
            } else if (updatedAtObj instanceof com.google.firebase.Timestamp) {
                task.setUpdatedAt(((com.google.firebase.Timestamp) updatedAtObj).toDate());
            }
        }

        // Set ID if available
        if (map.containsKey("id")) {
            task.setId((String) map.get("id"));
        }

        return task;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAssignedToUserId() {
        return assignedToUserId;
    }

    public void setAssignedToUserId(String assignedToUserId) {
        this.assignedToUserId = assignedToUserId;
    }

    public String getAssignedToName() {
        return assignedToName;
    }

    public void setAssignedToName(String assignedToName) {
        this.assignedToName = assignedToName;
    }

    public String getAssignerUserId() {
        return assignerUserId;
    }

    public void setAssignerUserId(String assignerUserId) {
        this.assignerUserId = assignerUserId;
    }

    public String getAssignerName() {
        return assignerName;
    }

    public void setAssignerName(String assignerName) {
        this.assignerName = assignerName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
    }

    // Utility methods
    public boolean isOverdue() {
        if (dueDate == null || "completed".equals(status)) {
            return false;
        }
        return dueDate.before(new Date());
    }

    public boolean isCompleted() {
        return "completed".equals(status);
    }

    public boolean isPending() {
        return "pending".equals(status);
    }

    public boolean isInProgress() {
        return "in_progress".equals(status);
    }

    public boolean isHighPriority() {
        return "high".equals(priority);
    }

    public boolean isMediumPriority() {
        return "medium".equals(priority);
    }

    public boolean isLowPriority() {
        return "low".equals(priority);
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", status='" + status + '\'' +
                ", priority='" + priority + '\'' +
                ", assignedTo='" + assignedToName + '\'' +
                ", dueDate=" + dueDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Task task = (Task) o;
        return id != null && id.equals(task.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }
}