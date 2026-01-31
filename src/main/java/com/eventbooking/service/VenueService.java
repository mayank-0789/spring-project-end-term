package com.eventbooking.service;

import com.eventbooking.dto.request.VenueRequest;
import com.eventbooking.dto.response.VenueResponse;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.exception.ValidationException;
import com.eventbooking.model.Venue;
import com.eventbooking.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VenueService {

    private final VenueRepository venueRepository;

    @Transactional
    @CacheEvict(value = "venues", allEntries = true)
    public VenueResponse createVenue(VenueRequest request) {
        if (venueRepository.existsByNameAndCity(request.getName(), request.getCity())) {
            throw new ValidationException("Venue with this name already exists in the city");
        }

        Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .capacity(request.getCapacity())
                .description(request.getDescription())
                .build();

        venue = venueRepository.save(venue);
        return mapToResponse(venue);
    }

    public VenueResponse getVenueById(Long id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));
        return mapToResponse(venue);
    }

    public Page<VenueResponse> getAllVenues(Pageable pageable) {
        return venueRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    public Page<VenueResponse> searchVenuesByCity(String city, Pageable pageable) {
        return venueRepository.findByCityContainingIgnoreCase(city, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    @CacheEvict(value = { "venues", "venue" }, allEntries = true)
    public VenueResponse updateVenue(Long id, VenueRequest request) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", id));

        venue.setName(request.getName());
        venue.setAddress(request.getAddress());
        venue.setCity(request.getCity());
        venue.setCapacity(request.getCapacity());

        venue = venueRepository.save(venue);
        return mapToResponse(venue);
    }

    @Transactional
    @CacheEvict(value = { "venues", "venue" }, allEntries = true)
    public void deleteVenue(Long id) {
        if (!venueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Venue", id);
        }
        venueRepository.deleteById(id);
    }

    private VenueResponse mapToResponse(Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .capacity(venue.getCapacity())
                .description(venue.getDescription())
                .createdAt(venue.getCreatedAt())
                .build();
    }
}
