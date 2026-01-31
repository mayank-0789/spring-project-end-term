package com.eventbooking.controller;

import com.eventbooking.dto.request.EventRequest;
import com.eventbooking.dto.request.TicketTypeRequest;
import com.eventbooking.dto.response.EventResponse;
import com.eventbooking.dto.response.MessageResponse;
import com.eventbooking.dto.response.TicketTypeResponse;
import com.eventbooking.model.enums.EventStatus;
import com.eventbooking.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping
    public ResponseEntity<EventResponse> create(
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(request, user.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping
    public ResponseEntity<Page<EventResponse>> getAll(Pageable pageable) {
        return ResponseEntity.ok(eventService.getAllPublishedEvents(pageable));
    }

    @GetMapping("/popular")
    public ResponseEntity<Page<EventResponse>> getPopular(Pageable pageable) {
        return ResponseEntity.ok(eventService.getPopularEvents(pageable));
    }

    @GetMapping("/my-events")
    public ResponseEntity<Page<EventResponse>> getMyEvents(
            @AuthenticationPrincipal UserDetails user, Pageable pageable) {
        return ResponseEntity.ok(eventService.getOrganizerEvents(user.getUsername(), pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, user.getUsername()));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<EventResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam EventStatus status,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(eventService.updateEventStatus(id, status, user.getUsername()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails user) {
        eventService.deleteEvent(id, user.getUsername());
        return ResponseEntity.ok(MessageResponse.of("Event deleted"));
    }

    @PostMapping(value = "/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadBanner(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails user) {
        String imageUrl = eventService.uploadEventBanner(id, file, user.getUsername());
        return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
    }

    @PostMapping("/{id}/ticket-types")
    public ResponseEntity<TicketTypeResponse> addTicketType(
            @PathVariable Long id,
            @Valid @RequestBody TicketTypeRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.addTicketType(id, request, user.getUsername()));
    }

    @GetMapping("/{id}/ticket-types")
    public ResponseEntity<List<TicketTypeResponse>> getTicketTypes(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventTicketTypes(id));
    }
}
