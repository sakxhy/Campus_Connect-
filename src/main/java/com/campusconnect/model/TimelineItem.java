package com.campusconnect.model;

import java.io.Serializable;

public class TimelineItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String status;
    private String label;
    private String date;
    private String note;

    public TimelineItem() {}

    public TimelineItem(String status, String label, String date, String note) {
        this.status = status;
        this.label = label;
        this.date = date;
        this.note = note;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
