package com.example.resourcebooking.dto;

import com.example.resourcebooking.entity.Reservation;
import com.example.resourcebooking.enums.ReservationStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class ReservationResponse {

    private final Long id;
    private final Long resourceId;
    private final String resourceName;
    private final Long userId;
    private final String username;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final BigDecimal price;
    private final ReservationStatus status;

    public ReservationResponse(Reservation reservation) {
        this.id = reservation.getId();
        this.resourceId = reservation.getResource().getId();
        this.resourceName = reservation.getResource().getName();
        this.userId = reservation.getUser().getId();
        this.username = reservation.getUser().getUsername();
        this.startTime = reservation.getStartTime();
        this.endTime = reservation.getEndTime();
        this.price = reservation.getPrice();
        this.status = reservation.getStatus();
    }
}
