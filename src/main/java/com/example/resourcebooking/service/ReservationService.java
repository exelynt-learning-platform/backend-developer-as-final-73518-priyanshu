package com.example.resourcebooking.service;

import com.example.resourcebooking.dto.ReservationRequest;
import com.example.resourcebooking.dto.ReservationResponse;
import com.example.resourcebooking.entity.Reservation;
import com.example.resourcebooking.entity.Resource;
import com.example.resourcebooking.entity.User;
import com.example.resourcebooking.enums.ReservationStatus;
import com.example.resourcebooking.enums.Role;
import com.example.resourcebooking.exception.BadRequestException;
import com.example.resourcebooking.exception.ForbiddenException;
import com.example.resourcebooking.exception.ResourceNotFoundException;
import com.example.resourcebooking.repository.ReservationRepository;
import com.example.resourcebooking.repository.ResourceRepository;
import com.example.resourcebooking.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              ResourceRepository resourceRepository,
                              UserRepository userRepository) {
        this.reservationRepository = reservationRepository;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    public Page<ReservationResponse> findAll(String username, ReservationStatus status,
                                             BigDecimal minPrice, BigDecimal maxPrice,
                                             Pageable pageable) {
        User caller = getUser(username);
        Specification<Reservation> spec = buildSpec(
                caller.getRole() == Role.ADMIN ? null : caller.getId(),
                status, minPrice, maxPrice
        );
        return reservationRepository.findAll(spec, pageable).map(ReservationResponse::new);
    }

    public ReservationResponse findById(String username, Long id) {
        User caller = getUser(username);
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        if (caller.getRole() != Role.ADMIN && !reservation.getUser().getId().equals(caller.getId())) {
            throw new ForbiddenException("Access denied to reservation: " + id);
        }
        return new ReservationResponse(reservation);
    }

    public ReservationResponse create(String username, ReservationRequest request) {
        validateTimes(request);
        User user = getUser(username);
        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + request.getResourceId()));

        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        reservation.setStatus(request.getStatus() != null ? request.getStatus() : ReservationStatus.PENDING);

        return new ReservationResponse(reservationRepository.save(reservation));
    }

    public ReservationResponse update(String username, Long id, ReservationRequest request) {
        validateTimes(request);
        User caller = getUser(username);
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));

        if (caller.getRole() != Role.ADMIN && !reservation.getUser().getId().equals(caller.getId())) {
            throw new ForbiddenException("Access denied to reservation: " + id);
        }

        Resource resource = resourceRepository.findById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + request.getResourceId()));

        reservation.setResource(resource);
        reservation.setStartTime(request.getStartTime());
        reservation.setEndTime(request.getEndTime());
        reservation.setPrice(request.getPrice());
        if (request.getStatus() != null) {
            reservation.setStatus(request.getStatus());
        }

        return new ReservationResponse(reservationRepository.save(reservation));
    }

    public void delete(String username, Long id) {
        User caller = getUser(username);
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation not found: " + id));
        if (caller.getRole() != Role.ADMIN && !reservation.getUser().getId().equals(caller.getId())) {
            throw new ForbiddenException("Access denied to reservation: " + id);
        }
        reservationRepository.delete(reservation);
    }

    private Specification<Reservation> buildSpec(Long userId, ReservationStatus status,
                                                  BigDecimal minPrice, BigDecimal maxPrice) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (userId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), userId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validateTimes(ReservationRequest request) {
        if (request.getStartTime() != null && request.getEndTime() != null
                && !request.getStartTime().isBefore(request.getEndTime())) {
            throw new BadRequestException("Start time must be before end time");
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}
