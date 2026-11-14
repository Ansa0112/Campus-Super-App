package com.example.campussuperapp.events;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * One document of the "events" collection.
 * Students who register are stored two ways:
 *   registeredUids        -> quick "am I registered?" check + "My Events" filter
 *   registrations/{uid}   -> sub-collection with name + time, for the attendee list
 */
public class Event {

    @DocumentId
    private String id;

    private String title;
    private String description;
    private String venue;
    private String category;       // Seminar, Workshop, Sports, ...
    private Date eventTime;        // stored in Firestore as a Timestamp
    private long capacity;         // 0 = unlimited seats
    private long registeredCount;
    private List<String> registeredUids = new ArrayList<>();
    private String organizerId;
    private String organizerName;
    private String organizerRole;
    private boolean approved;      // faculty events wait for admin approval
    private boolean published;     // false = draft
    private Date createdAt;

    public Event() { }

    // ---- helpers (not stored in Firestore) ----
    @Exclude
    public boolean isUpcoming() {
        return eventTime != null && eventTime.getTime() >= System.currentTimeMillis();
    }

    @Exclude
    public boolean isFull() {
        return capacity > 0 && registeredCount >= capacity;
    }

    @Exclude
    public long seatsLeft() {
        return capacity > 0 ? Math.max(0, capacity - registeredCount) : Long.MAX_VALUE;
    }

    @Exclude
    public boolean isRegistered(String uid) {
        return uid != null && registeredUids != null && registeredUids.contains(uid);
    }

    // ---- getters / setters ----
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Date getEventTime() { return eventTime; }
    public void setEventTime(Date eventTime) { this.eventTime = eventTime; }

    public long getCapacity() { return capacity; }
    public void setCapacity(long capacity) { this.capacity = capacity; }

    public long getRegisteredCount() { return registeredCount; }
    public void setRegisteredCount(long registeredCount) { this.registeredCount = registeredCount; }

    public List<String> getRegisteredUids() { return registeredUids; }
    public void setRegisteredUids(List<String> registeredUids) { this.registeredUids = registeredUids; }

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }

    public String getOrganizerRole() { return organizerRole; }
    public void setOrganizerRole(String organizerRole) { this.organizerRole = organizerRole; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
