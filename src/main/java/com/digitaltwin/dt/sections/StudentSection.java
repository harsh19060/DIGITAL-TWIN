package com.digitaltwin.dt.sections;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_sections")
public class StudentSection {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String code;

    @Column(name = "student_count", nullable = false)
    private Integer studentCount;

    protected StudentSection() { }
    public StudentSection(String code, Integer studentCount) { this.code = code; this.studentCount = studentCount; }
    public Long getId() { return id; }
    public String getCode() { return code; }
    public Integer getStudentCount() { return studentCount; }
}
