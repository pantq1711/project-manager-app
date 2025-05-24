package com.example.projectmanager.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projectmanager.R;
import com.example.projectmanager.models.Budget;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adapter cho danh sách ngân sách với giao diện được cải thiện
 */
public class BudgetAdapter extends RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder> {
    private List<Budget> budgetList;
    private Context context;
    private OnBudgetClickListener onBudgetClickListener;
    private DecimalFormat decimalFormat;
    private SimpleDateFormat dateFormat;

    public interface OnBudgetClickListener {
        void onBudgetClick(Budget budget, int position);
        void onBudgetLongClick(Budget budget, int position);
    }

    public BudgetAdapter(List<Budget> budgetList, OnBudgetClickListener listener) {
        this.budgetList = budgetList;
        this.onBudgetClickListener = listener;
        this.decimalFormat = new DecimalFormat("#,###,### VNĐ");
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    }

    @NonNull
    @Override
    public BudgetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_budget, parent, false);
        return new BudgetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BudgetViewHolder holder, int position) {
        Budget budget = budgetList.get(position);

        if (budget == null) return;

        // Set basic information
        holder.tvTitle.setText(budget.getTitle() != null ? budget.getTitle() : "");
        holder.tvCategory.setText(getCategoryDisplayName(budget.getCategory()));
        holder.tvDescription.setText(budget.getDescription() != null ? budget.getDescription() : "");
        holder.tvAmount.setText(formatAmount(budget.getAmount()));

        // Set date
        String formattedDate = formatDate(budget.getCreatedAt());
        holder.tvDate.setText(formattedDate);

        // Set status with colors and icons
        if (budget.isApproved()) {
            setApprovedStatus(holder);
        } else {
            setPendingStatus(holder);
        }

        // Set category background color
        setCategoryBackground(holder.tvCategory, budget.getCategory());

        // Set amount background based on value
        setAmountBackground(holder.tvAmount, budget.getAmount());

        // Set click listeners
        holder.itemView.setOnClickListener(v -> {
            if (onBudgetClickListener != null) {
                onBudgetClickListener.onBudgetClick(budget, position);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onBudgetClickListener != null) {
                onBudgetClickListener.onBudgetLongClick(budget, position);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return budgetList != null ? budgetList.size() : 0;
    }

    /**
     * Update data in adapter
     */
    public void updateData(List<Budget> newBudgetList) {
        this.budgetList = newBudgetList;
        notifyDataSetChanged();
    }

    /**
     * Add new budget item
     */
    public void addBudget(Budget budget, int position) {
        if (budgetList != null) {
            budgetList.add(position, budget);
            notifyItemInserted(position);
        }
    }

    /**
     * Remove budget item
     */
    public void removeBudget(int position) {
        if (budgetList != null && position >= 0 && position < budgetList.size()) {
            budgetList.remove(position);
            notifyItemRemoved(position);
        }
    }

    /**
     * Update budget item
     */
    public void updateBudget(Budget budget, int position) {
        if (budgetList != null && position >= 0 && position < budgetList.size()) {
            budgetList.set(position, budget);
            notifyItemChanged(position);
        }
    }

    /**
     * Set approved status appearance
     */
    private void setApprovedStatus(BudgetViewHolder holder) {
        holder.tvStatus.setText("Đã duyệt");
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_approved));
        holder.tvStatus.setBackgroundResource(R.drawable.status_background_approved);
        holder.ivStatusIcon.setImageResource(R.drawable.ic_approved);
        holder.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_approved));
        holder.indicatorLine.setBackgroundColor(ContextCompat.getColor(context, R.color.status_approved));
    }

    /**
     * Set pending status appearance
     */
    private void setPendingStatus(BudgetViewHolder holder) {
        holder.tvStatus.setText("Chờ duyệt");
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_pending));
        holder.tvStatus.setBackgroundResource(R.drawable.status_background_pending);
        holder.ivStatusIcon.setImageResource(R.drawable.ic_pending);
        holder.ivStatusIcon.setColorFilter(ContextCompat.getColor(context, R.color.status_pending));
        holder.indicatorLine.setBackgroundColor(ContextCompat.getColor(context, R.color.status_pending));
    }

    /**
     * Set category background based on type
     */
    private void setCategoryBackground(TextView tvCategory, String category) {
        int colorRes;
        switch (category) {
            case "human_resource":
                colorRes = R.color.category_human_resource;
                break;
            case "equipment":
                colorRes = R.color.category_equipment;
                break;
            case "material":
                colorRes = R.color.category_material;
                break;
            default:
                colorRes = R.color.category_other;
                break;
        }
        tvCategory.setBackgroundColor(ContextCompat.getColor(context, colorRes));
    }

    /**
     * Set amount background based on value
     */
    private void setAmountBackground(TextView tvAmount, double amount) {
        if (amount > 1000000) { // > 1 triệu
            tvAmount.setBackgroundResource(R.drawable.badge_high_priority);
        } else if (amount > 500000) { // > 500k
            tvAmount.setBackgroundResource(R.drawable.badge_medium_priority);
        } else {
            tvAmount.setBackgroundResource(R.drawable.badge_low_priority);
        }
    }

    /**
     * Get category display name in Vietnamese
     */
    private String getCategoryDisplayName(String category) {
        if (category == null) return "Khác";

        switch (category) {
            case "human_resource":
                return "Nhân sự";
            case "equipment":
                return "Thiết bị";
            case "material":
                return "Vật liệu";
            default:
                return "Khác";
        }
    }

    /**
     * Format amount to Vietnamese currency format
     */
    private String formatAmount(double amount) {
        return decimalFormat.format(amount);
    }

    /**
     * Format date to Vietnamese format
     */
    private String formatDate(Date date) {
        if (date == null) {
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());
        }
        return dateFormat.format(date);
    }

    /**
     * ViewHolder class
     */
    static class BudgetViewHolder extends RecyclerView.ViewHolder {
        View indicatorLine;
        TextView tvTitle, tvCategory, tvDescription, tvDate, tvAmount, tvStatus;
        ImageView ivStatusIcon;

        BudgetViewHolder(View itemView) {
            super(itemView);
            indicatorLine = itemView.findViewById(R.id.indicator_line);
            tvTitle = itemView.findViewById(R.id.tv_budget_title);
            tvCategory = itemView.findViewById(R.id.tv_budget_category);
            tvDescription = itemView.findViewById(R.id.tv_budget_description);
            tvDate = itemView.findViewById(R.id.tv_budget_date);
            tvAmount = itemView.findViewById(R.id.tv_budget_amount);
            tvStatus = itemView.findViewById(R.id.tv_budget_status);
            ivStatusIcon = itemView.findViewById(R.id.iv_status_icon);
        }
    }
}