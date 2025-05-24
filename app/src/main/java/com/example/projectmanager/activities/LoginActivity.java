package com.example.projectmanager.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.projectmanager.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";

    // UI Components
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvRegister;
    private ProgressBar progressBar;

    // Firebase
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "LoginActivity started");

        // Khởi tạo Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        // Kiểm tra user đã đăng nhập chưa
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already logged in, redirecting to MainActivity");
            navigateToMain();
            return;
        }

        // Khởi tạo views
        if (!initViews()) {
            Log.e(TAG, "Failed to initialize views");
            return;
        }

        setupClickListeners();
    }

    /**
     * Khởi tạo các view components
     */
    private boolean initViews() {
        try {
            etEmail = findViewById(R.id.et_email);
            etPassword = findViewById(R.id.et_password);
            btnLogin = findViewById(R.id.btn_login);
            tvRegister = findViewById(R.id.tv_register);
            progressBar = findViewById(R.id.progress_bar);

            // Kiểm tra null cho các views quan trọng
            if (etEmail == null || etPassword == null || btnLogin == null) {
                Log.e(TAG, "One or more critical views are null");
                return false;
            }

            // ProgressBar có thể null, sẽ check khi sử dụng
            if (progressBar == null) {
                Log.w(TAG, "ProgressBar is null, creating a dummy one");
                progressBar = new ProgressBar(this);
            }

            Log.d(TAG, "All views initialized successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error in initViews: " + e.getMessage(), e);
            return false;
        }
    }

    /**
     * Thiết lập click listeners
     */
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            Log.d(TAG, "Người dùng click Đăng nhập");
            loginUser();
        });

        if (tvRegister != null) {
            tvRegister.setOnClickListener(v -> {
                Log.d(TAG, "Người dùng click Đăng ký");
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                startActivity(intent);
            });
        }
    }

    /**
     * Thực hiện đăng nhập
     */
    private void loginUser() {
        try {
            // Show progress bar if available
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
            }
            btnLogin.setEnabled(false);

            // Lấy email và password
            String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
            String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

            // Validate input
            if (TextUtils.isEmpty(email)) {
                hideProgress();
                showError("Vui lòng nhập email");
                return;
            }

            if (TextUtils.isEmpty(password)) {
                hideProgress();
                showError("Vui lòng nhập mật khẩu");
                return;
            }

            if (password.length() < 6) {
                hideProgress();
                showError("Mật khẩu phải có ít nhất 6 ký tự");
                return;
            }

            Log.d(TAG, "Attempting to login with email: " + email);

            // Đăng nhập với Firebase Auth
            firebaseAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        hideProgress();

                        if (task.isSuccessful()) {
                            Log.d(TAG, "Login successful");
                            FirebaseUser user = firebaseAuth.getCurrentUser();
                            if (user != null) {
                                Log.d(TAG, "User logged in: " + user.getEmail());
                                showSuccess("Đăng nhập thành công!");
                                navigateToMain();
                            }
                        } else {
                            Log.e(TAG, "Login failed", task.getException());
                            String errorMessage = "Đăng nhập thất bại";
                            if (task.getException() != null) {
                                errorMessage = task.getException().getMessage();
                            }
                            showError(errorMessage);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error in loginUser: " + e.getMessage(), e);
            hideProgress();
            showError("Có lỗi xảy ra: " + e.getMessage());
        }
    }

    /**
     * Ẩn progress bar và enable button
     */
    private void hideProgress() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
        btnLogin.setEnabled(true);
    }

    /**
     * Hiển thị lỗi
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.w(TAG, "Error: " + message);
    }

    /**
     * Hiển thị thành công
     */
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.i(TAG, "Success: " + message);
    }

    /**
     * Chuyển đến MainActivity
     */
    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "LoginActivity destroyed");
    }
}