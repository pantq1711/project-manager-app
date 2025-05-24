package com.example.projectmanager.services;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSearchService {
    private static final String TAG = "UserSearchService";
    private static final String USERS_COLLECTION = "users";

    public interface OnUserFoundListener {
        void onUserFound(String userId, String userName, String userEmail);
        void onUserNotFound();
        void onError(String error);
    }

    public interface OnUsersLoadedListener {
        void onUsersLoaded(List<UserInfo> users);
        void onError(String error);
    }

    // Class thông tin user
    public static class UserInfo {
        public String id;
        public String displayName;
        public String email;

        public UserInfo(String id, String displayName, String email) {
            this.id = id;
            this.displayName = displayName != null && !displayName.isEmpty() ? displayName : email;
            this.email = email;
        }

        @Override
        public String toString() {
            return displayName + " (" + email + ")";
        }

        public String getDisplayText() {
            return displayName;
        }
    }

    /**
     * Lấy tất cả users cho autocomplete với cải thiện debug
     */
    public static void getAllUsers(OnUsersLoadedListener listener) {
        Log.d(TAG, "Starting to load all users...");

        FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UserInfo> users = new ArrayList<>();

                    Log.d(TAG, "Query returned " + queryDocumentSnapshots.size() + " documents");

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            String email = doc.getString("email");
                            String displayName = doc.getString("displayName");
                            String name = doc.getString("name");

                            // Debug log cho mỗi user
                            Log.d(TAG, "Processing user: ID=" + doc.getId() +
                                    ", email=" + email +
                                    ", displayName=" + displayName +
                                    ", name=" + name);

                            // Đảm bảo có email
                            if (email != null && !email.isEmpty()) {
                                // Ưu tiên displayName, nếu không có thì dùng name, cuối cùng là email
                                String finalDisplayName = displayName;
                                if (finalDisplayName == null || finalDisplayName.isEmpty()) {
                                    finalDisplayName = name;
                                }
                                if (finalDisplayName == null || finalDisplayName.isEmpty()) {
                                    finalDisplayName = email;
                                }

                                UserInfo user = new UserInfo(doc.getId(), finalDisplayName, email);
                                users.add(user);
                                Log.d(TAG, "Added user: " + user.toString());
                            } else {
                                Log.w(TAG, "Skipping user with null/empty email: " + doc.getId());
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error processing user document: " + doc.getId(), e);
                        }
                    }

                    // Thêm current user nếu chưa có
                    addCurrentUserIfMissing(users);

                    Log.d(TAG, "Total valid users loaded: " + users.size());
                    listener.onUsersLoaded(users);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading users", e);
                    // Fallback: thêm current user
                    List<UserInfo> fallbackUsers = new ArrayList<>();
                    addCurrentUserIfMissing(fallbackUsers);
                    listener.onUsersLoaded(fallbackUsers);
                });
    }

    /**
     * Thêm current user vào list nếu chưa có
     */
    private static void addCurrentUserIfMissing(List<UserInfo> users) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String currentUserId = currentUser.getUid();
        String currentEmail = currentUser.getEmail();
        String currentDisplayName = currentUser.getDisplayName();

        // Kiểm tra xem current user đã có trong list chưa
        boolean found = false;
        for (UserInfo user : users) {
            if (user.id.equals(currentUserId) || user.email.equals(currentEmail)) {
                found = true;
                break;
            }
        }

        if (!found) {
            Log.d(TAG, "Adding current user to list: " + currentEmail);
            UserInfo currentUserInfo = new UserInfo(currentUserId, currentDisplayName, currentEmail);
            users.add(0, currentUserInfo); // Thêm ở đầu list
        }
    }

    /**
     * Save current user to Firestore users collection
     */
    public static void saveCurrentUserToFirestore() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        String email = currentUser.getEmail();
        String displayName = currentUser.getDisplayName();

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("displayName", displayName != null ? displayName : email);
        userData.put("name", displayName);
        userData.put("createdAt", new java.util.Date());
        userData.put("lastLoginAt", new java.util.Date());
        userData.put("isActive", true);

        Log.d(TAG, "Saving user to Firestore: " + email);

        FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION)
                .document(userId)
                .set(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User saved successfully: " + email);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving user: " + email, e);
                });
    }

    // Tìm user bằng tên
    public static void findUserByName(String searchName, OnUserFoundListener listener) {
        if (searchName == null || searchName.trim().isEmpty()) {
            listener.onUserNotFound();
            return;
        }

        String searchTerm = searchName.trim();
        Log.d(TAG, "Searching for user: " + searchTerm);

        // Tìm theo displayName trước
        FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION)
                .whereEqualTo("displayName", searchTerm)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String userId = doc.getId();
                        String userName = doc.getString("displayName");
                        String userEmail = doc.getString("email");
                        listener.onUserFound(userId, userName, userEmail);
                    } else {
                        // Thử tìm bằng name
                        searchUserByField("name", searchTerm, listener);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching user by displayName", e);
                    listener.onError("Lỗi khi tìm kiếm người dùng: " + e.getMessage());
                });
    }

    private static void searchUserByField(String field, String value, OnUserFoundListener listener) {
        FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION)
                .whereEqualTo(field, value)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String userId = doc.getId();
                        String userName = doc.getString("displayName");
                        String userEmail = doc.getString("email");
                        listener.onUserFound(userId, userName, userEmail);
                    } else if ("name".equals(field)) {
                        // Thử tìm bằng email
                        searchUserByField("email", value, listener);
                    } else {
                        listener.onUserNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching user by " + field, e);
                    if ("email".equals(field)) {
                        listener.onUserNotFound();
                    } else {
                        listener.onError("Lỗi khi tìm kiếm: " + e.getMessage());
                    }
                });
    }

    // Kiểm tra user có tồn tại không
    public static void checkUserExists(String userId, OnUserFoundListener listener) {
        if (userId == null || userId.trim().isEmpty()) {
            listener.onUserNotFound();
            return;
        }

        FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION)
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String userName = documentSnapshot.getString("displayName");
                        String userEmail = documentSnapshot.getString("email");
                        listener.onUserFound(userId, userName, userEmail);
                    } else {
                        listener.onUserNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking user existence", e);
                    listener.onError("Lỗi khi kiểm tra người dùng: " + e.getMessage());
                });
    }
}