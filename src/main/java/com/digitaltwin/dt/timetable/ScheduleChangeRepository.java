package com.digitaltwin.dt.timetable;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

public interface ScheduleChangeRepository extends JpaRepository<ScheduleChange, Long> {
    boolean existsByTimetableEntry_IdAndClassDate(Long timetableEntryId, LocalDate classDate);
    java.util.List<ScheduleChange> findAllByClassDate(LocalDate classDate);
}

