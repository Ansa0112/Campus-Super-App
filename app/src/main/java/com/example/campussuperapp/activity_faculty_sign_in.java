package com.example.campussuperapp;

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

public class activity_faculty_sign_in extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etIdentifier, etPassword;
    private CheckBox cbRememberMe;
    private MaterialButton btnSignInFaculty;
    private TextView tvForgotPassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_faculty_sign_in);

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

        // Bind layout components
        etIdentifier = findViewById(R.id.etIdentifier);
        etPassword = findViewById(R.id.etPassword);
        cbRememberMe = findViewById(R.id.cbRememberMe);
        btnSignInFaculty = findViewById(R.id.btnSignInFaculty);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // Faculty sign-in action
        btnSignInFaculty.setOnClickListener(v -> loginFacultyUser());

        // Forgot password click listener
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }
    }

    private void showForgotPasswordDialog() {
        // Create an input field directly inside the reset dialog box
        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("Enter your email address");
        inputEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // Pre-fill with whatever text is already in the email box if available
        String currentText = etIdentifier.getText().toString().trim();
        if (!currentText.isEmpty()) {
            inputEmail.setText(currentText);
            inputEmail.setSelection(currentText.length());
        }

        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email address to receive a password reset link:")
                .setView(inputEmail)
                .setPositiveButton("Send", (dialog, which) -> {
                    String resetEmail = inputEmail.getText().toString().trim();

                    if (TextUtils.isEmpty(resetEmail)) {
                        Toast.makeText(this, "Email cannot be empty.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Send password reset email via Firebase Auth
                    mAuth.sendPasswordResetEmail(resetEmail)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, "Reset link sent! Please check your email inbox.", Toast.LENGTH_LONG).show();
                                } else {
                                    String error = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email.";
                                    Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loginFacultyUser() {
        String email = etIdentifier.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etIdentifier.setError("Email is required");
            etIdentifier.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        btnSignInFaculty.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            // Query Firestore to verify role
                            db.collection("Users").document(userId).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        btnSignInFaculty.setEnabled(true);
                                        if (documentSnapshot.exists()) {
                                            String role = documentSnapshot.getString("role");

                                            if ("Faculty".equalsIgnoreCase(role)) {
                                                Toast.makeText(this, "Sign In Successful!", Toast.LENGTH_SHORT).show();

                                                // TODO: Navigate to your Faculty Dashboard Activity here
                                                // startActivity(new Intent(this, activity_faculty_dashboard.class));
                                                finish();
                                            } else {
                                                mAuth.signOut();
                                                Toast.makeText(this, "Access Denied: This account is not registered as Faculty.", Toast.LENGTH_LONG).show();
                                            }
                                        } else {
                                            mAuth.signOut();
                                            Toast.makeText(this, "User record not found in database.", Toast.LENGTH_LONG).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        btnSignInFaculty.setEnabled(true);
                                        Toast.makeText(this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        btnSignInFaculty.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}