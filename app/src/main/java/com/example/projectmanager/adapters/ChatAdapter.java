package com.example.projectmanager.adapters;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectmanager.R;
import com.example.projectmanager.utils.UserManager;
import com.google.firebase.Timestamp;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter cho tin nhắn với UserManager để phân biệt người gửi và hỗ trợ tệp đính kèm
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {
    private List<Map<String, Object>> messageList;
    private UserManager userManager;
    private SimpleDateFormat timeFormat;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat fullDateFormat;

    public ChatAdapter(List<Map<String, Object>> messageList, UserManager userManager) {
        this.messageList = messageList;
        this.userManager = userManager;
        this.timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        this.dateFormat = new SimpleDateFormat("dd/MM", Locale.getDefault());
        this.fullDateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        Map<String, Object> message = messageList.get(position);

        if (message == null) return;

        String content = (String) message.get("content");
        String senderName = (String) message.get("senderName");
        String senderId = (String) message.get("senderId");
        String senderEmail = (String) message.get("senderEmail");
        Object timestampObj = message.get("timestamp");

        // Kiểm tra và thiết lập attachment
        String attachmentUrl = (String) message.get("attachmentUrl");
        String attachmentName = (String) message.get("attachmentName");
        String attachmentType = (String) message.get("attachmentType");

        // Debug log để kiểm tra timestamp
        android.util.Log.d("ChatAdapter", "Message content: " + content);
        android.util.Log.d("ChatAdapter", "Timestamp object: " + timestampObj + " (type: " +
                (timestampObj != null ? timestampObj.getClass().getSimpleName() : "null") + ")");

        // Debug log cho attachment
        if (attachmentUrl != null) {
            android.util.Log.d("ChatAdapter", "Message has attachment: " + attachmentName +
                    " (type: " + attachmentType + ")");
        }

        // Hiển thị nội dung tin nhắn
        holder.tvMessage.setText(content != null ? content : "");

        // Hiển thị attachment nếu có
        if (attachmentUrl != null && !attachmentUrl.isEmpty()) {
            holder.layoutAttachment.setVisibility(View.VISIBLE);

            // Kiểm tra loại tệp đính kèm
            boolean isImage = attachmentType != null && attachmentType.startsWith("image/");

            if (isImage) {
                // Hiển thị hình ảnh sử dụng AsyncTask để load image
                holder.ivAttachmentImage.setVisibility(View.VISIBLE);
                holder.layoutFileAttachment.setVisibility(View.GONE);

                // Set mặc định trước khi load
                holder.ivAttachmentImage.setImageResource(R.drawable.ic_image_placeholder);

                // Load ảnh từ URL
                new ImageLoadTask(attachmentUrl, holder.ivAttachmentImage).execute();

                // Set click listener để xem ảnh full screen
                holder.ivAttachmentImage.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(attachmentUrl));
                    holder.itemView.getContext().startActivity(intent);
                });

            } else {
                // Hiển thị icon và tên file
                holder.ivAttachmentImage.setVisibility(View.GONE);
                holder.layoutFileAttachment.setVisibility(View.VISIBLE);

                // Đặt icon phù hợp với loại file
                if (attachmentType != null) {
                    if (attachmentType.contains("pdf")) {
                        holder.ivFileIcon.setImageResource(R.drawable.ic_file);
                    } else if (attachmentType.contains("word") || attachmentType.contains("document")) {
                        holder.ivFileIcon.setImageResource(R.drawable.ic_file);
                    } else if (attachmentType.contains("excel") || attachmentType.contains("sheet")) {
                        holder.ivFileIcon.setImageResource(R.drawable.ic_file);
                    } else if (attachmentType.contains("video")) {
                        holder.ivFileIcon.setImageResource(R.drawable.ic_file);
                    } else if (attachmentType.contains("audio")) {
                        holder.ivFileIcon.setImageResource(R.drawable.ic_file);
                    } else {
                        holder.ivFileIcon.setImageResource(R.drawable.ic_file);
                    }
                } else {
                    holder.ivFileIcon.setImageResource(R.drawable.ic_file);
                }

                // Hiển thị tên file
                holder.tvFileName.setText(attachmentName != null ? attachmentName : "File đính kèm");

                // Set click listener để mở file
                holder.layoutFileAttachment.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(attachmentUrl));
                    holder.itemView.getContext().startActivity(intent);
                });
            }

        } else {
            // Không có attachment
            holder.layoutAttachment.setVisibility(View.GONE);
        }

        // Hiển thị thời gian
        String timeText = formatTime(timestampObj);
        holder.tvTime.setText(timeText);

        // Kiểm tra xem tin nhắn có phải của user hiện tại không
        boolean isOwnMessage = userManager.isCurrentUser(senderName, senderEmail);

        if (isOwnMessage) {
            setupOwnMessage(holder);
        } else {
            setupOtherMessage(holder, senderName);
        }
    }

    private void setupOwnMessage(ChatViewHolder holder) {
        // Tin nhắn của mình - hiển thị bên phải
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                holder.messageContainer.getLayoutParams();
        if (params == null) {
            params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        params.gravity = android.view.Gravity.END;
        params.setMargins(50, 8, 8, 8);
        holder.messageContainer.setLayoutParams(params);

        // Hiển thị "Bạn" cho tin nhắn của mình
        holder.tvSender.setText("Bạn");
        holder.tvSender.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), android.R.color.white));
        holder.tvSender.setVisibility(View.VISIBLE);

        // Màu bubble xanh cho tin nhắn của mình
        holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), android.R.color.holo_blue_light));

        // Chữ màu trắng cho tin nhắn của mình
        holder.tvMessage.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), android.R.color.white));
        holder.tvTime.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), android.R.color.white));
        holder.tvTime.setAlpha(0.8f);
    }

    private void setupOtherMessage(ChatViewHolder holder, String senderName) {
        // Tin nhắn của người khác - hiển thị bên trái
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)
                holder.messageContainer.getLayoutParams();
        if (params == null) {
            params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
        }
        params.gravity = android.view.Gravity.START;
        params.setMargins(8, 8, 50, 8);
        holder.messageContainer.setLayoutParams(params);

        // Hiển thị tên người gửi
        holder.tvSender.setText(senderName != null ? senderName : "Ẩn danh");
        holder.tvSender.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), android.R.color.holo_blue_dark));
        holder.tvSender.setVisibility(View.VISIBLE);

        // Màu bubble trắng cho tin nhắn của người khác
        holder.cardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), android.R.color.white));

        // Chữ màu đen cho tin nhắn của người khác
        holder.tvMessage.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), android.R.color.black));
        holder.tvTime.setTextColor(ContextCompat.getColor(
                holder.itemView.getContext(), android.R.color.darker_gray));
    }

    private String formatTime(Object timestampObj) {
        try {
            Date messageDate = null;

            // Xử lý các loại timestamp khác nhau
            if (timestampObj == null) {
                android.util.Log.w("ChatAdapter", "Timestamp is null, using current time");
                messageDate = new Date();
            } else if (timestampObj instanceof Date) {
                messageDate = (Date) timestampObj;
                android.util.Log.d("ChatAdapter", "Timestamp is Date: " + messageDate);
            } else if (timestampObj instanceof Long) {
                messageDate = new Date((Long) timestampObj);
                android.util.Log.d("ChatAdapter", "Timestamp is Long: " + timestampObj + " -> " + messageDate);
            } else if (timestampObj instanceof Timestamp) {
                // Xử lý Firebase Firestore Timestamp
                messageDate = ((Timestamp) timestampObj).toDate();
                android.util.Log.d("ChatAdapter", "Timestamp is Firestore Timestamp: " + messageDate);
            } else {
                // Thử parse string nếu có thể
                try {
                    String timestampStr = timestampObj.toString();
                    long timestamp = Long.parseLong(timestampStr);
                    messageDate = new Date(timestamp);
                    android.util.Log.d("ChatAdapter", "Parsed timestamp from string: " + timestampStr + " -> " + messageDate);
                } catch (Exception e) {
                    android.util.Log.e("ChatAdapter", "Cannot parse timestamp: " + timestampObj, e);
                    messageDate = new Date();
                }
            }

            // Format thời gian
            if (messageDate != null) {
                Calendar today = Calendar.getInstance();
                Calendar messageCalendar = Calendar.getInstance();
                messageCalendar.setTime(messageDate);

                boolean isToday = today.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) == messageCalendar.get(Calendar.DAY_OF_YEAR);

                boolean isYesterday = today.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR) &&
                        today.get(Calendar.DAY_OF_YEAR) - messageCalendar.get(Calendar.DAY_OF_YEAR) == 1;

                if (isToday) {
                    String time = timeFormat.format(messageDate);
                    android.util.Log.d("ChatAdapter", "Today's message: " + time);
                    return time;
                } else if (isYesterday) {
                    String time = "Hôm qua " + timeFormat.format(messageDate);
                    android.util.Log.d("ChatAdapter", "Yesterday's message: " + time);
                    return time;
                } else {
                    // Kiểm tra xem có phải cùng năm không
                    if (today.get(Calendar.YEAR) == messageCalendar.get(Calendar.YEAR)) {
                        String time = dateFormat.format(messageDate) + " " + timeFormat.format(messageDate);
                        android.util.Log.d("ChatAdapter", "Same year message: " + time);
                        return time;
                    } else {
                        String time = fullDateFormat.format(messageDate);
                        android.util.Log.d("ChatAdapter", "Different year message: " + time);
                        return time;
                    }
                }
            }
        } catch (Exception e) {
            android.util.Log.e("ChatAdapter", "Error formatting time", e);
        }

        // Fallback
        String fallback = timeFormat.format(new Date());
        android.util.Log.w("ChatAdapter", "Using fallback time: " + fallback);
        return fallback;
    }

    @Override
    public int getItemCount() {
        return messageList != null ? messageList.size() : 0;
    }

    /**
     * AsyncTask để tải ảnh từ URL
     */
    private static class ImageLoadTask extends AsyncTask<Void, Void, Bitmap> {
        private String url;
        private ImageView imageView;

        public ImageLoadTask(String url, ImageView imageView) {
            this.url = url;
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            try {
                URL urlConnection = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) urlConnection.openConnection();
                connection.setDoInput(true);
                connection.connect();
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            } catch (IOException e) {
                android.util.Log.e("ImageLoadTask", "Error loading image", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null && imageView != null) {
                imageView.setImageBitmap(result);
            } else if (imageView != null) {
                // Set error image if loading failed
                imageView.setImageResource(R.drawable.ic_broken_image);
            }
        }
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageContainer;
        CardView cardView;
        TextView tvMessage, tvSender, tvTime;

        // Attachment views
        LinearLayout layoutAttachment;
        ImageView ivAttachmentImage;
        LinearLayout layoutFileAttachment;
        ImageView ivFileIcon;
        TextView tvFileName;

        ChatViewHolder(View itemView) {
            super(itemView);
            messageContainer = itemView.findViewById(R.id.message_container);
            cardView = itemView.findViewById(R.id.cv_message);
            tvMessage = itemView.findViewById(R.id.tv_message);
            tvSender = itemView.findViewById(R.id.tv_sender);
            tvTime = itemView.findViewById(R.id.tv_time);

            // Initialize attachment views
            layoutAttachment = itemView.findViewById(R.id.layout_attachment);
            ivAttachmentImage = itemView.findViewById(R.id.iv_attachment_image);
            layoutFileAttachment = itemView.findViewById(R.id.layout_file_attachment);
            ivFileIcon = itemView.findViewById(R.id.iv_file_icon);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
        }
    }
}