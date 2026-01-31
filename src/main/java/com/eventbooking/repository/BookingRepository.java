package com.eventbooking.repository;

import com.eventbooking.model.Booking;
import com.eventbooking.model.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByUserId(Long userId, Pageable pageable);

    Page<Booking> findByUserEmail(String email, Pageable pageable);

    Optional<Booking> findByBookingReference(String bookingReference);

    List<Booking> findByStatusAndExpiresAtBefore(BookingStatus status, LocalDateTime expiresAt);

    @Query("SELECT b FROM Booking b WHERE b.event.id = :eventId AND b.status = 'CONFIRMED'")
    List<Booking> findConfirmedByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.event.id = :eventId AND b.status = 'CONFIRMED'")
    long countConfirmedByEventId(@Param("eventId") Long eventId);

    List<Booking> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.status = 'CONFIRMED'")
    long countConfirmedByUserId(@Param("userId") Long userId);
}
