package com.example.campussuperapp.announcements;

import androidx.annotation.NonNull;

import com.example.campussuperapp.common.Constants;
import com.example.campussuperapp.common.UserSession;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ALL Firestore code for announcements lives here, so the activities stay clean
 * and you can explain the database layer in one file.
 */
public final class AnnouncementRepository {

    private AnnouncementRepository() {}

    // ------------------------------------------------------------------ callbacks
    public interface ListListener {
        void onData(@NonNull List<Announcement> list);
        void onError(@NonNull String message);
    }

    public interface ItemListener {
        void onData(@NonNull Announcement a);
        void onDeleted();
        void onError(@NonNull String message);
    }

    public interface DoneCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }

    private static CollectionReference col() {
        return FirebaseFirestore.getInstance().collection(Constants.ANNOUNCEMENTS);
    }

    // ------------------------------------------------------------------ permissions
    /** Who is allowed to SEE an announcement. */
    public static boolean canView(UserSession s, Announcement a) {
        if (s.isAdmin()) return true;                                   // admin sees everything
        if (a.getAuthorId() != null && a.getAuthorId().equals(s.uid)) return true; // own posts
        if (!a.isPublished() || !a.isApproved()) return false;          // drafts / pending are hidden
        String aud = a.getAudience();
        if (aud == null || Constants.AUD_ALL.equals(aud)) return true;
        if (Constants.AUD_STUDENTS.equals(aud)) return s.isStudent();
        if (Constants.AUD_FACULTY.equals(aud)) return s.isFaculty();
        return true;
    }

    /** Who is allowed to EDIT / DELETE an announcement. */
    public static boolean canManage(UserSession s, Announcement a) {
        return s.isAdmin() || (s.isFaculty() && s.uid.equals(a.getAuthorId()));
    }

    // ------------------------------------------------------------------ READ (real time)
    /** Live list: the callback fires again automatically whenever anyone changes data. */
    public static ListenerRegistration listen(final UserSession s, final ListListener l) {
        return col().orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) { l.onError(String.valueOf(e.getMessage())); return; }
                    if (snap == null) return;
                    List<Announcement> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Announcement a = d.toObject(Announcement.class);
                        if (a != null && canView(s, a)) out.add(a);
                    }
                    // Pinned first; sort is stable so newest-first order is kept inside each group.
                    Collections.sort(out, (x, y) -> Boolean.compare(y.isPinned(), x.isPinned()));
                    l.onData(out);
                });
    }

    /** Live single announcement (detail screen). */
    public static ListenerRegistration listenOne(String id, final ItemListener l) {
        return col().document(id).addSnapshotListener((snap, e) -> {
            if (e != null) { l.onError(String.valueOf(e.getMessage())); return; }
            if (snap == null || !snap.exists()) { l.onDeleted(); return; }
            Announcement a = snap.toObject(Announcement.class);
            if (a != null) l.onData(a);
        });
    }

    /** One-time read (used to fill the edit form). */
    public static void fetch(String id, final ItemListener l) {
        col().document(id).get()
                .addOnSuccessListener(snap -> {
                    Announcement a = snap.toObject(Announcement.class);
                    if (a == null) l.onDeleted(); else l.onData(a);
                })
                .addOnFailureListener(e -> l.onError(String.valueOf(e.getMessage())));
    }

    /** Latest N announcements the user may see - handy for the Home screen preview. */
    public static void fetchLatest(final UserSession s, final int n, final ListListener l) {
        col().orderBy("createdAt", Query.Direction.DESCENDING).limit(40).get()
                .addOnSuccessListener(snap -> {
                    List<Announcement> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Announcement a = d.toObject(Announcement.class);
                        if (a != null && a.isPublished() && a.isApproved() && canView(s, a)) {
                            out.add(a);
                            if (out.size() == n) break;
                        }
                    }
                    l.onData(out);
                })
                .addOnFailureListener(e -> l.onError(String.valueOf(e.getMessage())));
    }

    // ------------------------------------------------------------------ CREATE / UPDATE
    /**
     * Creates (id == null) or updates (id != null) an announcement.
     * Rules built in here:
     *  - admin posts are approved immediately, faculty posts start as "pending"
     *  - only admin can pin
     */
    public static void save(UserSession s, Announcement a, final DoneCallback cb) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", a.getTitle());
        m.put("description", a.getDescription());
        m.put("category", a.getCategory());
        m.put("audience", a.getAudience());
        m.put("published", a.isPublished());
        m.put("updatedAt", FieldValue.serverTimestamp());
        if (s.isAdmin()) m.put("pinned", a.isPinned());

        if (a.getId() == null) {
            m.put("authorId", s.uid);
            m.put("authorName", s.name);
            m.put("authorRole", s.role);
            m.put("approved", s.isAdmin());
            if (!s.isAdmin()) m.put("pinned", false);
            m.put("viewCount", 0L);
            m.put("createdAt", FieldValue.serverTimestamp());
            col().add(m)
                    .addOnSuccessListener(r -> cb.onSuccess())
                    .addOnFailureListener(e -> cb.onError(String.valueOf(e.getMessage())));
        } else {
            col().document(a.getId()).update(m)
                    .addOnSuccessListener(r -> cb.onSuccess())
                    .addOnFailureListener(e -> cb.onError(String.valueOf(e.getMessage())));
        }
    }

    /** Small one-field updates: approve, pin, publish / unpublish. */
    public static void updateField(String id, String field, Object value, final DoneCallback cb) {
        col().document(id).update(field, value)
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(String.valueOf(e.getMessage())));
    }

    /** Atomic counter - safe even if 100 students open it at the same time. */
    public static void incrementViews(String id) {
        col().document(id).update("viewCount", FieldValue.increment(1));
    }

    // ------------------------------------------------------------------ DELETE
    public static void delete(String id, final DoneCallback cb) {
        col().document(id).delete()
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(e -> cb.onError(String.valueOf(e.getMessage())));
    }
}
