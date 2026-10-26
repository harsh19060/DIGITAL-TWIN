package com.digitaltwin.dt.live;

import java.time.LocalTime;

public record LiveClass(String subjectCode, String subjectName, String sectionCode,
                        String teacherCode, String teacherName, String roomNumber,
                        LocalTime startTime, LocalTime endTime, String source) { }
