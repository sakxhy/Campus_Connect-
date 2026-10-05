package com.campusconnect.model;

import java.io.Serializable;

public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String author;
    private String role;
    private String text;
    private String date;

    public Comment() {}

    public Comment(String id, String author, String role, String text, String date) {
        this.id = id;
        this.author = author;
        this.role = role;
        this.text = text;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
