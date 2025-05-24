package com.example.projectmanager.repositories;

import android.util.Log;

import com.example.projectmanager.models.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskRepository {
    private static final String TAG = "TaskRepository";
    private static final String TASKS_COLLECTION = "tasks";
    private static final int PAGE_SIZE = 10; // Số items mỗi trang
    private FirebaseFirestore db;

    public interface OnTasksLoadedListener {
        void onTasksLoaded(List<Map<String, Object>> tasks);
        void onError(String error);
    }

    public interface OnPagedTasksLoadedListener {
        void onTasksLoaded(List<Map<String, Object>> tasks, boolean hasMore, DocumentSnapshot lastDocument);
        void onError(String error);
    }

    public interface OnTaskAddedListener {
        void onTaskAdded(String taskId);
        void onError(String error);
    }

    public interface OnTaskUpdatedListener {
        void onTaskUpdated();
        void onError(String error);
    }

    public interface OnTaskDeletedListener {
        void onTaskDeleted();
        void onError(String error);
    }

    public interface OnTaskLoadedListener {
        void onTaskLoaded(Task task);
        void onError(String error);
    }

    public TaskRepository() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Lấy tất cả tasks với real-time listener
     */
    public void getTasks(OnTasksLoadedListener listener) {
        db.collection(TASKS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting tasks", error);
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> tasks = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> task = doc.getData();
                            task.put("id", doc.getId());
                            tasks.add(task);
                        }
                    }
                    Log.d(TAG, "Loaded " + tasks.size() + " tasks");
                    listener.onTasksLoaded(tasks);
                });
    }

    /**
     * Lấy tasks với phân trang - trang đầu tiên
     */
    public void getTasksWithPagination(OnPagedTasksLoadedListener listener) {
        getTasksWithPagination(null, listener);
    }

    /**
     * Lấy tasks với phân trang - trang tiếp theo
     */
    public void getTasksWithPagination(DocumentSnapshot lastDocument, OnPagedTasksLoadedListener listener) {
        Log.d(TAG, "Loading tasks with pagination, lastDocument: " + (lastDocument != null ? lastDocument.getId() : "null"));

        Query query = db.collection(TASKS_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE);

        // Nếu có lastDocument, bắt đầu từ document tiếp theo
        if (lastDocument != null) {
            query = query.startAfter(lastDocument);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> tasks = new ArrayList<>();
                    DocumentSnapshot lastDoc = null;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> task = doc.getData();
                        task.put("id", doc.getId());
                        tasks.add(task);
                        lastDoc = doc;
                    }

                    // Check if there are more documents
                    boolean hasMore = queryDocumentSnapshots.size() == PAGE_SIZE;

                    Log.d(TAG, "Loaded " + tasks.size() + " tasks, hasMore: " + hasMore);
                    listener.onTasksLoaded(tasks, hasMore, lastDoc);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading tasks with pagination", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Lấy tasks của một user cụ thể với phân trang - FIXED để tránh index
     */
    public void getTasksForUserWithPagination(String userId, DocumentSnapshot lastDocument, OnPagedTasksLoadedListener listener) {
        if (userId == null) {
            listener.onError("User ID is null");
            return;
        }

        Log.d(TAG, "Loading tasks for user with pagination: " + userId);

        // Thay đổi strategy: Load tất cả tasks for user, không dùng pagination với ORDER BY
        // để tránh cần composite index
        db.collection(TASKS_COLLECTION)
                .whereEqualTo("assignedToUserId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> allTasks = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Map<String, Object> task = doc.getData();
                        task.put("id", doc.getId());
                        allTasks.add(task);
                    }

                    // Sort in memory by createdAt using Collections.sort()
                    Collections.sort(allTasks, new Comparator<Map<String, Object>>() {
                        @Override
                        public int compare(Map<String, Object> task1, Map<String, Object> task2) {
                            Object date1 = task1.get("createdAt");
                            Object date2 = task2.get("createdAt");

                            if (date1 == null && date2 == null) return 0;
                            if (date1 == null) return 1;
                            if (date2 == null) return -1;

                            try {
                                Date d1 = (Date) date1;
                                Date d2 = (Date) date2;
                                return d2.compareTo(d1); // DESC order
                            } catch (Exception e) {
                                return 0;
                            }
                        }
                    });

                    // Implement pagination in memory
                    int startIndex = 0;
                    if (lastDocument != null) {
                        // Find the index of lastDocument
                        String lastDocId = lastDocument.getId();
                        for (int i = 0; i < allTasks.size(); i++) {
                            if (lastDocId.equals(allTasks.get(i).get("id"))) {
                                startIndex = i + 1;
                                break;
                            }
                        }
                    }

                    int endIndex = Math.min(startIndex + PAGE_SIZE, allTasks.size());
                    List<Map<String, Object>> paginatedTasks = allTasks.subList(startIndex, endIndex);

                    boolean hasMore = endIndex < allTasks.size();
                    DocumentSnapshot lastDoc = null;

                    Log.d(TAG, "Loaded " + paginatedTasks.size() + " tasks for user (total: " + allTasks.size() + "), hasMore: " + hasMore);
                    listener.onTasksLoaded(paginatedTasks, hasMore, lastDoc);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user tasks with pagination", e);
                    listener.onError(e.getMessage());
                });
    }

    /**
     * Lấy tasks của một user cụ thể - FIXED
     */
    public void getTasksForUser(String userId, OnTasksLoadedListener listener) {
        if (userId == null) {
            listener.onError("User ID is null");
            return;
        }

        // Sử dụng chỉ WHERE clause, không ORDER BY để tránh cần index
        db.collection(TASKS_COLLECTION)
                .whereEqualTo("assignedToUserId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting user tasks", error);
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> tasks = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> task = doc.getData();
                            task.put("id", doc.getId());
                            tasks.add(task);
                        }

                        // Sort in memory để tránh cần index - sử dụng Collections.sort()
                        Collections.sort(tasks, new Comparator<Map<String, Object>>() {
                            @Override
                            public int compare(Map<String, Object> task1, Map<String, Object> task2) {
                                Object date1 = task1.get("createdAt");
                                Object date2 = task2.get("createdAt");

                                if (date1 == null && date2 == null) return 0;
                                if (date1 == null) return 1;
                                if (date2 == null) return -1;

                                try {
                                    Date d1 = (Date) date1;
                                    Date d2 = (Date) date2;
                                    return d2.compareTo(d1); // DESC order
                                } catch (Exception e) {
                                    return 0;
                                }
                            }
                        });
                    }
                    Log.d(TAG, "Loaded " + tasks.size() + " tasks for user " + userId);
                    listener.onTasksLoaded(tasks);
                });
    }

    /**
     * Lấy tasks được tạo bởi một user cụ thể
     */
    public void getTasksCreatedByUser(String userId, OnTasksLoadedListener listener) {
        if (userId == null) {
            listener.onError("User ID is null");
            return;
        }

        db.collection(TASKS_COLLECTION)
                .whereEqualTo("assignerUserId", userId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting created tasks", error);
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> tasks = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> task = doc.getData();
                            task.put("id", doc.getId());
                            tasks.add(task);
                        }

                        // Sort in memory - sử dụng Collections.sort()
                        Collections.sort(tasks, new Comparator<Map<String, Object>>() {
                            @Override
                            public int compare(Map<String, Object> task1, Map<String, Object> task2) {
                                Object date1 = task1.get("createdAt");
                                Object date2 = task2.get("createdAt");

                                if (date1 == null && date2 == null) return 0;
                                if (date1 == null) return 1;
                                if (date2 == null) return -1;

                                try {
                                    Date d1 = (Date) date1;
                                    Date d2 = (Date) date2;
                                    return d2.compareTo(d1); // DESC order
                                } catch (Exception e) {
                                    return 0;
                                }
                            }
                        });
                    }
                    Log.d(TAG, "Loaded " + tasks.size() + " tasks created by user " + userId);
                    listener.onTasksLoaded(tasks);
                });
    }

    /**
     * Thêm task mới
     */
    public void addTask(Task task, OnTaskAddedListener listener) {
        // Ensure timestamps are set
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(new Date());
        }
        task.setUpdatedAt(new Date());

        Map<String, Object> taskData = task.toMap();
        Log.d(TAG, "Adding task: " + task.getTitle());

        db.collection(TASKS_COLLECTION)
                .add(taskData)
                .addOnSuccessListener(documentReference -> {
                    String taskId = documentReference.getId();
                    Log.d(TAG, "Task added with ID: " + taskId);
                    listener.onTaskAdded(taskId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding task", e);
                    listener.onError("Lỗi khi thêm task: " + e.getMessage());
                });
    }

    /**
     * Cập nhật status của task
     */
    public void updateTaskStatus(String taskId, String status, OnTaskUpdatedListener listener) {
        if (taskId == null || taskId.isEmpty()) {
            listener.onError("Task ID is null or empty");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        updates.put("updatedAt", new Date());

        Log.d(TAG, "Updating task status: " + taskId + " -> " + status);

        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task status updated successfully");
                    listener.onTaskUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task status", e);
                    listener.onError("Lỗi khi cập nhật trạng thái: " + e.getMessage());
                });
    }

    /**
     * Cập nhật task hoàn toàn
     */
    public void updateTask(Task task, OnTaskUpdatedListener listener) {
        if (task.getId() == null || task.getId().isEmpty()) {
            listener.onError("Task ID is null or empty");
            return;
        }

        task.setUpdatedAt(new Date());
        Map<String, Object> taskData = task.toMap();

        Log.d(TAG, "Updating task: " + task.getId());

        db.collection(TASKS_COLLECTION)
                .document(task.getId())
                .set(taskData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task updated successfully");
                    listener.onTaskUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task", e);
                    listener.onError("Lỗi khi cập nhật task: " + e.getMessage());
                });
    }

    /**
     * Reassign task to another user
     */
    public void reassignTask(String taskId, String newAssigneeId, String newAssigneeName,
                             OnTaskUpdatedListener listener) {
        if (taskId == null || taskId.isEmpty()) {
            listener.onError("Task ID is null or empty");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("assignedToUserId", newAssigneeId);
        updates.put("assignedToName", newAssigneeName);
        updates.put("status", "pending"); // Reset status when reassigning
        updates.put("updatedAt", new Date());

        Log.d(TAG, "Reassigning task: " + taskId + " to " + newAssigneeName);

        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task reassigned successfully");
                    listener.onTaskUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error reassigning task", e);
                    listener.onError("Lỗi khi phân việc lại: " + e.getMessage());
                });
    }

    /**
     * Xóa task
     */
    public void deleteTask(String taskId, OnTaskDeletedListener listener) {
        if (taskId == null || taskId.isEmpty()) {
            listener.onError("Task ID is null or empty");
            return;
        }

        Log.d(TAG, "Deleting task: " + taskId);

        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task deleted successfully");
                    listener.onTaskDeleted();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting task", e);
                    listener.onError("Lỗi khi xóa task: " + e.getMessage());
                });
    }

    /**
     * Xóa task với kiểm tra quyền
     */
    public void deleteTaskWithPermission(String taskId, String currentUserId, OnTaskDeletedListener listener) {
        if (taskId == null || taskId.isEmpty()) {
            listener.onError("Task ID is null or empty");
            return;
        }

        if (currentUserId == null || currentUserId.isEmpty()) {
            listener.onError("Current user ID is required");
            return;
        }

        // Kiểm tra task có tồn tại và user có quyền xóa không
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        listener.onError("Task không tồn tại");
                        return;
                    }

                    // Kiểm tra quyền
                    String assignerUserId = documentSnapshot.getString("assignerUserId");
                    if (!currentUserId.equals(assignerUserId)) {
                        listener.onError("Chỉ người tạo task mới có quyền xóa");
                        return;
                    }

                    // Xóa task
                    deleteTask(taskId, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking task permissions", e);
                    listener.onError("Lỗi khi kiểm tra quyền: " + e.getMessage());
                });
    }

    /**
     * Lấy task theo ID
     */
    public void getTaskById(String taskId, OnTaskLoadedListener listener) {
        if (taskId == null || taskId.isEmpty()) {
            listener.onError("Task ID is null or empty");
            return;
        }

        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> taskData = documentSnapshot.getData();
                        taskData.put("id", documentSnapshot.getId());
                        Task task = Task.fromMap(taskData);
                        listener.onTaskLoaded(task);
                    } else {
                        listener.onError("Task not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting task", e);
                    listener.onError("Lỗi khi tải task: " + e.getMessage());
                });
    }

    /**
     * Lấy tasks theo trạng thái
     */
    public void getTasksByStatus(String status, OnTasksLoadedListener listener) {
        db.collection(TASKS_COLLECTION)
                .whereEqualTo("status", status)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting tasks by status", error);
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> tasks = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> task = doc.getData();
                            task.put("id", doc.getId());
                            tasks.add(task);
                        }

                        // Sort in memory - sử dụng Collections.sort()
                        Collections.sort(tasks, new Comparator<Map<String, Object>>() {
                            @Override
                            public int compare(Map<String, Object> task1, Map<String, Object> task2) {
                                Object date1 = task1.get("createdAt");
                                Object date2 = task2.get("createdAt");

                                if (date1 == null && date2 == null) return 0;
                                if (date1 == null) return 1;
                                if (date2 == null) return -1;

                                try {
                                    Date d1 = (Date) date1;
                                    Date d2 = (Date) date2;
                                    return d2.compareTo(d1); // DESC order
                                } catch (Exception e) {
                                    return 0;
                                }
                            }
                        });
                    }
                    Log.d(TAG, "Loaded " + tasks.size() + " tasks with status " + status);
                    listener.onTasksLoaded(tasks);
                });
    }

    /**
     * Lấy tasks theo mức độ ưu tiên
     */
    public void getTasksByPriority(String priority, OnTasksLoadedListener listener) {
        db.collection(TASKS_COLLECTION)
                .whereEqualTo("priority", priority)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting tasks by priority", error);
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> tasks = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> task = doc.getData();
                            task.put("id", doc.getId());
                            tasks.add(task);
                        }

                        // Sort in memory - sử dụng Collections.sort()
                        Collections.sort(tasks, new Comparator<Map<String, Object>>() {
                            @Override
                            public int compare(Map<String, Object> task1, Map<String, Object> task2) {
                                Object date1 = task1.get("createdAt");
                                Object date2 = task2.get("createdAt");

                                if (date1 == null && date2 == null) return 0;
                                if (date1 == null) return 1;
                                if (date2 == null) return -1;

                                try {
                                    Date d1 = (Date) date1;
                                    Date d2 = (Date) date2;
                                    return d2.compareTo(d1); // DESC order
                                } catch (Exception e) {
                                    return 0;
                                }
                            }
                        });
                    }
                    Log.d(TAG, "Loaded " + tasks.size() + " tasks with priority " + priority);
                    listener.onTasksLoaded(tasks);
                });
    }

    /**
     * Thêm comment vào task
     */
    public void addTaskComment(String taskId, String comment, String authorId, String authorName,
                               OnTaskUpdatedListener listener) {
        if (taskId == null || taskId.isEmpty()) {
            listener.onError("Task ID is null or empty");
            return;
        }

        // Tạo comment object
        Map<String, Object> commentData = new HashMap<>();
        commentData.put("comment", comment);
        commentData.put("authorId", authorId);
        commentData.put("authorName", authorName);
        commentData.put("timestamp", new Date());

        // Thêm comment vào array trong task document
        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(commentData))
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Comment added to task: " + taskId);
                    listener.onTaskUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error adding comment", e);
                    listener.onError("Lỗi khi thêm comment: " + e.getMessage());
                });
    }

    /**
     * Cập nhật attachment cho task
     */
    public void updateTaskAttachment(String taskId, String attachmentUrl, String attachmentName,
                                     OnTaskUpdatedListener listener) {
        if (taskId == null || taskId.isEmpty()) {
            listener.onError("Task ID is null or empty");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("attachmentUrl", attachmentUrl);
        updates.put("attachmentName", attachmentName);
        updates.put("updatedAt", new Date());

        Log.d(TAG, "Updating task attachment: " + taskId);

        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task attachment updated successfully");
                    listener.onTaskUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task attachment", e);
                    listener.onError("Lỗi khi cập nhật tệp đính kèm: " + e.getMessage());
                });
    }

    /**
     * Lấy tasks theo khoảng thời gian
     */
    public void getTasksByDateRange(Date startDate, Date endDate, OnTasksLoadedListener listener) {
        db.collection(TASKS_COLLECTION)
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .whereLessThanOrEqualTo("createdAt", endDate)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting tasks by date range", error);
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> tasks = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> task = doc.getData();
                            task.put("id", doc.getId());
                            tasks.add(task);
                        }

                        // Sort in memory - sử dụng Collections.sort()
                        Collections.sort(tasks, new Comparator<Map<String, Object>>() {
                            @Override
                            public int compare(Map<String, Object> task1, Map<String, Object> task2) {
                                Object date1 = task1.get("createdAt");
                                Object date2 = task2.get("createdAt");

                                if (date1 == null && date2 == null) return 0;
                                if (date1 == null) return 1;
                                if (date2 == null) return -1;

                                try {
                                    Date d1 = (Date) date1;
                                    Date d2 = (Date) date2;
                                    return d2.compareTo(d1); // DESC order
                                } catch (Exception e) {
                                    return 0;
                                }
                            }
                        });
                    }
                    Log.d(TAG, "Loaded " + tasks.size() + " tasks within date range");
                    listener.onTasksLoaded(tasks);
                });
    }

    /**
     * Cập nhật due date của task
     */
    public void updateTaskDueDate(String taskId, Date newDueDate, OnTaskUpdatedListener listener) {
        if (taskId == null || taskId.isEmpty()) {
            listener.onError("Task ID is null or empty");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("dueDate", newDueDate);
        updates.put("updatedAt", new Date());

        Log.d(TAG, "Updating task due date: " + taskId);

        db.collection(TASKS_COLLECTION)
                .document(taskId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Task due date updated successfully");
                    listener.onTaskUpdated();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating task due date", e);
                    listener.onError("Lỗi khi cập nhật hạn chót: " + e.getMessage());
                });
    }

    /**
     * Lấy tasks sắp hết hạn (trong vòng n ngày)
     */
    public void getTasksDueSoon(int daysFromNow, OnTasksLoadedListener listener) {
        Date now = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(now);
        calendar.add(Calendar.DAY_OF_MONTH, daysFromNow);
        Date futureDate = calendar.getTime();

        // Fixed: Use 'dueDate' field instead of compound query that requires index
        db.collection(TASKS_COLLECTION)
                .whereGreaterThanOrEqualTo("dueDate", now)
                .whereLessThanOrEqualTo("dueDate", futureDate)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error getting tasks due soon", error);
                        listener.onError(error.getMessage());
                        return;
                    }

                    List<Map<String, Object>> tasks = new ArrayList<>();
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Map<String, Object> task = doc.getData();
                            // Filter non-completed tasks in memory to avoid complex query
                            String status = (String) task.get("status");
                            if (!"completed".equals(status)) {
                                task.put("id", doc.getId());
                                tasks.add(task);
                            }
                        }

                        // Sort in memory - sử dụng Collections.sort()
                        Collections.sort(tasks, new Comparator<Map<String, Object>>() {
                            @Override
                            public int compare(Map<String, Object> task1, Map<String, Object> task2) {
                                Object date1 = task1.get("dueDate");
                                Object date2 = task2.get("dueDate");

                                if (date1 == null && date2 == null) return 0;
                                if (date1 == null) return 1;
                                if (date2 == null) return -1;

                                try {
                                    Date d1 = (Date) date1;
                                    Date d2 = (Date) date2;
                                    return d1.compareTo(d2); // ASC order for due dates
                                } catch (Exception e) {
                                    return 0;
                                }
                            }
                        });
                    }
                    Log.d(TAG, "Loaded " + tasks.size() + " tasks due soon");
                    listener.onTasksLoaded(tasks);
                });
    }
}