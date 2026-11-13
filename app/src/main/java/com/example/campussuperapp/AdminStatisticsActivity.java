package com.example.campussuperapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.Map;

public class AdminStatisticsActivity extends AppCompatActivity {

    private TextView tvStatTotal, tvStatPending, tvStatInProgress, tvStatResolved;
    private TextView tvStatCritical, tvStatHigh, tvStatMedium, tvStatLow;
    private TextView tvStatTotalAffected, tvStatAvgAffected;
    private LinearLayout containerCategoryStats;

    private FirebaseFirestore db;
    private ListenerRegistration complaintsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_statistics);

        ImageButton btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatPending = findViewById(R.id.tvStatPending);
        tvStatInProgress = findViewById(R.id.tvStatInProgress);
        tvStatResolved = findViewById(R.id.tvStatResolved);

        tvStatCritical = findViewById(R.id.tvStatCritical);
        tvStatHigh = findViewById(R.id.tvStatHigh);
        tvStatMedium = findViewById(R.id.tvStatMedium);
        tvStatLow = findViewById(R.id.tvStatLow);

        tvStatTotalAffected = findViewById(R.id.tvStatTotalAffected);
        tvStatAvgAffected = findViewById(R.id.tvStatAvgAffected);

        containerCategoryStats = findViewById(R.id.containerCategoryStats);

        db = FirebaseFirestore.getInstance();

        listenToAllStats();
    }

    private void listenToAllStats() {
        complaintsListener = db.collection("complaints")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null || querySnapshot == null) {
                        return;
                    }

                    int total = querySnapshot.size();
                    int pending = 0;
                    int inProgress = 0;
                    int resolved = 0;

                    int critical = 0;
                    int high = 0;
                    int medium = 0;
                    int low = 0;

                    long totalAffected = 0;

                    Map<String, Integer> categoryCounts = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if (status != null) {
                            if (status.equalsIgnoreCase("Pending")) {
                                pending++;
                            } else if (status.equalsIgnoreCase("In Progress")) {
                                inProgress++;
                            } else if (status.equalsIgnoreCase("Resolved")) {
                                resolved++;
                            }
                        }

                        String severity = doc.getString("severity");
                        if (severity != null) {
                            if (severity.equalsIgnoreCase("Critical")) {
                                critical++;
                            } else if (severity.equalsIgnoreCase("High")) {
                                high++;
                            } else if (severity.equalsIgnoreCase("Medium")) {
                                medium++;
                            } else if (severity.equalsIgnoreCase("Low")) {
                                low++;
                            }
                        }

                        Long affected = doc.getLong("affectedUsers");
                        if (affected != null) {
                            totalAffected += affected;
                        }

                        String category = doc.getString("category");
                        if (category == null || category.trim().isEmpty()) {
                            category = "Other";
                        }
                        categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
                    }

                    // Update UI
                    tvStatTotal.setText(String.valueOf(total));
                    tvStatPending.setText(String.valueOf(pending));
                    tvStatInProgress.setText(String.valueOf(inProgress));
                    tvStatResolved.setText(String.valueOf(resolved));

                    tvStatCritical.setText(String.valueOf(critical));
                    tvStatHigh.setText(String.valueOf(high));
                    tvStatMedium.setText(String.valueOf(medium));
                    tvStatLow.setText(String.valueOf(low));

                    tvStatTotalAffected.setText(String.valueOf(totalAffected));
                    int avgAffected = total > 0 ? (int) Math.round((double) totalAffected / total) : 0;
                    tvStatAvgAffected.setText(String.valueOf(avgAffected));

                    updateCategoryList(categoryCounts, total);
                });
    }

    private void updateCategoryList(Map<String, Integer> categoryCounts, int total) {
        if (containerCategoryStats == null) return;

        containerCategoryStats.removeAllViews();

        if (categoryCounts.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No categories recorded yet.");
            emptyText.setTextColor(Color.parseColor("#64748B"));
            emptyText.setTextSize(13);
            containerCategoryStats.addView(emptyText);
            return;
        }

        boolean isFirst = true;
        for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
            if (!isFirst) {
                View divider = new View(this);
                LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                divParams.setMargins(0, dp(8), 0, dp(8));
                divider.setLayoutParams(divParams);
                divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
                containerCategoryStats.addView(divider);
            }
            isFirst = false;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(4), 0, dp(4));

            TextView tvCategory = new TextView(this);
            tvCategory.setText(entry.getKey());
            tvCategory.setTextColor(Color.parseColor("#0F172A"));
            tvCategory.setTextSize(14);
            LinearLayout.LayoutParams catParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            tvCategory.setLayoutParams(catParams);

            TextView tvCount = new TextView(this);
            int percent = total > 0 ? (entry.getValue() * 100 / total) : 0;
            tvCount.setText(entry.getValue() + " (" + percent + "%)");
            tvCount.setTextColor(Color.parseColor("#1E295B"));
            tvCount.setTypeface(null, Typeface.BOLD);
            tvCount.setTextSize(14);

            row.addView(tvCategory);
            row.addView(tvCount);

            containerCategoryStats.addView(row);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (complaintsListener != null) {
            complaintsListener.remove();
        }
    }
}
