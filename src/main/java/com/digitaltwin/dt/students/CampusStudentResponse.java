package com.digitaltwin.dt.students;

public record CampusStudentResponse(String studentCode, String fullName, String sectionCode) {
    static CampusStudentResponse from(CampusStudent student) {
        return new CampusStudentResponse(student.getStudentCode(), student.getFullName(), student.getSection().getCode());
    }
}
