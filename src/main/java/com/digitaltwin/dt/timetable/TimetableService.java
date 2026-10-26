package com.digitaltwin.dt.timetable;

import com.digitaltwin.dt.rooms.Room;
import com.digitaltwin.dt.rooms.RoomRepository;
import com.digitaltwin.dt.sections.SectionRepository;
import com.digitaltwin.dt.sections.StudentSection;
import com.digitaltwin.dt.subjects.Subject;
import com.digitaltwin.dt.subjects.SubjectRepository;
import com.digitaltwin.dt.teachers.Teacher;
import com.digitaltwin.dt.teachers.TeacherRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Service
public class TimetableService {
    private static final LocalTime OPEN_TIME = LocalTime.of(7, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(19, 0);

    private final TimetableEntryRepository entryRepository;
    private final RoomRepository roomRepository;
    private final TeacherRepository teacherRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;

    public TimetableService(TimetableEntryRepository entryRepository, RoomRepository roomRepository,
                            TeacherRepository teacherRepository, SectionRepository sectionRepository,
                            SubjectRepository subjectRepository) {
        this.entryRepository = entryRepository;
        this.roomRepository = roomRepository;
        this.teacherRepository = teacherRepository;
        this.sectionRepository = sectionRepository;
        this.subjectRepository = subjectRepository;
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> list() {
        return entryRepository.findAllByOrderByDayOfWeekAscStartTimeAsc().stream()
                .map(TimetableEntryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> listForTeacher(String staffCode) {
        if (teacherRepository.findByStaffCodeIgnoreCase(staffCode.trim()).isEmpty()) {
            throw notFound("Teacher", staffCode);
        }
        return entryRepository.findAllByTeacher_StaffCodeIgnoreCaseOrderByDayOfWeekAscStartTimeAsc(staffCode.trim())
                .stream().map(TimetableEntryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<TimetableEntryResponse> listForSection(String sectionCode) {
        if (sectionRepository.findByCodeIgnoreCase(sectionCode.trim()).isEmpty()) {
            throw notFound("Section", sectionCode);
        }
        return entryRepository.findAllBySection_CodeIgnoreCaseOrderByDayOfWeekAscStartTimeAsc(sectionCode.trim())
                .stream().map(TimetableEntryResponse::from).toList();
    }

    @Transactional
    public TimetableEntryResponse create(@Valid CreateTimetableEntryRequest request) {
        if (!request.startTime().isBefore(request.endTime())
                || request.startTime().isBefore(OPEN_TIME)
                || request.endTime().isAfter(CLOSE_TIME)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Class time must be within 07:00–19:00 and end after start");
        }

        Room room = roomRepository.findByRoomNumberIgnoreCase(request.roomNumber().trim())
                .orElseThrow(() -> notFound("Room", request.roomNumber()));
        Teacher teacher = teacherRepository.findByStaffCodeIgnoreCase(request.teacherCode().trim())
                .orElseThrow(() -> notFound("Teacher", request.teacherCode()));
        StudentSection section = sectionRepository.findByCodeIgnoreCase(request.sectionCode().trim())
                .orElseThrow(() -> notFound("Section", request.sectionCode()));
        Subject subject = subjectRepository.findByCodeIgnoreCase(request.subjectCode().trim())
                .orElseThrow(() -> notFound("Subject", request.subjectCode()));

        for (TimetableEntry existing : entryRepository.findAllByDayOfWeek(request.dayOfWeek())) {
            if (weeksOverlap(existing.getWeekPattern(), request.weekPattern())
                    && timesOverlap(existing.getStartTime(), existing.getEndTime(), request.startTime(), request.endTime())
                    && (existing.getRoom().getId().equals(room.getId())
                        || existing.getTeacher().getId().equals(teacher.getId())
                        || existing.getSection().getId().equals(section.getId()))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "This room, teacher, or section already has a class during that time");
            }
        }

        TimetableEntry entry = entryRepository.save(new TimetableEntry(room, teacher, section, subject,
                request.dayOfWeek(), request.weekPattern(), request.startTime(), request.endTime()));
        return TimetableEntryResponse.from(entry);
    }

    private boolean weeksOverlap(WeekPattern first, WeekPattern second) {
        return first == WeekPattern.EVERY_WEEK || second == WeekPattern.EVERY_WEEK || first == second;
    }

    private boolean timesOverlap(LocalTime firstStart, LocalTime firstEnd,
                                 LocalTime secondStart, LocalTime secondEnd) {
        return firstStart.isBefore(secondEnd) && secondStart.isBefore(firstEnd);
    }

    private ResponseStatusException notFound(String type, String identifier) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, type + " not found: " + identifier);
    }

    public record CreateTimetableEntryRequest(
            @NotBlank @Size(max = 30) String roomNumber,
            @NotBlank @Size(max = 40) String teacherCode,
            @NotBlank @Size(max = 40) String sectionCode,
            @NotBlank @Size(max = 40) String subjectCode,
            @NotNull DayOfWeek dayOfWeek,
            @NotNull WeekPattern weekPattern,
            @NotNull LocalTime startTime,
            @NotNull LocalTime endTime) { }
}

