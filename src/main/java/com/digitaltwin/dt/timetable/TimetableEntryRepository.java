package com.digitaltwin.dt.timetable;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.DayOfWeek;
import java.util.List;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {
    List<TimetableEntry> findAllByDayOfWeek(DayOfWeek dayOfWeek);
    List<TimetableEntry> findAllByOrderByDayOfWeekAscStartTimeAsc();
    List<TimetableEntry> findAllByTeacher_StaffCodeIgnoreCaseOrderByDayOfWeekAscStartTimeAsc(String staffCode);
    List<TimetableEntry> findAllBySection_CodeIgnoreCaseOrderByDayOfWeekAscStartTimeAsc(String sectionCode);
}


