package com.example.campussuperapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
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

public class activity_faculty_create_account extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText etFullName, etEmployeeId, etEmail, etPassword, etConfirmPassword;
    private Spinner spinnerDepartment;
    private CheckBox cbTerms;
    private MaterialButton btnCreateFacultyAccount;
    private TextView tvSignInLink, btnRoleStudentTab, btnRoleAdminTab;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_faculty_create_account);

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

        etFullName = findViewById(R.id.etFullName);
        etEmployeeId = findViewById(R.id.etEmployeeId);
        etEmail = findViewById(R.id.etEmail);
        spinnerDepartment = findViewById(R.id.spinnerDepartment);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        cbTerms = findViewById(R.id.cbTerms);
        btnCreateFacultyAccount = findViewById(R.id.btnCreateFacultyAccount);
        tvSignInLink = findViewById(R.id.tvSignInLink);
        btnBack = findViewById(R.id.btnBack);
        btnRoleStudentTab = findViewById(R.id.btnRoleStudentTab);
        btnRoleAdminTab = findViewById(R.id.btnRoleAdminTab);

        // Official FUSST Departments & Faculties List
        String[] departments = {
                // Faculty of Engineering & IT
                "Department of Computer Science",
                "Department of Software Engineering",
                "Department of Electrical Engineering",
                "Department of Computer Engineering",
                "Department of Engineering Technology",

                // Faculty of Management Sciences
                "Department of Management Sciences",
                "Department of Accounting & Finance",
                "Department of Business Analytics & FinTech",

                // Faculty of Arts & Social Sciences
                "Department of Computer Arts",
                "Department of Media & Communication Studies",
                "Department of Psychology",
                "Department of English"
        };

        // Use custom navy theme layout to display the dropdown arrow
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_theme,
                departments
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(adapter);

        // Switch to Student Create Account screen
        if (btnRoleStudentTab != null) {
            btnRoleStudentTab.setOnClickListener(v -> {
                startActivity(new Intent(activity_faculty_create_account.this, activity_create_account.class));
                finish();
            });
        }

        // Switch to Admin Create Account screen
        if (btnRoleAdminTab != null) {
            btnRoleAdminTab.setOnClickListener(v -> {
                startActivity(new Intent(activity_faculty_create_account.this, activity_admin_create_account.class));
                finish();
            });
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        btnCreateFacultyAccount.setOnClickListener(v -> registerFacultyUser());

        tvSignInLink.setOnClickListener(v -> {
            startActivity(new Intent(activity_faculty_create_account.this, activity_faculty_sign_in.class));
            finish();
        });
    }

    private void registerFacultyUser() {
        String fullName = etFullName.getText().toString().trim();
        String employeeId = etEmployeeId.getText().toString().trim();
        String email = etEmail.getText().toString().trim().toLowerCase();
        String department = spinnerDepartment.getSelectedItem() != null ? spinnerDepartment.getSelectedItem().toString() : "";

        if (TextUtils.isEmpty(fullName)) {
            etFullName.setError("Full name is required");
            etFullName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(employeeId)) {
            etEmployeeId.setError("Employee ID is required");
            etEmployeeId.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Email is required");
            etEmail.requestFocus();
            return;
        }

        // Strictly enforce faculty email domain
        if (!email.endsWith("@faculty.fui.edu.pk")) {
            etEmail.setError("Faculty email must end with @faculty.fui.edu.pk");
            etEmail.requestFocus();
            return;
        }

        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

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

        if (cbTerms != null && !cbTerms.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms of Use and Privacy Policy", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateFacultyAccount.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        if (firebaseUser != null) {
                            String userId = firebaseUser.getUid();

                            Map<String, Object> userMap = new HashMap<>();
                            userMap.put("fullName", fullName);
                            userMap.put("employeeId", employeeId);
                            userMap.put("department", department);
                            userMap.put("email", email);
                            userMap.put("uid", userId);
                            userMap.put("role", "Faculty");

                            db.collection("Users").document(userId)
                                    .set(userMap)
                                    .addOnSuccessListener(aVoid -> {
                                        btnCreateFacultyAccount.setEnabled(true);
                                        Toast.makeText(activity_faculty_create_account.this, "Faculty Account Created Successfully!", Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(activity_faculty_create_account.this, HomeActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        btnCreateFacultyAccount.setEnabled(true);
                                        Toast.makeText(activity_faculty_create_account.this, "Firestore Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        btnCreateFacultyAccount.setEnabled(true);
                        String error = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(activity_faculty_create_account.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }
}