package com.example.projectmanager.utils;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Lớp quản lý Firebase Firestore
 * Xử lý tất cả các thao tác với cơ sở dữ liệu
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private FirebaseFirestore db;

    // Tên các collection trong Firestore
    private static final String TASKS_COLLECTION = "tasks";
    private static final String MESSAGES_COLLECTION = "messages";
    private static final String BUDGETS_COLLECTION = "budgets";
    private static final String MEMBERS_COLLECTION = "members";

    public FirebaseManager() {
        // Khởi tạo Firestore
        db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Firebase khởi tạo thành công");
    }

    // ===== QUẢN LÝ NHIỆM VỤ =====

    /**
     * Thêm nhiệm vụ mới vào Firebase
     */
    public void addTask(Map<String, Object> task, OnCompleteListener listener) {
        db.collection(TASKS_COLLECTION)
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Thêm nhiệm vụ thành công: " + documentReference.getId());
                    listener.onSuccess(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi thêm nhiệm vụ", e);
                    listener.onFailure(e.getMessage());
                });
    }

    /**
     * Lấy danh sách nhiệm vụ theo thời gian thực
     */
    public void getTasks(OnDataLoadListener listener) {
        db.collection(TASKS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Lỗi khi lấy dữ liệu nhiệm vụ", error);
                            listener.onError(error.getMessage());
                            return;
                        }

                        List<Map<String, Object>> tasks = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                Map<String, Object> task = doc.getData();
                                task.put("id", doc.getId());
                                tasks.add(task);
                            }
                        }
                        Log.d(TAG, "Lấy được " + tasks.size() + " nhiệm vụ");
                        listener.onDataLoaded(tasks);
                    }
                });
    }

    /**
     * Cập nhật trạng thái nhiệm vụ
     */
    public void updateTaskStatus(String taskId, String status, OnCompleteListener listener) {
        db.collection(TASKS_COLLECTION).document(taskId)
                .update("status", status)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cập nhật trạng thái nhiệm vụ thành công");
                    listener.onSuccess(taskId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi cập nhật trạng thái", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // ===== QUẢN LÝ TIN NHẮN =====

    /**
     * Gửi tin nhắn mới với timestamp chính xác
     */
    public void sendMessage(Map<String, Object> message, OnCompleteListener listener) {
        // Đảm bảo timestamp được set đúng cách
        message.put("timestamp", FieldValue.serverTimestamp());

        Log.d(TAG, "Sending message with timestamp: " + message.get("timestamp"));

        db.collection(MESSAGES_COLLECTION)
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Gửi tin nhắn thành công: " + documentReference.getId());
                    listener.onSuccess(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi gửi tin nhắn", e);
                    listener.onFailure(e.getMessage());
                });
    }

    /**
     * Lấy tin nhắn theo thời gian thực với sắp xếp chính xác
     */
    public void getMessages(OnDataLoadListener listener) {
        db.collection(MESSAGES_COLLECTION)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Lỗi khi lấy dữ liệu tin nhắn", error);
                            listener.onError(error.getMessage());
                            return;
                        }

                        List<Map<String, Object>> messages = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                Map<String, Object> message = doc.getData();
                                message.put("id", doc.getId());

                                // Debug log timestamp
                                Object timestamp = message.get("timestamp");
                                Log.d(TAG, "Message timestamp: " + timestamp +
                                        " (type: " + (timestamp != null ? timestamp.getClass().getSimpleName() : "null") + ")");

                                messages.add(message);
                            }
                        }
                        Log.d(TAG, "Lấy được " + messages.size() + " tin nhắn");
                        listener.onDataLoaded(messages);
                    }
                });
    }

    // ===== QUẢN LÝ NGÂN SÁCH =====

    /**
     * Thêm khoản chi ngân sách
     */
    public void addBudget(Map<String, Object> budget, OnCompleteListener listener) {
        db.collection(BUDGETS_COLLECTION)
                .add(budget)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Thêm ngân sách thành công: " + documentReference.getId());
                    listener.onSuccess(documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi thêm ngân sách", e);
                    listener.onFailure(e.getMessage());
                });
    }

    /**
     * Lấy danh sách ngân sách
     */
    public void getBudgets(OnDataLoadListener listener) {
        db.collection(BUDGETS_COLLECTION)
                .orderBy("date", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(QuerySnapshot value, FirebaseFirestoreException error) {
                        if (error != null) {
                            Log.e(TAG, "Lỗi khi lấy dữ liệu ngân sách", error);
                            listener.onError(error.getMessage());
                            return;
                        }

                        List<Map<String, Object>> budgets = new ArrayList<>();
                        if (value != null) {
                            for (QueryDocumentSnapshot doc : value) {
                                Map<String, Object> budget = doc.getData();
                                budget.put("id", doc.getId());
                                budgets.add(budget);
                            }
                        }
                        Log.d(TAG, "Lấy được " + budgets.size() + " khoản ngân sách");
                        listener.onDataLoaded(budgets);
                    }
                });
    }

    /**
     * Phê duyệt ngân sách
     */
    public void approveBudget(String budgetId, OnCompleteListener listener) {
        db.collection(BUDGETS_COLLECTION).document(budgetId)
                .update("isApproved", true)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Phê duyệt ngân sách thành công");
                    listener.onSuccess(budgetId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi phê duyệt ngân sách", e);
                    listener.onFailure(e.getMessage());
                });
    }

    // ===== INTERFACE CALLBACK =====

    public interface OnCompleteListener {
        void onSuccess(String result);
        void onFailure(String error);
    }

    public interface OnDataLoadListener {
        void onDataLoaded(List<Map<String, Object>> data);
        void onError(String error);
    }
}