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

public class FacultyProfileActivity extends AppCompatActivity {

    private TextView tvProfileName, tvProfileEmailHeader, tvEmployeeId, tvDepartment, tvEmail;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_faculty_profile);

        // Window insets padding matching your layout's root view ID ("main")
        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // 1. Initialize UI Elements from your XML layout
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmailHeader = findViewById(R.id.tvProfileEmailHeader);
        tvEmployeeId = findViewById(R.id.tvEmployeeId);
        tvDepartment = findViewById(R.id.tvDepartment);
        tvEmail = findViewById(R.id.tvEmail);
        LinearLayout menuSignOut = findViewById(R.id.menuSignOut);

        // 2. Initialize Firebase instances
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // 3. Load logged-in faculty profile dynamically from Firestore
        loadFacultyProfile();

        // 4. Handle Sign Out option
        if (menuSignOut != null) {
            menuSignOut.setOnClickListener(v -> {
                mAuth.signOut(); // Ends active session
                Intent intent = new Intent(FacultyProfileActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loadFacultyProfile() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String uid = currentUser.getUid();
            String authEmail = currentUser.getEmail();

            // Display email from Auth session immediately as a fallback/header preview
            if (authEmail != null) {
                if (tvProfileEmailHeader != null) tvProfileEmailHeader.setText(authEmail);
                if (tvEmail != null) tvEmail.setText(authEmail);
            }

            // Fetch specific user document from Firestore using the faculty's UID
            db.collection("Users").document(uid)
                .get()
                .addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        // Extract fields stored during faculty registration
                        String fullName = document.getString("fullName");
                        String employeeId = document.getString("employeeId");
                        String department = document.getString("department");
                        String email = document.getString("email");

                        // Update UI dynamically with fetched data or fallback defaults
                        if (tvProfileName != null) {
                            tvProfileName.setText(fullName != null && !fullName.isEmpty() ? fullName : "N/A");
                        }
                        if (tvEmployeeId != null) {
                            tvEmployeeId.setText(employeeId != null && !employeeId.isEmpty() ? employeeId : "N/A");
                        }
                        if (tvDepartment != null) {
                            tvDepartment.setText(department != null && !department.isEmpty() ? department : "N/A");
                        }
                        if (tvEmail != null && email != null) {
                            tvEmail.setText(email);
                        }
                    } else {
                        Toast.makeText(FacultyProfileActivity.this, "Profile data not found.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(FacultyProfileActivity.this, "Failed to load profile.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Redirect to login if no active user session exists
            Toast.makeText(this, "No active session. Redirecting to login...", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(FacultyProfileActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}