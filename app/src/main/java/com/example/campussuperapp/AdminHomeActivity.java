package com.example.campussuperapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class AdminHomeActivity extends AppCompatActivity {

    private TextView tvTotalComplaints;
    private TextView tvPendingComplaints;
    private TextView tvResolvedComplaints;
    private TextView tvAnnouncementCount;

    private FirebaseFirestore db;
    private ListenerRegistration complaintsListener;
    private ListenerRegistration announcementsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_home);

        tvTotalComplaints = findViewById(R.id.tvTotalComplaints);
        tvPendingComplaints = findViewById(R.id.tvPendingComplaints);
        tvResolvedComplaints = findViewById(R.id.tvResolvedComplaints);
        tvAnnouncementCount = findViewById(R.id.tvAnnouncementCount);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnReviewComplaints).setOnClickListener(v -> openComplaints());

        setupBottomNavigation();

        listenToComplaintStats();
        listenToAnnouncementStats();
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            bottomNavigation.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    return true;
                }

                if (itemId == R.id.nav_issues) {
                    openComplaints();
                    return true;
                }

                if (itemId == R.id.nav_profile) {
                    Intent intent = new Intent(AdminHomeActivity.this, AdminProfileActivity.class);
                    startActivity(intent);
                    return true;
                }

                return false;
            });
        }
    }

    private void openComplaints() {
        Intent intent = new Intent(
                AdminHomeActivity.this,
                AdminComplaintsActivity.class
        );
        startActivity(intent);
    }

    private void listenToComplaintStats() {
        complaintsListener = db.collection("complaints")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) {
                        tvTotalComplaints.setText("0");
                        tvPendingComplaints.setText("0");
                        tvResolvedComplaints.setText("0");
                        return;
                    }

                    int total = querySnapshot.size();
                    int pending = 0;
                    int resolved = 0;

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if (status != null) {
                            if (status.equalsIgnoreCase("Pending")) {
                                pending++;
                            } else if (status.equalsIgnoreCase("Resolved")) {
                                resolved++;
                            }
                        }
                    }

                    tvTotalComplaints.setText(String.valueOf(total));
                    tvPendingComplaints.setText(String.valueOf(pending));
                    tvResolvedComplaints.setText(String.valueOf(resolved));
                });
    }

    private void listenToAnnouncementStats() {
        announcementsListener = db.collection("announcements")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) {
                        tvAnnouncementCount.setText("0");
                        return;
                    }
                    tvAnnouncementCount.setText(String.valueOf(querySnapshot.size()));
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (complaintsListener != null) {
            complaintsListener.remove();
        }
        if (announcementsListener != null) {
            announcementsListener.remove();
        }
    }
}
