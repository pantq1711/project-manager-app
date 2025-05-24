package com.example.projectmanager.viewmodels;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.projectmanager.models.Task;
import com.example.projectmanager.repositories.TaskRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskViewModel extends ViewModel {
    private static final String TAG = "TaskViewModel";

    private TaskRepository taskRepository;
    private FirebaseAuth firebaseAuth;

    // LiveData for tasks
    private MutableLiveData<List<Map<String, Object>>> tasks = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private MutableLiveData<Boolean> hasMoreTasks = new MutableLiveData<>();

    // Pagination state
    private DocumentSnapshot lastTaskDocument = null;
    private boolean isPaginationMode = false;
    private String currentUserId = null; // For filtering tasks

    public TaskViewModel() {
        taskRepository = new TaskRepository();
        firebaseAuth = FirebaseAuth.getInstance();
        loadTasks();
    }

    // Getters for LiveData
    public LiveData<List<Map<String, Object>>> getTasks() {
        return tasks;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<Boolean> getHasMoreTasks() {
        return hasMoreTasks;
    }

    // Enable pagination mode
    public void enablePagination() {
        isPaginationMode = true;
        hasMoreTasks.setValue(false);
        lastTaskDocument = null;
        loadTasksWithPagination();
    }

    // Load tasks with pagination
    public void loadTasksWithPagination() {
        Log.d(TAG, "Loading tasks with pagination...");
        isLoading.setValue(true);

        if (currentUserId != null) {
            // Load only user's tasks with pagination
            taskRepository.getTasksForUserWithPagination(currentUserId, lastTaskDocument,
                    new TaskRepository.OnPagedTasksLoadedListener() {
                        @Override
                        public void onTasksLoaded(List<Map<String, Object>> newTasks, boolean hasMore, DocumentSnapshot lastDocument) {
                            handlePaginatedTasksLoaded(newTasks, hasMore, lastDocument);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error loading user tasks with pagination: " + error);
                            isLoading.setValue(false);
                            errorMessage.setValue(error);
                        }
                    });
        } else {
            // Load all tasks with pagination
            taskRepository.getTasksWithPagination(lastTaskDocument,
                    new TaskRepository.OnPagedTasksLoadedListener() {
                        @Override
                        public void onTasksLoaded(List<Map<String, Object>> newTasks, boolean hasMore, DocumentSnapshot lastDocument) {
                            handlePaginatedTasksLoaded(newTasks, hasMore, lastDocument);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error loading tasks with pagination: " + error);
                            isLoading.setValue(false);
                            errorMessage.setValue(error);
                        }
                    });
        }
    }

    // Load more tasks (for pagination)
    public void loadMoreTasks() {
        if (!isPaginationMode || Boolean.FALSE.equals(hasMoreTasks.getValue()) || lastTaskDocument == null) {
            return;
        }

        Log.d(TAG, "Loading more tasks...");
        isLoading.setValue(true);

        if (currentUserId != null) {
            taskRepository.getTasksForUserWithPagination(currentUserId, lastTaskDocument,
                    new TaskRepository.OnPagedTasksLoadedListener() {
                        @Override
                        public void onTasksLoaded(List<Map<String, Object>> newTasks, boolean hasMore, DocumentSnapshot lastDocument) {
                            handlePaginatedTasksLoaded(newTasks, hasMore, lastDocument);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error loading more user tasks: " + error);
                            isLoading.setValue(false);
                            errorMessage.setValue(error);
                        }
                    });
        } else {
            taskRepository.getTasksWithPagination(lastTaskDocument,
                    new TaskRepository.OnPagedTasksLoadedListener() {
                        @Override
                        public void onTasksLoaded(List<Map<String, Object>> newTasks, boolean hasMore, DocumentSnapshot lastDocument) {
                            handlePaginatedTasksLoaded(newTasks, hasMore, lastDocument);
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error loading more tasks: " + error);
                            isLoading.setValue(false);
                            errorMessage.setValue(error);
                        }
                    });
        }
    }

    private void handlePaginatedTasksLoaded(List<Map<String, Object>> newTasks, boolean hasMore, DocumentSnapshot lastDocument) {
        isLoading.setValue(false);

        List<Map<String, Object>> currentTasks = tasks.getValue();
        if (currentTasks == null) {
            currentTasks = new ArrayList<>();
        }

        if (lastTaskDocument == null) {
            // First load
            currentTasks.clear();
        }

        currentTasks.addAll(newTasks);
        tasks.setValue(currentTasks);
        hasMoreTasks.setValue(hasMore);
        lastTaskDocument = lastDocument;

        Log.d(TAG, "Tasks loaded successfully: " + newTasks.size() + " new tasks, total: " + currentTasks.size() + ", hasMore: " + hasMore);
    }

    // Set filter to show only user's tasks
    public void setMyTasksFilter(boolean showOnlyMyTasks) {
        if (showOnlyMyTasks) {
            if (firebaseAuth.getCurrentUser() != null) {
                currentUserId = firebaseAuth.getCurrentUser().getUid();
            }
        } else {
            currentUserId = null;
        }

        // Reload with filter
        if (isPaginationMode) {
            lastTaskDocument = null;
            loadTasksWithPagination();
        } else {
            loadTasks();
        }
    }

    // Load all tasks (non-pagination mode)
    public void loadTasks() {
        Log.d(TAG, "Loading tasks...");
        isLoading.setValue(true);

        taskRepository.getTasks(new TaskRepository.OnTasksLoadedListener() {
            @Override
            public void onTasksLoaded(List<Map<String, Object>> taskList) {
                Log.d(TAG, "Tasks loaded successfully: " + taskList.size() + " tasks");
                isLoading.setValue(false);
                tasks.setValue(taskList);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading tasks: " + error);
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    // Refresh tasks (reset pagination)
    public void refreshTasks() {
        if (isPaginationMode) {
            lastTaskDocument = null;
            loadTasksWithPagination();
        } else {
            loadTasks();
        }
    }

    // Load tasks for specific user
    public void loadTasksForUser(String userId) {
        Log.d(TAG, "Loading tasks for user: " + userId);
        isLoading.setValue(true);

        taskRepository.getTasksForUser(userId, new TaskRepository.OnTasksLoadedListener() {
            @Override
            public void onTasksLoaded(List<Map<String, Object>> taskList) {
                Log.d(TAG, "User tasks loaded successfully: " + taskList.size() + " tasks");
                isLoading.setValue(false);
                tasks.setValue(taskList);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading user tasks: " + error);
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    // Load tasks created by specific user
    public void loadTasksCreatedByUser(String userId) {
        Log.d(TAG, "Loading tasks created by user: " + userId);
        isLoading.setValue(true);

        taskRepository.getTasksCreatedByUser(userId, new TaskRepository.OnTasksLoadedListener() {
            @Override
            public void onTasksLoaded(List<Map<String, Object>> taskList) {
                Log.d(TAG, "Created tasks loaded successfully: " + taskList.size() + " tasks");
                isLoading.setValue(false);
                tasks.setValue(taskList);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading created tasks: " + error);
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    // Add new task
    public void addTask(Task task) {
        Log.d(TAG, "Adding new task: " + task.getTitle());
        isLoading.setValue(true);

        taskRepository.addTask(task, new TaskRepository.OnTaskAddedListener() {
            @Override
            public void onTaskAdded(String taskId) {
                Log.d(TAG, "Task added successfully with ID: " + taskId);
                isLoading.setValue(false);
                // In pagination mode, we might want to refresh to show the new task at the top
                if (isPaginationMode) {
                    refreshTasks();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error adding task: " + error);
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    // Update task status
    public void updateTaskStatus(String taskId, String status) {
        Log.d(TAG, "Updating task status: " + taskId + " -> " + status);
        isLoading.setValue(true);

        taskRepository.updateTaskStatus(taskId, status, new TaskRepository.OnTaskUpdatedListener() {
            @Override
            public void onTaskUpdated() {
                Log.d(TAG, "Task status updated successfully");
                isLoading.setValue(false);
                // No need to reload, real-time listener will update automatically
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error updating task status: " + error);
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    // Update entire task
    public void updateTask(Task task) {
        Log.d(TAG, "Updating task: " + task.getId());
        isLoading.setValue(true);

        taskRepository.updateTask(task, new TaskRepository.OnTaskUpdatedListener() {
            @Override
            public void onTaskUpdated() {
                Log.d(TAG, "Task updated successfully");
                isLoading.setValue(false);
                // No need to reload, real-time listener will update automatically
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error updating task: " + error);
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    // Reassign task to another user
    public void reassignTask(String taskId, String newAssigneeId, String newAssigneeName) {
        Log.d(TAG, "Reassigning task: " + taskId + " to " + newAssigneeName);
        isLoading.setValue(true);

        taskRepository.reassignTask(taskId, newAssigneeId, newAssigneeName,
                new TaskRepository.OnTaskUpdatedListener() {
                    @Override
                    public void onTaskUpdated() {
                        Log.d(TAG, "Task reassigned successfully");
                        isLoading.setValue(false);
                        // No need to reload, real-time listener will update automatically
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error reassigning task: " + error);
                        isLoading.setValue(false);
                        errorMessage.setValue(error);
                    }
                });
    }

    // Delete task (with permission check)
    public void deleteTask(String taskId) {
        Log.d(TAG, "Deleting task: " + taskId);
        isLoading.setValue(true);

        String currentUserId = firebaseAuth.getCurrentUser() != null ?
                firebaseAuth.getCurrentUser().getUid() : null;

        taskRepository.deleteTaskWithPermission(taskId, currentUserId,
                new TaskRepository.OnTaskDeletedListener() {
                    @Override
                    public void onTaskDeleted() {
                        Log.d(TAG, "Task deleted successfully");
                        isLoading.setValue(false);
                        // No need to reload, real-time listener will update automatically
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error deleting task: " + error);
                        isLoading.setValue(false);
                        errorMessage.setValue(error);
                    }
                });
    }

    // Get tasks by status
    public void getTasksByStatus(String status) {
        Log.d(TAG, "Loading tasks with status: " + status);
        isLoading.setValue(true);

        taskRepository.getTasksByStatus(status, new TaskRepository.OnTasksLoadedListener() {
            @Override
            public void onTasksLoaded(List<Map<String, Object>> taskList) {
                Log.d(TAG, "Tasks by status loaded successfully: " + taskList.size() + " tasks");
                isLoading.setValue(false);
                tasks.setValue(taskList);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading tasks by status: " + error);
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    // Get tasks by priority
    public void getTasksByPriority(String priority) {
        Log.d(TAG, "Loading tasks with priority: " + priority);
        isLoading.setValue(true);

        taskRepository.getTasksByPriority(priority, new TaskRepository.OnTasksLoadedListener() {
            @Override
            public void onTasksLoaded(List<Map<String, Object>> taskList) {
                Log.d(TAG, "Tasks by priority loaded successfully: " + taskList.size() + " tasks");
                isLoading.setValue(false);
                tasks.setValue(taskList);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading tasks by priority: " + error);
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    // Add comment to task
    public void addTaskComment(String taskId, String comment) {
        Log.d(TAG, "Adding comment to task: " + taskId);
        isLoading.setValue(true);

        String authorId = firebaseAuth.getCurrentUser() != null ?
                firebaseAuth.getCurrentUser().getUid() : null;
        String authorName = firebaseAuth.getCurrentUser() != null ?
                (firebaseAuth.getCurrentUser().getDisplayName() != null ?
                        firebaseAuth.getCurrentUser().getDisplayName() :
                        firebaseAuth.getCurrentUser().getEmail()) : "Unknown";

        taskRepository.addTaskComment(taskId, comment, authorId, authorName,
                new TaskRepository.OnTaskUpdatedListener() {
                    @Override
                    public void onTaskUpdated() {
                        Log.d(TAG, "Comment added successfully");
                        isLoading.setValue(false);
                        // No need to reload, real-time listener will update automatically
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error adding comment: " + error);
                        isLoading.setValue(false);
                        errorMessage.setValue(error);
                    }
                });
    }

    // Update task attachment
    public void updateTaskAttachment(String taskId, String attachmentUrl, String attachmentName) {
        Log.d(TAG, "Updating task attachment: " + taskId);
        isLoading.setValue(true);

        taskRepository.updateTaskAttachment(taskId, attachmentUrl, attachmentName,
                new TaskRepository.OnTaskUpdatedListener() {
                    @Override
                    public void onTaskUpdated() {
                        Log.d(TAG, "Task attachment updated successfully");
                        isLoading.setValue(false);
                        // No need to reload, real-time listener will update automatically
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error updating task attachment: " + error);
                        isLoading.setValue(false);
                        errorMessage.setValue(error);
                    }
                });
    }

    // Get task by ID
    public void getTaskById(String taskId, TaskRepository.OnTaskLoadedListener listener) {
        Log.d(TAG, "Loading task by ID: " + taskId);
        taskRepository.getTaskById(taskId, listener);
    }

    // Load tasks due soon (within specified days)
    public void getTasksDueSoon(int daysFromNow) {
        Log.d(TAG, "Loading tasks due within " + daysFromNow + " days");
        isLoading.setValue(true);

        taskRepository.getTasksDueSoon(daysFromNow, new TaskRepository.OnTasksLoadedListener() {
            @Override
            public void onTasksLoaded(List<Map<String, Object>> taskList) {
                Log.d(TAG, "Tasks due soon loaded successfully: " + taskList.size() + " tasks");
                isLoading.setValue(false);
                tasks.setValue(taskList);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading tasks due soon: " + error);
                isLoading.setValue(false);
                errorMessage.setValue(error);
            }
        });
    }

    // Update task due date
    public void updateTaskDueDate(String taskId, java.util.Date newDueDate) {
        Log.d(TAG, "Updating task due date: " + taskId);
        isLoading.setValue(true);

        taskRepository.updateTaskDueDate(taskId, newDueDate,
                new TaskRepository.OnTaskUpdatedListener() {
                    @Override
                    public void onTaskUpdated() {
                        Log.d(TAG, "Task due date updated successfully");
                        isLoading.setValue(false);
                        // No need to reload, real-time listener will update automatically
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "Error updating task due date: " + error);
                        isLoading.setValue(false);
                        errorMessage.setValue(error);
                    }
                });
    }

    // Clear error message
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    // Check if current user has permission to edit task
    public boolean canEditTask(Map<String, Object> task) {
        if (firebaseAuth.getCurrentUser() == null) return false;

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String assignedToUserId = (String) task.get("assignedToUserId");
        String assignerUserId = (String) task.get("assignerUserId");

        return currentUserId.equals(assignedToUserId) || currentUserId.equals(assignerUserId);
    }

    // Check if current user can update task status
    public boolean canUpdateTaskStatus(Map<String, Object> task) {
        if (firebaseAuth.getCurrentUser() == null) return false;

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String assignedToUserId = (String) task.get("assignedToUserId");

        return currentUserId.equals(assignedToUserId);
    }

    // Check if current user is the task assigner
    public boolean isTaskAssigner(Map<String, Object> task) {
        if (firebaseAuth.getCurrentUser() == null) return false;

        String currentUserId = firebaseAuth.getCurrentUser().getUid();
        String assignerUserId = (String) task.get("assignerUserId");

        return currentUserId.equals(assignerUserId);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "TaskViewModel cleared");
    }
}