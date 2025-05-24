package com.example.projectmanager.utils;

import android.content.Context;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.HashMap;
import java.util.Map;

public class FCMTokenManager {
    private static final String TAG = "FCMTokenManager";
    private static FCMTokenManager instance;
    private Context context;
    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    private FCMTokenManager(Context context) {
        this.context = context.getApplicationContext();
        this.firestore = FirebaseFirestore.getInstance();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public static FCMTokenManager getInstance(Context context) {
        if (instance == null) {
            instance = new FCMTokenManager(context);
        }
        return instance;
    }

    public interface TokenCallback {
        void onSuccess(String token);
        void onFailure(Exception exception);
    }

    public void getToken(TokenCallback callback) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                        callback.onFailure(task.getException());
                        return;
                    }

                    // Get new FCM registration token
                    String token = task.getResult();
                    Log.d(TAG, "FCM Registration Token: " + token);

                    // Save token to Firestore
                    saveTokenToFirestore(token);
                    callback.onSuccess(token);
                });
    }

    public void subscribeToTopic(String topic) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener(task -> {
                    String msg = "Subscribed to " + topic;
                    if (!task.isSuccessful()) {
                        msg = "Failed to subscribe to " + topic;
                    }
                    Log.d(TAG, msg);
                });
    }

    public void unsubscribeFromTopic(String topic) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener(task -> {
                    String msg = "Unsubscribed from " + topic;
                    if (!task.isSuccessful()) {
                        msg = "Failed to unsubscribe from " + topic;
                    }
                    Log.d(TAG, msg);
                });
    }

    private void saveTokenToFirestore(String token) {
        if (firebaseAuth.getCurrentUser() != null) {
            String userId = firebaseAuth.getCurrentUser().getUid();

            Map<String, Object> tokenData = new HashMap<>();
            tokenData.put("fcmToken", token);
            tokenData.put("lastUpdated", System.currentTimeMillis());

            firestore.collection("users")
                    .document(userId)
                    .update(tokenData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Token saved to Firestore"))
                    .addOnFailureListener(e -> Log.e(TAG, "Error saving token to Firestore", e));
        }
    }

    // Phương thức gửi thông báo đến user cụ thể
    public void sendNotificationToUser(String targetUserId, String title, String body, Map<String, String> data) {
        // Lấy token của user đích từ Firestore
        firestore.collection("users")
                .document(targetUserId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String fcmToken = document.getString("fcmToken");
                        if (fcmToken != null) {
                            // TODO: Call Cloud Function to send notification
                            // hoặc sử dụng Firebase Admin SDK (cần server)
                            Log.d(TAG, "Ready to send notification to token: " + fcmToken);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error getting user token", e));
    }
}