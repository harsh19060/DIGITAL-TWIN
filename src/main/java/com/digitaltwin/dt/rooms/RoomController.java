package com.digitaltwin.dt.rooms;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomRepository roomRepository;

    public RoomController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @GetMapping
    public List<Room> listRooms() {
        return roomRepository.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Room createRoom(@Valid @RequestBody CreateRoomRequest request) {
        if (roomRepository.findByRoomNumberIgnoreCase(request.roomNumber()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A room with this number already exists");
        }

        Room room = new Room(
                request.roomNumber().trim(),
                request.floor(),
                request.capacity(),
                request.roomType().trim(),
                request.equipment() == null ? null : request.equipment().trim());
        return roomRepository.save(room);
    }

    public record CreateRoomRequest(
            @NotBlank @Size(max = 30) String roomNumber,
            @NotNull Integer floor,
            @NotNull @Min(1) Integer capacity,
            @NotBlank @Size(max = 40) String roomType,
            @Size(max = 500) String equipment) {
    }
}
