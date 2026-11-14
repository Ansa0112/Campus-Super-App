package com.example.campussuperapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campussuperapp.announcements.AnnouncementsActivity;
import com.example.campussuperapp.events.EventsActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // 1. Verify HomeActivity successfully loaded
        Toast.makeText(this, "HomeActivity Loaded!", Toast.LENGTH_SHORT).show();

        // Click listeners for Quick Access items & See All
        View btnEvents = findViewById(R.id.btnEvents);
        if (btnEvents != null) {
            btnEvents.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, EventsActivity.class)));
        }

        View tvSeeAllEvents = findViewById(R.id.tvSeeAllEvents);
        if (tvSeeAllEvents != null) {
            tvSeeAllEvents.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, EventsActivity.class)));
        }

        View tvSeeAllAnnouncements = findViewById(R.id.tvSeeAllAnnouncements);
        if (tvSeeAllAnnouncements != null) {
            tvSeeAllAnnouncements.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, AnnouncementsActivity.class)));
        }

        View btnReport = findViewById(R.id.btnReport);
        if (btnReport != null) {
            btnReport.setOnClickListener(v -> startActivity(new Intent(HomeActivity.this, ComplaintsActivity.class)));
        }

        // 2. Find Bottom Navigation View safely
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        if (bottomNavigation == null) {
            Toast.makeText(this, "ERROR: bottomNavigation is NULL!", Toast.LENGTH_LONG).show();
            Log.e("NavError", "bottomNavigation not found in activity_home.xml!");
            return;
        }

        // 3. Set Item Selected Listener
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            }

            if (itemId == R.id.nav_issues) {
                startActivity(new Intent(HomeActivity.this, ComplaintsActivity.class));
                return true;
            }

            if (itemId == R.id.nav_profile) {
                Toast.makeText(HomeActivity.this, "Opening Profile...", Toast.LENGTH_SHORT).show();

                // Fetch the current user's role dynamically from Firestore to open the correct profile activity
                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                if (currentUser != null) {
                    String uid = currentUser.getUid();
                    FirebaseFirestore.getInstance().collection("Users").document(uid)
                            .get()
                            .addOnSuccessListener(documentSnapshot -> {
                                Intent intent;
                                if (documentSnapshot.exists()) {
                                    String role = documentSnapshot.getString("role");

                                    if ("Faculty".equals(role)) {
                                        intent = new Intent(HomeActivity.this, FacultyProfileActivity.class);
                                    } else if ("Admin".equals(role)) {
                                        intent = new Intent(HomeActivity.this, AdminProfileActivity.class);
                                    } else {
                                        // Default fallback to Student Profile
                                        intent = new Intent(HomeActivity.this, StudentProfileActivity.class);
                                    }
                                } else {
                                    // Fallback if document doesn't exist yet
                                    intent = new Intent(HomeActivity.this, StudentProfileActivity.class);
                                }
                                startActivity(intent);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(HomeActivity.this, "Error checking user role: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                // Fallback intent on error
                                startActivity(new Intent(HomeActivity.this, StudentProfileActivity.class));
                            });
                } else {
                    Toast.makeText(HomeActivity.this, "No active session found.", Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            return false;
        });
    }
}
