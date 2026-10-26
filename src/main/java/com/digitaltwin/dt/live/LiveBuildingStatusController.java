package com.digitaltwin.dt.live;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class LiveBuildingStatusController {
    private final LiveBuildingStatusService statusService;
    public LiveBuildingStatusController(LiveBuildingStatusService statusService) { this.statusService = statusService; }

    @GetMapping("/api/teachers/{staffCode}/current-location")
    public TeacherCurrentLocation teacherNow(@PathVariable String staffCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return statusService.teacherNow(staffCode, at);
    }

    @GetMapping("/api/rooms/current")
    public List<RoomCurrentStatus> roomsNow(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime at) {
        return statusService.roomsNow(at);
    }
}
