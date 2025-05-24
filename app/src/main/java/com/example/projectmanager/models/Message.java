package com.example.projectmanager.models;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class for chat messages with attachment support
 */
public class Message {
    private String content;
    private String senderName;
    private String senderId;
    private String senderEmail;
    private Date timestamp;

    // New attachment fields
    private String attachmentUrl;
    private String attachmentName;
    private String attachmentType; // "image", "document", "video", etc.
    private long attachmentSize;

    // Default constructor for Firebase
    public Message() {
        // Required empty constructor for Firebase
    }

    // Constructor for text-only messages
    public Message(String content, String senderName, String senderId, String senderEmail) {
        this.content = content;
        this.senderName = senderName;
        this.senderId = senderId;
        this.senderEmail = senderEmail;
        this.timestamp = new Date();
    }

    // Constructor for messages with attachments
    public Message(String content, String senderName, String senderId, String senderEmail,
                   String attachmentUrl, String attachmentName, String attachmentType, long attachmentSize) {
        this.content = content;
        this.senderName = senderName;
        this.senderId = senderId;
        this.senderEmail = senderEmail;
        this.timestamp = new Date();
        this.attachmentUrl = attachmentUrl;
        this.attachmentName = attachmentName;
        this.attachmentType = attachmentType;
        this.attachmentSize = attachmentSize;
    }

    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("content", content);
        map.put("senderName", senderName);
        map.put("senderId", senderId);
        map.put("senderEmail", senderEmail);
        map.put("timestamp", timestamp);

        // Add attachment fields if present
        if (attachmentUrl != null && !attachmentUrl.isEmpty()) {
            map.put("attachmentUrl", attachmentUrl);
            map.put("attachmentName", attachmentName);
            map.put("attachmentType", attachmentType);
            map.put("attachmentSize", attachmentSize);
        }

        return map;
    }

    // Getters and setters
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
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

    public String getAttachmentType() {
        return attachmentType;
    }

    public void setAttachmentType(String attachmentType) {
        this.attachmentType = attachmentType;
    }

    public long getAttachmentSize() {
        return attachmentSize;
    }

    public void setAttachmentSize(long attachmentSize) {
        this.attachmentSize = attachmentSize;
    }

    public boolean hasAttachment() {
        return attachmentUrl != null && !attachmentUrl.isEmpty();
    }

    // Helper method to check if attachment is an image
    public boolean isImageAttachment() {
        if (attachmentType == null) return false;
        return attachmentType.startsWith("image/");
    }
}