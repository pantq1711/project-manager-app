package com.example.projectmanager.activities;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectmanager.R;
import com.example.projectmanager.adapters.ChatAdapter;
import com.example.projectmanager.models.Message;
import com.example.projectmanager.services.FileUploadService;
import com.example.projectmanager.utils.FirebaseManager;
import com.example.projectmanager.utils.UserManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Chat Activity với phân biệt người gửi và hỗ trợ tệp đính kèm
 */
public class GroupChatActivity extends AppCompatActivity {
    private static final String TAG = "GroupChatActivity";
    private static final int REQUEST_FILE_PICK = 100;

    // Components giao diện
    private RecyclerView rvMessages;
    private EditText etMessage;
    private FloatingActionButton btnSend;
    private ImageButton btnAttach;
    private LinearLayout layoutAttachmentPreview;
    private ImageView ivAttachmentPreview;
    private TextView tvAttachmentName, tvAttachmentSize;
    private ImageButton btnRemoveAttachment;

    // Adapter và dữ liệu
    private ChatAdapter chatAdapter;
    private List<Map<String, Object>> messageList;

    // Firebase manager
    private FirebaseManager firebaseManager;
    private UserManager userManager;
    private FileUploadService fileUploadService;

    // Attachment data
    private Uri selectedFileUri;
    private String selectedFileName;
    private String selectedFileType;
    private long selectedFileSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        Log.d(TAG, "GroupChatActivity được khởi tạo");

        // Khởi tạo Firebase Manager và User Manager
        firebaseManager = new FirebaseManager();
        userManager = UserManager.getInstance(this);
        fileUploadService = new FileUploadService(this);

        // Khởi tạo giao diện
        initViews();
        setupRecyclerView();
        setupClickListeners();

