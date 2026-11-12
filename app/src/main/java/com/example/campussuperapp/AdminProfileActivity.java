package com.example.campussuperapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileEmailHeader, tvAdminId, tvEmail;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_profile);

        // Window insets padding matching root view ID ("main")
        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 1. Initialize UI Elements from XML layout
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmailHeader = findViewById(R.id.tvProfileEmailHeader);
        tvAdminId = findViewById(R.id.tvAdminId);
        tvEmail = findViewById(R.id.tvEmail);
        LinearLayout menuSignOut = findViewById(R.id.menuSignOut);

        // 2. Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. Load logged-in admin profile dynamically from Firestore
        loadAdminProfile();

        // 4. Handle Sign Out option
        if (menuSignOut != null) {
            menuSignOut.setOnClickListener(v -> {
                mAuth.signOut();
                Intent intent = new Intent(AdminProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadAdminProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            String authEmail = currentUser.getEmail();

            if (authEmail != null) {
                if (tvProfileEmailHeader != null) tvProfileEmailHeader.setText(authEmail);
                if (tvEmail != null) tvEmail.setText(authEmail);
            }

            // Fetch specific user document from Firestore using the admin's UID
            db.collection("Users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String fullName = document.getString("fullName");
                        String adminId = document.getString("adminId"); // Stored during admin registration
                        String email = document.getString("email");

                        if (tvProfileName != null) {
                            tvProfileName.setText(fullName != null && !fullName.isEmpty() ? fullName : "N/A");
                        }
                        if (tvAdminId != null) {
                            tvAdminId.setText(adminId != null && !adminId.isEmpty() ? adminId : "N/A");
                        }
                        if (tvEmail != null && email != null) {
                            tvEmail.setText(email);
                        }
                    } else {
                        Toast.makeText(AdminProfileActivity.this, "Profile data not found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(AdminProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(this, "No active session. Redirecting to login...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(AdminProfileActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}