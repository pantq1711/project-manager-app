package com.example.projectmanager.repositories;

import android.util.Log;

import com.example.projectmanager.models.Budget;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Repository to handle Budget operations with Firebase
 */
public class BudgetRepository {
    private static final String TAG = "BudgetRepository";
    private static final String COLLECTION_BUDGETS = "budgets";
    private static final int PAGE_SIZE = 10; // Số items mỗi trang

    private FirebaseFirestore firestore;
    private FirebaseAuth firebaseAuth;

    public BudgetRepository() {
        firestore = FirebaseFirestore.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();
    }

    // Listener interfaces
    public interface OnBudgetsLoadedListener {
        void onBudgetsLoaded(List<Map<String, Object>> budgets);
        void onError(String error);
    }

    public interface OnPagedBudgetsLoadedListener {
        void onBudgetsLoaded(List<Map<String, Object>> budgets, boolean hasMore, DocumentSnapshot lastDocument);
        void onError(String error);
    }

    public interface OnBudgetOperationListener {
        void onSuccess(String message);
        void onError(String error);
    }

    /**
     * Get all budgets
     */
    public void getAllBudgets(OnBudgetsLoadedListener listener) {
        Log.d(TAG, "Loading budgets from Firestore");

        firestore.collection(COLLECTION_BUDGETS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> budgetsList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> budgetData = doc.getData();
                        budgetData.put("id", doc.getId()); // Add document ID
                        budgetsList.add(budgetData);
                    }

                    Log.d(TAG, "Successfully loaded " + budgetsList.size() + " budgets");
                    listener.onBudgetsLoaded(budgetsList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading budgets: " + e.getMessage(), e);
                    listener.onError("Lỗi khi tải danh sách ngân sách: " + e.getMessage());
                });
    }

    /**
     * Get budgets with pagination - first page
     */
    public void getBudgetsWithPagination(OnPagedBudgetsLoadedListener listener) {
        getBudgetsWithPagination(null, listener);
    }

    /**
     * Get budgets with pagination - next page
     */
    public void getBudgetsWithPagination(DocumentSnapshot lastDocument, OnPagedBudgetsLoadedListener listener) {
        Log.d(TAG, "Loading budgets with pagination, lastDocument: " + (lastDocument != null ? lastDocument.getId() : "null"));

        Query query = firestore.collection(COLLECTION_BUDGETS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        // If there is a lastDocument, start after it
        if (lastDocument != null) {
            query = query.startAfter(lastDocument);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> budgetsList = new ArrayList<>();
                    DocumentSnapshot lastDoc = null;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> budgetData = doc.getData();
                        budgetData.put("id", doc.getId());
                        budgetsList.add(budgetData);
                        lastDoc = doc;
                    }

                    // Check if there are more documents
                    boolean hasMore = queryDocumentSnapshots.size() == PAGE_SIZE;

                    Log.d(TAG, "Successfully loaded " + budgetsList.size() + " budgets with pagination, hasMore: " + hasMore);
                    listener.onBudgetsLoaded(budgetsList, hasMore, lastDoc);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading budgets with pagination: " + e.getMessage(), e);
                    listener.onError("Lỗi khi tải danh sách ngân sách: " + e.getMessage());
                });
    }

    /**
     * Get budgets for specific user with pagination
     */
    public void getBudgetsForUserWithPagination(String userId, DocumentSnapshot lastDocument, OnPagedBudgetsLoadedListener listener) {
        Log.d(TAG, "Loading budgets for user with pagination: " + userId);

        Query query = firestore.collection(COLLECTION_BUDGETS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        if (lastDocument != null) {
            query = query.startAfter(lastDocument);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> budgetsList = new ArrayList<>();
                    DocumentSnapshot lastDoc = null;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> budgetData = doc.getData();
                        budgetData.put("id", doc.getId());
                        budgetsList.add(budgetData);
                        lastDoc = doc;
                    }

                    boolean hasMore = queryDocumentSnapshots.size() == PAGE_SIZE;
                    Log.d(TAG, "Successfully loaded " + budgetsList.size() + " budgets for user with pagination");
                    listener.onBudgetsLoaded(budgetsList, hasMore, lastDoc);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user budgets with pagination: " + e.getMessage(), e);
                    listener.onError("Lỗi khi tải ngân sách của người dùng: " + e.getMessage());
                });
    }

    /**
     * Add new budget
     */
    public void addBudget(Budget budget, OnBudgetOperationListener listener) {
        Log.d(TAG, "Adding new budget: " + budget.getTitle());

        // Set user ID if available
        if (firebaseAuth.getCurrentUser() != null) {
            budget.setUserId(firebaseAuth.getCurrentUser().getUid());
        }

        // Ensure timestamps are set
        if (budget.getCreatedAt() == null) {
            budget.setCreatedAt(new Date());
        }
        budget.setUpdatedAt(new Date());

        firestore.collection(COLLECTION_BUDGETS)
                .add(budget.toMap())
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Budget added with ID: " + documentReference.getId());
                    listener.onSuccess("Thêm ngân sách thành công");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding budget: " + e.getMessage(), e);
                    listener.onError("Lỗi khi thêm ngân sách: " + e.getMessage());
                });
    }

    /**
     * Update existing budget
     */
    public void updateBudget(Budget budget, OnBudgetOperationListener listener) {
        Log.d(TAG, "Updating budget with ID: " + budget.getId());

        if (budget.getId() == null || budget.getId().isEmpty()) {
            listener.onError("ID ngân sách không hợp lệ");
            return;
        }

        budget.setUpdatedAt(new Date());

        firestore.collection(COLLECTION_BUDGETS)
                .document(budget.getId())
                .set(budget.toMap())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Budget updated successfully");
                    listener.onSuccess("Cập nhật ngân sách thành công");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating budget: " + e.getMessage(), e);
                    listener.onError("Lỗi khi cập nhật ngân sách: " + e.getMessage());
                });
    }

    /**
     * Approve a budget
     */
    public void approveBudget(String budgetId, OnBudgetOperationListener listener) {
        Log.d(TAG, "Approving budget with ID: " + budgetId);

        if (budgetId == null || budgetId.isEmpty()) {
            listener.onError("ID ngân sách không hợp lệ");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("approved", true);
        updates.put("updatedAt", new Date());

        firestore.collection(COLLECTION_BUDGETS)
                .document(budgetId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Budget approved successfully");
                    listener.onSuccess("Duyệt ngân sách thành công");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error approving budget: " + e.getMessage(), e);
                    listener.onError("Lỗi khi duyệt ngân sách: " + e.getMessage());
                });
    }

    /**
     * Revoke budget approval
     */
    public void revokeBudget(String budgetId, OnBudgetOperationListener listener) {
        Log.d(TAG, "Revoking budget approval with ID: " + budgetId);

        if (budgetId == null || budgetId.isEmpty()) {
            listener.onError("ID ngân sách không hợp lệ");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("approved", false);
        updates.put("updatedAt", new Date());

        firestore.collection(COLLECTION_BUDGETS)
                .document(budgetId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Budget approval revoked successfully");
                    listener.onSuccess("Hủy duyệt ngân sách thành công");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error revoking budget approval: " + e.getMessage(), e);
                    listener.onError("Lỗi khi hủy duyệt ngân sách: " + e.getMessage());
                });
    }

    /**
     * Delete a budget
     */
    public void deleteBudget(String budgetId, OnBudgetOperationListener listener) {
        Log.d(TAG, "Deleting budget with ID: " + budgetId);

        if (budgetId == null || budgetId.isEmpty()) {
            listener.onError("ID ngân sách không hợp lệ");
            return;
        }

        firestore.collection(COLLECTION_BUDGETS)
                .document(budgetId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Budget deleted successfully");
                    listener.onSuccess("Xóa ngân sách thành công");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting budget: " + e.getMessage(), e);
                    listener.onError("Lỗi khi xóa ngân sách: " + e.getMessage());
                });
    }

    /**
     * Get budgets for specific user
     */
    public void getBudgetsForUser(String userId, OnBudgetsLoadedListener listener) {
        Log.d(TAG, "Loading budgets for user: " + userId);

        firestore.collection(COLLECTION_BUDGETS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> budgetsList = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> budgetData = doc.getData();
                        budgetData.put("id", doc.getId());
                        budgetsList.add(budgetData);
                    }

                    Log.d(TAG, "Successfully loaded " + budgetsList.size() + " budgets for user");
                    listener.onBudgetsLoaded(budgetsList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user budgets: " + e.getMessage(), e);
                    listener.onError("Lỗi khi tải ngân sách của người dùng: " + e.getMessage());
                });
    }
}