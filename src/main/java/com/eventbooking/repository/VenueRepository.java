package com.eventbooking.repository;

import com.eventbooking.model.Venue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

    List<Venue> findByCity(String city);

    Page<Venue> findByCityContainingIgnoreCase(String city, Pageable pageable);

    boolean existsByNameAndCity(String name, String city);
}
