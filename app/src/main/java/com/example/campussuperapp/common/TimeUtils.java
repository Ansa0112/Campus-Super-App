package com.example.campussuperapp.common;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class TimeUtils {

    private TimeUtils() {}

    /** "Just now", "5 min ago", "3 hours ago", "2 days ago", or a date after a week. */
    public static String timeAgo(Date date) {
        if (date == null) return "Just now"; // server timestamp not yet confirmed
        long diff = System.currentTimeMillis() - date.getTime();
        if (diff < 0) diff = 0;
        long minutes = diff / 60000;
        if (minutes < 1) return "Just now";
        if (minutes < 60) return minutes + " min ago";
        long hours = minutes / 60;
        if (hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");
        long days = hours / 24;
        if (days < 7) return days + (days == 1 ? " day ago" : " days ago");
        return new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date);
    }

    public static String fullDateTime(Date date) {
        if (date == null) return "-";
        return new SimpleDateFormat("EEE, dd MMM yyyy  •  hh:mm a", Locale.getDefault()).format(date);
    }

    public static String dateOnly(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(date);
    }

    public static String timeOnly(Date date) {
        if (date == null) return "";
        return new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date);
    }

    public static String day(Date date) {
        if (date == null) return "--";
        return new SimpleDateFormat("dd", Locale.getDefault()).format(date);
    }

    public static String month(Date date) {
        if (date == null) return "---";
        return new SimpleDateFormat("MMM", Locale.getDefault()).format(date).toUpperCase(Locale.getDefault());
    }
}
