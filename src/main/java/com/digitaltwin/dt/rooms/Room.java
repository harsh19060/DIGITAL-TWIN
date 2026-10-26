package com.digitaltwin.dt.rooms;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_number", nullable = false, unique = true, length = 30)
    private String roomNumber;

    @Column(nullable = false)
    private Integer floor;

    @Column(nullable = false)
    private Integer capacity;

    @Column(name = "room_type", nullable = false, length = 40)
    private String roomType;

    @Column(length = 500)
    private String equipment;

    protected Room() {
    }

    public Room(String roomNumber, Integer floor, Integer capacity, String roomType, String equipment) {
        this.roomNumber = roomNumber;
        this.floor = floor;
        this.capacity = capacity;
        this.roomType = roomType;
        this.equipment = equipment;
    }

    public Long getId() {
        return id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public Integer getFloor() {
        return floor;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public String getRoomType() {
        return roomType;
    }

    public String getEquipment() {
        return equipment;
    }
}
