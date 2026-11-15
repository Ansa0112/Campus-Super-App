package com.example.campussuperapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MyComplaintsActivity extends AppCompatActivity {

    private LinearLayout complaintsContainer;
    private TextView tvNoComplaints;
    private TextView tvMyComplaintsCount;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_complaints);

        ImageView ivBack = findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        complaintsContainer = findViewById(R.id.complaintsContainer);
        tvNoComplaints = findViewById(R.id.tvNoComplaints);
        tvMyComplaintsCount = findViewById(R.id.tvMyComplaintsCount);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (complaintsContainer != null) {
            loadMyComplaints();
        }
    }

    private void loadMyComplaints() {

        if (auth.getCurrentUser() == null) {
            Toast.makeText(
                    this,
                    "Please log in to view your complaints.",
                    Toast.LENGTH_LONG
            ).show();
            finish();
            return;
        }

        String userId = auth.getCurrentUser().getUid();

        db.collection("complaints")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    complaintsContainer.removeAllViews();

                    int count = querySnapshot.size();

                    tvMyComplaintsCount.setText(
                            "My Complaints (" + count + ")"
                    );

                    if (count == 0) {
                        tvNoComplaints.setVisibility(View.VISIBLE);
                        return;
                    }

                    tvNoComplaints.setVisibility(View.GONE);

                    for (DocumentSnapshot document :
                            querySnapshot.getDocuments()) {

                        String title = getText(document, "title");
                        String category = getText(document, "category");
                        String severity = getText(document, "severity");
                        String location = getText(document, "location");
                        String description = getText(document, "description");
                        String priority = getText(document, "priority");
                        String status = getText(document, "status");

                        Long scoreValue = document.getLong("score");
                        int score = scoreValue == null
                                ? 0 : scoreValue.intValue();

                        String dateText = "Pending";

                        if (document.getTimestamp("createdAt") != null) {
                            Date date = document.getTimestamp(
                                    "createdAt"
                            ).toDate();

                            SimpleDateFormat formatter =
                                    new SimpleDateFormat(
                                            "dd MMM yyyy, hh:mm a",
                                            Locale.getDefault()
                                    );

                            dateText = formatter.format(date);
                        }

                        addComplaintCard(
                                title,
                                category,
                                severity,
                                location,
                                description,
                                priority,
                                status,
                                score,
                                dateText
                        );
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(
                            MyComplaintsActivity.this,
                            "Could not load complaints: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private String getText(
            DocumentSnapshot document,
            String field
    ) {
        String value = document.getString(field);

        return value == null || value.trim().isEmpty()
                ? "Not specified"
                : value;
    }

    private void addComplaintCard(
            String title,
            String category,
            String severity,
            String location,
            String description,
            String priority,
            String status,
            int score,
            String date
    ) {
        // Outer Card Container
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.WHITE);
        cardBg.setCornerRadius(dp(16));
        cardBg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(14);
        card.setLayoutParams(cardParams);

        // Header Row inside Card: Title + Status Badge
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(8));

        TextView tvTitle = new TextView(this);
        tvTitle.setText(title);
        tvTitle.setTextSize(17);
        tvTitle.setTextColor(Color.parseColor("#1E295B"));
        tvTitle.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.rightMargin = dp(8);
        tvTitle.setLayoutParams(titleParams);

        // Status Badge
        TextView tvStatusBadge = new TextView(this);
        tvStatusBadge.setText(status);
        tvStatusBadge.setTextSize(11);
        tvStatusBadge.setTypeface(null, Typeface.BOLD);
        tvStatusBadge.setPadding(dp(10), dp(4), dp(10), dp(4));

        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setCornerRadius(dp(8));

        if (status.equalsIgnoreCase("Resolved")) {
            statusBg.setColor(Color.parseColor("#DCFCE7")); // Green 100
            tvStatusBadge.setTextColor(Color.parseColor("#15803D")); // Green 700
        } else if (status.equalsIgnoreCase("In Progress")) {
            statusBg.setColor(Color.parseColor("#DBEAFE")); // Blue 100
            tvStatusBadge.setTextColor(Color.parseColor("#1D4ED8")); // Blue 700
        } else { // Pending
            statusBg.setColor(Color.parseColor("#FEF3C7")); // Amber 100
            tvStatusBadge.setTextColor(Color.parseColor("#D97706")); // Amber 700
        }
        tvStatusBadge.setBackground(statusBg);

        headerRow.addView(tvTitle);
        headerRow.addView(tvStatusBadge);
        card.addView(headerRow);

        // Details Section
        addDetailRow(card, "Category:", category);
        addDetailRow(card, "Severity:", severity);
        addDetailRow(card, "Location:", location);
        addDetailRow(card, "Priority:", priority + " (" + score + "/11)");

        // Description
        TextView tvDescLabel = new TextView(this);
        tvDescLabel.setText("Description:");
        tvDescLabel.setTextSize(12);
        tvDescLabel.setTextColor(Color.parseColor("#64748B"));
        tvDescLabel.setPadding(0, dp(6), 0, dp(2));
        card.addView(tvDescLabel);

        TextView tvDescValue = new TextView(this);
        tvDescValue.setText(description);
        tvDescValue.setTextSize(13);
        tvDescValue.setTextColor(Color.parseColor("#334155"));
        tvDescValue.setPadding(0, 0, 0, dp(8));
        card.addView(tvDescValue);

        // Footer Divider & Date
        View divider = new View(this);
        LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divParams.setMargins(0, dp(4), 0, dp(8));
        divider.setLayoutParams(divParams);
        divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
        card.addView(divider);

        TextView tvDate = new TextView(this);
        tvDate.setText("Submitted: " + date);
        tvDate.setTextSize(11);
        tvDate.setTextColor(Color.parseColor("#94A3B8"));
        card.addView(tvDate);

        complaintsContainer.addView(card);
    }

    private void addDetailRow(LinearLayout parent, String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(2), 0, dp(2));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(13);
        tvLabel.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                dp(80), LinearLayout.LayoutParams.WRAP_CONTENT);
        tvLabel.setLayoutParams(labelParams);

        TextView tvValue = new TextView(this);
        tvValue.setText(value);
        tvValue.setTextSize(13);
        tvValue.setTextColor(Color.parseColor("#0F172A"));
        tvValue.setTypeface(null, Typeface.BOLD);

        row.addView(tvLabel);
        row.addView(tvValue);
        parent.addView(row);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
