package com.digitaltwin.dt.students;

import com.digitaltwin.dt.sections.StudentSection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "campus_students")
public class CampusStudent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_code", nullable = false, unique = true, length = 60)
    private String studentCode;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "section_id", nullable = false)
    private StudentSection section;

    protected CampusStudent() { }
    public CampusStudent(String studentCode, String fullName, StudentSection section) {
        this.studentCode = studentCode;
        this.fullName = fullName;
        this.section = section;
    }

    public Long getId() { return id; }
    public String getStudentCode() { return studentCode; }
    public String getFullName() { return fullName; }
    public StudentSection getSection() { return section; }
}
