package com.eventbooking.repository;

import com.eventbooking.model.Event;
import com.eventbooking.model.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.startDate > :now ORDER BY e.startDate ASC")
    Page<Event> findUpcomingEvents(@Param("status") EventStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable);

    @Query("SELECT e.title, e.startDate, COUNT(b.id) as totalBookings, COALESCE(SUM(p.amount), 0) as totalRevenue " +
            "FROM Event e " +
            "LEFT JOIN Booking b ON e.id = b.event.id AND b.status = 'CONFIRMED' " +
            "LEFT JOIN Payment p ON b.id = p.booking.id AND p.status = 'SUCCESS' " +
            "WHERE e.organizer.id = :organizerId " +
            "GROUP BY e.id, e.title, e.startDate " +
            "ORDER BY totalRevenue DESC")
    List<Object[]> getRevenueByEvent(@Param("organizerId") Long organizerId);

    @Query("SELECT e FROM Event e " +
            "LEFT JOIN Booking b ON e.id = b.event.id AND b.status = 'CONFIRMED' " +
            "WHERE e.status = 'PUBLISHED' " +
            "GROUP BY e.id " +
            "ORDER BY COUNT(b.id) DESC")
    Page<Event> findPopularEvents(Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.organizer.id = :organizerId")
    long countByOrganizerId(@Param("organizerId") Long organizerId);

    List<Event> findByStatusAndEndDateBefore(EventStatus status, LocalDateTime endDate);
}
