package com.digitaltwin.dt.rooms;

import com.digitaltwin.dt.timetable.TimetableEntry;
import com.digitaltwin.dt.timetable.TimetableEntryRepository;
import com.digitaltwin.dt.timetable.WeekPattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
public class RoomAvailabilityController {
    private static final LocalTime OPEN_TIME = LocalTime.of(7, 0);
    private static final LocalTime CLOSE_TIME = LocalTime.of(19, 0);

    private final RoomRepository roomRepository;
    private final TimetableEntryRepository timetableRepository;

    public RoomAvailabilityController(RoomRepository roomRepository,
                                      TimetableEntryRepository timetableRepository) {
        this.roomRepository = roomRepository;
        this.timetableRepository = timetableRepository;
    }

    @GetMapping("/available")
    @Transactional(readOnly = true)
    public List<Room> findAvailableRooms(
            @RequestParam DayOfWeek dayOfWeek,
            @RequestParam WeekPattern weekPattern,
            @RequestParam LocalTime startTime,
            @RequestParam LocalTime endTime,
            @RequestParam(defaultValue = "1") int minCapacity) {
        if (startTime == null || endTime == null || !startTime.isBefore(endTime)
                || startTime.isBefore(OPEN_TIME) || endTime.isAfter(CLOSE_TIME)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Requested time must be within 07:00–19:00 and end after start");
        }
        if (minCapacity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Minimum capacity must be at least 1");
        }

        Set<Long> busyRoomIds = timetableRepository.findAllByDayOfWeek(dayOfWeek).stream()
                .filter(entry -> weeksOverlap(entry.getWeekPattern(), weekPattern))
                .filter(entry -> timesOverlap(entry, startTime, endTime))
                .map(entry -> entry.getRoom().getId())
                .collect(Collectors.toCollection(HashSet::new));

        return roomRepository.findAll().stream()
                .filter(room -> room.getCapacity() >= minCapacity)
                .filter(room -> !busyRoomIds.contains(room.getId()))
                .sorted((first, second) -> first.getRoomNumber().compareToIgnoreCase(second.getRoomNumber()))
                .toList();
    }

    private boolean weeksOverlap(WeekPattern scheduled, WeekPattern requested) {
        return scheduled == WeekPattern.EVERY_WEEK
                || requested == WeekPattern.EVERY_WEEK
                || scheduled == requested;
    }

    private boolean timesOverlap(TimetableEntry entry, LocalTime start, LocalTime end) {
        return entry.getStartTime().isBefore(end) && start.isBefore(entry.getEndTime());
    }
}
