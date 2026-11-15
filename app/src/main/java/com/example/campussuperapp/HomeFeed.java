package com.example.campussuperapp;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.campussuperapp.announcements.Announcement;
import com.example.campussuperapp.announcements.AnnouncementDetailActivity;
import com.example.campussuperapp.announcements.AnnouncementRepository;
import com.example.campussuperapp.announcements.AnnouncementsActivity;
import com.example.campussuperapp.common.Constants;
import com.example.campussuperapp.common.TimeUtils;
import com.example.campussuperapp.common.UiUtils;
import com.example.campussuperapp.common.UserSession;
import com.example.campussuperapp.events.Event;
import com.example.campussuperapp.events.EventDetailActivity;
import com.example.campussuperapp.events.EventRepository;
import com.example.campussuperapp.events.EventsActivity;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Fills the Home screen (student, faculty AND admin) with REAL data from Firestore:
 *   - greeting + user name
 *   - the latest approved announcement (big navy card)
 *   - the next 2 announcements
 *   - the next 3 upcoming events
 *   - a "pending approval" note (admin: everything waiting; faculty: their own posts)
 *
 * Usage in an Activity:  onStart() -> feed.start();   onStop() -> feed.stop();
 * Everything updates live (Firestore snapshot listeners), no refresh needed.
 */
public class HomeFeed {

    private final Activity activity;
    private UserSession session;
    private ListenerRegistration annReg, evReg;
    private boolean started = false;

    private List<Announcement> allAnnouncements = new ArrayList<>();
    private List<Event> allEvents = new ArrayList<>();
    private int pendingAnn = 0;
    private int pendingEv = 0;
    private String latestId = null;

    // views (any of them may be missing in a layout - everything is null-checked)
    private final TextView tvGreeting, tvUserName, tvAdminName;
    private final TextView tvLatestBadge, tvLatestTitle, tvLatestMeta, tvPendingNote;
    private final View cardLatest;
    private final LinearLayout layoutAnnouncements, layoutEvents;

    public HomeFeed(Activity activity) {
        this.activity = activity;
        tvGreeting = activity.findViewById(R.id.tvGreeting);
        tvUserName = activity.findViewById(R.id.tvUserName);
        tvAdminName = activity.findViewById(R.id.tvAdminName);
        tvLatestBadge = activity.findViewById(R.id.tvLatestBadge);
        tvLatestTitle = activity.findViewById(R.id.tvLatestTitle);
        tvLatestMeta = activity.findViewById(R.id.tvLatestMeta);
        tvPendingNote = activity.findViewById(R.id.tvPendingNote);
        cardLatest = activity.findViewById(R.id.cardLatest);
        layoutAnnouncements = activity.findViewById(R.id.layoutAnnouncements);
        layoutEvents = activity.findViewById(R.id.layoutEvents);
    }

    // ------------------------------------------------------------------ lifecycle
    public void start() {
        if (started) return;
        started = true;
        UserSession.load(new UserSession.Callback() {
            @Override public void onReady(@NonNull UserSession s) {
                if (!started || activity.isFinishing() || activity.isDestroyed()) return;
                session = s;
                showGreetingAndName();
                listen();
            }

            @Override public void onError(@NonNull String message) {
                // keep the layout's default text; nothing else to do
            }
        });
    }

    public void stop() {
        started = false;
        removeListeners();
    }

    private void removeListeners() {
        if (annReg != null) { annReg.remove(); annReg = null; }
        if (evReg != null) { evReg.remove(); evReg = null; }
    }

    private void listen() {
        removeListeners(); // never keep two listeners alive

        annReg = AnnouncementRepository.listen(session, new AnnouncementRepository.ListListener() {
            @Override public void onData(@NonNull List<Announcement> list) {
                allAnnouncements = list;
                renderAnnouncements();
                renderPendingNote();
            }

            @Override public void onError(@NonNull String message) { }
        });

        evReg = EventRepository.listen(session, new EventRepository.ListListener() {
            @Override public void onData(@NonNull List<Event> list) {
                allEvents = list;
                renderEvents();
                renderPendingNote();
            }

            @Override public void onError(@NonNull String message) { }
        });
    }

    // ------------------------------------------------------------------ greeting
    private void showGreetingAndName() {
        if (tvGreeting != null) {
            int h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            tvGreeting.setText(h < 12 ? "Good morning" : (h < 17 ? "Good afternoon" : "Good evening"));
        }
        if (tvUserName != null) tvUserName.setText("Hello, " + session.name);
        if (tvAdminName != null) tvAdminName.setText("Welcome back, " + session.name);
    }

    // ------------------------------------------------------------------ announcements
    private static int newestFirst(Date a, Date b) {
        // a missing date = a post that was just created and the server time has not arrived yet
        long x = a == null ? Long.MAX_VALUE : a.getTime();
        long y = b == null ? Long.MAX_VALUE : b.getTime();
        return Long.compare(y, x);
    }

    private boolean isMine(String ownerId) {
        return session != null && session.uid.equals(ownerId);
    }