        // Tải tin nhắn
        loadMessages();
    }

    /**
     * Khởi tạo các thành phần giao diện
     */
    private void initViews() {
        // Find toolbar và setup
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Chat Nhóm");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnAttach = findViewById(R.id.btn_attach);

        // Attachment preview views
        layoutAttachmentPreview = findViewById(R.id.layout_attachment_preview);
        ivAttachmentPreview = findViewById(R.id.iv_attachment_preview);
        tvAttachmentName = findViewById(R.id.tv_attachment_name);
        tvAttachmentSize = findViewById(R.id.tv_attachment_size);
        btnRemoveAttachment = findViewById(R.id.btn_remove_attachment);

        Log.d(TAG, "Khởi tạo giao diện thành công");
    }

    /**
     * Thiết lập RecyclerView
     */
    private void setupRecyclerView() {
        messageList = new ArrayList<>();
        // Truyền UserManager thay vì chuỗi cố định
        chatAdapter = new ChatAdapter(messageList, userManager);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Hiển thị tin nhắn mới nhất ở cuối

        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);

        Log.d(TAG, "RecyclerView được thiết lập");
    }

    /**
     * Thiết lập sự kiện click
     */
    private void setupClickListeners() {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Người dùng click Gửi tin nhắn");
                sendMessage();
            }
        });

        btnAttach.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Người dùng click Đính kèm tệp");
                openFilePicker();
            }
        });

        btnRemoveAttachment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Người dùng click Xóa tệp đính kèm");
                clearSelectedFile();
            }
        });
    }

    /**
     * Mở file picker để chọn tệp đính kèm
     */
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Tất cả các loại tệp
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        try {
            startActivityForResult(Intent.createChooser(intent, "Chọn tệp đính kèm"), REQUEST_FILE_PICK);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Vui lòng cài đặt File Manager", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_FILE_PICK && resultCode == Activity.RESULT_OK) {
            if (data != null && data.getData() != null) {
                selectedFileUri = data.getData();

                // Kiểm tra xem file có tồn tại
                if (!isFileUriValid(selectedFileUri)) {
                    Toast.makeText(this, "Không thể truy cập tệp, vui lòng thử lại", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Lấy thông tin tệp
                if (!getFileDetails(selectedFileUri)) {
                    Toast.makeText(this, "Không thể đọc thông tin tệp", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Hiển thị preview
                showAttachmentPreview();
            }
        }
    }

    /**
     * Kiểm tra xem URI file có hợp lệ không
     */
    private boolean isFileUriValid(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream != null) {
                inputStream.close();
                return true;
            }
            return false;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found: " + e.getMessage());
            return false;
        } catch (IOException e) {
            Log.e(TAG, "Error accessing file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy thông tin chi tiết của tệp được chọn
     */
    private boolean getFileDetails(Uri uri) {
        Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                // Lấy tên tệp
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    selectedFileName = cursor.getString(nameIndex);
                } else {
                    // Fallback nếu không lấy được tên
                    selectedFileName = "file_" + System.currentTimeMillis();
                }

                // Lấy kích thước tệp
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    selectedFileSize = cursor.getLong(sizeIndex);
                } else {
                    // Ước tính kích thước nếu không lấy được
                    try {
                        InputStream inputStream = getContentResolver().openInputStream(uri);
                        if (inputStream != null) {
                            selectedFileSize = inputStream.available();
                            inputStream.close();
                        } else {
                            selectedFileSize = 0;
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Error getting file size: " + e.getMessage());
                        selectedFileSize = 0;
                    }
                }

                // Lấy loại tệp
                selectedFileType = getContentResolver().getType(uri);
                if (selectedFileType == null) {
                    // Fallback nếu không lấy được loại
                    if (selectedFileName.toLowerCase().endsWith(".pdf")) {
                        selectedFileType = "application/pdf";
                    } else if (selectedFileName.toLowerCase().endsWith(".doc") ||
                            selectedFileName.toLowerCase().endsWith(".docx")) {
                        selectedFileType = "application/msword";
                    } else {
                        selectedFileType = "application/octet-stream";
                    }
                }

                Log.d(TAG, "File selected: " + selectedFileName + ", size: " + selectedFileSize +
                        ", type: " + selectedFileType);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file details: " + e.getMessage());
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Hiển thị preview tệp đính kèm
     */
    private void showAttachmentPreview() {
        if (selectedFileUri == null) return;

        layoutAttachmentPreview.setVisibility(View.VISIBLE);
        tvAttachmentName.setText(selectedFileName);

        // Hiển thị kích thước tệp
        String readableSize = formatFileSize(selectedFileSize);
        tvAttachmentSize.setText(readableSize);

        // Thiết lập icon dựa trên loại tệp
        if (selectedFileType != null) {
            if (selectedFileType.startsWith("image/")) {
                // Nếu là hình ảnh, hiển thị preview
                try {
                    ivAttachmentPreview.setImageURI(selectedFileUri);
                } catch (Exception e) {
                    Log.e(TAG, "Error setting image URI: " + e.getMessage());
                    ivAttachmentPreview.setImageResource(R.drawable.ic_file);
                }
            } else if (selectedFileType.contains("pdf")) {
                ivAttachmentPreview.setImageResource(R.drawable.ic_file);
            } else if (selectedFileType.contains("word") || selectedFileType.contains("document")) {
                ivAttachmentPreview.setImageResource(R.drawable.ic_file);
            } else if (selectedFileType.contains("excel") || selectedFileType.contains("sheet")) {
                ivAttachmentPreview.setImageResource(R.drawable.ic_file);
            } else if (selectedFileType.contains("video")) {
                ivAttachmentPreview.setImageResource(R.drawable.ic_file);
            } else if (selectedFileType.contains("audio")) {
                ivAttachmentPreview.setImageResource(R.drawable.ic_file);
            } else {
                ivAttachmentPreview.setImageResource(R.drawable.ic_file);
            }
        } else {
            ivAttachmentPreview.setImageResource(R.drawable.ic_file);
        }
    }

    /**
     * Định dạng kích thước tệp cho dễ đọc
     */
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * Xóa tệp đã chọn
     */
    private void clearSelectedFile() {
        selectedFileUri = null;
        selectedFileName = null;
        selectedFileType = null;
        selectedFileSize = 0;

        layoutAttachmentPreview.setVisibility(View.GONE);
    }

    /**
     * Gửi tin nhắn mới
     */
    private void sendMessage() {
        String content = etMessage.getText().toString().trim();

        // Kiểm tra nội dung tin nhắn
        if (content.isEmpty() && selectedFileUri == null) {
            Log.w(TAG, "Nội dung tin nhắn trống và không có tệp đính kèm");
            return;
        }

        // Lấy thông tin người gửi từ Firebase Auth và UserManager
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserName = userManager.getCurrentUserDisplayName();
        String currentUserId = userManager.getCurrentUserId();
        String currentUserEmail = userManager.getCurrentUserEmail();

        if (selectedFileUri != null) {
            // Upload tệp trước, sau đó gửi tin nhắn với URL tệp
            uploadFileAndSendMessage(content, currentUserName, currentUserId, currentUserEmail);
        } else {
            // Gửi tin nhắn văn bản bình thường
            Message message = new Message(content, currentUserName, currentUserId, currentUserEmail);
            sendMessageToFirebase(message.toMap());
        }
    }

    /**
     * Upload tệp lên Firebase Storage và gửi tin nhắn với URL tệp
     */
    private void uploadFileAndSendMessage(String content, String senderName, String senderId, String senderEmail) {
        // Kiểm tra lại file tồn tại trước khi upload
        if (selectedFileUri == null || !isFileUriValid(selectedFileUri)) {
            Toast.makeText(this, "Lỗi: Không thể truy cập tệp đính kèm", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị thông báo đang tải lên
        Toast.makeText(this, "Đang tải lên tệp đính kèm...", Toast.LENGTH_SHORT).show();

        // Upload tệp lên Firebase Storage
        fileUploadService.uploadTaskAttachment(selectedFileUri, new FileUploadService.FileUploadCallback() {
            @Override
            public void onProgress(int progress) {
                // Có thể hiển thị thanh tiến trình ở đây nếu cần
                Log.d(TAG, "Upload progress: " + progress + "%");
            }

            @Override
            public void onSuccess(String downloadUrl, String fileName) {
                // Tạo tin nhắn mới với URL tệp đính kèm
                Message message = new Message(
                        content, senderName, senderId, senderEmail,
                        downloadUrl, selectedFileName, selectedFileType, selectedFileSize);

                // Gửi tin nhắn vào Firebase
                sendMessageToFirebase(message.toMap());

                // Xóa tệp đã chọn
                runOnUiThread(() -> clearSelectedFile());
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Lỗi khi tải lên tệp: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(GroupChatActivity.this, "Lỗi khi upload file: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Gửi tin nhắn vào Firebase
     */
    private void sendMessageToFirebase(Map<String, Object> messageData) {
        firebaseManager.sendMessage(messageData, new FirebaseManager.OnCompleteListener() {
            @Override
            public void onSuccess(String result) {
                Log.d(TAG, "Gửi tin nhắn thành công: " + result);
                runOnUiThread(() -> {
                    // Xóa nội dung EditText
                    etMessage.setText("");

                    // Cuộn xuống tin nhắn mới nhất
                    rvMessages.smoothScrollToPosition(messageList.size() - 1);
                });
            }

            @Override
            public void onFailure(String error) {
                Log.e(TAG, "Lỗi khi gửi tin nhắn: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(GroupChatActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * Tải tin nhắn từ Firebase theo thời gian thực
     */
    private void loadMessages() {
        Log.d(TAG, "Bắt đầu tải tin nhắn");

        firebaseManager.getMessages(new FirebaseManager.OnDataLoadListener() {
            @Override
            public void onDataLoaded(List<Map<String, Object>> data) {
                Log.d(TAG, "Tải được " + data.size() + " tin nhắn");
                runOnUiThread(() -> {
                    // Kiểm tra có tin nhắn mới không
                    int oldSize = messageList.size();

                    messageList.clear();
                    messageList.addAll(data);
                    chatAdapter.notifyDataSetChanged();

                    // Cuộn xuống nếu có tin nhắn mới
                    if (data.size() > oldSize) {
                        rvMessages.smoothScrollToPosition(data.size() - 1);
                    }

                    // Hiển thị số lượng tin nhắn
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(data.size() + " tin nhắn");
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Lỗi khi tải tin nhắn: " + error);
                runOnUiThread(() -> {
                    Toast.makeText(GroupChatActivity.this, "Lỗi: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "GroupChatActivity bị hủy");
    }
}