package com.example.campussuperapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class TimetableActivity extends AppCompatActivity {

    private LinearLayout containerTimetables;
    private LinearLayout containerDayChips;
    private TextView tvEmptyTimetable;
    private Button btnUploadTimetable;
    private Spinner spFilterDepartment;

    private TextView btnModeGrid, btnModeList;
    private HorizontalScrollView scrollGridHorizontal;
    private ScrollView scrollListContainer;
    private TableLayout tableGridMatrix;
    private TextView tvActiveDayBanner;

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration timetableListener;

    private boolean isAdmin = false;
    private boolean isGridViewMode = true;
    private String selectedDepartmentFilter = "All Departments";
    private String selectedDayFilter = "All Days";

    private final String[] departments = new String[]{
            "All Departments", "BSSE", "BSCS", "BBAH", "BCA",
            "BCSE", "BSAF", "BSEE", "BSCE", "BSCP", "BSEN"
    };

    private final String[] uploadDepartments = new String[]{
            "BSSE", "BSCS", "BBAH", "BCA", "BCSE",
            "BSAF", "BSEE", "BSCE", "BSCP", "BSEN"
    };

    private final String[] filterDays = new String[]{
            "All Days", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private final String[] dayShortNames = new String[]{
            "All Days", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
    };

    private final String[] daysOfWeek = new String[]{
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
    };

    private final String[] standardTimeSlots = new String[]{
            "08:30 AM - 10:00 AM",
            "10:00 AM - 11:30 AM",
            "11:30 AM - 01:00 PM",
            "01:30 PM - 03:00 PM",
            "03:00 PM - 04:30 PM",
            "04:30 PM - 06:00 PM"
    };

    private final String[] gridTimeSlots = new String[]{
            "08:30 - 09:45",
            "09:50 - 11:05",
            "11:10 - 12:25",
            "12:30 - 13:45",
            "13:50 - 15:05"
    };

    private final String[] gridVenues = new String[]{
            "LH 22", "LH 20", "LH 14",
            "LH 23", "LH 24", "LH 25", "LH 26", "LH 27", "LH 28",
            "LH 42", "LH 43", "LH 44", "LH 52",
            "DLD Lab", "FYP Lab 1", "FYP Lab 2",
            "Prog. Lab - I", "Prog. Lab - II",
            "System Lab - I", "System Lab - II", "General Lab - VI"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        ImageView ivBack = findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish());
        }

        btnModeGrid = findViewById(R.id.btnModeGrid);
        btnModeList = findViewById(R.id.btnModeList);
        scrollGridHorizontal = findViewById(R.id.scrollGridHorizontal);
        scrollListContainer = findViewById(R.id.scrollListContainer);
        tableGridMatrix = findViewById(R.id.tableGridMatrix);
        tvActiveDayBanner = findViewById(R.id.tvActiveDayBanner);

        containerTimetables = findViewById(R.id.containerTimetables);
        containerDayChips = findViewById(R.id.containerDayChips);
        tvEmptyTimetable = findViewById(R.id.tvEmptyTimetable);
        btnUploadTimetable = findViewById(R.id.btnUploadTimetable);
        spFilterDepartment = findViewById(R.id.spFilterDepartment);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        setupModeSwitching();
        setupDayCalendarBar();
        setupFilterSpinner();
        checkAdminRoleAndLoadStats();

        if (btnUploadTimetable != null) {
            btnUploadTimetable.setOnClickListener(v -> showUploadDialog(null, null, null, null));
        }
    }

    private void setupModeSwitching() {
        if (btnModeGrid != null && btnModeList != null) {
            btnModeGrid.setOnClickListener(v -> {
                isGridViewMode = true;
                scrollGridHorizontal.setVisibility(View.VISIBLE);
                scrollListContainer.setVisibility(View.GONE);

                btnModeGrid.setBackgroundResource(R.drawable.bg_role_selected);
                btnModeGrid.setTextColor(Color.WHITE);
                btnModeGrid.setTypeface(null, Typeface.BOLD);

                btnModeList.setBackgroundColor(Color.TRANSPARENT);
                btnModeList.setTextColor(Color.parseColor("#475569"));
                btnModeList.setTypeface(null, Typeface.NORMAL);
            });

            btnModeList.setOnClickListener(v -> {
                isGridViewMode = false;
                scrollGridHorizontal.setVisibility(View.GONE);
                scrollListContainer.setVisibility(View.VISIBLE);

                btnModeList.setBackgroundResource(R.drawable.bg_role_selected);
                btnModeList.setTextColor(Color.WHITE);
                btnModeList.setTypeface(null, Typeface.BOLD);

                btnModeGrid.setBackgroundColor(Color.TRANSPARENT);
                btnModeGrid.setTextColor(Color.parseColor("#475569"));
                btnModeGrid.setTypeface(null, Typeface.NORMAL);
            });
        }
    }

    private void setupDayCalendarBar() {
        if (containerDayChips == null) return;
        containerDayChips.removeAllViews();

        for (int i = 0; i < filterDays.length; i++) {
            final String fullDay = filterDays[i];
            final String shortDay = dayShortNames[i];

            TextView chip = new TextView(this);
            chip.setText(shortDay);
            chip.setTextSize(13);
            chip.setPadding(dp(14), dp(8), dp(14), dp(8));

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(dp(4), 0, dp(4), 0);
            chip.setLayoutParams(params);

            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setCornerRadius(dp(20));

            if (fullDay.equalsIgnoreCase(selectedDayFilter)) {
                chipBg.setColor(Color.parseColor("#1E295B"));
                chip.setTextColor(Color.WHITE);
                chip.setTypeface(null, Typeface.BOLD);
            } else {
                chipBg.setColor(Color.WHITE);
                chipBg.setStroke(dp(1), Color.parseColor("#CBD5E1"));
                chip.setTextColor(Color.parseColor("#475569"));
                chip.setTypeface(null, Typeface.NORMAL);
            }

            chip.setBackground(chipBg);
            chip.setClickable(true);
            chip.setFocusable(true);

            chip.setOnClickListener(v -> {
                selectedDayFilter = fullDay;
                setupDayCalendarBar();
                listenToTimetables();
            });

            containerDayChips.addView(chip);
        }
    }

    private void setupFilterSpinner() {
        if (spFilterDepartment == null) return;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item,
                departments
        );
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spFilterDepartment.setAdapter(adapter);

        spFilterDepartment.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedDepartmentFilter = departments[position];
                listenToTimetables();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void checkAdminRoleAndLoadStats() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            String email = currentUser.getEmail();

            if (email != null && email.toLowerCase().contains("admin")) {
                isAdmin = true;
                if (btnUploadTimetable != null) {
                    btnUploadTimetable.setVisibility(View.VISIBLE);
                }
            }

            db.collection("Users").document(uid)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String role = documentSnapshot.getString("role");
                            String userRole = documentSnapshot.getString("userRole");

                            if ("Admin".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(userRole)) {
                                isAdmin = true;
                                if (btnUploadTimetable != null) {
                                    btnUploadTimetable.setVisibility(View.VISIBLE);
                                }
                            }
                        }
                        listenToTimetables();
                    })
                    .addOnFailureListener(e -> listenToTimetables());
        } else {
            listenToTimetables();
        }
    }

    private void listenToTimetables() {
        if (timetableListener != null) {
            timetableListener.remove();
        }

        if (tvActiveDayBanner != null) {
            tvActiveDayBanner.setText(selectedDayFilter + " - Weekly Class & Lab Schedule");
        }

        timetableListener = db.collection("timetables")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (containerTimetables == null) return;
                    containerTimetables.removeAllViews();

                    if (error != null) {
                        Toast.makeText(TimetableActivity.this, "Notice: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        renderGridMatrix(new HashMap<>());
                        if (tvEmptyTimetable != null) tvEmptyTimetable.setVisibility(View.VISIBLE);
                        return;
                    }

                    Map<String, DocumentSnapshot> bookedCardSlots = new LinkedHashMap<>();
                    Map<String, DocumentSnapshot> gridDocMap = new HashMap<>();

                    if (querySnapshot != null && !querySnapshot.isEmpty()) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String dept = doc.getString("department");
                            String day = doc.getString("day");
                            String room = doc.getString("room");
                            String timeSlot = doc.getString("timeSlot");

                            // Filter by Department
                            if (!"All Departments".equalsIgnoreCase(selectedDepartmentFilter)
                                    && dept != null && !dept.equalsIgnoreCase(selectedDepartmentFilter)) {
                                continue;
                            }

                            // Filter by Day
                            if (!"All Days".equalsIgnoreCase(selectedDayFilter)
                                    && day != null && !day.equalsIgnoreCase(selectedDayFilter)) {
                                continue;
                            }

                            if (timeSlot != null) {
                                bookedCardSlots.put(normalizeSlot(timeSlot), doc);
                            }

                            if (room != null && timeSlot != null) {
                                String key = matchGridKey(room, timeSlot);
                                gridDocMap.put(key, doc);
                            }
                        }
                    }

                    // Render 2D Grid Table Matrix View
                    renderGridMatrix(gridDocMap);

                    // Render Cards List View
                    for (String slot : standardTimeSlots) {
                        String normSlot = normalizeSlot(slot);
                        if (bookedCardSlots.containsKey(normSlot)) {
                            DocumentSnapshot doc = bookedCardSlots.remove(normSlot);
                            String docId = doc.getId();
                            String subject = getValue(doc, "subject", "Untitled Course");
                            String dept = getValue(doc, "department", "General");
                            String day = getValue(doc, "day", "Day N/A");
                            String timeSlot = getValue(doc, "timeSlot", slot);
                            String room = getValue(doc, "room", "Room N/A");
                            String instructor = getValue(doc, "instructor", "Instructor N/A");

                            addReservedSlotCard(docId, subject, dept, day, timeSlot, room, instructor);
                        } else {
                            addEmptySlotCard(slot);
                        }
                    }

                    for (Map.Entry<String, DocumentSnapshot> entry : bookedCardSlots.entrySet()) {
                        DocumentSnapshot doc = entry.getValue();
                        String docId = doc.getId();
                        String subject = getValue(doc, "subject", "Untitled Course");
                        String dept = getValue(doc, "department", "General");
                        String day = getValue(doc, "day", "Day N/A");
                        String timeSlot = getValue(doc, "timeSlot", "Custom Slot");
                        String room = getValue(doc, "room", "Room N/A");
                        String instructor = getValue(doc, "instructor", "Instructor N/A");

                        addReservedSlotCard(docId, subject, dept, day, timeSlot, room, instructor);
                    }

                    if (tvEmptyTimetable != null) {
                        tvEmptyTimetable.setVisibility(View.GONE);
                    }
                });
    }

    private String matchGridKey(String room, String timeSlot) {
        String normRoom = normalizeSlot(room);
        String normSlot = normalizeSlot(timeSlot);

        String matchedVenue = normRoom;
        for (String v : gridVenues) {
            String normV = normalizeSlot(v);
            if (normV.equals(normRoom) || normRoom.contains(normV) || normV.contains(normRoom)) {
                matchedVenue = normV;
                break;
            }
        }

        String matchedSlot = normSlot;
        for (String s : gridTimeSlots) {
            String normS = normalizeSlot(s);
            if (normS.equals(normSlot) || normSlot.contains(normS) || normS.contains(normSlot)) {
                matchedSlot = normS;
                break;
            }
        }

        return matchedVenue + "_" + matchedSlot;
    }

    private void renderGridMatrix(Map<String, DocumentSnapshot> gridDocMap) {
        if (tableGridMatrix == null) return;
        tableGridMatrix.removeAllViews();

        // 1. Table Header Row (Venue | Slot 1 | Slot 2 | Slot 3 | Slot 4 | Slot 5)
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(Color.parseColor("#1E295B"));

        TextView tvVenueHeader = createGridCell("Venue", dp(100), dp(46), Color.parseColor("#1E295B"), Color.WHITE, true, 12);
        headerRow.addView(tvVenueHeader);

        for (String slot : gridTimeSlots) {
            TextView tvSlotHeader = createGridCell(slot, dp(135), dp(46), Color.parseColor("#1E295B"), Color.WHITE, true, 12);
            headerRow.addView(tvSlotHeader);
        }

        tableGridMatrix.addView(headerRow);

        // 2. Table Data Rows (Venues x Time Slots)
        for (String venue : gridVenues) {
            TableRow row = new TableRow(this);
            row.setBackgroundColor(Color.parseColor("#CBD5E1"));

            // Venue Cell
            TextView tvVenue = createGridCell(venue, dp(100), dp(72), Color.parseColor("#F8FAFC"), Color.parseColor("#1E295B"), true, 12);
            row.addView(tvVenue);

            // Time Slot Cells
            for (String slot : gridTimeSlots) {
                String key = normalizeSlot(venue) + "_" + normalizeSlot(slot);
                DocumentSnapshot doc = gridDocMap.get(key);

                LinearLayout cellLayout = new LinearLayout(this);
                cellLayout.setOrientation(LinearLayout.VERTICAL);
                cellLayout.setGravity(Gravity.CENTER);
                cellLayout.setPadding(dp(6), dp(6), dp(6), dp(6));

                TableRow.LayoutParams cellParams = new TableRow.LayoutParams(dp(135), dp(72));
                cellParams.setMargins(dp(1), dp(1), dp(1), dp(1));
                cellLayout.setLayoutParams(cellParams);

                if (doc != null) {
                    // Booked Slot Cell
                    cellLayout.setBackgroundColor(Color.parseColor("#FFFBEB")); // Cream/gold tint

                    String subject = getValue(doc, "subject", "Subject");
                    String dept = getValue(doc, "department", "Dept");
                    String instructor = getValue(doc, "instructor", "Faculty");
                    String docId = doc.getId();

                    TextView tvSubject = new TextView(this);
                    tvSubject.setText(subject);
                    tvSubject.setTextSize(11);
                    tvSubject.setTextColor(Color.parseColor("#0F172A"));
                    tvSubject.setTypeface(null, Typeface.BOLD);
                    tvSubject.setGravity(Gravity.CENTER);
                    tvSubject.setSingleLine(true);

                    TextView tvDept = new TextView(this);
                    tvDept.setText(dept);
                    tvDept.setTextSize(12);
                    tvDept.setTextColor(Color.parseColor("#1E295B"));
                    tvDept.setTypeface(null, Typeface.BOLD);
                    tvDept.setGravity(Gravity.CENTER);

                    TextView tvInstructor = new TextView(this);
                    tvInstructor.setText(instructor);
                    tvInstructor.setTextSize(10);
                    tvInstructor.setTextColor(Color.parseColor("#475569"));
                    tvInstructor.setGravity(Gravity.CENTER);
                    tvInstructor.setSingleLine(true);

                    cellLayout.addView(tvSubject);
                    cellLayout.addView(tvDept);
                    cellLayout.addView(tvInstructor);

                    cellLayout.setClickable(true);
                    cellLayout.setFocusable(true);
                    cellLayout.setOnClickListener(v -> showSlotOptionsDialog(docId, subject, dept, venue, slot, instructor));
                } else {
                    // Empty Cell
                    cellLayout.setBackgroundColor(Color.WHITE);

                    if (isAdmin) {
                        TextView tvPlus = new TextView(this);
                        tvPlus.setText("+");
                        tvPlus.setTextSize(16);
                        tvPlus.setTextColor(Color.parseColor("#94A3B8"));
                        tvPlus.setGravity(Gravity.CENTER);
                        cellLayout.addView(tvPlus);

                        cellLayout.setClickable(true);
                        cellLayout.setFocusable(true);
                        cellLayout.setOnClickListener(v -> showUploadDialog(
                                venue,
                                slot,
                                selectedDayFilter,
                                selectedDepartmentFilter
                        ));
                    }
                }

                row.addView(cellLayout);
            }

            tableGridMatrix.addView(row);
        }
    }

    private TextView createGridCell(String text, int width, int height, int bgColor, int textColor, boolean bold, int textSize) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(textSize);
        tv.setTextColor(textColor);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(bgColor);

        TableRow.LayoutParams params = new TableRow.LayoutParams(width, height);
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        tv.setLayoutParams(params);

        if (bold) {
            tv.setTypeface(null, Typeface.BOLD);
        }
        return tv;
    }

    private void showSlotOptionsDialog(String docId, String subject, String dept, String venue, String timeSlot, String instructor) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(subject + " (" + dept + ")")
                .setMessage("Venue: " + venue + "\nTime: " + timeSlot + "\nInstructor: " + instructor)
                .setPositiveButton("Close", null);

        if (isAdmin) {
            builder.setNegativeButton("Delete Slot", (dialog, which) -> {
                if (auth.getCurrentUser() == null) {
                    auth.signInAnonymously().addOnCompleteListener(task -> deleteTimetableSlot(docId));
                } else {
                    deleteTimetableSlot(docId);
                }
            });
        }

        builder.show();
    }

    private void deleteTimetableSlot(String docId) {
        db.collection("timetables").document(docId).delete()
                .addOnSuccessListener(aVoid -> Toast.makeText(TimetableActivity.this, "Slot deleted successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(TimetableActivity.this, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String normalizeSlot(String slot) {
        if (slot == null) return "";
        return slot.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
    }

    private void addReservedSlotCard(
            String docId,
            String subject,
            String dept,
            String day,
            String timeSlot,
            String room,
            String instructor
    ) {
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

        // Header Row: Subject Name + RESERVED Badge
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);
        headerRow.setPadding(0, 0, 0, dp(8));

        TextView tvSubject = new TextView(this);
        tvSubject.setText(subject);
        tvSubject.setTextSize(17);
        tvSubject.setTextColor(Color.parseColor("#1E295B"));
        tvSubject.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        subParams.rightMargin = dp(8);
        tvSubject.setLayoutParams(subParams);

        TextView tvBadge = new TextView(this);
        tvBadge.setText("RESERVED");
        tvBadge.setTextSize(11);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setTextColor(Color.parseColor("#1D4ED8"));
        tvBadge.setPadding(dp(10), dp(4), dp(10), dp(4));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(Color.parseColor("#DBEAFE"));
        badgeBg.setCornerRadius(dp(8));
        tvBadge.setBackground(badgeBg);

        headerRow.addView(tvSubject);
        headerRow.addView(tvBadge);
        card.addView(headerRow);

        // Details Section
        if (dept != null && !dept.isEmpty()) {
            addInfoRow(card, "Department:", dept, "#2563EB");
        }
        addInfoRow(card, "Time Slot:", day + "  •  " + timeSlot, "#1E295B");
        addInfoRow(card, "Room / Venue:", room, "#D97706");
        addInfoRow(card, "Instructor:", instructor, "#0F172A");

        // Admin: Delete Button
        if (isAdmin) {
            View divider = new View(this);
            LinearLayout.LayoutParams divParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
            divParams.setMargins(0, dp(10), 0, dp(8));
            divider.setLayoutParams(divParams);
            divider.setBackgroundColor(Color.parseColor("#F1F5F9"));
            card.addView(divider);

            TextView btnDelete = new TextView(this);
            btnDelete.setText("Delete Slot");
            btnDelete.setTextColor(Color.parseColor("#DC2626"));
            btnDelete.setTextSize(13);
            btnDelete.setTypeface(null, Typeface.BOLD);
            btnDelete.setGravity(Gravity.END);
            btnDelete.setPadding(0, dp(4), 0, dp(4));
            btnDelete.setClickable(true);
            btnDelete.setFocusable(true);

            btnDelete.setOnClickListener(v -> new AlertDialog.Builder(this)
                    .setTitle("Delete Timetable Slot")
                    .setMessage("Are you sure you want to remove this timetable entry?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (auth.getCurrentUser() == null) {
                            auth.signInAnonymously().addOnCompleteListener(task -> deleteTimetableSlot(docId));
                        } else {
                            deleteTimetableSlot(docId);
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show());

            card.addView(btnDelete);
        }

        containerTimetables.addView(card);
    }

    private void addEmptySlotCard(String timeSlot) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#F8FAFC"));
        cardBg.setCornerRadius(dp(16));
        cardBg.setStroke(dp(1), Color.parseColor("#E2E8F0"));
        card.setBackground(cardBg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = dp(12);
        card.setLayoutParams(cardParams);

        // Header Row: Time Slot + AVAILABLE Badge
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTime = new TextView(this);
        tvTime.setText(timeSlot);
        tvTime.setTextSize(15);
        tvTime.setTextColor(Color.parseColor("#334155"));
        tvTime.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvTime.setLayoutParams(timeParams);

        TextView tvBadge = new TextView(this);
        tvBadge.setText("AVAILABLE");
        tvBadge.setTextSize(11);
        tvBadge.setTypeface(null, Typeface.BOLD);
        tvBadge.setTextColor(Color.parseColor("#15803D"));
        tvBadge.setPadding(dp(10), dp(4), dp(10), dp(4));

        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(Color.parseColor("#DCFCE7"));
        badgeBg.setCornerRadius(dp(8));
        tvBadge.setBackground(badgeBg);

        headerRow.addView(tvTime);
        headerRow.addView(tvBadge);
        card.addView(headerRow);

        TextView tvSubtitle = new TextView(this);
        tvSubtitle.setText("No class scheduled for this time slot");
        tvSubtitle.setTextSize(12);
        tvSubtitle.setTextColor(Color.parseColor("#94A3B8"));
        tvSubtitle.setPadding(0, dp(4), 0, 0);
        card.addView(tvSubtitle);

        if (isAdmin) {
            TextView btnReserve = new TextView(this);
            btnReserve.setText("+ Reserve This Slot");
            btnReserve.setTextColor(Color.parseColor("#2563EB"));
            btnReserve.setTextSize(13);
            btnReserve.setTypeface(null, Typeface.BOLD);
            btnReserve.setPadding(0, dp(8), 0, 0);
            btnReserve.setClickable(true);
            btnReserve.setFocusable(true);

            btnReserve.setOnClickListener(v -> showUploadDialog(
                    null,
                    timeSlot,
                    selectedDayFilter,
                    selectedDepartmentFilter
            ));

            card.addView(btnReserve);
        }

        containerTimetables.addView(card);
    }

    private void addInfoRow(LinearLayout parent, String label, String value, String valueColor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(3), 0, dp(3));

        TextView tvLabel = new TextView(this);
        tvLabel.setText(label);
        tvLabel.setTextSize(13);
        tvLabel.setTextColor(Color.parseColor("#64748B"));
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                dp(100), LinearLayout.LayoutParams.WRAP_CONTENT);
        tvLabel.setLayoutParams(labelParams);

        TextView tvVal = new TextView(this);
        tvVal.setText(value);
        tvVal.setTextSize(13);
        tvVal.setTextColor(Color.parseColor(valueColor));
        tvVal.setTypeface(null, Typeface.BOLD);

        row.addView(tvLabel);
        row.addView(tvVal);
        parent.addView(row);
    }

    private void showUploadDialog(String prefilledRoom, String prefilledTimeSlot, String prefilledDay, String prefilledDept) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload_timetable, null);

        EditText etSubject = dialogView.findViewById(R.id.etTtSubject);
        Spinner spDept = dialogView.findViewById(R.id.spTtDepartment);
        Spinner spDay = dialogView.findViewById(R.id.spTtDay);
        Spinner spTimeSlot = dialogView.findViewById(R.id.spTtTimeSlot);
        Spinner spRoom = dialogView.findViewById(R.id.spTtRoom);
        EditText etInstructor = dialogView.findViewById(R.id.etTtInstructor);
        Button btnSave = dialogView.findViewById(R.id.btnSaveTtEntry);

        // Populate Spinners
        ArrayAdapter<String> deptAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, uploadDepartments);
        deptAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spDept.setAdapter(deptAdapter);

        if (prefilledDept != null && !"All Departments".equalsIgnoreCase(prefilledDept)) {
            for (int i = 0; i < uploadDepartments.length; i++) {
                if (uploadDepartments[i].equalsIgnoreCase(prefilledDept)) {
                    spDept.setSelection(i);
                    break;
                }
            }
        }

        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, daysOfWeek);
        dayAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spDay.setAdapter(dayAdapter);

        if (prefilledDay != null && !"All Days".equalsIgnoreCase(prefilledDay)) {
            for (int i = 0; i < daysOfWeek.length; i++) {
                if (daysOfWeek[i].equalsIgnoreCase(prefilledDay)) {
                    spDay.setSelection(i);
                    break;
                }
            }
        }

        ArrayAdapter<String> roomAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, gridVenues);
        roomAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spRoom.setAdapter(roomAdapter);

        if (prefilledRoom != null && !prefilledRoom.isEmpty()) {
            for (int i = 0; i < gridVenues.length; i++) {
                if (gridVenues[i].equalsIgnoreCase(prefilledRoom)) {
                    spRoom.setSelection(i);
                    break;
                }
            }
        }

        ArrayAdapter<String> slotAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, gridTimeSlots);
        slotAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spTimeSlot.setAdapter(slotAdapter);

        if (prefilledTimeSlot != null && !prefilledTimeSlot.isEmpty()) {
            for (int i = 0; i < gridTimeSlots.length; i++) {
                if (gridTimeSlots[i].equalsIgnoreCase(prefilledTimeSlot)) {
                    spTimeSlot.setSelection(i);
                    break;
                }
            }
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        btnSave.setOnClickListener(v -> {
            String subject = etSubject.getText().toString().trim();
            String instructor = etInstructor.getText().toString().trim();

            if (subject.isEmpty()) {
                etSubject.setError("Enter subject name");
                etSubject.requestFocus();
                return;
            }

            String room = gridVenues[spRoom.getSelectedItemPosition()];
            String timeSlot = gridTimeSlots[spTimeSlot.getSelectedItemPosition()];
            String dept = uploadDepartments[spDept.getSelectedItemPosition()];
            String day = daysOfWeek[spDay.getSelectedItemPosition()];

            btnSave.setEnabled(false);

            if (auth.getCurrentUser() == null) {
                auth.signInAnonymously().addOnCompleteListener(task -> {
                    saveTimetableEntry(subject, dept, day, timeSlot, room, instructor, dialog, btnSave);
                });
            } else {
                saveTimetableEntry(subject, dept, day, timeSlot, room, instructor, dialog, btnSave);
            }
        });

        dialog.show();
    }

    private void saveTimetableEntry(
            String subject,
            String dept,
            String day,
            String timeSlot,
            String room,
            String instructor,
            AlertDialog dialog,
            Button btnSave
    ) {
        FirebaseUser user = auth.getCurrentUser();
        String uid = user != null ? user.getUid() : "admin_uid";

        Map<String, Object> entry = new HashMap<>();
        entry.put("userId", uid);
        entry.put("createdBy", uid);
        entry.put("subject", subject);
        entry.put("department", dept);
        entry.put("day", day);
        entry.put("timeSlot", timeSlot);
        entry.put("room", room);
        entry.put("instructor", instructor.isEmpty() ? "N/A" : instructor);
        entry.put("uploadedByRole", "Admin");
        entry.put("createdAt", FieldValue.serverTimestamp());

        db.collection("timetables")
                .add(entry)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(TimetableActivity.this, "Timetable slot uploaded!", Toast.LENGTH_SHORT).show();
                    selectedDayFilter = day;
                    setupDayCalendarBar();
                    if (dialog != null && dialog.isShowing()) {
                        dialog.dismiss();
                    }
                })
                .addOnFailureListener(e -> {
                    btnSave.setEnabled(true);
                    Toast.makeText(TimetableActivity.this, "Upload error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String getValue(DocumentSnapshot doc, String field, String defaultValue) {
        String val = doc.getString(field);
        return val != null && !val.trim().isEmpty() ? val : defaultValue;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timetableListener != null) {
            timetableListener.remove();
        }
    }
}
