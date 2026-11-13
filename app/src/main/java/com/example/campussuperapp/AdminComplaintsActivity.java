
package com.example.campussuperapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AdminComplaintsActivity extends AppCompatActivity {

    private TextView tvCount, tvMessage;
    private LinearLayout complaintsContainer;
    private ScrollView complaintsScroll;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_complaints);

        tvCount = findViewById(R.id.tvAdminComplaintsCount);
        tvMessage = findViewById(R.id.tvAdminComplaintsMessage);
        complaintsContainer = findViewById(R.id.adminComplaintsContainer);
        complaintsScroll = findViewById(R.id.adminComplaintsScroll);

        db = FirebaseFirestore.getInstance();

        loadAllComplaints();
    }

    private void loadAllComplaints() {
        tvCount.setText("Loading complaints...");
        tvMessage.setVisibility(View.GONE);
        complaintsScroll.setVisibility(View.VISIBLE);
        complaintsContainer.removeAllViews();

        db.collection("complaints")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    complaintsContainer.removeAllViews();

                    int total = querySnapshot.size();
                    tvCount.setText("All Complaints (" + total + ")");

                    if (total == 0) {
                        complaintsScroll.setVisibility(View.GONE);
                        tvMessage.setText(
                                "No complaints have been submitted yet."
                        );
                        tvMessage.setVisibility(View.VISIBLE);
                        return;
                    }

                    complaintsScroll.setVisibility(View.VISIBLE);

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {
                        addComplaintCard(document);
                    }
                })
                .addOnFailureListener(e -> {
                    tvCount.setText("Unable to load complaints");
                    complaintsScroll.setVisibility(View.GONE);

                    tvMessage.setText(
                            "Could not retrieve complaints. Check your " +
                                    "connection and Firestore permissions."
                    );
                    tvMessage.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            AdminComplaintsActivity.this,
                            "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void addComplaintCard(DocumentSnapshot document) {

        String title = getValue(
                document, "title", "Untitled complaint"
        );

        String category = getValue(
                document, "category", "Not specified"
        );

        String severity = getValue(
                document, "severity", "Not specified"
        );

        String location = getValue(
                document, "location", "Not specified"
        );

        String priority = getValue(
                document, "priority", "Not assigned"
        );

        String status = getValue(
                document, "status", "Pending"
        );

        String description = getValue(
                document, "description", "No description provided"
        );

        String userId = getValue(
                document, "userId", "Unknown"
        );

        String role = getValue(
                document, "submitterRole", "Role not recorded"
        );

        Long affectedUsers = document.getLong("affectedUsers");

        String affected = affectedUsers == null
                ? "Not specified"
                : String.valueOf(affectedUsers);

        String dateText = "Date not available";
        Date date = document.getDate("createdAt");

        if (date != null) {
            dateText = new SimpleDateFormat(
                    "dd MMM yyyy, hh:mm a",
                    Locale.getDefault()
            ).format(date);
        }

        // Complaint card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(Color.WHITE);
        cardBackground.setCornerRadius(dp(16));
        cardBackground.setStroke(
                dp(1), Color.rgb(226, 232, 240)
        );

        card.setBackground(cardBackground);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);

        // Complaint information
        card.addView(
                makeText(title, 18, "#1E295B", true)
        );

        card.addView(makeText(
                "Category: " + category + "   •   Severity: " + severity,
                13, "#475569", false
        ));

        card.addView(makeText(
                "Priority: " + priority + "   •   Status: " + status,
                14, "#2563EB", true
        ));

        card.addView(makeText(
                "Location: " + location, 14, "#334155", false
        ));

        card.addView(makeText(
                "Affected users: " + affected, 14, "#334155", false
        ));

        card.addView(makeText(
                "Submitted by: " + role, 14, "#334155", false
        ));

        card.addView(makeText(
                "User ID: " + userId, 12, "#64748B", false
        ));

        card.addView(makeText(
                "Submitted: " + dateText, 12, "#64748B", false
        ));

        TextView tvDescription = makeText(
                "Description: " + description,
                14, "#334155", false
        );

        tvDescription.setPadding(0, dp(8), 0, dp(8));
        card.addView(tvDescription);

        // Status label
        TextView statusLabel = makeText(
                "Update complaint status:",
                14, "#1E295B", true
        );

        statusLabel.setPadding(0, dp(8), 0, dp(4));
        card.addView(statusLabel);

        // Status dropdown container matching app theme
        RelativeLayout spinnerContainer = new RelativeLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
        );
        containerParams.topMargin = dp(6);
        containerParams.bottomMargin = dp(10);
        spinnerContainer.setLayoutParams(containerParams);
        spinnerContainer.setBackgroundResource(R.drawable.bg_input_field);
        spinnerContainer.setPadding(dp(14), 0, dp(14), 0);

        Spinner statusSpinner = new Spinner(this);
        statusSpinner.setBackgroundColor(Color.TRANSPARENT);
        statusSpinner.setPopupBackgroundResource(R.drawable.bg_dropdown_popup);

        ImageView arrowIcon = new ImageView(this);
        int arrowId = View.generateViewId();
        arrowIcon.setId(arrowId);
        arrowIcon.setImageResource(R.drawable.ic_chevron_down);
        arrowIcon.setColorFilter(Color.parseColor("#64748B"));

        RelativeLayout.LayoutParams arrowParams = new RelativeLayout.LayoutParams(
                dp(18),
                dp(18)
        );
        arrowParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        arrowParams.addRule(RelativeLayout.CENTER_VERTICAL);
        arrowIcon.setLayoutParams(arrowParams);

        RelativeLayout.LayoutParams spinnerParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        );
        spinnerParams.addRule(RelativeLayout.ALIGN_PARENT_START);
        spinnerParams.addRule(RelativeLayout.START_OF, arrowId);
        spinnerParams.addRule(RelativeLayout.CENTER_VERTICAL);
        statusSpinner.setLayoutParams(spinnerParams);

        String[] statuses = {
                "Pending",
                "In Progress",
                "Resolved"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                statuses
        );

        adapter.setDropDownViewResource(
                R.layout.spinner_dropdown_item
        );

        statusSpinner.setAdapter(adapter);

        // Select the current status
        int selectedPosition = 0;

        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(status)) {
                selectedPosition = i;
                break;
            }
        }

        statusSpinner.setSelection(selectedPosition);

        spinnerContainer.addView(statusSpinner);
        spinnerContainer.addView(arrowIcon);
        card.addView(spinnerContainer);

        // Update Status button matching header color (#1E295B)
        Button btnUpdateStatus = new Button(this);

        btnUpdateStatus.setText("Update Status");
        btnUpdateStatus.setTextColor(Color.WHITE);
        btnUpdateStatus.setTextSize(14);
        btnUpdateStatus.setAllCaps(false);
        btnUpdateStatus.setTypeface(null, Typeface.BOLD);
        btnUpdateStatus.setElevation(0);

        btnUpdateStatus.setBackgroundResource(R.drawable.bg_submit_complaint);

        LinearLayout.LayoutParams buttonParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(50)
                );

        buttonParams.topMargin = dp(4);
        btnUpdateStatus.setLayoutParams(buttonParams);

        card.addView(btnUpdateStatus);

        // Save status to Firestore
        btnUpdateStatus.setOnClickListener(v -> {

            String newStatus =
                    statusSpinner.getSelectedItem().toString();

            if (newStatus.equalsIgnoreCase(status)) {
                Toast.makeText(
                        AdminComplaintsActivity.this,
                        "Status is already " + status,
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            btnUpdateStatus.setEnabled(false);
            btnUpdateStatus.setText("Updating...");

            db.collection("complaints")
                    .document(document.getId())
                    .update("status", newStatus)
                    .addOnSuccessListener(unused -> {

                        Toast.makeText(
                                AdminComplaintsActivity.this,
                                "Complaint status updated",
                                Toast.LENGTH_SHORT
                        ).show();

                        loadAllComplaints();
                    })
                    .addOnFailureListener(e -> {

                        btnUpdateStatus.setEnabled(true);
                        btnUpdateStatus.setText("Update Status");

                        Toast.makeText(
                                AdminComplaintsActivity.this,
                                "Update failed: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show();
                    });
        });

        // Add completed card to the screen
        complaintsContainer.addView(card);
    }

    private String getValue(
            DocumentSnapshot document,
            String field,
            String defaultValue
    ) {
        String value = document.getString(field);

        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }

        return value;
    }

    private TextView makeText(
            String text,
            int size,
            String color,
            boolean bold
    ) {
        TextView view = new TextView(this);

        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(Color.parseColor(color));
        view.setPadding(0, dp(4), 0, dp(4));

        if (bold) {
            view.setTypeface(null, Typeface.BOLD);
        }

        return view;
    }

    private int dp(int value) {
        return (int) (
                value * getResources().getDisplayMetrics().density
                        + 0.5f
        );
    }
}
