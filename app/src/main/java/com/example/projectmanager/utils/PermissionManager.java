package com.example.projectmanager.utils;

import com.example.projectmanager.models.User;

/**
 * Central permission management class
 */
public class PermissionManager {

    public enum Permission {
        // Task permissions
        CREATE_TASK,
        EDIT_TASK,
        DELETE_TASK,
        ASSIGN_TASK,

        // Budget permissions
        CREATE_BUDGET,
        APPROVE_BUDGET,
        EDIT_BUDGET,
        DELETE_BUDGET,

        // Chat permissions
        SEND_MESSAGE,
        DELETE_MESSAGE,

        // Admin permissions
        MANAGE_USERS,
        VIEW_REPORTS,
        MANAGE_SETTINGS
    }

    public static boolean hasPermission(User.Role role, Permission permission) {
        switch (role) {
            case ADMIN:
                // Admin có tất cả permissions
                return true;

            case MANAGER:
                // Manager có hầu hết permissions except admin-only
                switch (permission) {
                    case MANAGE_USERS:
                    case MANAGE_SETTINGS:
                        return false;
                    default:
                        return true;
                }

            case MEMBER:
                // Member chỉ có permissions cơ bản
                switch (permission) {
                    case CREATE_TASK:
                    case EDIT_TASK:
                    case CREATE_BUDGET:
                    case SEND_MESSAGE:
                        return true;
                    default:
                        return false;
                }

            default:
                return false;
        }
    }

    // Convenience methods
    public static boolean canManageTasks(User.Role role) {
        return hasPermission(role, Permission.CREATE_TASK) &&
                hasPermission(role, Permission.EDIT_TASK);
    }

    public static boolean canApproveBudget(User.Role role) {
        return hasPermission(role, Permission.APPROVE_BUDGET);
    }

    public static boolean canDeleteTasks(User.Role role) {
        return hasPermission(role, Permission.DELETE_TASK);
    }

    public static boolean canManageUsers(User.Role role) {
        return hasPermission(role, Permission.MANAGE_USERS);
    }

    public static boolean canViewReports(User.Role role) {
        return hasPermission(role, Permission.VIEW_REPORTS);
    }
}