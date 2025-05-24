package com.example.projectmanager.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectmanager.R;
import com.example.projectmanager.adapters.UserManagementAdapter;
import com.example.projectmanager.models.User;
import com.example.projectmanager.utils.UserManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity cho Admin quản lý users và phân quyền
 */
public class UserManagementActivity extends AppCompatActivity {
    private static final String TAG = "UserManagementActivity";

    private RecyclerView rvUsers;
    private UserManagementAdapter userAdapter;
    private List<User> userList;
    private FirebaseFirestore firestore;
    private UserManager userManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // Initialize
        firestore = FirebaseFirestore.getInstance();
        userManager = UserManager.getInstance(this);

        // Setup toolbar
        try {
            Toolbar toolbar = findViewById(R.id.toolbar);
            if (toolbar != null) {
                setSupportActionBar(toolbar);
                if (getSupportActionBar() != null) {
                    getSupportActionBar().setTitle("Quản lý Người dùng");
                    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                }
            } else {
                Log.e(TAG, "Toolbar not found in layout");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up toolbar", e);
            setTitle("Quản lý Người dùng");
        }

        // Check admin permission
        if (!userManager.canManageUsers()) {
            Toast.makeText(this, "Bạn không có quyền truy cập trang này", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadUsers();
    }

    private void initViews() {
        rvUsers = findViewById(R.id.rv_users);

        userList = new ArrayList<>();
        userAdapter = new UserManagementAdapter(userList, new UserManagementAdapter.OnUserActionListener() {
            @Override
            public void onChangeRole(User user, int position) {
                showRoleChangeDialog(user, position);
            }

            @Override
            public void onToggleActive(User user, int position) {
                toggleUserActive(user, position);
            }
        });

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        rvUsers.setAdapter(userAdapter);
    }

    private void loadUsers() {
        firestore.collection("users")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        User user = User.fromMap(document.getData());
                        userList.add(user);
                    }
                    userAdapter.notifyDataSetChanged();

                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(userList.size() + " người dùng");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading users", e);
                    Toast.makeText(this, "Lỗi tải danh sách người dùng", Toast.LENGTH_SHORT).show();
                });
    }

    private void showRoleChangeDialog(User user, int position) {
        String[] roles = {"Member", "Manager", "Admin"};
        User.Role[] roleValues = {User.Role.MEMBER, User.Role.MANAGER, User.Role.ADMIN};

        int currentRoleIndex = 0;
        switch (user.getRole()) {
            case MANAGER: currentRoleIndex = 1; break;
            case ADMIN: currentRoleIndex = 2; break;
            default: currentRoleIndex = 0; break;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn vai trò cho " + user.getDisplayName());

        builder.setSingleChoiceItems(roles, currentRoleIndex, (dialog, which) -> {
            User.Role newRole = roleValues[which];

            // Không cho phép admin tự hạ cấp mình
            if (user.getId().equals(userManager.getCurrentUserId()) &&
                    newRole != User.Role.ADMIN) {
                Toast.makeText(this, "Bạn không thể hạ cấp chính mình", Toast.LENGTH_SHORT).show();
                return;
            }

            // Confirm dialog
            AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(this);
            confirmBuilder.setTitle("Xác nhận thay đổi");
            confirmBuilder.setMessage("Bạn có chắc chắn muốn thay đổi vai trò của " +
                    user.getDisplayName() + " thành " + roles[which] + "?");

            confirmBuilder.setPositiveButton("Xác nhận", (confirmDialog, i) -> {
                updateUserRole(user, newRole, position);
                confirmDialog.dismiss();
            });

            confirmBuilder.setNegativeButton("Hủy", null);
            confirmBuilder.show();

            dialog.dismiss();
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void updateUserRole(User user, User.Role newRole, int position) {
        firestore.collection("users").document(user.getId())
                .update("role", newRole.getValue())
                .addOnSuccessListener(aVoid -> {
                    user.setRole(newRole);
                    userAdapter.notifyItemChanged(position);
                    Toast.makeText(this, "Đã cập nhật vai trò thành công", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating role", e);
                    Toast.makeText(this, "Lỗi cập nhật vai trò", Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleUserActive(User user, int position) {
        // Không cho phép admin tắt tài khoản của chính mình
        if (user.getId().equals(userManager.getCurrentUserId())) {
            Toast.makeText(this, "Bạn không thể tắt tài khoản của chính mình", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean newActiveStatus = !user.isActive();
        String action = newActiveStatus ? "kích hoạt" : "vô hiệu hóa";

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận " + action);
        builder.setMessage("Bạn có chắc chắn muốn " + action + " tài khoản của " +
                user.getDisplayName() + "?");

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            firestore.collection("users").document(user.getId())
                    .update("isActive", newActiveStatus)
                    .addOnSuccessListener(aVoid -> {
                        user.setActive(newActiveStatus);
                        userAdapter.notifyItemChanged(position);
                        Toast.makeText(this, "Đã " + action + " tài khoản thành công", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error updating user status", e);
                        Toast.makeText(this, "Lỗi " + action + " tài khoản", Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}