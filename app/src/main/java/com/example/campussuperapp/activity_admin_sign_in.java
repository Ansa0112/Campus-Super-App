package com.example.campussuperapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class activity_admin_sign_in extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etAdminEmail, etPassword;
    private CheckBox cbRememberMe;
    private MaterialButton btnSignInAdmin;
    private TextView tvRequestAccessLink, tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_sign_in);

        // Window insets padding configuration
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

        // Bind layout components (removed student/faculty switches)
        etAdminEmail = findViewById(R.id.etAdminEmail);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        btnSignInAdmin = findViewById(R.id.btnSignInAdmin);
        tvRequestAccessLink = findViewById(R.id.tvRequestAccessLink);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Admin sign-in action
        btnSignInAdmin.setOnClickListener(v -> loginAdminUser());

        // Forgot password action
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }

        // Navigate to Admin Create Account / Request Access screen
        if (tvRequestAccessLink != null) {
            tvRequestAccessLink.setOnClickListener(v -> {
                startActivity(new Intent(activity_admin_sign_in.this, activity_admin_create_account.class));
            });
        }
    }

    private void showForgotPasswordDialog() {
        String email = etAdminEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your Email above first, then click Forgot Password.", Toast.LENGTH_LONG).show();
            etAdminEmail.requestFocus();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Send a password reset link to: " + email + "?")
                .setPositiveButton("Send", (dialog, which) -> {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Reset link sent! Check your email.", Toast.LENGTH_LONG).show();
                                } else {
                                    String error = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                                    Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loginAdminUser() {
        String email = etAdminEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etAdminEmail.setError("Email is required");
            etAdminEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        btnSignInAdmin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // Query Firestore to verify role
                            db.collection("Users").document(userId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        btnSignInAdmin.setEnabled(true);
                                        if (documentSnapshot.exists()) {
                                            String role = documentSnapshot.getString("role");

                                            if ("Admin".equalsIgnoreCase(role)) {
                                                Toast.makeText(this, "Sign In Successful!", Toast.LENGTH_SHORT).show();

                                                // TODO: Navigate to your Admin Dashboard Activity here
                                                // startActivity(new Intent(this, activity_admin_dashboard.class));
                                                finish();
                                            } else {
                                                mAuth.signOut();
                                                Toast.makeText(this, "Access Denied: This account is not registered as an Admin.", Toast.LENGTH_LONG).show();
                                            }
                                        } else {
                                            mAuth.signOut();
                                            Toast.makeText(this, "User record not found in database.", Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        btnSignInAdmin.setEnabled(true);
                                        Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        btnSignInAdmin.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}