package com.example.projectmanager.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.example.projectmanager.models.Budget;

/**
 * Utility class for sorting budget items
 */
public class BudgetSorter {

    public enum SortOption {
        DEFAULT,           // Default sort (by creation date, newest first)
        AMOUNT_ASCENDING,  // Sort by amount (low to high)
        AMOUNT_DESCENDING, // Sort by amount (high to low)
        PENDING_FIRST,     // Sort by approval status (pending first)
        APPROVED_FIRST     // Sort by approval status (approved first)
    }

    /**
     * Convert string option to enum
     */
    public static SortOption getSortOptionFromString(String option) {
        switch (option) {
            case "Số tiền tăng dần":
                return SortOption.AMOUNT_ASCENDING;
            case "Số tiền giảm dần":
                return SortOption.AMOUNT_DESCENDING;
            case "Chưa duyệt trước":
                return SortOption.PENDING_FIRST;
            case "Đã duyệt trước":
                return SortOption.APPROVED_FIRST;
            default:
                return SortOption.DEFAULT;
        }
    }

    /**
     * Sort a list of budgets based on the provided option
     */
    public static void sortBudgets(List<Budget> budgetList, SortOption option) {
        if (budgetList == null || budgetList.isEmpty()) return;

        switch (option) {
            case AMOUNT_ASCENDING:
                Collections.sort(budgetList, new AmountAscendingComparator());
                break;
            case AMOUNT_DESCENDING:
                Collections.sort(budgetList, new AmountDescendingComparator());
                break;
            case PENDING_FIRST:
                Collections.sort(budgetList, new PendingFirstComparator());
                break;
            case APPROVED_FIRST:
                Collections.sort(budgetList, new ApprovedFirstComparator());
                break;
            default:
                Collections.sort(budgetList, new CreationDateComparator());
                break;
        }
    }

    /**
     * Comparator for sorting by amount (ascending)
     */
    private static class AmountAscendingComparator implements Comparator<Budget> {
        @Override
        public int compare(Budget a, Budget b) {
            return Double.compare(a.getAmount(), b.getAmount());
        }
    }

    /**
     * Comparator for sorting by amount (descending)
     */
    private static class AmountDescendingComparator implements Comparator<Budget> {
        @Override
        public int compare(Budget a, Budget b) {
            return Double.compare(b.getAmount(), a.getAmount());
        }
    }

    /**
     * Comparator for sorting by approval status (pending first)
     */
    private static class PendingFirstComparator implements Comparator<Budget> {
        @Override
        public int compare(Budget a, Budget b) {
            if (a.isApproved() == b.isApproved()) {
                // If same approval status, sort by date
                if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
            return Boolean.compare(a.isApproved(), b.isApproved());
        }
    }

    /**
     * Comparator for sorting by approval status (approved first)
     */
    private static class ApprovedFirstComparator implements Comparator<Budget> {
        @Override
        public int compare(Budget a, Budget b) {
            if (a.isApproved() == b.isApproved()) {
                // If same approval status, sort by date
                if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
                return b.getCreatedAt().compareTo(a.getCreatedAt());
            }
            return Boolean.compare(b.isApproved(), a.isApproved());
        }
    }

    /**
     * Comparator for sorting by creation date (newest first)
     */
    private static class CreationDateComparator implements Comparator<Budget> {
        @Override
        public int compare(Budget a, Budget b) {
            if (a.getCreatedAt() == null || b.getCreatedAt() == null) return 0;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        }
    }
}