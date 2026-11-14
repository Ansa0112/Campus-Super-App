package com.example.campussuperapp.events;

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

/** Events list for all three roles. Faculty/admin additionally get a "New" button and "Pending" tab. */
public class EventsActivity extends AppCompatActivity {

    private EventAdapter adapter;
    private UserSession session;
    private ListenerRegistration registration;

    private ProgressBar progressBar;
    private View layoutEmpty;
    private TextView tvCount, tvEmpty;
    private ChipGroup chipGroup;
    private ExtendedFloatingActionButton fabAdd;
    private String currentFilter = Constants.FILTER_UPCOMING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);
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

        adapter = new EventAdapter(e -> {
            Intent i = new Intent(this, EventDetailActivity.class);
            i.putExtra(EventDetailActivity.EXTRA_ID, e.getId());
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

        fabAdd.setOnClickListener(v -> startActivity(new Intent(this, CreateEventActivity.class)));

        progressBar.setVisibility(View.VISIBLE);
        UserSession.load(new UserSession.Callback() {
            @Override public void onReady(@NonNull UserSession s) {
                if (isFinishing() || isDestroyed()) return;
                session = s;
                adapter.setUid(s.uid);
                setupForRole();
                startListening();
            }

            @Override public void onError(@NonNull String message) {
                progressBar.setVisibility(View.GONE);
                UiUtils.toast(EventsActivity.this, message);
            }
        });
    }

    private void setupForRole() {
        fabAdd.setVisibility(session.canPublish() ? View.VISIBLE : View.GONE);

        List<String> filters = new ArrayList<>();
        filters.add(Constants.FILTER_UPCOMING);
        filters.add(Constants.FILTER_MINE);
        filters.add(Constants.FILTER_PAST);
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
            if (chip != null) {
                currentFilter = chip.getText().toString();
                adapter.setFilter(currentFilter);
            }
        });
    }

    private void startListening() {
        registration = EventRepository.listen(session, new EventRepository.ListListener() {
            @Override public void onData(@NonNull List<Event> list) {
                progressBar.setVisibility(View.GONE);
                adapter.setData(list);
            }

            @Override public void onError(@NonNull String message) {
                progressBar.setVisibility(View.GONE);
                UiUtils.toast(EventsActivity.this, message);
            }
        });
    }

    private void onCountChanged(int count) {
        tvCount.setText(count + (count == 1 ? " event" : " events"));
        layoutEmpty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        String msg;
        if (Constants.FILTER_MINE.equals(currentFilter)) msg = "You have not registered for any event yet.";
        else if (Constants.FILTER_PAST.equals(currentFilter)) msg = "No past events.";
        else if (Constants.FILTER_PENDING.equals(currentFilter)) msg = "Nothing is waiting for approval.";
        else msg = session != null && session.canPublish()
                    ? "No upcoming events.\nTap + New to create one."
                    : "No upcoming events.\nCheck back soon.";
        tvEmpty.setText(msg);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (registration != null) registration.remove();
    }
}