    private void renderAnnouncements() {
        List<Announcement> live = new ArrayList<>();
        pendingAnn = 0;
        for (Announcement a : allAnnouncements) {
            if (!a.isApproved()) {
                if (session.isAdmin() || (session.isFaculty() && isMine(a.getAuthorId()))) pendingAnn++;
            } else if (a.isPublished()) {
                live.add(a);
            }
        }
        Collections.sort(live, (x, y) -> newestFirst(x.getCreatedAt(), y.getCreatedAt()));

        // ---- big "latest announcement" card
        if (live.isEmpty()) {
            latestId = null;
            if (tvLatestTitle != null) tvLatestTitle.setText("No announcements yet");
            if (tvLatestMeta != null) tvLatestMeta.setText("New campus updates will appear here");
            if (tvLatestBadge != null) tvLatestBadge.setVisibility(View.GONE);
        } else {
            final Announcement a = live.get(0);
            latestId = a.getId();
            if (tvLatestTitle != null) tvLatestTitle.setText(a.getTitle());
            if (tvLatestMeta != null) {
                tvLatestMeta.setText("Posted by " + (a.getAuthorName() == null ? "Campus Office" : a.getAuthorName())
                        + "  •  " + TimeUtils.timeAgo(a.getCreatedAt()));
            }
            if (tvLatestBadge != null) {
                boolean official = Constants.ROLE_ADMIN.equals(a.getAuthorRole());
                tvLatestBadge.setText(official ? "Verified" : (a.getCategory() == null ? "Update" : a.getCategory()));
                tvLatestBadge.setVisibility(View.VISIBLE);
            }
        }
        if (cardLatest != null) {
            cardLatest.setOnClickListener(v -> {
                if (latestId == null) {
                    activity.startActivity(new Intent(activity, AnnouncementsActivity.class));
                } else {
                    Intent i = new Intent(activity, AnnouncementDetailActivity.class);
                    i.putExtra(AnnouncementDetailActivity.EXTRA_ID, latestId);
                    activity.startActivity(i);
                }
            });
        }

        // ---- the next two announcements
        if (layoutAnnouncements != null) {
            layoutAnnouncements.removeAllViews();
            LayoutInflater inflater = LayoutInflater.from(activity);
            for (int i = 1; i < live.size() && i <= 2; i++) {
                final Announcement a = live.get(i);
                View row = inflater.inflate(R.layout.item_home_announcement, layoutAnnouncements, false);
                ((TextView) row.findViewById(R.id.tvHomeAnnTitle)).setText(a.getTitle());
                ((TextView) row.findViewById(R.id.tvHomeAnnMeta)).setText(
                        (a.getCategory() == null ? "General" : a.getCategory())
                                + "  •  " + TimeUtils.timeAgo(a.getCreatedAt()));
                row.setOnClickListener(v -> {
                    Intent in = new Intent(activity, AnnouncementDetailActivity.class);
                    in.putExtra(AnnouncementDetailActivity.EXTRA_ID, a.getId());
                    activity.startActivity(in);
                });
                layoutAnnouncements.addView(row);
            }
        }
    }

    // ------------------------------------------------------------------ events
    private void renderEvents() {
        List<Event> upcoming = new ArrayList<>();
        pendingEv = 0;
        for (Event e : allEvents) {
            if (!e.isApproved()) {
                if (session.isAdmin() || (session.isFaculty() && isMine(e.getOrganizerId()))) pendingEv++;
            } else if (e.isPublished() && e.isUpcoming()) {
                upcoming.add(e);   // repository already sorts by date, soonest first
            }
        }

        if (layoutEvents == null) return;
        layoutEvents.removeAllViews();

        if (upcoming.isEmpty()) {
            TextView empty = new TextView(activity);
            empty.setText("No upcoming events right now");
            empty.setTextColor(0xFF64748B);
            empty.setTextSize(13);
            empty.setPadding(UiUtils.dp(activity, 4), UiUtils.dp(activity, 6), 0, UiUtils.dp(activity, 6));
            layoutEvents.addView(empty);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(activity);
        for (int i = 0; i < upcoming.size() && i < 3; i++) {
            final Event e = upcoming.get(i);
            View row = inflater.inflate(R.layout.item_home_event, layoutEvents, false);
            ((TextView) row.findViewById(R.id.tvHomeEvMonth)).setText(TimeUtils.month(e.getEventTime()));
            ((TextView) row.findViewById(R.id.tvHomeEvDay)).setText(TimeUtils.day(e.getEventTime()));
            ((TextView) row.findViewById(R.id.tvHomeEvTitle)).setText(e.getTitle());
            ((TextView) row.findViewById(R.id.tvHomeEvMeta)).setText(
                    TimeUtils.timeOnly(e.getEventTime()) + "  •  " + (e.getVenue() == null ? "" : e.getVenue()));
            row.setOnClickListener(v -> {
                Intent in = new Intent(activity, EventDetailActivity.class);
                in.putExtra(EventDetailActivity.EXTRA_ID, e.getId());
                activity.startActivity(in);
            });
            layoutEvents.addView(row);
        }
    }

    // ------------------------------------------------------------------ pending approvals
    private void renderPendingNote() {
        if (tvPendingNote == null || session == null) return;
        int total = pendingAnn + pendingEv;
        if (total == 0 || session.isStudent()) {
            tvPendingNote.setVisibility(View.GONE);
            return;
        }

        String what = (pendingAnn > 0 ? pendingAnn + (pendingAnn == 1 ? " announcement" : " announcements") : "")
                + (pendingAnn > 0 && pendingEv > 0 ? " and " : "")
                + (pendingEv > 0 ? pendingEv + (pendingEv == 1 ? " event" : " events") : "");

        tvPendingNote.setText(session.isAdmin()
                ? what + " waiting for your approval. Tap to review."
                : what + " of yours waiting for admin approval.");
        tvPendingNote.setVisibility(View.VISIBLE);
        tvPendingNote.setOnClickListener(v -> activity.startActivity(new Intent(activity,
                pendingAnn > 0 ? AnnouncementsActivity.class : EventsActivity.class)));
    }
}
