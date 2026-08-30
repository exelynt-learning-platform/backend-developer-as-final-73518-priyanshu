package com.example.resourcebooking.repository;

import com.example.resourcebooking.entity.Reservation;
import com.example.resourcebooking.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.math.BigDecimal;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long>,
        JpaSpecificationExecutor<Reservation> {

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);
}
