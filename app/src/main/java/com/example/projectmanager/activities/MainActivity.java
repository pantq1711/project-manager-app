package com.example.projectmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.projectmanager.services.UserSearchService;
import com.example.projectmanager.utils.FCMTokenManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.appcompat.app.AlertDialog;

import com.example.projectmanager.R;
import com.example.projectmanager.utils.FirebaseManager;
import com.example.projectmanager.utils.UserManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Hoạt động chính của ứng dụng - Chỉ sử dụng Activity
 * Hiển thị menu chính với các chức năng:
 * - Quản lý nhiệm vụ
 * - Chat nhóm
 * - Quản lý ngân sách
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    // Components giao diện
    private CardView cvTasks, cvChat, cvBudget;
    private TextView tvWelcome;
    private Button btnRefresh, btnLogout;

    // Firebase manager
    private FirebaseManager firebaseManager;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity được khởi tạo");

        // Khởi tạo Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseManager = new FirebaseManager();

        // Kiểm tra đăng nhập
        checkLoginStatus();

        // Khởi tạo giao diện
        initViews();
        setupClickListeners();

        // Hiển thị thông báo chào mừng
        showWelcomeMessage();
        // Hiển thị thông báo chào mừng
        showWelcomeMessage();

        // Khởi tạo UserManager
        initUserManager();

        // Khởi tạo FCM
        initFCM();
    }
    private void initFCM() {
        FCMTokenManager tokenManager = FCMTokenManager.getInstance(this);

        // Get FCM token
        tokenManager.getToken(new FCMTokenManager.TokenCallback() {
            @Override
            public void onSuccess(String token) {
                Log.d(TAG, "FCM Token: " + token);
                // Subscribe to general notifications
                tokenManager.subscribeToTopic("all");
            }

            @Override
            public void onFailure(Exception exception) {
                Log.e(TAG, "Failed to get FCM token", exception);
            }
        });
    }
    private void initUserManager() {
        UserManager userManager = UserManager.getInstance(this);
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser != null) {
            // Lưu display name nếu có
            if (currentUser.getDisplayName() != null) {
                userManager.saveUserDisplayName(currentUser.getDisplayName());
            }

            // Save current user to Firestore users collection
            UserSearchService.saveCurrentUserToFirestore();

            Log.d(TAG, "UserManager initialized for user: " + userManager.getCurrentUserDisplayName());
        }
    }
    /**
     * Kiểm tra trạng thái đăng nhập
     */
    private void checkLoginStatus() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            // Người dùng chưa đăng nhập, chuyển về màn hình login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /**
     * Khởi tạo các thành phần giao diện
     */
    private void initViews() {
        cvTasks = findViewById(R.id.cv_tasks);
        cvChat = findViewById(R.id.cv_chat);
        cvBudget = findViewById(R.id.cv_budget);
        tvWelcome = findViewById(R.id.tv_welcome);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnLogout = findViewById(R.id.btn_logout);

        Log.d(TAG, "Khởi tạo giao diện thành công");
    }

    /**
     * Thiết lập sự kiện click cho các thành phần
     */
    private void setupClickListeners() {
        // Click vào quản lý nhiệm vụ
        cvTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Người dùng click vào Quản lý nhiệm vụ");
                Intent intent = new Intent(MainActivity.this, TaskManagerActivity.class);
                startActivity(intent);
            }
        });

        // Click vào chat nhóm
        cvChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Người dùng click vào Chat nhóm");
                Intent intent = new Intent(MainActivity.this, GroupChatActivity.class);
                startActivity(intent);
            }
        });

        // Click vào quản lý ngân sách
        cvBudget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Người dùng click vào Quản lý ngân sách");
                Intent intent = new Intent(MainActivity.this, BudgetActivity.class);
                startActivity(intent);
            }
        });

        // Click nút refresh
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Người dùng click Refresh");
                refreshData();
            }
        });

        // Click nút đăng xuất
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Người dùng click Đăng xuất");
                showLogoutDialog();
            }
        });
    }

    /**
     * Hiển thị thông báo chào mừng
     */
    private void showWelcomeMessage() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            String userName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();
            tvWelcome.setText("Chào mừng, " + userName + "!");
        } else {
            tvWelcome.setText("Chào mừng đến với Ứng dụng Quản lý Dự án!");
        }

        Toast.makeText(this, "Ứng dụng đã sẵn sàng!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Hiển thị thông báo chào mừng");
    }

    /**
     * Hiển thị dialog xác nhận đăng xuất
     */
    private void showLogoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận đăng xuất");
        builder.setMessage("Bạn có chắc chắn muốn đăng xuất?");

        builder.setPositiveButton("Đăng xuất", (dialog, which) -> {
            performLogout();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Thực hiện đăng xuất
     */
    private void performLogout() {
        // Hiển thị loading
        Toast.makeText(this, "Đang đăng xuất...", Toast.LENGTH_SHORT).show();

        // Đăng xuất khỏi Firebase
        firebaseAuth.signOut();

        // Chuyển về màn hình login
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Log.d(TAG, "Đăng xuất thành công");
    }

    /**
     * Làm mới dữ liệu
     */
    private void refreshData() {
        // Hiển thị Toast thông báo
        Toast.makeText(this, "Đang làm mới dữ liệu...", Toast.LENGTH_SHORT).show();

        // Log để theo dõi
        Log.d(TAG, "Bắt đầu làm mới dữ liệu");

        // Có thể thêm logic làm mới dữ liệu ở đây
        // Ví dụ: reload thông tin user, kiểm tra notifications, etc.

        // Simulate refresh completion
        // Trong thực tế, bạn có thể làm mới dữ liệu từ Firebase
        btnRefresh.postDelayed(() -> {
            Toast.makeText(MainActivity.this, "Làm mới hoàn tất!", Toast.LENGTH_SHORT).show();
        }, 1000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "MainActivity resume");

        // Kiểm tra lại trạng thái đăng nhập khi quay lại
        checkLoginStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "MainActivity pause");
    }

    @Override
    public void onBackPressed() {
        // Hiển thị dialog xác nhận thoát app
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Thoát ứng dụng");
        builder.setMessage("Bạn có chắc chắn muốn thoát?");

        builder.setPositiveButton("Thoát", (dialog, which) -> {
            super.onBackPressed();
            finish();
        });

        builder.setNegativeButton("Ở lại", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}