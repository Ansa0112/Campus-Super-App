package com.example.campussuperapp.events;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campussuperapp.R;
import com.example.campussuperapp.common.Constants;
import com.example.campussuperapp.common.TimeUtils;
import com.example.campussuperapp.common.UiUtils;
import com.example.campussuperapp.common.UserSession;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Calendar;
import java.util.Date;

/** Create or edit an event (edit mode = an id is passed in the Intent). */
public class CreateEventActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "event_id";

    private String editId;
    private UserSession session;

    private final Calendar when = Calendar.getInstance();
    private boolean dateSet = false;
    private boolean timeSet = false;

    private TextInputLayout tilTitle, tilDescription, tilVenue, tilDate, tilTime;
    private TextInputEditText etTitle, etDescription, etVenue, etDate, etTime, etCapacity;
    private AutoCompleteTextView actCategory;
    private MaterialSwitch switchPublish;
    private TextView tvFormTitle, tvApprovalNote;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_event);
        UiUtils.padForSystemBars(findViewById(R.id.header), true, false);

        editId = getIntent().getStringExtra(EXTRA_ID);

        tilTitle = findViewById(R.id.tilTitle);
        tilDescription = findViewById(R.id.tilDescription);
        tilVenue = findViewById(R.id.tilVenue);
        tilDate = findViewById(R.id.tilDate);
        tilTime = findViewById(R.id.tilTime);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etVenue = findViewById(R.id.etVenue);
        etDate = findViewById(R.id.etDate);
        etTime = findViewById(R.id.etTime);
        etCapacity = findViewById(R.id.etCapacity);
        actCategory = findViewById(R.id.actCategory);
        switchPublish = findViewById(R.id.switchPublish);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvApprovalNote = findViewById(R.id.tvApprovalNote);
        btnSave = findViewById(R.id.btnSave);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        actCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Constants.EVENT_CATEGORIES));
        actCategory.setText(Constants.EVENT_CATEGORIES[0], false);
        switchPublish.setChecked(true);

        when.set(Calendar.SECOND, 0);
        when.set(Calendar.MILLISECOND, 0);

        etDate.setOnClickListener(v -> pickDate());
        etTime.setOnClickListener(v -> pickTime());
        tvFormTitle.setText(editId == null ? "New event" : "Edit event");
        btnSave.setEnabled(false);
        btnSave.setOnClickListener(v -> save());

        UserSession.load(new UserSession.Callback() {
            @Override public void onReady(@NonNull UserSession s) {
                if (isFinishing() || isDestroyed()) return;
                session = s;
                if (!s.canPublish()) {
                    UiUtils.toast(CreateEventActivity.this, "Only faculty and admin can create events");
                    finish();
                    return;
                }
                tvApprovalNote.setVisibility(s.isFaculty() && editId == null
                        ? android.view.View.VISIBLE : android.view.View.GONE);
                btnSave.setEnabled(true);
                if (editId != null) loadForEdit();
            }

            @Override public void onError(@NonNull String message) {
                UiUtils.toast(CreateEventActivity.this, message);
                finish();
            }
        });
    }

    private void pickDate() {
        DatePickerDialog dp = new DatePickerDialog(this, (view, y, m, d) -> {
            when.set(y, m, d);
            dateSet = true;
            etDate.setText(TimeUtils.dateOnly(when.getTime()));
            tilDate.setError(null);
        }, when.get(Calendar.YEAR), when.get(Calendar.MONTH), when.get(Calendar.DAY_OF_MONTH));
        if (editId == null) dp.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        dp.show();
    }

    private void pickTime() {
        new TimePickerDialog(this, (view, h, min) -> {
            when.set(Calendar.HOUR_OF_DAY, h);
            when.set(Calendar.MINUTE, min);
            when.set(Calendar.SECOND, 0);
            timeSet = true;
            etTime.setText(TimeUtils.timeOnly(when.getTime()));
            tilTime.setError(null);
        }, when.get(Calendar.HOUR_OF_DAY), when.get(Calendar.MINUTE), false).show();
    }

    private void loadForEdit() {
        EventRepository.fetch(editId, new EventRepository.ItemListener() {
            @Override public void onData(@NonNull Event e) {
                etTitle.setText(e.getTitle());
                etDescription.setText(e.getDescription());
                etVenue.setText(e.getVenue());
                if (e.getCategory() != null) actCategory.setText(e.getCategory(), false);
                etCapacity.setText(e.getCapacity() > 0 ? String.valueOf(e.getCapacity()) : "");
                switchPublish.setChecked(e.isPublished());
                if (e.getEventTime() != null) {
                    when.setTime(e.getEventTime());
                    dateSet = true;
                    timeSet = true;
                    etDate.setText(TimeUtils.dateOnly(e.getEventTime()));
                    etTime.setText(TimeUtils.timeOnly(e.getEventTime()));
                }
            }

            @Override public void onDeleted() { finish(); }

            @Override public void onError(@NonNull String message) {
                UiUtils.toast(CreateEventActivity.this, message);
            }
        });
    }

    private static String text(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void save() {
        String title = text(etTitle);
        String desc = text(etDescription);
        String venue = text(etVenue);
        String capText = text(etCapacity);

        tilTitle.setError(null);
        tilDescription.setError(null);
        tilVenue.setError(null);
        tilDate.setError(null);
        tilTime.setError(null);

        if (title.length() < 5) { tilTitle.setError("Title must be at least 5 characters"); return; }
        if (desc.length() < 10) { tilDescription.setError("Please write at least 10 characters"); return; }
        if (venue.isEmpty()) { tilVenue.setError("Where is it happening?"); return; }
        if (!dateSet) { tilDate.setError("Choose a date"); return; }
        if (!timeSet) { tilTime.setError("Choose a start time"); return; }
        if (editId == null && when.getTimeInMillis() < System.currentTimeMillis()) {
            tilTime.setError("Pick a time in the future");
            return;
        }

        long capacity = 0;
        if (!capText.isEmpty()) {
            try {
                capacity = Long.parseLong(capText);
            } catch (NumberFormatException ex) {
                capacity = -1;
            }
            if (capacity < 0 || capacity > 100000) {
                UiUtils.toast(this, "Seats must be a positive number (leave empty for unlimited)");
                return;
            }
        }

        Event e = new Event();
        e.setId(editId);
        e.setTitle(title);
        e.setDescription(desc);
        e.setVenue(venue);
        e.setCategory(actCategory.getText().toString());
        e.setEventTime(new Date(when.getTimeInMillis()));
        e.setCapacity(capacity);
        e.setPublished(switchPublish.isChecked());

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");
        EventRepository.save(session, e, new EventRepository.DoneCallback() {
            @Override public void onSuccess() {
                UiUtils.toast(CreateEventActivity.this,
                        (editId == null && session.isFaculty()) ? "Sent to admin for approval" : "Saved");
                finish();
            }

            @Override public void onError(@NonNull String message) {
                btnSave.setEnabled(true);
                btnSave.setText("Save");
                UiUtils.toast(CreateEventActivity.this, message);
            }
        });
    }
}
