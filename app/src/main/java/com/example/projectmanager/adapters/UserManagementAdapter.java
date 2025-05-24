package com.example.projectmanager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectmanager.R;
import com.example.projectmanager.models.User;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adapter để hiển thị danh sách users trong UserManagementActivity
 */
public class UserManagementAdapter extends RecyclerView.Adapter<UserManagementAdapter.UserViewHolder> {

    private List<User> userList;
    private OnUserActionListener listener;
    private SimpleDateFormat dateFormat;

    public interface OnUserActionListener {
        void onChangeRole(User user, int position);
        void onToggleActive(User user, int position);
    }

    public UserManagementAdapter(List<User> userList, OnUserActionListener listener) {
        this.userList = userList;
        this.listener = listener;
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_user_management, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        // Basic info
        holder.tvDisplayName.setText(user.getDisplayName());
        holder.tvEmail.setText(user.getEmail());

        // Role badge
        holder.tvRole.setText(getRoleDisplayName(user.getRole()));
        int roleColor = getRoleColor(user.getRole());
        holder.tvRole.setBackgroundColor(roleColor);

        // Status
        if (user.isActive()) {
            holder.ivStatus.setImageResource(R.drawable.ic_check_circle);
            holder.ivStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_approved));
            holder.tvStatus.setText("Hoạt động");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_approved));
        } else {
            holder.ivStatus.setImageResource(R.drawable .ic_cancel);
            holder.ivStatus.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_rejected));
            holder.tvStatus.setText("Tạm khóa");
            holder.tvStatus.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.status_rejected));
        }

        // Last login
        if (user.getLastLogin() != null) {
            holder.tvLastLogin.setText("Đăng nhập lần cuối: " + dateFormat.format(user.getLastLogin()));
        } else {
            holder.tvLastLogin.setText("Chưa từng đăng nhập");
        }

        // Created date
        if (user.getCreatedAt() != null) {
            holder.tvCreatedAt.setText("Tạo tài khoản: " + dateFormat.format(user.getCreatedAt()));
        }

        // Click listeners
        holder.tvRole.setOnClickListener(v -> {
            if (listener != null) {
                listener.onChangeRole(user, position);
            }
        });

        holder.ivStatus.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggleActive(user, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onChangeRole(user, position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return userList != null ? userList.size() : 0;
    }

    private String getRoleDisplayName(User.Role role) {
        switch (role) {
            case ADMIN: return "Admin";
            case MANAGER: return "Manager";
            case MEMBER: return "Member";
            default: return "Unknown";
        }
    }

    private int getRoleColor(User.Role role) {
        switch (role) {
            case ADMIN: return 0xFFE57373; // Light red
            case MANAGER: return 0xFF81C784; // Light green
            case MEMBER: return 0xFF81D4FA; // Light blue
            default: return 0xFFE0E0E0; // Gray
        }
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvDisplayName, tvEmail, tvRole, tvStatus, tvLastLogin, tvCreatedAt;
        ImageView ivStatus;

        UserViewHolder(View itemView) {
            super(itemView);
            tvDisplayName = itemView.findViewById(R.id.tv_display_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvRole = itemView.findViewById(R.id.tv_role);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvLastLogin = itemView.findViewById(R.id.tv_last_login);
            tvCreatedAt = itemView.findViewById(R.id.tv_created_at);
            ivStatus = itemView.findViewById(R.id.iv_status);
        }
    }
}