package com.example.campussuperapp.announcements;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.campussuperapp.R;
import com.example.campussuperapp.common.Constants;
import com.example.campussuperapp.common.TimeUtils;
import com.example.campussuperapp.common.UiUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Shows announcement cards and handles search + category filtering on the phone. */
public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.VH> {

    public interface OnItemClick { void onClick(Announcement a); }
    public interface OnCountChanged { void onChanged(int count); }

    private final List<Announcement> all = new ArrayList<>();
    private final List<Announcement> shown = new ArrayList<>();
    private final OnItemClick clickListener;
    private OnCountChanged countListener;

    private String query = "";
    private String filter = Constants.FILTER_ALL;

    public AnnouncementAdapter(OnItemClick clickListener) {
        this.clickListener = clickListener;
    }

    public void setCountListener(OnCountChanged l) { this.countListener = l; }

    public void setData(List<Announcement> data) {
        all.clear();
        all.addAll(data);
        applyFilter();
    }

    public void setQuery(String q) {
        query = (q == null) ? "" : q.trim().toLowerCase(Locale.getDefault());
        applyFilter();
    }

    public void setFilter(String f) {
        filter = (f == null) ? Constants.FILTER_ALL : f;
        applyFilter();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private void applyFilter() {
        shown.clear();
        for (Announcement a : all) {
            boolean ok;
            if (Constants.FILTER_ALL.equals(filter)) ok = true;
            else if (Constants.FILTER_PENDING.equals(filter)) ok = !a.isApproved();
            else ok = filter.equalsIgnoreCase(a.getCategory());

            if (ok && !query.isEmpty()) {
                String haystack = (safe(a.getTitle()) + " " + safe(a.getDescription()) + " "
                        + safe(a.getAuthorName())).toLowerCase(Locale.getDefault());
                ok = haystack.contains(query);
            }
            if (ok) shown.add(a);
        }
        notifyDataSetChanged();
        if (countListener != null) countListener.onChanged(shown.size());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_announcement, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        final Announcement a = shown.get(position);
        int color = UiUtils.categoryColor(a.getCategory());

        h.strip.setBackgroundColor(color);
        h.tvCategory.setText(safe(a.getCategory()).isEmpty() ? "General" : a.getCategory());
        UiUtils.styleTag(h.tvCategory, color);

        h.tvTitle.setText(safe(a.getTitle()));
        h.tvDescription.setText(safe(a.getDescription()));
        h.tvAuthor.setText(safe(a.getAuthorName()).isEmpty() ? "Campus Office" : a.getAuthorName());
        h.tvTime.setText(TimeUtils.timeAgo(a.getCreatedAt()));

        h.ivPinned.setVisibility(a.isPinned() ? View.VISIBLE : View.GONE);
        // Blue tick = posted by an admin (official)
        h.ivVerified.setVisibility(Constants.ROLE_ADMIN.equals(a.getAuthorRole()) ? View.VISIBLE : View.GONE);

        if (!a.isApproved()) {
            h.tvStatus.setText("Pending approval");
            UiUtils.styleTag(h.tvStatus, Color.parseColor("#F08A24"));
            h.tvStatus.setVisibility(View.VISIBLE);
        } else if (!a.isPublished()) {
            h.tvStatus.setText("Draft");
            UiUtils.styleTag(h.tvStatus, Color.parseColor("#64748B"));
            h.tvStatus.setVisibility(View.VISIBLE);
        } else {
            h.tvStatus.setVisibility(View.GONE);
        }

        h.itemView.setOnClickListener(v -> clickListener.onClick(a));
    }

    @Override
    public int getItemCount() { return shown.size(); }

    static class VH extends RecyclerView.ViewHolder {
        final View strip;
        final TextView tvCategory, tvStatus, tvTitle, tvDescription, tvAuthor, tvTime;
        final ImageView ivPinned, ivVerified;

        VH(@NonNull View v) {
            super(v);
            strip = v.findViewById(R.id.stripCategory);
            tvCategory = v.findViewById(R.id.tvCategory);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvDescription = v.findViewById(R.id.tvDescription);
            tvAuthor = v.findViewById(R.id.tvAuthor);
            tvTime = v.findViewById(R.id.tvTime);
            ivPinned = v.findViewById(R.id.ivPinned);
            ivVerified = v.findViewById(R.id.ivVerified);
        }
    }
}
