package com.example.campussuperapp.announcements;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.campussuperapp.R;
import com.example.campussuperapp.common.Constants;
import com.example.campussuperapp.common.TimeUtils;
import com.example.campussuperapp.common.UiUtils;
import com.example.campussuperapp.common.UserSession;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;

public class AnnouncementDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ID = "announcement_id";

    private String id;
    private UserSession session;
    private Announcement current;
    private ListenerRegistration registration;
    private boolean viewCounted = false;

    private TextView tvCategory, tvTitle, tvAuthor, tvTime, tvDescription, tvAudience, tvViews, tvBanner;
    private ImageView ivVerified;
    private View layoutActions;
    private MaterialButton btnApprove, btnPin, btnPublish, btnEdit, btnDelete;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcement_detail);
        UiUtils.padForSystemBars(findViewById(R.id.header), true, false);

        id = getIntent().getStringExtra(EXTRA_ID);
        if (id == null) { finish(); return; }

        tvCategory = findViewById(R.id.tvCategory);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvTime = findViewById(R.id.tvTime);
        tvDescription = findViewById(R.id.tvDescription);
        tvAudience = findViewById(R.id.tvAudience);
        tvViews = findViewById(R.id.tvViews);
        tvBanner = findViewById(R.id.tvBanner);
        ivVerified = findViewById(R.id.ivVerified);
        layoutActions = findViewById(R.id.layoutActions);
        btnApprove = findViewById(R.id.btnApprove);
        btnPin = findViewById(R.id.btnPin);
        btnPublish = findViewById(R.id.btnPublish);
        btnEdit = findViewById(R.id.btnEdit);
        btnDelete = findViewById(R.id.btnDelete);
        ImageButton btnBack = findViewById(R.id.btnBack);
        ImageButton btnShare = findViewById(R.id.btnShare);

        btnBack.setOnClickListener(v -> finish());
        btnShare.setOnClickListener(v -> share());
        btnApprove.setOnClickListener(v -> update("approved", true, "Announcement approved"));
        btnPin.setOnClickListener(v -> update("pinned", !current.isPinned(),
                current.isPinned() ? "Unpinned" : "Pinned to top"));
        btnPublish.setOnClickListener(v -> update("published", !current.isPublished(),
                current.isPublished() ? "Moved to drafts" : "Published"));
        btnEdit.setOnClickListener(v -> {
            Intent i = new Intent(this, CreateAnnouncementActivity.class);
            i.putExtra(CreateAnnouncementActivity.EXTRA_ID, id);
            startActivity(i);
        });
        btnDelete.setOnClickListener(v -> UiUtils.confirm(this, "Delete announcement?",
                "This cannot be undone.", "Delete", this::deleteNow));

        layoutActions.setVisibility(View.GONE);

        UserSession.load(new UserSession.Callback() {
            @Override public void onReady(@NonNull UserSession s) {
                if (isFinishing() || isDestroyed()) return;
                session = s;
                startListening();
            }

            @Override public void onError(@NonNull String message) {
                UiUtils.toast(AnnouncementDetailActivity.this, message);
            }
        });
    }

    private void startListening() {
        registration = AnnouncementRepository.listenOne(id, new AnnouncementRepository.ItemListener() {
            @Override public void onData(@NonNull Announcement a) {
                render(a);
                // Count one view per opening, not for the author and not for hidden items.
                if (!viewCounted && a.isApproved() && a.isPublished()
                        && !session.uid.equals(a.getAuthorId())) {
                    viewCounted = true;
                    AnnouncementRepository.incrementViews(id);
                }
            }

            @Override public void onDeleted() {
                UiUtils.toast(AnnouncementDetailActivity.this, "This announcement was removed");
                finish();
            }

            @Override public void onError(@NonNull String message) {
                UiUtils.toast(AnnouncementDetailActivity.this, message);
            }
        });
    }

    private void render(Announcement a) {
        current = a;
        int color = UiUtils.categoryColor(a.getCategory());
        tvCategory.setText(a.getCategory() == null ? "General" : a.getCategory());
        UiUtils.styleTag(tvCategory, color);

        tvTitle.setText(a.getTitle());
        tvDescription.setText(a.getDescription());
        tvAuthor.setText((a.getAuthorName() == null ? "Campus Office" : a.getAuthorName())
                + (a.getAuthorRole() == null ? "" : "  •  " + capitalize(a.getAuthorRole())));
        tvTime.setText(TimeUtils.timeAgo(a.getCreatedAt()));
        tvAudience.setText("For: " + (a.getAudience() == null ? Constants.AUD_ALL : a.getAudience()));
        tvViews.setText(a.getViewCount() + (a.getViewCount() == 1 ? " view" : " views"));
        ivVerified.setVisibility(Constants.ROLE_ADMIN.equals(a.getAuthorRole()) ? View.VISIBLE : View.GONE);

        if (!a.isApproved()) {
            tvBanner.setText("Waiting for admin approval. Students cannot see this yet.");
            tvBanner.setVisibility(View.VISIBLE);
        } else if (!a.isPublished()) {
            tvBanner.setText("Draft. Only you and the admin can see this.");
            tvBanner.setVisibility(View.VISIBLE);
        } else {
            tvBanner.setVisibility(View.GONE);
        }

        boolean manage = AnnouncementRepository.canManage(session, a);
        layoutActions.setVisibility(manage ? View.VISIBLE : View.GONE);
        btnApprove.setVisibility(session.isAdmin() && !a.isApproved() ? View.VISIBLE : View.GONE);
        btnPin.setVisibility(session.isAdmin() ? View.VISIBLE : View.GONE);
        btnPin.setText(a.isPinned() ? "Unpin" : "Pin to top");
        btnPublish.setText(a.isPublished() ? "Make draft" : "Publish");
    }

    private void update(String field, Object value, final String successMessage) {
        AnnouncementRepository.updateField(id, field, value, new AnnouncementRepository.DoneCallback() {
            @Override public void onSuccess() { UiUtils.toast(AnnouncementDetailActivity.this, successMessage); }
            @Override public void onError(@NonNull String message) { UiUtils.toast(AnnouncementDetailActivity.this, message); }
        });
    }

    private void deleteNow() {
        AnnouncementRepository.delete(id, new AnnouncementRepository.DoneCallback() {
            @Override public void onSuccess() {
                UiUtils.toast(AnnouncementDetailActivity.this, "Announcement deleted");
                finish();
            }

            @Override public void onError(@NonNull String message) {
                UiUtils.toast(AnnouncementDetailActivity.this, message);
            }
        });
    }

    private void share() {
        if (current == null) return;
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, current.getTitle());
        send.putExtra(Intent.EXTRA_TEXT, current.getTitle() + "\n\n" + current.getDescription()
                + "\n\n- Shared from Campus Super App");
        startActivity(Intent.createChooser(send, "Share announcement"));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
