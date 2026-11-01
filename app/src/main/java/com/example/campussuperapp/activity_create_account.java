package com.example.campussuperapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class activity_create_account extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etFullName, etRollNoSuffix, etEmail, etPassword, etConfirmPassword;
    private Spinner spinnerProgram;
    private MaterialButton btnCreateAccount;
    private TextView tvSignInLink, btnRoleFaculty, btnRoleAdmin;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_account);

        // Window insets padding
        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Bind all views to match your advanced XML layout IDs perfectly
        etFullName = findViewById(R.id.etFullName);
        spinnerProgram = findViewById(R.id.spinnerProgram);
        etRollNoSuffix = findViewById(R.id.etRollNoSuffix);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnCreateAccount = findViewById(R.id.btnCreateAccount);
        tvSignInLink = findViewById(R.id.tvSignInLink);
        btnBack = findViewById(R.id.btnBack);
        btnRoleFaculty = findViewById(R.id.btnRoleFaculty);
        btnRoleAdmin = findViewById(R.id.btnRoleAdmin);

        // Populate the Program Spinner (e.g., BCS, BSE, BBA, etc.)
        String[] programs = {"BCS", "BSE", "BBA", "BSEE", "BSAF"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, programs);
        spinnerProgram.setAdapter(adapter);

        // Switch to Faculty Create Account screen when Faculty tab is clicked
        if (btnRoleFaculty != null) {
            btnRoleFaculty.setOnClickListener(v -> {
                startActivity(new Intent(activity_create_account.this, activity_faculty_create_account.class));
                finish();
            });
        }

        // Switch to Admin Create Account screen when Admin tab is clicked
        if (btnRoleAdmin != null) {
            btnRoleAdmin.setOnClickListener(v -> {
                startActivity(new Intent(activity_create_account.this, activity_admin_create_account.class));
                finish();
            });
        }

        // Back button action
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Register button action
        btnCreateAccount.setOnClickListener(v -> registerUser());

        // Navigation link to Sign In
        tvSignInLink.setOnClickListener(v -> {
            Intent intent = new Intent(activity_create_account.this, activity_sign_in.class);
            startActivity(intent);
            finish();
        });
    }

    private void registerUser() {
        String fullName = etFullName.getText().toString().trim();
        String selectedProgram = spinnerProgram.getSelectedItem() != null ? spinnerProgram.getSelectedItem().toString() : "";
        String rollSuffix = etRollNoSuffix.getText().toString().trim();
        String rollNo = selectedProgram + "-" + rollSuffix; // e.g., BCS-119
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Validations
        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(rollSuffix)) {
            etRollNoSuffix.setError("Roll number suffix is required");
            etRollNoSuffix.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email) || !email.endsWith("@students.fui.edu.pk")) {
            etEmail.setError("Valid email ending in @students.fui.edu.pk is required");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            etConfirmPassword.requestFocus();
            return;
        }

        btnCreateAccount.setEnabled(false);

        // Create user with Firebase Auth
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // Prepare Firestore document data
                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("fullName", fullName);
                            userMap.put("rollNo", rollNo);
                            userMap.put("email", email);
                            userMap.put("uid", userId);
                            userMap.put("role", "Student");

                            // Save profile to Cloud Firestore
                            db.collection("Users").document(userId)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        btnCreateAccount.setEnabled(true);
                                        Toast.makeText(activity_create_account.this, "Account Created Successfully!", Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(activity_create_account.this, activity_home.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        btnCreateAccount.setEnabled(true);
                                        Toast.makeText(activity_create_account.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        btnCreateAccount.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(activity_create_account.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}