package com.example.resourcebooking.controller;

import com.example.resourcebooking.dto.ReservationRequest;
import com.example.resourcebooking.dto.ReservationResponse;
import com.example.resourcebooking.enums.ReservationStatus;
import com.example.resourcebooking.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/reservations")
@Tag(name = "Reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping
    @Operation(summary = "List reservations with optional filtering, pagination and sorting")
    public ResponseEntity<Page<ReservationResponse>> list(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            Pageable pageable) {
        return ResponseEntity.ok(
                reservationService.findAll(principal.getUsername(), status, minPrice, maxPrice, pageable)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a reservation by ID")
    public ResponseEntity<ReservationResponse> get(@AuthenticationPrincipal UserDetails principal,
                                                   @PathVariable Long id) {
        return ResponseEntity.ok(reservationService.findById(principal.getUsername(), id));
    }

    @PostMapping
    @Operation(summary = "Create a reservation (USER identity from JWT)")
    public ResponseEntity<ReservationResponse> create(@AuthenticationPrincipal UserDetails principal,
                                                      @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reservationService.create(principal.getUsername(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a reservation (owner or ADMIN)")
    public ResponseEntity<ReservationResponse> update(@AuthenticationPrincipal UserDetails principal,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationService.update(principal.getUsername(), id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a reservation (owner or ADMIN)")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserDetails principal,
                                       @PathVariable Long id) {
        reservationService.delete(principal.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
