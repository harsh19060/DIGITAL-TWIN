package com.digitaltwin.dt.live;

import java.time.Instant;

public record TeacherCurrentLocation(String teacherCode, String teacherName, String status,
                                     LiveClass currentClass, LiveClass nextClass, Instant observedAt,
                                     String dataSource) { }
