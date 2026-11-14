package com.example.campussuperapp.events;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campussuperapp.R;
import com.example.campussuperapp.common.Constants;
import com.example.campussuperapp.common.TimeUtils;
import com.example.campussuperapp.common.UiUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Event cards with a date badge. Handles Upcoming / Past / My Events / Pending + search. */
public class EventAdapter extends RecyclerView.Adapter<EventAdapter.VH> {

    public interface OnItemClick { void onClick(Event e); }
    public interface OnCountChanged { void onChanged(int count); }

    private final List<Event> all = new ArrayList<>();
    private final List<Event> shown = new ArrayList<>();
    private final OnItemClick clickListener;
    private OnCountChanged countListener;

    private String query = "";
    private String filter = Constants.FILTER_UPCOMING;
    private String uid = "";

    public EventAdapter(OnItemClick clickListener) {
        this.clickListener = clickListener;
    }

    public void setCountListener(OnCountChanged l) { this.countListener = l; }
    public void setUid(String uid) { this.uid = uid == null ? "" : uid; }

    public void setData(List<Event> data) {
        all.clear();
        all.addAll(data);
        applyFilter();
    }

    public void setQuery(String q) {
        query = (q == null) ? "" : q.trim().toLowerCase(Locale.getDefault());
        applyFilter();
    }

    public void setFilter(String f) {
        filter = (f == null) ? Constants.FILTER_UPCOMING : f;
        applyFilter();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private void applyFilter() {
        shown.clear();
        for (Event e : all) {
            boolean ok;
            switch (filter) {
                case Constants.FILTER_PAST:    ok = !e.isUpcoming(); break;
                case Constants.FILTER_MINE:    ok = e.isRegistered(uid); break;
                case Constants.FILTER_PENDING: ok = !e.isApproved(); break;
                default:                       ok = e.isUpcoming(); break; // Upcoming
            }
            if (ok && !query.isEmpty()) {
                String hay = (safe(e.getTitle()) + " " + safe(e.getVenue()) + " "
                        + safe(e.getCategory()) + " " + safe(e.getOrganizerName()))
                        .toLowerCase(Locale.getDefault());
                ok = hay.contains(query);
            }
            if (ok) shown.add(e);
        }
        // Source list is oldest -> newest. Past events read better newest first.
        if (Constants.FILTER_PAST.equals(filter)) Collections.reverse(shown);
        notifyDataSetChanged();
        if (countListener != null) countListener.onChanged(shown.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_event, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Event e = shown.get(position);
        int color = UiUtils.categoryColor(e.getCategory());

        h.tvDay.setText(TimeUtils.day(e.getEventTime()));
        h.tvMonth.setText(TimeUtils.month(e.getEventTime()));
        h.tvTitle.setText(safe(e.getTitle()));
        h.tvVenue.setText(safe(e.getVenue()));
        h.tvTime.setText(TimeUtils.timeOnly(e.getEventTime()));
        h.tvCategory.setText(safe(e.getCategory()).isEmpty() ? "Event" : e.getCategory());
        UiUtils.styleTag(h.tvCategory, color);

        // seats
        if (e.getCapacity() > 0) {
            h.tvSeats.setText(e.isFull() ? "Full" : e.seatsLeft() + " seats left");
            h.progressSeats.setVisibility(View.VISIBLE);
            h.progressSeats.setMax((int) e.getCapacity());
            h.progressSeats.setProgress((int) Math.min(e.getRegisteredCount(), e.getCapacity()));
        } else {
            h.tvSeats.setText(e.getRegisteredCount() + " going");
            h.progressSeats.setVisibility(View.GONE);
        }

        // status tag (most important state wins)
        String status = null;
        int statusColor = Color.parseColor("#64748B");
        if (!e.isApproved()) { status = "Pending approval"; statusColor = Color.parseColor("#F08A24"); }
        else if (!e.isPublished()) { status = "Draft"; }
        else if (e.isRegistered(uid)) { status = "Registered"; statusColor = Color.parseColor("#1E9E6A"); }
        else if (!e.isUpcoming()) { status = "Ended"; }
        else if (e.isFull()) { status = "Full"; statusColor = Color.parseColor("#D93B3B"); }

        if (status == null) {
            h.tvStatus.setVisibility(View.GONE);
        } else {
            h.tvStatus.setText(status);
            UiUtils.styleTag(h.tvStatus, statusColor);
            h.tvStatus.setVisibility(View.VISIBLE);
        }

        h.itemView.setOnClickListener(v -> clickListener.onClick(e));
    }

    @Override
    public int getItemCount() { return shown.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final TextView tvDay, tvMonth, tvTitle, tvVenue, tvTime, tvCategory, tvStatus, tvSeats;
        final ProgressBar progressSeats;

        VH(@NonNull View v) {
            super(v);
            tvDay = v.findViewById(R.id.tvDay);
            tvMonth = v.findViewById(R.id.tvMonth);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvVenue = v.findViewById(R.id.tvVenue);
            tvTime = v.findViewById(R.id.tvTime);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvSeats = v.findViewById(R.id.tvSeats);
            progressSeats = v.findViewById(R.id.progressSeats);
        }
    }
}
