
package com.example.campussuperapp;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class ComplaintsActivity extends AppCompatActivity {

    EditText etTitle, etAffectedUsers, etDescription;
    Spinner spCategory, spSeverity, spLocation;
    Button btnSubmit;
    TextView tvPriorityResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_complaints);

        // Connect UI elements
        etTitle = findViewById(R.id.etComplaintTitle);
        etAffectedUsers = findViewById(R.id.etAffectedUsers);
        etDescription = findViewById(R.id.etComplaintDescription);

        spCategory = findViewById(R.id.spComplaintCategory);
        spSeverity = findViewById(R.id.spComplaintSeverity);
        spLocation = findViewById(R.id.spComplaintLocation);

        btnSubmit = findViewById(R.id.btnSubmitComplaint);
        tvPriorityResult = findViewById(R.id.tvPriorityResult);

        // Populate dropdowns
        setSpinner(spCategory, new String[]{
                "Select a category",
                "Classroom or Facilities",
                "Cleanliness",
                "IT or Internet",
                "Administration",
                "Safety",
                "Other"
        });

        setSpinner(spSeverity, new String[]{
                "Select severity",
                "Low",
                "Medium",
                "High",
                "Critical"
        });

        setSpinner(spLocation, new String[]{
                "Select location",
                "Classroom",
                "Building",
                "Laboratory",
                "Library",
                "Cafeteria",
                "Entire Campus",
                "Other"
        });

        // Submit button
        btnSubmit.setOnClickListener(v -> validateComplaint());
    }

    private void setSpinner(Spinner spinner, String[] items) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                R.layout.spinner_item,
                items
        );  adapter.setDropDownViewResource(
                R.layout.spinner_dropdown_item
        );

        spinner.setAdapter(adapter);
    }

    private void validateComplaint() {
        String title = etTitle.getText().toString().trim();
        String affectedText = etAffectedUsers.getText().toString().trim();
        String description = etDescription.getText().toString().trim();

        if (title.isEmpty()) {
            etTitle.setError("Please enter a complaint title");
            etTitle.requestFocus();
            return;
        }

        if (spCategory.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a category",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (spSeverity.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select severity",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (affectedText.isEmpty()) {
            etAffectedUsers.setError("Enter affected users");
            etAffectedUsers.requestFocus();
            return;
        }

        int affectedUsers;

        try {
            affectedUsers = Integer.parseInt(affectedText);
        } catch (NumberFormatException e) {
            etAffectedUsers.setError("Enter a valid whole number");
            etAffectedUsers.requestFocus();
            return;
        }

        if (affectedUsers < 1) {
            etAffectedUsers.setError("At least 1 user is required");
            etAffectedUsers.requestFocus();
            return;
        }

        if (spLocation.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select a location",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (description.isEmpty()) {
            etDescription.setError("Please describe the complaint");
            etDescription.requestFocus();
            return;
        }

        // Step 3: Calculate priority
        String severity = spSeverity.getSelectedItem().toString();
        String location = spLocation.getSelectedItem().toString();

        int severityPoints = getSeverityPoints(severity);
        int userPoints = getAffectedUserPoints(affectedUsers);
        int locationPoints = getLocationPoints(location);

        int totalScore = severityPoints + userPoints + locationPoints;

        String priority = calculatePriority(
                severity, totalScore
        );

        // Display the calculated result
        String category = spCategory.getSelectedItem().toString();

        String analysisText =
                "COMPLAINT ANALYSIS\n\n" +
                        "Title: " + title + "\n" +
                        "Category: " + category + "\n" +
                        "Severity: " + severity + "\n" +
                        "Affected users: " + affectedUsers + "\n" +
                        "Location: " + location + "\n\n" +
                        "PRIORITY: " + priority + "\n" +
                        "Total score: " + totalScore + "/11\n\n" +
                        "Score breakdown:\n" +
                        "Severity: " + severityPoints + "/4\n" +
                        "Affected users: " + userPoints + "/4\n" +
                        "Location impact: " + locationPoints + "/3\n\n" +
                        "Description: " + description + "\n\n" +
                        "Saving complaint to database...";

        tvPriorityResult.setVisibility(View.VISIBLE);
        tvPriorityResult.setText(analysisText);

        saveComplaint(
                title,
                category,
                severity,
                affectedUsers,
                location,
                description,
                priority,
                totalScore,
                analysisText
        );
    }
    private void saveComplaint(
            String title,
            String category,
            String severity,
            int affectedUsers,
            String location,
            String description,
            String priority,
            int score,
            String analysisText
    ) {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please log in before submitting.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        Map<String, Object> complaint = new HashMap<>();
        complaint.put("userId", userId);
        complaint.put("title", title);
        complaint.put("category", category);
        complaint.put("severity", severity);
        complaint.put("affectedUsers", affectedUsers);
        complaint.put("location", location);
        complaint.put("description", description);
        complaint.put("priority", priority);
        complaint.put("score", score);
        complaint.put("status", "Pending");
        complaint.put("createdAt", FieldValue.serverTimestamp());

        btnSubmit.setEnabled(false);

        FirebaseFirestore.getInstance()
                .collection("complaints")
                .add(complaint)
                .addOnSuccessListener(documentReference -> {

                    tvPriorityResult.setText(
                            analysisText.replace(
                                    "Saving complaint to database...",
                                    "Complaint saved successfully!"
                            )
                    );

                    Toast.makeText(
                            ComplaintsActivity.this,
                            "Complaint submitted successfully!",
                            Toast.LENGTH_LONG
                    ).show();

                    // Clear the form after a successful save.
                    etTitle.setText("");
                    etAffectedUsers.setText("");
                    etDescription.setText("");

                    spCategory.setSelection(0);
                    spSeverity.setSelection(0);
                    spLocation.setSelection(0);

                    btnSubmit.setEnabled(true);
                })
                .addOnFailureListener(e -> {

                    btnSubmit.setEnabled(true);

                    tvPriorityResult.setText(
                            analysisText.replace(
                                    "Saving complaint to database...",
                                    "Complaint could not be saved."
                            )
                    );

                    Toast.makeText(
                            ComplaintsActivity.this,
                            "Save failed: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
    private int getSeverityPoints(String severity) {
        switch (severity) {
            case "Low":
                return 1;
            case "Medium":
                return 2;
            case "High":
                return 3;
            case "Critical":
                return 4;
            default:
                return 1;
        }
    }

    private int getAffectedUserPoints(int users) {
        if (users >= 100) {
            return 4;
        } else if (users >= 50) {
            return 3;
        } else if (users >= 10) {
            return 2;
        } else {
            return 1;
        }
    }

    private int getLocationPoints(String location) {
        if (location.equals("Entire Campus")) {
            return 3;
        } else if (location.equals("Building")
                || location.equals("Laboratory")
                || location.equals("Library")
                || location.equals("Cafeteria")) {
            return 2;
        } else {
            return 1;
        }
    }

    private String calculatePriority(String severity, int score) {

        // Critical severity always receives critical priority.
        if (severity.equals("Critical")) {
            return "CRITICAL";
        }

        // High severity must receive at least high priority.
        if (severity.equals("High")) {
            return "HIGH";
        }

        if (score >= 9) {
            return "CRITICAL";
        } else if (score >= 7) {
            return "HIGH";
        } else if (score >= 5) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }


}