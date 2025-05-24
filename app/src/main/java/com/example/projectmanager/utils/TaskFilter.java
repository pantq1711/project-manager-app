package com.example.projectmanager.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskFilter {
    public enum SortBy {
        DATE_CREATED("createdAt"),
        PRIORITY("priority"),
        STATUS("status"),
        DUE_DATE("dueDate");

        private final String field;

        SortBy(String field) {
            this.field = field;
        }

        public String getField() {
            return field;
        }
    }

    public enum SortOrder {
        ASCENDING,
        DESCENDING
    }

    public static class FilterCriteria {
        private String searchQuery;
        private String statusFilter;
        private String priorityFilter;
        private String assignedToFilter;
        private SortBy sortBy;
        private SortOrder sortOrder;

        public FilterCriteria() {
            this.sortBy = SortBy.DATE_CREATED;
            this.sortOrder = SortOrder.DESCENDING;
        }

        // Getters and Setters
        public String getSearchQuery() { return searchQuery; }
        public void setSearchQuery(String searchQuery) { this.searchQuery = searchQuery; }

        public String getStatusFilter() { return statusFilter; }
        public void setStatusFilter(String statusFilter) { this.statusFilter = statusFilter; }

        public String getPriorityFilter() { return priorityFilter; }
        public void setPriorityFilter(String priorityFilter) { this.priorityFilter = priorityFilter; }

        public String getAssignedToFilter() { return assignedToFilter; }
        public void setAssignedToFilter(String assignedToFilter) { this.assignedToFilter = assignedToFilter; }

        public SortBy getSortBy() { return sortBy; }
        public void setSortBy(SortBy sortBy) { this.sortBy = sortBy; }

        public SortOrder getSortOrder() { return sortOrder; }
        public void setSortOrder(SortOrder sortOrder) { this.sortOrder = sortOrder; }
    }

    public static List<Map<String, Object>> filterAndSortTasks(
            List<Map<String, Object>> tasks,
            FilterCriteria criteria) {

        List<Map<String, Object>> filteredTasks = new ArrayList<>();

        // Step 1: Filter tasks
        for (Map<String, Object> task : tasks) {
            if (matchesFilter(task, criteria)) {
                filteredTasks.add(task);
            }
        }

        // Step 2: Sort tasks
        filteredTasks.sort((task1, task2) -> {
            int comparison = compareTasks(task1, task2, criteria.getSortBy());
            return criteria.getSortOrder() == SortOrder.DESCENDING ? -comparison : comparison;
        });

        return filteredTasks;
    }

    private static boolean matchesFilter(Map<String, Object> task, FilterCriteria criteria) {
        // Search query filter
        if (criteria.getSearchQuery() != null && !criteria.getSearchQuery().isEmpty()) {
            String searchQuery = criteria.getSearchQuery().toLowerCase();
            String title = (String) task.get("title");
            String description = (String) task.get("description");
            String assignedTo = (String) task.get("assignedTo");

            boolean matchesSearch = false;
            if (title != null && title.toLowerCase().contains(searchQuery)) matchesSearch = true;
            if (description != null && description.toLowerCase().contains(searchQuery)) matchesSearch = true;
            if (assignedTo != null && assignedTo.toLowerCase().contains(searchQuery)) matchesSearch = true;

            if (!matchesSearch) return false;
        }

        // Status filter
        if (criteria.getStatusFilter() != null && !criteria.getStatusFilter().isEmpty()) {
            String status = (String) task.get("status");
            if (!criteria.getStatusFilter().equals(status)) return false;
        }

        // Priority filter
        if (criteria.getPriorityFilter() != null && !criteria.getPriorityFilter().isEmpty()) {
            String priority = (String) task.get("priority");
            if (!criteria.getPriorityFilter().equals(priority)) return false;
        }

        // Assigned to filter
        if (criteria.getAssignedToFilter() != null && !criteria.getAssignedToFilter().isEmpty()) {
            String assignedTo = (String) task.get("assignedTo");
            if (!criteria.getAssignedToFilter().equals(assignedTo)) return false;
        }

        return true;
    }

    private static int compareTasks(Map<String, Object> task1, Map<String, Object> task2, SortBy sortBy) {
        switch (sortBy) {
            case DATE_CREATED:
                return compareObjects(task1.get("createdAt"), task2.get("createdAt"));
            case DUE_DATE:
                return compareObjects(task1.get("dueDate"), task2.get("dueDate"));
            case PRIORITY:
                return comparePriority((String) task1.get("priority"), (String) task2.get("priority"));
            case STATUS:
                return compareStatus((String) task1.get("status"), (String) task2.get("status"));
            default:
                return 0;
        }
    }

    @SuppressWarnings("unchecked")
    private static int compareObjects(Object obj1, Object obj2) {
        if (obj1 == null && obj2 == null) return 0;
        if (obj1 == null) return -1;
        if (obj2 == null) return 1;

        if (obj1 instanceof Comparable && obj2 instanceof Comparable) {
            return ((Comparable<Object>) obj1).compareTo(obj2);
        }

        return obj1.toString().compareTo(obj2.toString());
    }

    private static int comparePriority(String priority1, String priority2) {
        int value1 = getPriorityValue(priority1);
        int value2 = getPriorityValue(priority2);
        return Integer.compare(value1, value2);
    }

    private static int getPriorityValue(String priority) {
        if (priority == null) return 1;
        switch (priority) {
            case "high": return 3;
            case "medium": return 2;
            case "low": return 1;
            default: return 1;
        }
    }

    private static int compareStatus(String status1, String status2) {
        int value1 = getStatusValue(status1);
        int value2 = getStatusValue(status2);
        return Integer.compare(value1, value2);
    }

    private static int getStatusValue(String status) {
        if (status == null) return 1;
        switch (status) {
            case "pending": return 1;
            case "in_progress": return 2;
            case "completed": return 3;
            default: return 1;
        }
    }
}