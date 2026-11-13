
package com.example.campussuperapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
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
                            "My Complaints: " + count
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

                        String dateText = "Date: Pending";

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
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 24, 24, 24);

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(24);
        background.setStroke(2, Color.rgb(226, 232, 240));

        card.setBackground(background);
        card.setElevation(3);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = 24;
        card.setLayoutParams(cardParams);

        addText(card, title, 20, "#1E295B", true);
        addText(card, "Category: " + category, 14, "#475569", false);
        addText(card, "Severity: " + severity, 14, "#475569", false);
        addText(card, "Location: " + location, 14, "#475569", false);
        addText(card, "Priority: " + priority, 16, "#2563EB", true);
        addText(card, "Status: " + status, 15, "#15803D", true);
        addText(card, "Score: " + score + "/11", 14, "#475569", false);
        addText(card, "Description: " + description, 14, "#334155", false);
        addText(card, date, 12, "#64748B", false);

        complaintsContainer.addView(card);
    }

    private void addText(
            LinearLayout parent,
            String text,
            int size,
            String color,
            boolean bold
    ) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(size);
        textView.setTextColor(Color.parseColor(color));
        textView.setPadding(0, 5, 0, 5);

        if (bold) {
            textView.setTypeface(null, Typeface.BOLD);
        }

        parent.addView(textView);
    }
}
