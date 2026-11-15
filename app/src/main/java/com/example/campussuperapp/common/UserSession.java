package com.example.campussuperapp.common;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

/**
 * Tells our modules WHO is logged in and WHAT ROLE they have.
 *
 * It reads users/{uid} once (using the Firebase Auth uid), then caches the answer
 * so every screen gets the role instantly afterwards.
 */
public class UserSession {

    public final String uid;
    public final String name;
    public final String role;

    private static UserSession cached;

    private UserSession(String uid, String name, String role) {
        this.uid = uid;
        this.name = name;
        this.role = role;
    }

    public boolean isAdmin() { return Constants.ROLE_ADMIN.equals(role); }
    public boolean isFaculty() { return Constants.ROLE_FACULTY.equals(role); }
    public boolean isStudent() { return Constants.ROLE_STUDENT.equals(role); }

    /** Faculty and admin are allowed to create announcements / events. */
    public boolean canPublish() { return isAdmin() || isFaculty(); }

    public interface Callback {
        void onReady(@NonNull UserSession session);
        void onError(@NonNull String message);
    }

    public static void load(@NonNull final Callback callback) {
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            callback.onError("Please sign in first");
            return;
        }
        if (cached != null && cached.uid.equals(user.getUid())) {
            callback.onReady(cached);
            return;
        }
        FirebaseFirestore.getInstance()
                .collection(Constants.USERS)
                .document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString(Constants.USER_ROLE_FIELD);
                    String name = doc.getString(Constants.USER_NAME_FIELD);

                    role = (role == null) ? Constants.ROLE_STUDENT : role.trim().toLowerCase();
                    if (!role.equals(Constants.ROLE_ADMIN) && !role.equals(Constants.ROLE_FACULTY)) {
                        role = Constants.ROLE_STUDENT;
                    }
                    if (name == null || name.trim().isEmpty()) {
                        name = user.getDisplayName();
                    }
                    if (name == null || name.trim().isEmpty()) {
                        String email = user.getEmail();
                        name = (email != null && email.contains("@"))
                                ? email.substring(0, email.indexOf('@')) : "User";
                    }
                    cached = new UserSession(user.getUid(), name, role);
                    callback.onReady(cached);
                })
                .addOnFailureListener(e -> callback.onError(
                        "Could not load your profile: " + e.getMessage()));
    }

    /** Call this from the sign-out button of the team's code so the next login re-reads the role. */
    public static void clear() {
        cached = null;
    }
}
