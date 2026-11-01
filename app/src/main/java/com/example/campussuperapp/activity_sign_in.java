package com.example.campussuperapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class activity_sign_in extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etEmail, etPassword;
    private MaterialButton btnSignIn, btnNeedHelp;
    private TextView tvForgotPassword, tvCreateAccountLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        // Safe window insets handling matching the root view ID "@+id/main"
        View rootView = findViewById(R.id.main);
        if (rootView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
                var systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Initialize Firebase Auth instance
        mAuth = FirebaseAuth.getInstance();

        // Initialize all Views matching your exact activity_sign_in.xml IDs
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnSignIn = findViewById(R.id.btnSignIn);
        btnNeedHelp = findViewById(R.id.btnNeedHelp);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvCreateAccountLink = findViewById(R.id.tvCreateAccountLink);

        // 1. Sign In Button Click Listener
        btnSignIn.setOnClickListener(v -> loginUser());

        // 2. Forgot Password Click Listener (Opens Interactive Popup Dialog)
        tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // 3. Need Help / Contact Us Button Click Listener
        btnNeedHelp.setOnClickListener(v -> {
            Intent intent = new Intent(activity_sign_in.this, activity_contact_us.class);
            startActivity(intent);
        });

        // 4. Create Account Link Click Listener
        tvCreateAccountLink.setOnClickListener(v -> {
            Intent intent = new Intent(activity_sign_in.this, activity_create_account.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim().toLowerCase();
        String password = etPassword.getText().toString().trim();

        // --- FIELD VALIDATIONS ---
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("University email is required");
            etEmail.requestFocus();
            return;
        }

        // Validate FUI University Email Domain Format
        if (!email.endsWith("@students.fui.edu.pk") && !email.endsWith("@fui.edu.pk")) {
            etEmail.setError("Email must end with @students.fui.edu.pk");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Password is required");
            etPassword.requestFocus();
            return;
        }

        // --- FIREBASE AUTHENTICATION SIGN IN ---
        btnSignIn.setEnabled(false); // Temporarily disable button to prevent double-clicks

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    btnSignIn.setEnabled(true); // Re-enable button response

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Toast.makeText(activity_sign_in.this, "Sign In Successful!", Toast.LENGTH_SHORT).show();

                            // Navigate smoothly to Home Screen Activity
                            Intent intent = new Intent(activity_sign_in.this, activity_home.class);
                            startActivity(intent);
                            finish(); // Finish current activity so user can't click back into login
                        }
                    } else {
                        // Safe error capture to display precise reason on screen
                        String error = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Toast.makeText(activity_sign_in.this, "Sign In Failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showForgotPasswordDialog() {
        // Create an input field dynamically for the pop-up dialog
        final EditText inputEmail = new EditText(this);
        inputEmail.setHint("Enter your university email");
        inputEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // Pre-fill it if they already typed something in the main email field
        String currentText = etEmail.getText().toString().trim();
        if (!currentText.isEmpty()) {
            inputEmail.setText(currentText);
            inputEmail.setSelection(currentText.length());
        }

        // Build and display the interactive AlertDialog popup
        new AlertDialog.Builder(this)
                .setTitle("Reset Password")
                .setMessage("Enter your email address to receive a password reset link:")
                .setView(inputEmail)
                .setPositiveButton("Send", (dialog, which) -> {
                    String resetEmail = inputEmail.getText().toString().trim().toLowerCase();

                    if (TextUtils.isEmpty(resetEmail)) {
                        Toast.makeText(activity_sign_in.this, "Email cannot be empty.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    mAuth.sendPasswordResetEmail(resetEmail)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(activity_sign_in.this, "Password reset link sent! Check your inbox.", Toast.LENGTH_LONG).show();
                                } else {
                                    String error = task.getException() != null ? task.getException().getMessage() : "Failed to send reset email";
                                    Toast.makeText(activity_sign_in.this, "Error: " + error, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}