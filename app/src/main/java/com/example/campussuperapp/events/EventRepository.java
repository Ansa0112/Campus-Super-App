package com.example.campussuperapp.events;

import androidx.annotation.NonNull;

import com.example.campussuperapp.common.Constants;
import com.example.campussuperapp.common.UserSession;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** ALL Firestore code for events (including seat registration) lives here. */
public final class EventRepository {

    private EventRepository() {}

    // ------------------------------------------------------------------ callbacks
    public interface ListListener {
        void onData(@NonNull List<Event> list);
        void onError(@NonNull String message);
    }

    public interface ItemListener {
        void onData(@NonNull Event e);
        void onDeleted();
        void onError(@NonNull String message);
    }

    public interface DoneCallback {
        void onSuccess();
        void onError(@NonNull String message);
    }

    public interface AttendeesCallback {
        void onData(@NonNull List<String> lines);
        void onError(@NonNull String message);
    }

    private static FirebaseFirestore db() { return FirebaseFirestore.getInstance(); }
    private static CollectionReference col() { return db().collection(Constants.EVENTS); }

    // ------------------------------------------------------------------ permissions
    public static boolean canView(UserSession s, Event e) {
        if (s.isAdmin()) return true;
        if (e.getOrganizerId() != null && e.getOrganizerId().equals(s.uid)) return true;
        return e.isPublished() && e.isApproved();
    }

    public static boolean canManage(UserSession s, Event e) {
        return s.isAdmin() || (s.isFaculty() && s.uid.equals(e.getOrganizerId()));
    }

