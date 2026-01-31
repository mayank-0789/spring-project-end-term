package com.eventbooking.repository;

import com.eventbooking.model.TicketType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketTypeRepository extends JpaRepository<TicketType, Long> {

    List<TicketType> findByEventId(Long eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TicketType t WHERE t.id = :id")
    Optional<TicketType> findByIdWithLock(@Param("id") Long id);

    boolean existsByEventIdAndName(Long eventId, String name);

    @Query("SELECT t FROM TicketType t WHERE t.event.id = :eventId AND t.availableQuantity > 0")
    List<TicketType> findAvailableByEventId(@Param("eventId") Long eventId);
}
