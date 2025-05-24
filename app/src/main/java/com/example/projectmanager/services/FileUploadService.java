package com.example.projectmanager.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Service to handle file uploads to Firebase Storage with improved error handling
 */
public class FileUploadService {
    private static final String TAG = "FileUploadService";
    private static final String STORAGE_PATH_TASKS = "tasks/attachments/";
    private static final String STORAGE_PATH_MESSAGES = "messages/attachments/";
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50MB

    private FirebaseStorage storage;
    private Context context;

    public interface FileUploadCallback {
        void onProgress(int progress);
        void onSuccess(String downloadUrl, String fileName);
        void onError(String error);
    }

    public FileUploadService(Context context) {
        this.context = context.getApplicationContext();
        this.storage = FirebaseStorage.getInstance();
    }

    public void uploadTaskAttachment(Uri fileUri, FileUploadCallback callback) {
        uploadFile(fileUri, STORAGE_PATH_TASKS, callback);
    }

    public void uploadMessageAttachment(Uri fileUri, FileUploadCallback callback) {
        uploadFile(fileUri, STORAGE_PATH_MESSAGES, callback);
    }

    private void uploadFile(Uri fileUri, String storagePath, FileUploadCallback callback) {
        if (fileUri == null) {
            callback.onError("File URI is null");
            return;
        }

        // Validate the file can be accessed
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                callback.onError("Không thể truy cập tệp");
                return;
            }

            // Validate file size
            long fileSize;
            try {
                fileSize = inputStream.available();
                inputStream.close();

                if (fileSize > MAX_FILE_SIZE) {
                    callback.onError("File quá lớn. Vui lòng chọn file nhỏ hơn 50MB");
                    return;
                } else if (fileSize == 0) {
                    callback.onError("File trống");
                    return;
                }
            } catch (IOException sizeEx) {
                Log.e(TAG, "Error checking file size", sizeEx);
                // Continue despite not being able to check size
            }

        } catch (IOException e) {
            Log.e(TAG, "Error accessing file", e);
            callback.onError("Không thể truy cập tệp: " + e.getMessage());
            return;
        }

        // Generate unique filename
        String fileName = generateFileName(fileUri);
        String fullPath = storagePath + fileName;

        StorageReference storageRef = storage.getReference().child(fullPath);

        try {
            InputStream stream = context.getContentResolver().openInputStream(fileUri);
            if (stream == null) {
                callback.onError("Không thể đọc tệp");
                return;
            }

            UploadTask uploadTask = storageRef.putStream(stream);

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnProgressListener(taskSnapshot -> {
                double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                callback.onProgress((int) progress);
            }).addOnSuccessListener(taskSnapshot -> {
                // Upload succeeded
                storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    callback.onSuccess(uri.toString(), fileName);
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting download URL", e);
                    callback.onError("Lỗi khi lấy URL file: " + e.getMessage());
                });
            }).addOnFailureListener(e -> {
                // Upload failed
                Log.e(TAG, "File upload failed", e);
                callback.onError("Lỗi khi upload file: " + e.getMessage());
            });
        } catch (IOException e) {
            Log.e(TAG, "Error reading file for upload", e);
            callback.onError("Lỗi khi đọc tệp: " + e.getMessage());
        }
    }

    private String generateFileName(Uri fileUri) {
        String extension = getFileExtension(fileUri);
        String uniqueId = UUID.randomUUID().toString();
        return uniqueId + (extension != null ? "." + extension : "");
    }

    private String getFileExtension(Uri fileUri) {
        String mimeType = context.getContentResolver().getType(fileUri);
        if (mimeType != null) {
            return MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
        }

        // Fallback: try to get extension from URI path
        String path = fileUri.getPath();
        if (path != null) {
            int dot = path.lastIndexOf(".");
            if (dot > 0 && dot < path.length() - 1) {
                return path.substring(dot + 1);
            }
        }

        return null;
    }

    public void deleteFile(String downloadUrl, DeleteCallback callback) {
        try {
            StorageReference fileRef = storage.getReferenceFromUrl(downloadUrl);
            fileRef.delete().addOnSuccessListener(aVoid -> {
                callback.onSuccess();
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting file", e);
                callback.onError("Lỗi khi xóa file: " + e.getMessage());
            });
        } catch (Exception e) {
            Log.e(TAG, "Error parsing download URL", e);
            callback.onError("URL không hợp lệ");
        }
    }

    public interface DeleteCallback {
        void onSuccess();
        void onError(String error);
    }
}