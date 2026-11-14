package com.example.campussuperapp.events;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campussuperapp.R;
import com.example.campussuperapp.common.TimeUtils;
import com.example.campussuperapp.common.UiUtils;
import com.example.campussuperapp.common.UserSession;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

public class EventDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "event_id";

    private String id;
    private UserSession session;
    private Event current;
    private ListenerRegistration registration;
    private boolean busy = false;

    private TextView tvCategory, tvTitle, tvDay, tvMonth, tvDateTime, tvVenue, tvOrganizer,
            tvDescription, tvSeats, tvBanner;
    private ProgressBar progressSeats;
    private MaterialButton btnRegister, btnCalendar, btnAttendees, btnApprove, btnPublish, btnEdit, btnDelete;
    private View layoutManage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);
        UiUtils.padForSystemBars(findViewById(R.id.header), true, false);

        id = getIntent().getStringExtra(EXTRA_ID);
        if (id == null) { finish(); return; }

        tvCategory = findViewById(R.id.tvCategory);
        tvTitle = findViewById(R.id.tvTitle);
        tvDay = findViewById(R.id.tvDay);
        tvMonth = findViewById(R.id.tvMonth);
        tvDateTime = findViewById(R.id.tvDateTime);
        tvVenue = findViewById(R.id.tvVenue);
        tvOrganizer = findViewById(R.id.tvOrganizer);
        tvDescription = findViewById(R.id.tvDescription);
        tvSeats = findViewById(R.id.tvSeats);
        tvBanner = findViewById(R.id.tvBanner);
        progressSeats = findViewById(R.id.progressSeats);
        btnRegister = findViewById(R.id.btnRegister);
        btnCalendar = findViewById(R.id.btnCalendar);
        btnAttendees = findViewById(R.id.btnAttendees);
        btnApprove = findViewById(R.id.btnApprove);
        btnPublish = findViewById(R.id.btnPublish);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        layoutManage = findViewById(R.id.layoutManage);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnShare = findViewById(R.id.btnShare);

        btnBack.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> share());
        btnRegister.setOnClickListener(v -> onRegisterClicked());
        btnCalendar.setOnClickListener(v -> addToCalendar());
        btnAttendees.setOnClickListener(v -> showAttendees());
        btnApprove.setOnClickListener(v -> update("approved", true, "Event approved"));
        btnPublish.setOnClickListener(v -> update("published", !current.isPublished(),
                current.isPublished() ? "Moved to drafts" : "Published"));
        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, CreateEventActivity.class);
            i.putExtra(CreateEventActivity.EXTRA_ID, id);
            startActivity(i);
        });
        btnDelete.setOnClickListener(v -> UiUtils.confirm(this, "Delete event?",
                "Registrations for this event will no longer be reachable.", "Delete", this::deleteNow));

        layoutManage.setVisibility(View.GONE);
        btnAttendees.setVisibility(View.GONE);
        btnRegister.setEnabled(false);

        UserSession.load(new UserSession.Callback() {
            @Override public void onReady(@NonNull UserSession s) {
                if (isFinishing() || isDestroyed()) return;
                session = s;
                registration = EventRepository.listenOne(id, new EventRepository.ItemListener() {
                    @Override public void onData(@NonNull Event e) { render(e); }

                    @Override public void onDeleted() {
                        UiUtils.toast(EventDetailActivity.this, "This event was removed");
                        finish();
                    }

                    @Override public void onError(@NonNull String message) {
                        UiUtils.toast(EventDetailActivity.this, message);
                    }
                });
            }

            @Override public void onError(@NonNull String message) {
                UiUtils.toast(EventDetailActivity.this, message);
            }
        });
    }

    private void render(Event e) {
        current = e;
        UiUtils.styleTag(tvCategory, UiUtils.categoryColor(e.getCategory()));
        tvCategory.setText(e.getCategory() == null ? "Event" : e.getCategory());
        tvTitle.setText(e.getTitle());
        tvDay.setText(TimeUtils.day(e.getEventTime()));
        tvMonth.setText(TimeUtils.month(e.getEventTime()));
        tvDateTime.setText(TimeUtils.fullDateTime(e.getEventTime()));
        tvVenue.setText(e.getVenue());
        tvOrganizer.setText("Organised by " + (e.getOrganizerName() == null ? "Campus Office" : e.getOrganizerName()));
        tvDescription.setText(e.getDescription());

        // seats
        if (e.getCapacity() > 0) {
            tvSeats.setText(e.getRegisteredCount() + " / " + e.getCapacity() + " registered  •  "
                    + e.seatsLeft() + " seats left");
            progressSeats.setVisibility(View.VISIBLE);
            progressSeats.setMax((int) e.getCapacity());
            progressSeats.setProgress((int) Math.min(e.getRegisteredCount(), e.getCapacity()));
        } else {
            tvSeats.setText(e.getRegisteredCount() + " registered  •  Unlimited seats");
            progressSeats.setVisibility(View.GONE);
        }

        // banner
        if (!e.isApproved()) {
            tvBanner.setText("Waiting for admin approval. Students cannot see this event yet.");
            tvBanner.setVisibility(View.VISIBLE);
        } else if (!e.isPublished()) {
            tvBanner.setText("Draft. Only you and the admin can see this.");
            tvBanner.setVisibility(View.VISIBLE);
        } else {
            tvBanner.setVisibility(View.GONE);
        }

        // manage section
        boolean manage = EventRepository.canManage(session, e);
        layoutManage.setVisibility(manage ? View.VISIBLE : View.GONE);
        btnAttendees.setVisibility(manage ? View.VISIBLE : View.GONE);
        btnApprove.setVisibility(session.isAdmin() && !e.isApproved() ? View.VISIBLE : View.GONE);
        btnPublish.setText(e.isPublished() ? "Make draft" : "Publish");

        updateRegisterButton();
    }

    private void updateRegisterButton() {
        if (current == null) return;
        boolean registered = current.isRegistered(session.uid);

        if (!current.isApproved() || !current.isPublished()) {
            btnRegister.setText("Not open for registration");
            btnRegister.setEnabled(false);
        } else if (!current.isUpcoming()) {
            btnRegister.setText("Event ended");
            btnRegister.setEnabled(false);
        } else if (registered) {
            btnRegister.setText("Cancel registration");
            btnRegister.setEnabled(!busy);
        } else if (current.isFull()) {
            btnRegister.setText("Event full");
            btnRegister.setEnabled(false);
        } else {
            btnRegister.setText("Register now");
            btnRegister.setEnabled(!busy);
        }
    }

    private void onRegisterClicked() {
        if (current == null || busy) return;
        busy = true;
        btnRegister.setEnabled(false);

        EventRepository.DoneCallback cb = new EventRepository.DoneCallback() {
            @Override public void onSuccess() {
                busy = false;
                UiUtils.toast(EventDetailActivity.this, "Done");
                updateRegisterButton();
            }

            @Override public void onError(@NonNull String message) {
                busy = false;
                UiUtils.toast(EventDetailActivity.this, message);
                updateRegisterButton();
            }
        };
        if (current.isRegistered(session.uid)) EventRepository.unregister(session, id, cb);
        else EventRepository.register(session, id, cb);
    }

    private void addToCalendar() {
        if (current == null || current.getEventTime() == null) return;
        long start = current.getEventTime().getTime();
        Intent i = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, current.getTitle())
                .putExtra(CalendarContract.Events.EVENT_LOCATION, current.getVenue())
                .putExtra(CalendarContract.Events.DESCRIPTION, current.getDescription())
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 2 * 60 * 60 * 1000L);
        try {
            startActivity(i);
        } catch (ActivityNotFoundException ex) {
            UiUtils.toast(this, "No calendar app found on this phone");
        }
    }

    private void showAttendees() {
        EventRepository.fetchAttendees(id, new EventRepository.AttendeesCallback() {
            @Override public void onData(@NonNull List<String> lines) {
                MaterialAlertDialogBuilder b = new MaterialAlertDialogBuilder(EventDetailActivity.this)
                        .setTitle("Registered (" + lines.size() + ")")
                        .setPositiveButton("Close", null);
                if (lines.isEmpty()) b.setMessage("Nobody has registered yet.");
                else b.setItems(lines.toArray(new String[0]), null);
                b.show();
            }

            @Override public void onError(@NonNull String message) {
                UiUtils.toast(EventDetailActivity.this, message);
            }
        });
    }

    private void update(String field, Object value, final String successMessage) {
        EventRepository.updateField(id, field, value, new EventRepository.DoneCallback() {
            @Override public void onSuccess() { UiUtils.toast(EventDetailActivity.this, successMessage); }
            @Override public void onError(@NonNull String message) { UiUtils.toast(EventDetailActivity.this, message); }
        });
    }

    private void deleteNow() {
        EventRepository.delete(id, new EventRepository.DoneCallback() {
            @Override public void onSuccess() {
                UiUtils.toast(EventDetailActivity.this, "Event deleted");
                finish();
            }

            @Override public void onError(@NonNull String message) {
                UiUtils.toast(EventDetailActivity.this, message);
            }
        });
    }

    private void share() {
        if (current == null) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, current.getTitle() + "\n"
                + TimeUtils.fullDateTime(current.getEventTime()) + "\n"
                + "Venue: " + current.getVenue() + "\n\n" + current.getDescription()
                + "\n\n- Shared from Campus Super App");
        startActivity(Intent.createChooser(send, "Share event"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
