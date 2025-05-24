package com.example.projectmanager.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Manager để quản lý thông tin user hiện tại
 */
public class UserManager {
    private static final String TAG = "UserManager";
    private static final String PREF_NAME = "USER_PREF";
    private static final String KEY_USER_NAME = "USER_NAME";
    private static final String KEY_USER_EMAIL = "USER_EMAIL";
    private static final String KEY_USER_ID = "USER_ID";
    private static final String KEY_USER_ROLE = "USER_ROLE";

    private static UserManager instance;
    private SharedPreferences preferences;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    public enum Role {
        MEMBER("member"),
        MANAGER("manager"),
        ADMIN("admin");

        private final String value;

        Role(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Role fromString(String value) {
            for (Role role : Role.values()) {
                if (role.getValue().equals(value)) {
                    return role;
                }
            }
            return MEMBER; // Default role
        }
    }

    private UserManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public static synchronized UserManager getInstance(Context context) {
        if (instance == null) {
            instance = new UserManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Lấy tên hiển thị của user hiện tại
     */
    public String getCurrentUserDisplayName() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            // Ưu tiên displayName từ Firebase
            if (currentUser.getDisplayName() != null && !currentUser.getDisplayName().isEmpty()) {
                return currentUser.getDisplayName();
            }

            // Sau đó là email (lấy phần trước @)
            if (currentUser.getEmail() != null) {
                String email = currentUser.getEmail();
                String username = email.substring(0, email.indexOf('@'));
                return username;
            }
        }

        // Cuối cùng kiểm tra SharedPreferences
        String savedName = preferences.getString(KEY_USER_NAME, null);
        if (savedName != null && !savedName.isEmpty()) {
            return savedName;
        }

        // Default name
        return "Tôi";
    }

    /**
     * Lưu tên user vào SharedPreferences
     */
    public void saveUserDisplayName(String displayName) {
        preferences.edit().putString(KEY_USER_NAME, displayName).apply();
    }

    /**
     * Lấy email của user hiện tại
     */
    public String getCurrentUserEmail() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null && currentUser.getEmail() != null) {
            return currentUser.getEmail();
        }
        return preferences.getString(KEY_USER_EMAIL, "");
    }

    /**
     * Lấy ID của user hiện tại
     */
    public String getCurrentUserId() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            return currentUser.getUid();
        }
        return preferences.getString(KEY_USER_ID, "");
    }

    /**
     * Kiểm tra xem tin nhắn có phải của user hiện tại không
     */
    public boolean isCurrentUser(String senderName, String senderEmail) {
        String currentDisplayName = getCurrentUserDisplayName();
        String currentEmail = getCurrentUserEmail();

        // So sánh theo tên hiển thị
        if (senderName != null && currentDisplayName != null) {
            if (senderName.equals(currentDisplayName)) {
                return true;
            }
        }

        // So sánh theo email
        if (senderEmail != null && currentEmail != null) {
            if (senderEmail.equals(currentEmail)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Lấy role của user hiện tại từ SharedPreferences
     */
    public Role getCurrentUserRole() {
        String roleString = preferences.getString(KEY_USER_ROLE, Role.MEMBER.getValue());
        return Role.fromString(roleString);
    }

    /**
     * Lưu role vào SharedPreferences
     */
    public void saveUserRole(Role role) {
        preferences.edit().putString(KEY_USER_ROLE, role.getValue()).apply();
    }

    /**
     * Kiểm tra xem user có quyền quản lý users không
     * Chỉ ADMIN mới có quyền này
     */
    public boolean canManageUsers() {
        return getCurrentUserRole() == Role.ADMIN;
    }

    /**
     * Kiểm tra xem user có quyền quản lý budget không
     * MANAGER và ADMIN có quyền này
     */
    public boolean canManageBudget() {
        Role role = getCurrentUserRole();
        return role == Role.MANAGER || role == Role.ADMIN;
    }

    /**
     * Kiểm tra xem user có quyền phân công task không
     * MANAGER và ADMIN có quyền này
     */
    public boolean canAssignTasks() {
        Role role = getCurrentUserRole();
        return role == Role.MANAGER || role == Role.ADMIN;
    }

    /**
     * Kiểm tra xem user có phải admin không
     */
    public boolean isAdmin() {
        return getCurrentUserRole() == Role.ADMIN;
    }

    /**
     * Kiểm tra xem user có phải manager hoặc admin không
     */
    public boolean isManagerOrAdmin() {
        Role role = getCurrentUserRole();
        return role == Role.MANAGER || role == Role.ADMIN;
    }

    /**
     * Load role từ Firestore và cache vào SharedPreferences
     */
    public void loadUserRole(OnRoleLoadedListener listener) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null || currentUserId.isEmpty()) {
            if (listener != null) {
                listener.onError("User ID is null");
            }
            return;
        }

        firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String roleString = documentSnapshot.getString("role");
                        Role role = Role.fromString(roleString);
                        saveUserRole(role);

                        if (listener != null) {
                            listener.onRoleLoaded(role);
                        }

                        Log.d(TAG, "User role loaded: " + role.getValue());
                    } else {
                        // User not found in Firestore, set default role
                        saveUserRole(Role.MEMBER);

                        if (listener != null) {
                            listener.onRoleLoaded(Role.MEMBER);
                        }

                        Log.d(TAG, "User not found in Firestore, set default role: MEMBER");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user role", e);
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                });
    }

    /**
     * Interface để handle role loading
     */
    public interface OnRoleLoadedListener {
        void onRoleLoaded(Role role);
        void onError(String error);
    }

    /**
     * Clear user data (khi logout)
     */
    public void clearUserData() {
        preferences.edit().clear().apply();
    }
}