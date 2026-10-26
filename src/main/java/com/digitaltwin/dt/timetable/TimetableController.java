package com.digitaltwin.dt.timetable;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/timetable")
public class TimetableController {
    private final TimetableService timetableService;
    public TimetableController(TimetableService timetableService) { this.timetableService = timetableService; }

    @GetMapping
    public List<TimetableEntryResponse> list() { return timetableService.list(); }

    @GetMapping("/teacher/{staffCode}")
    public List<TimetableEntryResponse> listForTeacher(@PathVariable String staffCode) {
        return timetableService.listForTeacher(staffCode);
    }

    @GetMapping("/section/{sectionCode}")
    public List<TimetableEntryResponse> listForSection(@PathVariable String sectionCode) {
        return timetableService.listForSection(sectionCode);
    }

    @GetMapping("/room/{roomNumber}")
    public List<TimetableEntryResponse> listForRoom(@PathVariable String roomNumber) {
        return timetableService.listForRoom(roomNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimetableEntryResponse create(@Valid @RequestBody TimetableService.CreateTimetableEntryRequest request) {
        return timetableService.create(request);
    }
}



