package com.example.campussuperapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class StudentProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileEmailHeader, tvRollNo, tvDepartment, tvEmail;
    private TextView tvComplaintsCount, tvOpenCount, tvResolvedCount;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ListenerRegistration complaintsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_profile);

        // 1. Initialize UI Elements from XML layout
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmailHeader = findViewById(R.id.tvProfileEmailHeader);
        tvRollNo = findViewById(R.id.tvRollNo);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvEmail = findViewById(R.id.tvEmail);

        tvComplaintsCount = findViewById(R.id.tvComplaintsCount);
        tvOpenCount = findViewById(R.id.tvOpenCount);
        tvResolvedCount = findViewById(R.id.tvResolvedCount);

        LinearLayout menuSignOut = findViewById(R.id.menuSignOut);

        // 2. Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. Load user profile data dynamically
        loadLoggedInUserProfile();

        // my complaints activity
        View menuMyComplaints = findViewById(R.id.menuMyComplaints);
        if (menuMyComplaints != null) {
            menuMyComplaints.setOnClickListener(v -> {
                Intent intent = new Intent(
                        StudentProfileActivity.this,
                        MyComplaintsActivity.class
                );
                startActivity(intent);
            });
        }

        // 4. Handle Sign Out option
        if (menuSignOut != null) {
            menuSignOut.setOnClickListener(v -> {
                mAuth.signOut();
                Intent intent = new Intent(StudentProfileActivity.this, activity_create_account.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadLoggedInUserProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            String authEmail = currentUser.getEmail();

            if (authEmail != null) {
                if (tvProfileEmailHeader != null) tvProfileEmailHeader.setText(authEmail);
                if (tvEmail != null) tvEmail.setText(authEmail);
            }

            listenToComplaintStats(uid);

            // Fetch specific user document from Firestore using the user's UID
            db.collection("Users").document(uid)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                String fullName = document.getString("fullName");
                                String rollNo = document.getString("rollNo");
                                String department = document.getString("department");
                                String email = document.getString("email");

                                if (tvProfileName != null) {
                                    tvProfileName.setText(fullName != null && !fullName.isEmpty() ? fullName : "N/A");
                                }
                                if (tvRollNo != null) {
                                    tvRollNo.setText(rollNo != null && !rollNo.isEmpty() ? rollNo : "N/A");
                                }
                                if (tvDepartment != null) {
                                    tvDepartment.setText(department != null && !department.isEmpty() ? department : "N/A");
                                }
                                if (tvEmail != null && email != null) {
                                    tvEmail.setText(email);
                                }
                            } else {
                                Toast.makeText(StudentProfileActivity.this, "Profile data not found in database.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(StudentProfileActivity.this, "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "No active session. Redirecting...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(StudentProfileActivity.this, activity_create_account.class);
            startActivity(intent);
            finish();
        }
    }

    private void listenToComplaintStats(String uid) {
        complaintsListener = db.collection("complaints")
                .whereEqualTo("userId", uid)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) {
                        if (tvComplaintsCount != null) tvComplaintsCount.setText("0");
                        if (tvOpenCount != null) tvOpenCount.setText("0");
                        if (tvResolvedCount != null) tvResolvedCount.setText("0");
                        return;
                    }

                    int totalComplaints = querySnapshot.size();
                    int openCount = 0;
                    int resolvedCount = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if (status != null && status.equalsIgnoreCase("Resolved")) {
                            resolvedCount++;
                        } else {
                            openCount++;
                        }
                    }

                    if (tvComplaintsCount != null) {
                        tvComplaintsCount.setText(String.valueOf(totalComplaints));
                    }
                    if (tvOpenCount != null) {
                        tvOpenCount.setText(String.valueOf(openCount));
                    }
                    if (tvResolvedCount != null) {
                        tvResolvedCount.setText(String.valueOf(resolvedCount));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (complaintsListener != null) {
            complaintsListener.remove();
        }
    }
}
