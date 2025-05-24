package com.example.projectmanager.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Lớp tiện ích cho việc xử lý ngày tháng
 */
public class DateUtils {

    // Định dạng ngày chuẩn
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm", Locale.getDefault());
    public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    /**
     * Chuyển đổi Date thành chuỗi theo định dạng ngày
     */
    public static String formatDate(Date date) {
        if (date == null) return "";
        return DATE_FORMAT.format(date);
    }

    /**
     * Chuyển đổi Date thành chuỗi theo định dạng giờ
     */
    public static String formatTime(Date date) {
        if (date == null) return "";
        return TIME_FORMAT.format(date);
    }

    /**
     * Chuyển đổi Date thành chuỗi theo định dạng ngày giờ
     */
    public static String formatDateTime(Date date) {
        if (date == null) return "";
        return DATE_TIME_FORMAT.format(date);
    }

    /**
     * Kiểm tra xem ngày có quá hạn không
     */
    public static boolean isOverdue(Date date) {
        if (date == null) return false;
        return date.before(new Date());
    }

    /**
     * Tính số ngày còn lại đến hạn
     */
    public static int getDaysRemaining(Date dueDate) {
        if (dueDate == null) return 0;
        long diff = dueDate.getTime() - new Date().getTime();
        return (int) (diff / (24 * 60 * 60 * 1000));
    }
}