package com.example.campussuperapp;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class activity_admin_create_account extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etFullName, etAdminId, etEmail, etPassword, etConfirmPassword;
    private CheckBox cbTerms;
    private MaterialButton btnSubmitApproval;
    private TextView tvSignInLink, btnTabStudent, btnTabFaculty, btnTabAdmin;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_create_account);

        // Window insets safe padding
        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (View v, WindowInsetsCompat insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind View Components
        etFullName = findViewById(R.id.etFullName);
        etAdminId = findViewById(R.id.etAdminId);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnSubmitApproval = findViewById(R.id.btnSubmitApproval);
        tvSignInLink = findViewById(R.id.tvSignInLink);
        btnBack = findViewById(R.id.btnBack);

        // Bind Toggle Tabs
        btnTabStudent = findViewById(R.id.btnTabStudent);
        btnTabFaculty = findViewById(R.id.btnTabFaculty);
        btnTabAdmin = findViewById(R.id.btnTabAdmin);

        // Tab click listener to turn the clicked tab blue and navigate
        View.OnClickListener tabClickListener = v -> {
            // Reset all tabs to default unselected style
            btnTabStudent.setBackgroundResource(android.R.color.transparent);
            btnTabStudent.setTextColor(Color.parseColor("#64748B"));

            btnTabFaculty.setBackgroundResource(android.R.color.transparent);
            btnTabFaculty.setTextColor(Color.parseColor("#64748B"));

            btnTabAdmin.setBackgroundResource(android.R.color.transparent);
            btnTabAdmin.setTextColor(Color.parseColor("#64748B"));

            // Highlight the clicked tab blue
            TextView clickedTab = (TextView) v;
            clickedTab.setBackgroundResource(R.drawable.bg_role_selected);
            clickedTab.setTextColor(Color.parseColor("#FFFFFF"));

            // Handle navigation based on selected tab
            if (v.getId() == R.id.btnTabStudent) {
                startActivity(new Intent(this, activity_sign_in.class));
                finish();
            } else if (v.getId() == R.id.btnTabFaculty) {
                startActivity(new Intent(this, activity_faculty_sign_in.class));
                finish();
            } else if (v.getId() == R.id.btnTabAdmin) {
                // Already on Admin screen
            }
        };

        // Attach listeners to tabs
        if (btnTabStudent != null) btnTabStudent.setOnClickListener(tabClickListener);
        if (btnTabFaculty != null) btnTabFaculty.setOnClickListener(tabClickListener);
        if (btnTabAdmin != null) btnTabAdmin.setOnClickListener(tabClickListener);

        // Back button & Sign-in link
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        if (tvSignInLink != null) {
            tvSignInLink.setOnClickListener(v -> {
                startActivity(new Intent(this, activity_admin_sign_in.class));
                finish();
            });
        }

        // Submit approval action
        btnSubmitApproval.setOnClickListener(v -> registerAdminAccount());
    }

    private void registerAdminAccount() {
        String fullName = etFullName.getText().toString().trim();
        String adminId = etAdminId.getText().toString().trim();
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validations
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(adminId)) {
            etAdminId.setError("Admin ID is required");
            etAdminId.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!cbTerms.isChecked()) {
            Toast.makeText(this, "Please accept the Terms of Use and Privacy Policy", Toast.LENGTH_LONG).show();
            return;
        }

        btnSubmitApproval.setEnabled(false);

        // Create Firebase Auth User & Store in Firestore
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();

                        Map<String, Object> adminData = new HashMap<>();
                        adminData.put("fullName", fullName);
                        adminData.put("adminId", adminId);
                        adminData.put("email", email);
                        adminData.put("role", "Admin");

                        db.collection("Users").document(userId)
                                .set(adminData)
                                .addOnSuccessListener(aVoid -> {
                                    btnSubmitApproval.setEnabled(true);
                                    Toast.makeText(this, "Admin registration submitted for approval!", Toast.LENGTH_LONG).show();
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    btnSubmitApproval.setEnabled(true);
                                    Toast.makeText(this, "Database Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    } else {
                        btnSubmitApproval.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}