package com.example.campussuperapp.common;

/**
 * Single place for every Firestore collection / field name used by the
 * Announcements and Events modules.
 *
 * If your team's "users" collection uses different names (for example "userRole"
 * instead of "role"), change them HERE only and the whole module follows.
 */
public final class Constants {

    private Constants() {}

    // ---- Team's existing users collection (written by the sign-up / sign-in module) ----
    public static final String USERS = "Users";   // capital U - same as your Firestore console
    public static final String USER_ROLE_FIELD = "role";       // "student" | "faculty" | "admin"
    public static final String USER_NAME_FIELD = "fullName";   // display name

    // ---- Our collections ----
    public static final String ANNOUNCEMENTS = "announcements";
    public static final String EVENTS = "events";
    public static final String REGISTRATIONS = "registrations"; // sub-collection of events/{id}

    // ---- Roles (stored lower-case in Firestore) ----
    public static final String ROLE_STUDENT = "student";
    public static final String ROLE_FACULTY = "faculty";
    public static final String ROLE_ADMIN = "admin";

    // ---- Announcement audience ----
    public static final String AUD_ALL = "Everyone";
    public static final String AUD_STUDENTS = "Students";
    public static final String AUD_FACULTY = "Faculty";
    public static final String[] AUDIENCES = {AUD_ALL, AUD_STUDENTS, AUD_FACULTY};

    // ---- Categories ----
    public static final String[] ANNOUNCEMENT_CATEGORIES = {
            "General", "Academic", "Exam", "Fee", "Library", "Sports", "Emergency"
    };
    public static final String[] EVENT_CATEGORIES = {
            "Seminar", "Workshop", "Sports", "Cultural", "Society", "Career", "Other"
    };

    // ---- Filter chip labels ----
    public static final String FILTER_ALL = "All";
    public static final String FILTER_PENDING = "Pending";
    public static final String FILTER_UPCOMING = "Upcoming";
    public static final String FILTER_PAST = "Past";
    public static final String FILTER_MINE = "My Events";
}
