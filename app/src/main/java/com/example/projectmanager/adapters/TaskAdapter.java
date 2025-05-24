package com.example.projectmanager.adapters;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.ImageView;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectmanager.R;
import com.example.projectmanager.dialogs.UserSelectionDialog;
import com.example.projectmanager.models.Task;
import com.example.projectmanager.viewmodels.TaskViewModel;
import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.app.DatePickerDialog;

/**
 * Adapter cho danh sách nhiệm vụ với phân quyền
 */
public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Map<String, Object>> taskList;
    private OnTaskClickListener listener;
    private SimpleDateFormat dateFormat;
    private Context context;

    public interface OnTaskClickListener {
        void onTaskClick(Map<String, Object> task);
    }

    public TaskAdapter(List<Map<String, Object>> taskList, OnTaskClickListener listener) {
        this.taskList = taskList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Map<String, Object> task = taskList.get(position);
        String currentUserId = getCurrentUserId();

        // Lấy thông tin assignment
        String assignedToUserId = (String) task.get("assignedToUserId");
        String assignedToName = (String) task.get("assignedToName");
        String assignerUserId = (String) task.get("assignerUserId");
        String assignerName = (String) task.get("assignerName");

        // Kiểm tra quyền
        boolean canEdit = checkCanEdit(currentUserId, assignedToUserId, assignerUserId);
        boolean canUpdateStatus = checkCanUpdateStatus(currentUserId, assignedToUserId);
        boolean isAssigner = checkIsAssigner(currentUserId, assignerUserId);

        // Bind dữ liệu cơ bản
        holder.tvTitle.setText((String) task.get("title"));
        holder.tvDescription.setText((String) task.get("description"));

        // Hiển thị thông tin assignment
        String assignText = "Phụ trách: " + (assignedToName != null ? assignedToName : "Chưa có");
        if (assignerName != null) {
            assignText += " (Phân bởi: " + assignerName + ")";
        }
        holder.tvAssignedTo.setText(assignText);

        // Hiển thị ngày hết hạn
        Object dueDateObj = task.get("dueDate");
        if (dueDateObj instanceof Date) {
            holder.tvDueDate.setText("Hạn: " + dateFormat.format((Date) dueDateObj));
        } else if (dueDateObj instanceof com.google.firebase.Timestamp) {
            holder.tvDueDate.setText("Hạn: " + dateFormat.format(((com.google.firebase.Timestamp) dueDateObj).toDate()));
        } else {
            holder.tvDueDate.setText("Hạn: Chưa có");
        }

        // Hiển thị mức độ ưu tiên
        String priority = (String) task.get("priority");
        holder.tvPriority.setText(getPriorityText(priority));
        setPriorityColor(holder.tvPriority, priority);

        // Hiển thị trạng thái
        String status = (String) task.get("status");
        holder.tvStatus.setText(getStatusText(status));
        setStatusColor(holder.tvStatus, status);

        // Áp dụng phân quyền lên UI
        applyPermissionsToUI(holder, canEdit, canUpdateStatus, currentUserId, assignedToUserId);

        // Hiển thị icon tệp đính kèm
        setupAttachmentDisplay(holder, task);

        // Setup click listeners
        setupClickListeners(holder, task, canUpdateStatus, canEdit, isAssigner);
    }

    private void applyPermissionsToUI(TaskViewHolder holder, boolean canEdit, boolean canUpdateStatus,
                                      String currentUserId, String assignedToUserId) {
        // Visual feedback cho quyền
        if (!canEdit) {
            holder.cardView.setAlpha(0.7f);
            // Create lock drawable if needed - you may need to add this drawable to your res/drawable folder
            try {
                holder.tvTitle.setCompoundDrawablesWithIntrinsicBounds(
                        ContextCompat.getDrawable(context, android.R.drawable.ic_lock_lock),
                        null, null, null);
                holder.tvTitle.setCompoundDrawablePadding(8);
            } catch (Exception e) {
                // If lock drawable not found, just skip it
            }
        } else {
            holder.cardView.setAlpha(1.0f);
            holder.tvTitle.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null);
        }

        // Highlight task của user hiện tại
        if (currentUserId != null && currentUserId.equals(assignedToUserId)) {
            // Use a light blue color for current user's tasks
            holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, R.color.chat_my_message));
        } else {
            holder.cardView.setCardBackgroundColor(
                    ContextCompat.getColor(context, android.R.color.white));
        }
    }

    private void setupAttachmentDisplay(TaskViewHolder holder, Map<String, Object> task) {
        String attachmentUrl = (String) task.get("attachmentUrl");
        String attachmentName = (String) task.get("attachmentName");

        if (attachmentUrl != null && !attachmentUrl.isEmpty() && holder.ivAttachment != null) {
            holder.ivAttachment.setVisibility(View.VISIBLE);
            if (attachmentName != null && !attachmentName.isEmpty() && holder.tvAttachmentName != null) {
                holder.tvAttachmentName.setVisibility(View.VISIBLE);
                holder.tvAttachmentName.setText(attachmentName);
            } else if (holder.tvAttachmentName != null) {
                holder.tvAttachmentName.setVisibility(View.GONE);
            }
        } else {
            if (holder.ivAttachment != null) {
                holder.ivAttachment.setVisibility(View.GONE);
            }
            if (holder.tvAttachmentName != null) {
                holder.tvAttachmentName.setVisibility(View.GONE);
            }
        }
    }

    private void setupClickListeners(TaskViewHolder holder, Map<String, Object> task,
                                     boolean canUpdateStatus, boolean canEdit, boolean isAssigner) {
        // Click để update status
        holder.cardView.setOnClickListener(v -> {
            if (canUpdateStatus && listener != null) {
                listener.onTaskClick(task);
            } else {
                String message = canEdit ?
                        "Chỉ người được phân việc mới có thể cập nhật trạng thái" :
                        "Bạn không có quyền thao tác với task này";
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Long click cho menu options
        holder.cardView.setOnLongClickListener(v -> {
            if (canEdit || isAssigner) {
                showTaskOptions(task, isAssigner, canEdit);
            } else {
                Toast.makeText(context, "Bạn không có quyền thao tác với task này",
                        Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void showTaskOptions(Map<String, Object> task, boolean isAssigner, boolean canEdit) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Tùy chọn nhiệm vụ");

        String[] options;
        if (isAssigner) {
            options = new String[]{"Xem chi tiết", "Chỉnh sửa", "Phân việc lại", "Xóa nhiệm vụ"};
        } else if (canEdit) {
            options = new String[]{"Xem chi tiết", "Báo cáo tiến độ", "Yêu cầu chỉnh sửa"};
        } else {
            options = new String[]{"Xem chi tiết"};
        }

        builder.setItems(options, (dialog, which) -> {
            String action = "";
            if (isAssigner) {
                switch (which) {
                    case 0: action = "VIEW_DETAIL"; break;
                    case 1: action = "EDIT"; break;
                    case 2: action = "REASSIGN"; break;
                    case 3: action = "DELETE"; break;
                }
            } else if (canEdit) {
                switch (which) {
                    case 0: action = "VIEW_DETAIL"; break;
                    case 1: action = "REPORT_PROGRESS"; break;
                    case 2: action = "REQUEST_EDIT"; break;
                }
            } else {
                action = "VIEW_DETAIL";
            }

            // Handle action directly in adapter
            handleTaskAction(task, action);
        });

        builder.create().show();
    }

    private void handleTaskAction(Map<String, Object> task, String action) {
        switch (action) {
            case "VIEW_DETAIL":
                showTaskDetails(task);
                break;
            case "EDIT":
                editTask(task);
                break;
            case "REASSIGN":
                reassignTask(task);
                break;
            case "DELETE":
                // Gọi callback để thông báo Activity delete task
                if (listener != null) {
                    // Tạo một map để báo hiệu action delete
                    Map<String, Object> deleteAction = new HashMap<>();
                    deleteAction.put("action", "DELETE_TASK");
                    deleteAction.put("taskId", task.get("id"));
                    deleteAction.put("originalTask", task);
                    listener.onTaskClick(deleteAction);
                }
                break;
            case "REPORT_PROGRESS":
                showReportProgressDialog(task);
                break;
            case "REQUEST_EDIT":
                requestEditTask(task);
                break;
        }
    }

    private void editTask(Map<String, Object> task) {
        if (task == null) return;

        String taskId = (String) task.get("id");
        if (taskId == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_task, null);
        builder.setView(dialogView);

        // Khởi tạo các thành phần UI
        EditText etTitle = dialogView.findViewById(R.id.et_task_title);
        EditText etDescription = dialogView.findViewById(R.id.et_task_description);
        Spinner spPriority = dialogView.findViewById(R.id.sp_priority);
        TextView tvDueDate = dialogView.findViewById(R.id.tv_due_date);
        Button btnSelectDate = dialogView.findViewById(R.id.btn_select_date);
        Button btnSave = dialogView.findViewById(R.id.btn_save);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        // Thiết lập dữ liệu hiện tại
        etTitle.setText((String) task.get("title"));
        etDescription.setText((String) task.get("description"));

        // Thiết lập spinner ưu tiên
        String[] priorities = {"Thấp", "Trung bình", "Cao"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPriority.setAdapter(priorityAdapter);

        // Chọn priority hiện tại
        String currentPriority = (String) task.get("priority");
        int priorityIndex = 1; // Mặc định Trung bình
        if ("low".equals(currentPriority)) priorityIndex = 0;
        else if ("high".equals(currentPriority)) priorityIndex = 2;
        spPriority.setSelection(priorityIndex);

        // Xử lý date
        final Date[] selectedDate = new Date[1];
        Object dueDateObj = task.get("dueDate");
        if (dueDateObj instanceof Date) {
            selectedDate[0] = (Date) dueDateObj;
        } else if (dueDateObj instanceof com.google.firebase.Timestamp) {
            selectedDate[0] = ((com.google.firebase.Timestamp) dueDateObj).toDate();
        } else {
            selectedDate[0] = new Date(); // Mặc định là ngày hiện tại
        }

        // Hiển thị ngày hạn chót
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        tvDueDate.setText("Hạn chót: " + dateFormat.format(selectedDate[0]));

        // Xử lý chọn ngày
        btnSelectDate.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(selectedDate[0]);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    context,
                    (view, year, month, dayOfMonth) -> {
                        Calendar selectedCalendar = Calendar.getInstance();
                        selectedCalendar.set(year, month, dayOfMonth);
                        selectedDate[0] = selectedCalendar.getTime();
                        tvDueDate.setText("Hạn chót: " + dateFormat.format(selectedDate[0]));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );

            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            datePickerDialog.show();
        });

        // Tạo dialog
        AlertDialog dialog = builder.create();

        // Xử lý nút lưu
        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (title.isEmpty()) {
                etTitle.setError("Vui lòng nhập tiêu đề");
                return;
            }

            // Lấy priority từ spinner
            String priority;
            switch (spPriority.getSelectedItemPosition()) {
                case 0: priority = "low"; break;
                case 2: priority = "high"; break;
                default: priority = "medium"; break;
            }

            // Tạo Task object mới với dữ liệu đã cập nhật
            Task updatedTask = new Task();
            updatedTask.setId(taskId);
            updatedTask.setTitle(title);
            updatedTask.setDescription(description);
            updatedTask.setPriority(priority);
            updatedTask.setDueDate(selectedDate[0]);

            // Giữ nguyên những thông tin khác
            updatedTask.setAssignedToUserId((String) task.get("assignedToUserId"));
            updatedTask.setAssignedToName((String) task.get("assignedToName"));
            updatedTask.setAssignerUserId((String) task.get("assignerUserId"));
            updatedTask.setAssignerName((String) task.get("assignerName"));
            updatedTask.setStatus((String) task.get("status"));

            // Xử lý createdAt
            Object createdAtObj = task.get("createdAt");
            if (createdAtObj instanceof Date) {
                updatedTask.setCreatedAt((Date) createdAtObj);
            } else if (createdAtObj instanceof com.google.firebase.Timestamp) {
                updatedTask.setCreatedAt(((com.google.firebase.Timestamp) createdAtObj).toDate());
            } else {
                updatedTask.setCreatedAt(new Date());
            }

            updatedTask.setUpdatedAt(new Date()); // Cập nhật thời gian

            // Cập nhật task qua TaskViewModel
            TaskViewModel taskViewModel = new ViewModelProvider((FragmentActivity) context).get(TaskViewModel.class);
            taskViewModel.updateTask(updatedTask);

            Toast.makeText(context, "Đang cập nhật nhiệm vụ...", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void reassignTask(Map<String, Object> task) {
        if (task == null) return;

        String taskId = (String) task.get("id");
        if (taskId == null) return;

        // Sử dụng dialog tùy chỉnh UserSelectionDialog đã có trong ứng dụng của bạn
        UserSelectionDialog dialog = new UserSelectionDialog(context,
                (userId, userName, userEmail) -> {
                    // Callback khi người dùng được chọn
                    if (userId != null && !userId.isEmpty()) {
                        // Cập nhật task với người được giao mới
                        TaskViewModel taskViewModel = new ViewModelProvider((FragmentActivity) context).get(TaskViewModel.class);
                        taskViewModel.reassignTask(taskId, userId, userName);

                        Toast.makeText(context, "Đã phân công lại nhiệm vụ cho " + userName,
                                Toast.LENGTH_SHORT).show();
                    }
                });

        dialog.show();
    }

    private void showReportProgressDialog(Map<String, Object> task) {
        if (task == null) return;

        String taskId = (String) task.get("id");
        String currentStatus = (String) task.get("status");
        if (taskId == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Báo cáo tiến độ");

        // Tạo view cho dialog
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_report_progress, null);
        builder.setView(dialogView);

        EditText etComment = dialogView.findViewById(R.id.et_progress_comment);
        Spinner spStatus = dialogView.findViewById(R.id.sp_status);

        // Thiết lập spinner trạng thái
        String[] statuses = {"Chờ xử lý", "Đang thực hiện", "Hoàn thành"};
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(
                context, android.R.layout.simple_spinner_item, statuses);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spStatus.setAdapter(statusAdapter);

        // Chọn trạng thái hiện tại
        int statusIndex = 0;
        if ("in_progress".equals(currentStatus)) statusIndex = 1;
        else if ("completed".equals(currentStatus)) statusIndex = 2;
        spStatus.setSelection(statusIndex);

        builder.setPositiveButton("Cập nhật", (dialog, which) -> {
            String comment = etComment.getText().toString().trim();
            String newStatus;
            switch (spStatus.getSelectedItemPosition()) {
                case 1: newStatus = "in_progress"; break;
                case 2: newStatus = "completed"; break;
                default: newStatus = "pending"; break;
            }

            // Cập nhật trạng thái task
            TaskViewModel taskViewModel = new ViewModelProvider((FragmentActivity) context).get(TaskViewModel.class);
            taskViewModel.updateTaskStatus(taskId, newStatus);

            // Thêm comment nếu có
            if (!comment.isEmpty()) {
                taskViewModel.addTaskComment(taskId, comment);
            }

            Toast.makeText(context, "Đã cập nhật tiến độ", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void requestEditTask(Map<String, Object> task) {
        if (task == null) return;

        String taskId = (String) task.get("id");
        String assignerUserId = (String) task.get("assignerUserId");
        if (taskId == null || assignerUserId == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Yêu cầu chỉnh sửa nhiệm vụ");

        // Tạo EditText để nhập yêu cầu
        final EditText etRequest = new EditText(context);
        etRequest.setHint("Nhập yêu cầu chỉnh sửa của bạn");
        etRequest.setMinLines(3);
        etRequest.setGravity(Gravity.TOP | Gravity.START);

        // Thiết lập padding
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density);
        etRequest.setPadding(padding, padding, padding, padding);

        builder.setView(etRequest);

        builder.setPositiveButton("Gửi yêu cầu", (dialog, which) -> {
            String requestText = etRequest.getText().toString().trim();
            if (requestText.isEmpty()) {
                Toast.makeText(context, "Vui lòng nhập yêu cầu", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tạo comment với prefix đặc biệt để phân biệt yêu cầu chỉnh sửa
            String editRequest = "[YÊU CẦU CHỈNH SỬA] " + requestText;

            // Thêm comment vào task
            TaskViewModel taskViewModel = new ViewModelProvider((FragmentActivity) context).get(TaskViewModel.class);
            taskViewModel.addTaskComment(taskId, editRequest);

            Toast.makeText(context, "Đã gửi yêu cầu chỉnh sửa", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showTaskDetails(Map<String, Object> task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Chi tiết nhiệm vụ");

        StringBuilder details = new StringBuilder();
        details.append("Tiêu đề: ").append(task.get("title")).append("\n\n");
        details.append("Mô tả: ").append(task.get("description")).append("\n\n");
        details.append("Phụ trách: ").append(task.get("assignedToName")).append("\n");
        details.append("Phân việc bởi: ").append(task.get("assignerName")).append("\n");
        details.append("Mức độ: ").append(getPriorityText((String) task.get("priority"))).append("\n");
        details.append("Trạng thái: ").append(getStatusText((String) task.get("status"))).append("\n");

        // Add due date
        Object dueDate = task.get("dueDate");
        if (dueDate instanceof Date) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            details.append("Hạn chót: ").append(dateFormat.format((Date) dueDate)).append("\n");
        } else if (dueDate instanceof com.google.firebase.Timestamp) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            details.append("Hạn chót: ").append(dateFormat.format(((com.google.firebase.Timestamp) dueDate).toDate())).append("\n");
        }

        builder.setMessage(details.toString());
        builder.setPositiveButton("Đóng", null);
        builder.create().show();
    }

    // Helper methods cho permissions
    private String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    private boolean checkCanEdit(String currentUserId, String assignedToUserId, String assignerUserId) {
        if (currentUserId == null) return false;
        return currentUserId.equals(assignedToUserId) || currentUserId.equals(assignerUserId);
    }

    private boolean checkCanUpdateStatus(String currentUserId, String assignedToUserId) {
        if (currentUserId == null) return false;
        return currentUserId.equals(assignedToUserId);
    }

    private boolean checkIsAssigner(String currentUserId, String assignerUserId) {
        if (currentUserId == null) return false;
        return currentUserId.equals(assignerUserId);
    }

    // Helper methods cho display
    private String getPriorityText(String priority) {
        if (priority == null) return "Trung bình";
        switch (priority) {
            case "low": return "Thấp";
            case "medium": return "Trung bình";
            case "high": return "Cao";
            default: return "Trung bình";
        }
    }

    private void setPriorityColor(TextView tvPriority, String priority) {
        int color;
        switch (priority) {
            case "high":
                color = ContextCompat.getColor(context, R.color.priority_high);
                break;
            case "low":
                color = ContextCompat.getColor(context, R.color.priority_low);
                break;
            default:
                color = ContextCompat.getColor(context, R.color.priority_medium);
                break;
        }
        tvPriority.setBackgroundColor(color);
        tvPriority.setTextColor(ContextCompat.getColor(context, android.R.color.white));
    }

    private String getStatusText(String status) {
        if (status == null) return "Chờ xử lý";
        switch (status) {
            case "pending": return "Chờ xử lý";
            case "in_progress": return "Đang thực hiện";
            case "completed": return "Hoàn thành";
            default: return "Chờ xử lý";
        }
    }

    private void setStatusColor(TextView tvStatus, String status) {
        int color;
        switch (status) {
            case "completed":
                color = ContextCompat.getColor(context, R.color.status_completed);
                break;
            case "in_progress":
                color = ContextCompat.getColor(context, R.color.status_in_progress);
                break;
            default:
                color = ContextCompat.getColor(context, R.color.status_pending);
                break;
        }
        tvStatus.setBackgroundColor(color);
        tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white));
    }

    @Override
    public int getItemCount() {
        return taskList != null ? taskList.size() : 0;
    }

    // Filter methods
    public void filterMyTasks(boolean showOnlyMyTasks) {
        String currentUserId = getCurrentUserId();
        if (currentUserId == null) return;

        // Implementation would depend on your data structure
        // You might need to keep original list and filtered list
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvTitle, tvDescription, tvAssignedTo, tvDueDate, tvPriority, tvStatus, tvAttachmentName;
        ImageView ivAttachment;

        TaskViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cv_task);
            tvTitle = itemView.findViewById(R.id.tv_task_title);
            tvDescription = itemView.findViewById(R.id.tv_task_description);
            tvAssignedTo = itemView.findViewById(R.id.tv_assigned_to);
            tvDueDate = itemView.findViewById(R.id.tv_due_date);
            tvPriority = itemView.findViewById(R.id.tv_priority);
            tvStatus = itemView.findViewById(R.id.tv_status);
            ivAttachment = itemView.findViewById(R.id.iv_attachment);
            tvAttachmentName = itemView.findViewById(R.id.tv_attachment_name);
        }
    }
}