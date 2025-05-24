package com.example.projectmanager.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectmanager.R;
import com.example.projectmanager.adapters.TaskAdapter;
import com.example.projectmanager.models.Task;
import com.example.projectmanager.services.UserSearchService;
import com.example.projectmanager.viewmodels.TaskViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TaskManagerActivity extends AppCompatActivity {
    private static final String TAG = "TaskManagerActivity";

    // Components giao diện
    private RecyclerView rvTasks;
    private EditText etTaskTitle, etTaskDescription;
    private AutoCompleteTextView etAssignedTo;
    private ImageButton btnSearchUser;
    private Button btnSelectDueDate, btnAddTask, btnRefresh, btnLoadMore;
    private TextView tvSelectedDueDate;
    private Spinner spPriority;
    private CheckBox cbMyTasksOnly;
    private ProgressBar progressBar;

    // Adapter và dữ liệu
    private TaskAdapter taskAdapter;
    private List<Map<String, Object>> taskList;
    private List<Map<String, Object>> allTaskList;
    private ArrayAdapter<String> userAdapter;
    private List<UserSearchService.UserInfo> allUsers;

    // ViewModel
    private TaskViewModel taskViewModel;

    // User selection và due date
    private String selectedUserId = null;
    private String selectedUserName = null;
    private Date selectedDueDate = null;
    private SimpleDateFormat dateFormat;

    // Pagination controls
    private boolean isPaginationEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task_manager);

        Log.d(TAG, "TaskManagerActivity được khởi tạo");

        // Khởi tạo date format
        dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        // Set default due date (7 days from now)
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        selectedDueDate = calendar.getTime();

        // Khởi tạo ViewModel
        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        // Khởi tạo giao diện
        initViews();
        setupRecyclerView();
        setupSpinner();
        setupUserAutoComplete();
        setupClickListeners();
        observeViewModel();

        // Load dữ liệu
        loadUsers();

        // Enable pagination for better performance
        enablePagination();
    }

    private void initViews() {
        rvTasks = findViewById(R.id.rv_tasks);
        etTaskTitle = findViewById(R.id.et_task_title);
        etTaskDescription = findViewById(R.id.et_task_description);
        etAssignedTo = findViewById(R.id.et_assigned_to);
        btnSearchUser = findViewById(R.id.btn_search_user);
        btnSelectDueDate = findViewById(R.id.btn_select_due_date);
        tvSelectedDueDate = findViewById(R.id.tv_selected_due_date);
        spPriority = findViewById(R.id.sp_priority);
        btnAddTask = findViewById(R.id.btn_add_task);
        btnRefresh = findViewById(R.id.btn_refresh);
        btnLoadMore = findViewById(R.id.btn_load_more); // Nút "Xem thêm"
        cbMyTasksOnly = findViewById(R.id.cb_my_tasks_only);
        progressBar = findViewById(R.id.progress_bar);

        // Thiết lập title cho Action Bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý Nhiệm vụ");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Update due date display
        updateDueDateDisplay();

        Log.d(TAG, "Khởi tạo giao diện thành công");
    }

    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        allTaskList = new ArrayList<>();

        // Tạo listener đơn giản
        taskAdapter = new TaskAdapter(taskList, new TaskAdapter.OnTaskClickListener() {
            public void onTaskClick(Map<String, Object> task) {
                handleTaskClick(task);
            }
        });

        rvTasks.setLayoutManager(new LinearLayoutManager(this));
        rvTasks.setAdapter(taskAdapter);

        Log.d(TAG, "RecyclerView được thiết lập");
    }

    private void setupSpinner() {
        String[] priorities = {"Thấp", "Trung bình", "Cao"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, priorities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spPriority.setAdapter(adapter);
        spPriority.setSelection(1); // Mặc định "Trung bình"

        Log.d(TAG, "Spinner được thiết lập");
    }

    private void setupUserAutoComplete() {
        allUsers = new ArrayList<>();
        List<String> userNames = new ArrayList<>();

        userAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, userNames) {
            public CharSequence convertResultToString(Object resultValue) {
                return resultValue.toString();
            }
        };

        etAssignedTo.setAdapter(userAdapter);
        etAssignedTo.setThreshold(1);

        etAssignedTo.setOnItemClickListener((parent, view, position, id) -> {
            String selectedUserText = (String) parent.getItemAtPosition(position);
            for (UserSearchService.UserInfo user : allUsers) {
                if (user.getDisplayText().equals(selectedUserText)) {
                    selectedUserId = user.id;
                    selectedUserName = user.displayName;
                    Log.d(TAG, "Selected user: " + selectedUserName + " (ID: " + selectedUserId + ")");
                    break;
                }
            }
        });
    }

    private void setupClickListeners() {
        btnAddTask.setOnClickListener(v -> {
            Log.d(TAG, "Người dùng click Thêm nhiệm vụ");
            addNewTask();
        });

        btnRefresh.setOnClickListener(v -> {
            Log.d(TAG, "Người dùng click Refresh");
            taskViewModel.refreshTasks();
            loadUsers();
            Toast.makeText(TaskManagerActivity.this, "Đang làm mới dữ liệu...", Toast.LENGTH_SHORT).show();
        });

        btnLoadMore.setOnClickListener(v -> {
            Log.d(TAG, "Người dùng click Load More");
            taskViewModel.loadMoreTasks();
        });

        btnSearchUser.setOnClickListener(v -> {
            Log.d(TAG, "Người dùng click Search User");
            showUserSearchDialog();
        });

        btnSelectDueDate.setOnClickListener(v -> {
            Log.d(TAG, "Người dùng click Select Due Date");
            showDatePickerDialog();
        });

        cbMyTasksOnly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            taskViewModel.setMyTasksFilter(isChecked);
        });
    }

    private void enablePagination() {
        isPaginationEnabled = true;
        taskViewModel.enablePagination();
        btnLoadMore.setVisibility(View.VISIBLE);
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDueDate != null) {
            calendar.setTime(selectedDueDate);
        }

        // Tạo DatePickerDialog với custom theme rõ ràng
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                R.style.AppDatePickerTheme,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    selectedDueDate = selectedCalendar.getTime();
                    updateDueDateDisplay();
                    Log.d(TAG, "Selected due date: " + dateFormat.format(selectedDueDate));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        // Không cho chọn ngày trong quá khứ
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);

        // Tùy chỉnh title và button
        datePickerDialog.setTitle("Chọn hạn chót");

        // Customize button text
        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "Chọn", datePickerDialog);
        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "Hủy", datePickerDialog);

        datePickerDialog.show();

        // Customize button colors after show
        datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
                .setTextColor(getResources().getColor(R.color.colorPrimary));
        datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)
                .setTextColor(getResources().getColor(R.color.text_secondary));
    }

    private void updateDueDateDisplay() {
        if (selectedDueDate != null) {
            tvSelectedDueDate.setText("Hạn chót: " + dateFormat.format(selectedDueDate));
            tvSelectedDueDate.setVisibility(View.VISIBLE);
        } else {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 7);
            tvSelectedDueDate.setText("Hạn chót: " + dateFormat.format(calendar.getTime()));
            tvSelectedDueDate.setVisibility(View.VISIBLE);
        }
    }

    private void observeViewModel() {
        taskViewModel.getTasks().observe(this, tasks -> {
            if (isPaginationEnabled) {
                // In pagination mode, tasks are added progressively
                taskList.clear();
                taskList.addAll(tasks);
                taskAdapter.notifyDataSetChanged();
            } else {
                // In normal mode, replace all tasks
                allTaskList.clear();
                allTaskList.addAll(tasks);
                filterTasks(cbMyTasksOnly.isChecked());
            }

            if (getSupportActionBar() != null) {
                int totalTasks = isPaginationEnabled ? tasks.size() : allTaskList.size();
                int myTasksCount = isPaginationEnabled ? getMyTasksCountFromList(tasks) : getMyTasksCount();
                getSupportActionBar().setSubtitle(totalTasks + " nhiệm vụ" +
                        (myTasksCount > 0 ? " (" + myTasksCount + " của tôi)" : ""));
            }
        });

        // Observe pagination-specific states
        taskViewModel.getHasMoreTasks().observe(this, hasMore -> {
            if (isPaginationEnabled) {
                btnLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
                btnLoadMore.setEnabled(hasMore);
                if (hasMore) {
                    btnLoadMore.setText("Xem thêm");
                } else {
                    btnLoadMore.setText("Đã hết");
                    btnLoadMore.setEnabled(false);
                }
            }
        });

        taskViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(TaskManagerActivity.this, "Lỗi: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });

        taskViewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            // Disable load more button while loading
            if (isPaginationEnabled && btnLoadMore != null) {
                btnLoadMore.setEnabled(!isLoading && Boolean.TRUE.equals(taskViewModel.getHasMoreTasks().getValue()));
            }
        });
    }

    private void loadUsers() {
        Log.d(TAG, "Bắt đầu tải danh sách người dùng");

        UserSearchService.getAllUsers(new UserSearchService.OnUsersLoadedListener() {
            @Override
            public void onUsersLoaded(List<UserSearchService.UserInfo> users) {
                Log.d(TAG, "Loaded " + users.size() + " users");

                allUsers.clear();
                allUsers.addAll(users);

                List<String> userDisplayNames = new ArrayList<>();
                for (UserSearchService.UserInfo user : users) {
                    userDisplayNames.add(user.getDisplayText());
                }

                userAdapter.clear();
                userAdapter.addAll(userDisplayNames);
                userAdapter.notifyDataSetChanged();

                Log.d(TAG, "User autocomplete updated with " + userDisplayNames.size() + " entries");
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading users: " + error);
                Toast.makeText(TaskManagerActivity.this, "Lỗi tải danh sách người dùng: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addNewTask() {
        try {
            String title = etTaskTitle.getText().toString().trim();
            String description = etTaskDescription.getText().toString().trim();
            String assignedToText = etAssignedTo.getText().toString().trim();
            String priority = getSelectedPriority();

            if (title.isEmpty()) {
                etTaskTitle.setError("Vui lòng nhập tiêu đề nhiệm vụ");
                etTaskTitle.requestFocus();
                return;
            }

            if (assignedToText.isEmpty()) {
                etAssignedTo.setError("Vui lòng chọn người được giao");
                etAssignedTo.requestFocus();
                return;
            }

            if (selectedDueDate == null) {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, 7);
                selectedDueDate = calendar.getTime();
            }

            if (selectedUserId == null || !selectedUserName.equals(assignedToText)) {
                findAndAssignUser(assignedToText, title, description, priority);
            } else {
                createAndSaveTask(title, description, selectedUserId, selectedUserName, priority);
            }

        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi thêm nhiệm vụ", e);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void findAndAssignUser(String userText, String title, String description, String priority) {
        for (UserSearchService.UserInfo user : allUsers) {
            if (user.getDisplayText().equals(userText) ||
                    user.displayName.equals(userText) ||
                    user.email.equals(userText)) {
                createAndSaveTask(title, description, user.id, user.displayName, priority);
                return;
            }
        }

        UserSearchService.findUserByName(userText, new UserSearchService.OnUserFoundListener() {
            @Override
            public void onUserFound(String userId, String userName, String userEmail) {
                createAndSaveTask(title, description, userId, userName, priority);
            }

            @Override
            public void onUserNotFound() {
                etAssignedTo.setError("Không tìm thấy người dùng này");
                etAssignedTo.requestFocus();
                Toast.makeText(TaskManagerActivity.this,
                        "Không tìm thấy người dùng. Vui lòng chọn từ danh sách gợi ý.",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(TaskManagerActivity.this, "Lỗi tìm kiếm: " + error,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void createAndSaveTask(String title, String description, String assignedToUserId,
                                   String assignedToName, String priority) {
        Task task = new Task(title, description, assignedToUserId, assignedToName,
                selectedDueDate, priority);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            task.setAssignerUserId(currentUser.getUid());
            task.setAssignerName(currentUser.getDisplayName() != null ?
                    currentUser.getDisplayName() : currentUser.getEmail());
        }

        Log.d(TAG, "Tạo nhiệm vụ mới: " + title + " cho " + assignedToName +
                " với hạn " + dateFormat.format(selectedDueDate));

        taskViewModel.addTask(task);
        clearForm();
    }

    private void showUserSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn người được giao việc");

        String[] userNames = new String[allUsers.size()];
        for (int i = 0; i < allUsers.size(); i++) {
            userNames[i] = allUsers.get(i).displayName + " (" + allUsers.get(i).email + ")";
        }

        builder.setItems(userNames, (dialog, which) -> {
            UserSearchService.UserInfo selectedUser = allUsers.get(which);
            selectedUserId = selectedUser.id;
            selectedUserName = selectedUser.displayName;
            etAssignedTo.setText(selectedUser.displayName);

            Log.d(TAG, "User selected from dialog: " + selectedUserName);
        });

        builder.setNegativeButton("Hủy", null);
        builder.create().show();
    }

    private void filterTasks(boolean showOnlyMyTasks) {
        if (isPaginationEnabled) {
            // In pagination mode, filtering is handled by the ViewModel
            return;
        }

        taskList.clear();

        if (showOnlyMyTasks) {
            String currentUserId = getCurrentUserId();
            for (Map<String, Object> task : allTaskList) {
                String assignedToUserId = (String) task.get("assignedToUserId");
                if (currentUserId != null && currentUserId.equals(assignedToUserId)) {
                    taskList.add(task);
                }
            }
        } else {
            taskList.addAll(allTaskList);
        }

        taskAdapter.notifyDataSetChanged();
        Log.d(TAG, "Filtered tasks: " + taskList.size() + "/" + allTaskList.size());
    }

    private int getMyTasksCount() {
        String currentUserId = getCurrentUserId();
        int count = 0;
        for (Map<String, Object> task : allTaskList) {
            String assignedToUserId = (String) task.get("assignedToUserId");
            if (currentUserId != null && currentUserId.equals(assignedToUserId)) {
                count++;
            }
        }
        return count;
    }

    private int getMyTasksCountFromList(List<Map<String, Object>> tasks) {
        String currentUserId = getCurrentUserId();
        int count = 0;
        for (Map<String, Object> task : tasks) {
            String assignedToUserId = (String) task.get("assignedToUserId");
            if (currentUserId != null && currentUserId.equals(assignedToUserId)) {
                count++;
            }
        }
        return count;
    }

    private String getCurrentUserId() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getUid() : null;
    }

    private String getSelectedPriority() {
        int position = spPriority.getSelectedItemPosition();
        switch (position) {
            case 0: return "low";
            case 1: return "medium";
            case 2: return "high";
            default: return "medium";
        }
    }

    private void clearForm() {
        etTaskTitle.setText("");
        etTaskDescription.setText("");
        etAssignedTo.setText("");
        spPriority.setSelection(1);

        selectedUserId = null;
        selectedUserName = null;

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 7);
        selectedDueDate = calendar.getTime();
        updateDueDateDisplay();

        Log.d(TAG, "Form được xóa");
    }

    // Phương thức xử lý click task - KHÔNG CÓ @Override
    private void handleTaskClick(Map<String, Object> task) {
        try {
            // Kiểm tra xem có phải action delete không
            Object actionObj = task.get("action");
            if ("DELETE_TASK".equals(actionObj)) {
                String taskId = (String) task.get("taskId");
                Map<String, Object> originalTask = (Map<String, Object>) task.get("originalTask");

                Log.d(TAG, "Delete task request: " + taskId);
                taskViewModel.deleteTask(taskId);
                return;
            }

            // Xử lý click thường để update status
            String taskId = (String) task.get("id");
            String assignedToUserId = (String) task.get("assignedToUserId");
            String currentUserId = getCurrentUserId();

            if (currentUserId == null || !currentUserId.equals(assignedToUserId)) {
                Toast.makeText(this, "Chỉ người được phân việc mới có thể cập nhật trạng thái",
                        Toast.LENGTH_SHORT).show();
                return;
            }

            String currentStatus = (String) task.get("status");
            String newStatus = getNextStatus(currentStatus);

            Log.d(TAG, "Cập nhật trạng thái task " + taskId + " từ " + currentStatus + " sang " + newStatus);
            taskViewModel.updateTaskStatus(taskId, newStatus);

        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi xử lý click task", e);
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getNextStatus(String currentStatus) {
        switch (currentStatus) {
            case "pending": return "in_progress";
            case "in_progress": return "completed";
            case "completed": return "pending";
            default: return "pending";
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadUsers();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Handle file picker result if needed
    }
}