package com.eventbooking.repository;

import com.eventbooking.model.Ticket;
import com.eventbooking.model.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    List<Ticket> findByBookingId(Long bookingId);

    List<Ticket> findByBookingIdAndStatus(Long bookingId, TicketStatus status);

    long countByBookingIdAndStatus(Long bookingId, TicketStatus status);

    boolean existsByTicketNumber(String ticketNumber);
}
