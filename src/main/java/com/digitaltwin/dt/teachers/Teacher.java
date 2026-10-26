package com.digitaltwin.dt.teachers;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "teachers")
public class Teacher {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "staff_code", nullable = false, unique = true, length = 40)
    private String staffCode;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(length = 160)
    private String email;

    protected Teacher() { }

    public Teacher(String staffCode, String fullName, String email) {
        this.staffCode = staffCode;
        this.fullName = fullName;
        this.email = email;
    }

    public Long getId() { return id; }
    public String getStaffCode() { return staffCode; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
}
