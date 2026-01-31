package com.eventbooking.controller;

import com.eventbooking.dto.request.VenueRequest;
import com.eventbooking.dto.response.MessageResponse;
import com.eventbooking.dto.response.VenueResponse;
import com.eventbooking.service.VenueService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/venues")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<VenueResponse> create(@Valid @RequestBody VenueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venueService.createVenue(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VenueResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(venueService.getVenueById(id));
    }

    @GetMapping
    public ResponseEntity<Page<VenueResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(venueService.getAllVenues(pageable));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<VenueResponse>> searchByCity(@RequestParam String city, Pageable pageable) {
        return ResponseEntity.ok(venueService.searchVenuesByCity(city, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VenueResponse> update(@PathVariable Long id, @Valid @RequestBody VenueRequest request) {
        return ResponseEntity.ok(venueService.updateVenue(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(@PathVariable Long id) {
        venueService.deleteVenue(id);
        return ResponseEntity.ok(MessageResponse.of("Venue deleted"));
    }
}
