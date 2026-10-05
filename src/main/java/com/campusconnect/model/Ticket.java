package com.campusconnect.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Ticket implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String category;
    private String priority; // "Low", "Medium", "High", "Urgent"
    private String status;   // "open", "in_progress", "resolved", "closed"
    private String location;
    private String description;
    private UserSummary submittedBy;
    private String assignedTo;
    private String createdAt;
    private String updatedAt;
    private List<TimelineItem> timeline = new ArrayList<>();
    private List<Comment> comments = new ArrayList<>();

    public Ticket() {}

    public Ticket(String id, String title, String category, String priority, String status,
                  String location, String description, UserSummary submittedBy,
                  String assignedTo, String createdAt, String updatedAt) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.location = location;
        this.description = description;
        this.submittedBy = submittedBy;
        this.assignedTo = assignedTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.timeline = new ArrayList<>();
        this.comments = new ArrayList<>();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public UserSummary getSubmittedBy() { return submittedBy; }
    public void setSubmittedBy(UserSummary submittedBy) { this.submittedBy = submittedBy; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public List<TimelineItem> getTimeline() { return timeline; }
    public void setTimeline(List<TimelineItem> timeline) { this.timeline = timeline != null ? timeline : new ArrayList<>(); }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments != null ? comments : new ArrayList<>(); }

    public void addTimelineItem(TimelineItem item) {
        if (this.timeline == null) this.timeline = new ArrayList<>();
        this.timeline.add(item);
    }

    public void addComment(Comment comment) {
        if (this.comments == null) this.comments = new ArrayList<>();
        this.comments.add(comment);
    }
}
