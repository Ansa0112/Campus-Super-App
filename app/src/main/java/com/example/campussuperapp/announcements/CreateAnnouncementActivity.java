package com.example.campussuperapp.announcements;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campussuperapp.R;
import com.example.campussuperapp.common.Constants;
import com.example.campussuperapp.common.UiUtils;
import com.example.campussuperapp.common.UserSession;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/** One form used for BOTH creating and editing (edit mode = an id is passed in the Intent). */
public class CreateAnnouncementActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "announcement_id";

    private String editId;
    private UserSession session;

    private TextInputLayout tilTitle, tilDescription;
    private TextInputEditText etTitle, etDescription;
    private AutoCompleteTextView actCategory, actAudience;
    private MaterialSwitch switchPin, switchPublish;
    private TextView tvFormTitle, tvApprovalNote;
    private MaterialButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_announcement);
        UiUtils.padForSystemBars(findViewById(R.id.header), true, false);

        editId = getIntent().getStringExtra(EXTRA_ID);

        tilTitle = findViewById(R.id.tilTitle);
        tilDescription = findViewById(R.id.tilDescription);
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        actCategory = findViewById(R.id.actCategory);
        actAudience = findViewById(R.id.actAudience);
        switchPin = findViewById(R.id.switchPin);
        switchPublish = findViewById(R.id.switchPublish);
        tvFormTitle = findViewById(R.id.tvFormTitle);
        tvApprovalNote = findViewById(R.id.tvApprovalNote);
        btnSave = findViewById(R.id.btnSave);
        ImageButton btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        actCategory.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Constants.ANNOUNCEMENT_CATEGORIES));
        actAudience.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                Constants.AUDIENCES));
        actCategory.setText(Constants.ANNOUNCEMENT_CATEGORIES[0], false);
        actAudience.setText(Constants.AUDIENCES[0], false);
        switchPublish.setChecked(true);

        tvFormTitle.setText(editId == null ? "New announcement" : "Edit announcement");
        btnSave.setEnabled(false); // until we know the user's role

        UserSession.load(new UserSession.Callback() {
            @Override public void onReady(@NonNull UserSession s) {
                if (isFinishing() || isDestroyed()) return;
                session = s;
                if (!s.canPublish()) {          // students must never reach this screen
                    UiUtils.toast(CreateAnnouncementActivity.this, "Only faculty and admin can post announcements");
                    finish();
                    return;
                }
                findViewById(R.id.layoutPin).setVisibility(s.isAdmin() ? View.VISIBLE : View.GONE);
                tvApprovalNote.setVisibility(s.isFaculty() && editId == null ? View.VISIBLE : View.GONE);
                btnSave.setEnabled(true);
                if (editId != null) loadForEdit();
            }

            @Override public void onError(@NonNull String message) {
                UiUtils.toast(CreateAnnouncementActivity.this, message);
                finish();
            }
        });

        btnSave.setOnClickListener(v -> save());
    }

    private void loadForEdit() {
        AnnouncementRepository.fetch(editId, new AnnouncementRepository.ItemListener() {
            @Override public void onData(@NonNull Announcement a) {
                etTitle.setText(a.getTitle());
                etDescription.setText(a.getDescription());
                if (a.getCategory() != null) actCategory.setText(a.getCategory(), false);
                if (a.getAudience() != null) actAudience.setText(a.getAudience(), false);
                switchPin.setChecked(a.isPinned());
                switchPublish.setChecked(a.isPublished());
            }

            @Override public void onDeleted() { finish(); }

            @Override public void onError(@NonNull String message) {
                UiUtils.toast(CreateAnnouncementActivity.this, message);
            }
        });
    }

    private void save() {
        String title = etTitle.getText() == null ? "" : etTitle.getText().toString().trim();
        String desc = etDescription.getText() == null ? "" : etDescription.getText().toString().trim();

        tilTitle.setError(null);
        tilDescription.setError(null);
        if (title.length() < 5) { tilTitle.setError("Title must be at least 5 characters"); return; }
        if (desc.length() < 10) { tilDescription.setError("Please write at least 10 characters"); return; }

        Announcement a = new Announcement();
        a.setId(editId);
        a.setTitle(title);
        a.setDescription(desc);
        a.setCategory(actCategory.getText().toString());
        a.setAudience(actAudience.getText().toString());
        a.setPinned(switchPin.isChecked());
        a.setPublished(switchPublish.isChecked());

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");
        AnnouncementRepository.save(session, a, new AnnouncementRepository.DoneCallback() {
            @Override public void onSuccess() {
                UiUtils.toast(CreateAnnouncementActivity.this,
                        (editId == null && session.isFaculty())
                                ? "Sent to admin for approval"
                                : "Saved");
                finish();
            }

            @Override public void onError(@NonNull String message) {
                btnSave.setEnabled(true);
                btnSave.setText("Save");
                UiUtils.toast(CreateAnnouncementActivity.this, message);
            }
        });
    }
}
