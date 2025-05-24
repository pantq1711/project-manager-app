package com.example.projectmanager.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectmanager.R;
import com.example.projectmanager.models.User;
import com.example.projectmanager.services.UserSearchService;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog hiển thị danh sách người dùng với tìm kiếm và phân trang
 */
public class UserSelectionDialog extends Dialog {
    private static final String TAG = "UserSelectionDialog";
    private static final int PAGE_SIZE = 10; // Số lượng người dùng hiển thị mỗi trang

    // Di chuyển interface ra ngoài class UserAdapter
    interface OnUserClickListener {
        void onUserClick(UserSearchService.UserInfo user);
    }

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private EditText etSearch;
    private Button btnLoadMore;
    private ProgressBar progressBar;
    private TextView tvNoResults;

    private List<UserSearchService.UserInfo> allUsers = new ArrayList<>();
    private List<UserSearchService.UserInfo> filteredUsers = new ArrayList<>();
    private List<UserSearchService.UserInfo> displayedUsers = new ArrayList<>();

    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreData = true;

    private OnUserSelectedListener listener;

    public interface OnUserSelectedListener {
        void onUserSelected(String userId, String userName, String userEmail);
    }

    public UserSelectionDialog(@NonNull Context context, OnUserSelectedListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_user_selection);

        // Thiết lập dialog full width
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        initViews();
        setupRecyclerView();
        setupSearchBox();

        // Load danh sách người dùng ban đầu
        loadUsers();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.rv_users);
        etSearch = findViewById(R.id.et_search);
        btnLoadMore = findViewById(R.id.btn_load_more);
        progressBar = findViewById(R.id.progress_bar);
        tvNoResults = findViewById(R.id.tv_no_results);

        Button btnCancel = findViewById(R.id.btn_cancel);
        btnCancel.setOnClickListener(v -> dismiss());

        btnLoadMore.setOnClickListener(v -> {
            if (hasMoreData && !isLoading) {
                loadNextPage();
            }
        });
    }

    private void setupRecyclerView() {
        adapter = new UserAdapter(displayedUsers, (user) -> {
            if (listener != null) {
                listener.onUserSelected(user.id, user.displayName, user.email);
                dismiss();
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSearchBox() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                filterUsers(s.toString());
            }
        });
    }

    /**
     * Lọc danh sách người dùng theo từ khóa tìm kiếm
     */
    private void filterUsers(String query) {
        filteredUsers.clear();
        currentPage = 0;

        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers);
        } else {
            String searchTerm = query.toLowerCase();
            for (UserSearchService.UserInfo user : allUsers) {
                if (user.displayName.toLowerCase().contains(searchTerm) ||
                        user.email.toLowerCase().contains(searchTerm)) {
                    filteredUsers.add(user);
                }
            }
        }

        updateDisplayedUsers();
    }

    /**
     * Cập nhật danh sách người dùng hiển thị dựa trên trang hiện tại
     */
    private void updateDisplayedUsers() {
        displayedUsers.clear();

        int endIndex = Math.min((currentPage + 1) * PAGE_SIZE, filteredUsers.size());
        for (int i = 0; i < endIndex; i++) {
            displayedUsers.add(filteredUsers.get(i));
        }

        adapter.notifyDataSetChanged();

        // Cập nhật trạng thái của nút "Xem thêm"
        hasMoreData = filteredUsers.size() > (currentPage + 1) * PAGE_SIZE;
        btnLoadMore.setVisibility(hasMoreData ? View.VISIBLE : View.GONE);

        // Hiển thị thông báo khi không có kết quả
        if (displayedUsers.isEmpty()) {
            tvNoResults.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            tvNoResults.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Tải trang tiếp theo
     */
    private void loadNextPage() {
        currentPage++;
        updateDisplayedUsers();
    }

    /**
     * Tải danh sách người dùng từ service
     */
    private void loadUsers() {
        isLoading = true;
        progressBar.setVisibility(View.VISIBLE);

        UserSearchService.getAllUsers(new UserSearchService.OnUsersLoadedListener() {
            @Override
            public void onUsersLoaded(List<UserSearchService.UserInfo> users) {
                allUsers.clear();
                allUsers.addAll(users);

                // Mặc định filter là tất cả người dùng
                filteredUsers.clear();
                filteredUsers.addAll(allUsers);

                // Reset lại trang
                currentPage = 0;

                // Cập nhật UI
                updateDisplayedUsers();
                progressBar.setVisibility(View.GONE);
                isLoading = false;
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading users: " + error);
                progressBar.setVisibility(View.GONE);
                tvNoResults.setText("Lỗi tải dữ liệu: " + error);
                tvNoResults.setVisibility(View.VISIBLE);
                isLoading = false;
            }
        });
    }

    /**
     * Adapter cho RecyclerView
     */
    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<UserSearchService.UserInfo> users;
        private OnUserClickListener clickListener;

        UserAdapter(List<UserSearchService.UserInfo> users, OnUserClickListener clickListener) {
            this.users = users;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_selection, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            UserSearchService.UserInfo user = users.get(position);
            holder.tvDisplayName.setText(user.displayName);
            holder.tvEmail.setText(user.email);

            // Không cần thiết lập avatar

            holder.itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onUserClick(user);
                }
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        /**
         * Lấy màu cho phân loại người dùng dựa trên ID
         * Hiện không được sử dụng khi không có avatar
         */
        private int getAvatarColorResId(String userId) {
            // Giữ lại phương thức này để sử dụng trong tương lai nếu cần
            int[] colors = {
                    R.color.colorPrimary,
                    R.color.colorAccent,
                    R.color.status_approved,
                    R.color.priority_high,
                    R.color.priority_medium
            };

            int hash = 0;
            if (userId != null) {
                for (char c : userId.toCharArray()) {
                    hash += c;
                }
            }

            return colors[Math.abs(hash) % colors.length];
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvDisplayName, tvEmail;

            UserViewHolder(View itemView) {
                super(itemView);
                tvDisplayName = itemView.findViewById(R.id.tv_display_name);
                tvEmail = itemView.findViewById(R.id.tv_email);
            }
        }
    }
}