package com.example.campussuperapp.announcements;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campussuperapp.R;
import com.example.campussuperapp.common.Constants;
import com.example.campussuperapp.common.UiUtils;
import com.example.campussuperapp.common.UserSession;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Announcements feed - the same screen for Student, Faculty and Admin.
 * What changes per role:
 *   Student : sees approved + published announcements meant for students/everyone
 *   Faculty : same + their own drafts/pending posts, and a "New" button
 *   Admin   : sees everything, can approve / pin / delete
 */
public class AnnouncementsActivity extends AppCompatActivity {

    private AnnouncementAdapter adapter;
    private UserSession session;
    private ListenerRegistration registration;

    private ProgressBar progressBar;
    private View layoutEmpty;
    private TextView tvCount, tvEmpty;
    private ChipGroup chipGroup;
    private ExtendedFloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_announcements);
        UiUtils.padForSystemBars(findViewById(R.id.header), true, false);

        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);
        tvCount = findViewById(R.id.tvCount);
        tvEmpty = findViewById(R.id.tvEmpty);
        chipGroup = findViewById(R.id.chipGroup);
        fabAdd = findViewById(R.id.fabAdd);
        ImageButton btnBack = findViewById(R.id.btnBack);
        EditText etSearch = findViewById(R.id.etSearch);
        RecyclerView recyclerView = findViewById(R.id.recyclerView);

        btnBack.setOnClickListener(v -> finish());

        adapter = new AnnouncementAdapter(a -> {
            Intent i = new Intent(this, AnnouncementDetailActivity.class);
            i.putExtra(AnnouncementDetailActivity.EXTRA_ID, a.getId());
            startActivity(i);
        });
        adapter.setCountListener(this::onCountChanged);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { adapter.setQuery(s.toString()); }
        });

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, CreateAnnouncementActivity.class)));

        progressBar.setVisibility(View.VISIBLE);
        UserSession.load(new UserSession.Callback() {
            @Override public void onReady(@NonNull UserSession s) {
                if (isFinishing() || isDestroyed()) return;
                session = s;
                setupForRole();
                startListening();
            }

            @Override public void onError(@NonNull String message) {
                progressBar.setVisibility(View.GONE);
                UiUtils.toast(AnnouncementsActivity.this, message);
            }
        });
    }

    private void setupForRole() {
        fabAdd.setVisibility(session.canPublish() ? View.VISIBLE : View.GONE);

        List<String> filters = new ArrayList<>();
        filters.add(Constants.FILTER_ALL);
        for (String c : Constants.ANNOUNCEMENT_CATEGORIES) filters.add(c);
        if (session.canPublish()) filters.add(Constants.FILTER_PENDING);

        chipGroup.removeAllViews();
        for (int i = 0; i < filters.size(); i++) {
            Chip chip = new Chip(this);
            chip.setId(View.generateViewId());
            chip.setText(filters.get(i));
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setChipBackgroundColor(ContextCompat.getColorStateList(this, R.color.ae_chip_bg));
            chip.setTextColor(ContextCompat.getColorStateList(this, R.color.ae_chip_text));
            chip.setChipStrokeWidth(UiUtils.dp(this, 1));
            chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ae_border)));
            chipGroup.addView(chip);
            if (i == 0) chip.setChecked(true);
        }
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = group.findViewById(checkedIds.get(0));
            if (chip != null) adapter.setFilter(chip.getText().toString());
        });
    }

    private void startListening() {
        registration = AnnouncementRepository.listen(session, new AnnouncementRepository.ListListener() {
            @Override public void onData(@NonNull List<Announcement> list) {
                progressBar.setVisibility(View.GONE);
                adapter.setData(list);
            }

            @Override public void onError(@NonNull String message) {
                progressBar.setVisibility(View.GONE);
                UiUtils.toast(AnnouncementsActivity.this, message);
            }
        });
    }

    private void onCountChanged(int count) {
        tvCount.setText(count + (count == 1 ? " update" : " updates"));
        layoutEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        tvEmpty.setText(session != null && session.canPublish()
                ? "Nothing here yet.\nTap + New to post an announcement."
                : "No announcements right now.\nCheck back later.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove(); // stop listening -> no memory leak, no wasted reads
    }
}