    // ------------------------------------------------------------------ READ (real time)
    public static ListenerRegistration listen(final UserSession s, final ListListener l) {
        return col().orderBy("eventTime", Query.Direction.ASCENDING)
                .addSnapshotListener((snap, err) -> {
                    if (err != null) { l.onError(String.valueOf(err.getMessage())); return; }
                    if (snap == null) return;
                    List<Event> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Event e = d.toObject(Event.class);
                        if (e != null && canView(s, e)) out.add(e);
                    }
                    l.onData(out);
                });
    }

    public static ListenerRegistration listenOne(String id, final ItemListener l) {
        return col().document(id).addSnapshotListener((snap, err) -> {
            if (err != null) { l.onError(String.valueOf(err.getMessage())); return; }
            if (snap == null || !snap.exists()) { l.onDeleted(); return; }
            Event e = snap.toObject(Event.class);
            if (e != null) l.onData(e);
        });
    }

    public static void fetch(String id, final ItemListener l) {
        col().document(id).get()
                .addOnSuccessListener(snap -> {
                    Event e = snap.toObject(Event.class);
                    if (e == null) l.onDeleted(); else l.onData(e);
                })
                .addOnFailureListener(err -> l.onError(String.valueOf(err.getMessage())));
    }

    /** Next N upcoming events the user may see - handy for the Home screen preview. */
    public static void fetchUpcoming(final UserSession s, final int n, final ListListener l) {
        col().whereGreaterThanOrEqualTo("eventTime", new Date())
                .orderBy("eventTime", Query.Direction.ASCENDING).limit(40).get()
                .addOnSuccessListener(snap -> {
                    List<Event> out = new ArrayList<>();
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        Event e = d.toObject(Event.class);
                        if (e != null && e.isPublished() && e.isApproved() && canView(s, e)) {
                            out.add(e);
                            if (out.size() == n) break;
                        }
                    }
                    l.onData(out);
                })
                .addOnFailureListener(err -> l.onError(String.valueOf(err.getMessage())));
    }

    // ------------------------------------------------------------------ CREATE / UPDATE
    public static void save(UserSession s, Event e, final DoneCallback cb) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", e.getTitle());
        m.put("description", e.getDescription());
        m.put("venue", e.getVenue());
        m.put("category", e.getCategory());
        m.put("eventTime", e.getEventTime());
        m.put("capacity", e.getCapacity());
        m.put("published", e.isPublished());
        m.put("updatedAt", FieldValue.serverTimestamp());

        if (e.getId() == null) {
            m.put("organizerId", s.uid);
            m.put("organizerName", s.name);
            m.put("organizerRole", s.role);
            m.put("approved", s.isAdmin());            // faculty events need approval
            m.put("registeredCount", 0L);
            m.put("registeredUids", new ArrayList<String>());
            m.put("createdAt", FieldValue.serverTimestamp());
            col().add(m)
                    .addOnSuccessListener(r -> cb.onSuccess())
                    .addOnFailureListener(err -> cb.onError(String.valueOf(err.getMessage())));
        } else {
            col().document(e.getId()).update(m)
                    .addOnSuccessListener(r -> cb.onSuccess())
                    .addOnFailureListener(err -> cb.onError(String.valueOf(err.getMessage())));
        }
    }

    public static void updateField(String id, String field, Object value, final DoneCallback cb) {
        col().document(id).update(field, value)
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onError(String.valueOf(err.getMessage())));
    }

    public static void delete(final String id, final DoneCallback cb) {
        col().document(id).delete()
                .addOnSuccessListener(r -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onError(String.valueOf(err.getMessage())));
    }

    // ------------------------------------------------------------------ REGISTRATION (transactions)
    /**
     * Registers the user for an event.
     * A Firestore TRANSACTION is used so two students tapping "Register" for the last seat
     * at the same moment can never over-book the event: Firestore retries / rejects one of them.
     */
    public static void register(final UserSession s, final String eventId, final DoneCallback cb) {
        final DocumentReference eventRef = col().document(eventId);
        final DocumentReference regRef = eventRef.collection(Constants.REGISTRATIONS).document(s.uid);

        db().runTransaction(tr -> {
            DocumentSnapshot ev = tr.get(eventRef);
            DocumentSnapshot reg = tr.get(regRef);

            if (!ev.exists()) {
                throw new FirebaseFirestoreException("This event no longer exists",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            if (reg.exists()) {
                throw new FirebaseFirestoreException("You are already registered",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            Long capacity = ev.getLong("capacity");
            Long count = ev.getLong("registeredCount");
            long cap = capacity == null ? 0 : capacity;
            long cnt = count == null ? 0 : count;
            if (cap > 0 && cnt >= cap) {
                throw new FirebaseFirestoreException("Sorry, this event is full",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            Map<String, Object> data = new HashMap<>();
            data.put("uid", s.uid);
            data.put("name", s.name);
            data.put("role", s.role);
            data.put("registeredAt", FieldValue.serverTimestamp());
            tr.set(regRef, data);
            tr.update(eventRef,
                    "registeredCount", FieldValue.increment(1),
                    "registeredUids", FieldValue.arrayUnion(s.uid));
            return null;
        }).addOnSuccessListener(r -> cb.onSuccess())
          .addOnFailureListener(err -> cb.onError(String.valueOf(err.getMessage())));
    }

    public static void unregister(final UserSession s, final String eventId, final DoneCallback cb) {
        final DocumentReference eventRef = col().document(eventId);
        final DocumentReference regRef = eventRef.collection(Constants.REGISTRATIONS).document(s.uid);

        db().runTransaction(tr -> {
            DocumentSnapshot reg = tr.get(regRef);
            if (!reg.exists()) {
                throw new FirebaseFirestoreException("You are not registered",
                        FirebaseFirestoreException.Code.ABORTED);
            }
            tr.delete(regRef);
            tr.update(eventRef,
                    "registeredCount", FieldValue.increment(-1),
                    "registeredUids", FieldValue.arrayRemove(s.uid));
            return null;
        }).addOnSuccessListener(r -> cb.onSuccess())
          .addOnFailureListener(err -> cb.onError(String.valueOf(err.getMessage())));
    }

    /** Attendee list for organiser / admin: "Name (role)". */
    public static void fetchAttendees(String eventId, final AttendeesCallback cb) {
        col().document(eventId).collection(Constants.REGISTRATIONS)
                .orderBy("registeredAt", Query.Direction.ASCENDING).get()
                .addOnSuccessListener(snap -> {
                    List<String> lines = new ArrayList<>();
                    int i = 1;
                    for (DocumentSnapshot d : snap.getDocuments()) {
                        String name = d.getString("name");
                        String role = d.getString("role");
                        lines.add((i++) + ".  " + (name == null ? "Unknown" : name)
                                + (role == null ? "" : "  (" + role + ")"));
                    }
                    cb.onData(lines);
                })
                .addOnFailureListener(err -> cb.onError(String.valueOf(err.getMessage())));
    }
}
