package com.campusconnect.model;

import java.io.Serializable;

public class UserSummary implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String email;
    private String studentId;

    public UserSummary() {}

    public UserSummary(String name, String email, String studentId) {
        this.name = name;
        this.email = email;
        this.studentId = studentId;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
}
