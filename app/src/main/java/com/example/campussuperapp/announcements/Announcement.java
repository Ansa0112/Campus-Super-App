package com.example.campussuperapp.announcements;

import com.google.firebase.firestore.DocumentId;

import java.util.Date;

/**
 * One document of the "announcements" collection.
 *
 * Firestore needs: a public no-argument constructor + getters/setters.
 * @DocumentId fills "id" automatically with the document's ID.
 */
public class Announcement {

    @DocumentId
    private String id;

    private String title;
    private String description;
    private String category;     // General, Academic, Exam, Fee, Library, Sports, Emergency
    private String audience;     // Everyone | Students | Faculty
    private String authorId;
    private String authorName;
    private String authorRole;   // admin | faculty
    private boolean pinned;      // admin can pin to the top
    private boolean approved;    // faculty posts wait for admin approval
    private boolean published;   // false = draft
    private long viewCount;
    private Date createdAt;
    private Date updatedAt;

    public Announcement() { }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthorRole() { return authorRole; }
    public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }

    public boolean isPinned() { return pinned; }
    public void setPinned(boolean pinned) { this.pinned = pinned; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public long getViewCount() { return viewCount; }
    public void setViewCount(long viewCount) { this.viewCount = viewCount; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
