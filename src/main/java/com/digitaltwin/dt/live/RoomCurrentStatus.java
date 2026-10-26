package com.digitaltwin.dt.live;

import java.time.Instant;

public record RoomCurrentStatus(String roomNumber, Integer floor, Integer capacity,
                                String roomType, String status, LiveClass currentClass,
                                Instant observedAt, String dataSource) { }
