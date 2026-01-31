package com.eventbooking.service;

import com.eventbooking.dto.request.EventRequest;
import com.eventbooking.dto.request.TicketTypeRequest;
import com.eventbooking.dto.response.EventResponse;
import com.eventbooking.dto.response.TicketTypeResponse;
import com.eventbooking.dto.response.VenueResponse;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.exception.UnauthorizedException;
import com.eventbooking.exception.ValidationException;
import com.eventbooking.model.Event;
import com.eventbooking.model.TicketType;
import com.eventbooking.model.User;
import com.eventbooking.model.Venue;
import com.eventbooking.model.enums.EventStatus;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.TicketTypeRepository;
import com.eventbooking.repository.UserRepository;
import com.eventbooking.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final FileUploadService fileUploadService;

    @Transactional
    @CacheEvict(value = { "events", "popularEvents" }, allEntries = true)
    public EventResponse createEvent(EventRequest request, String organizerEmail) {
        User organizer = userRepository.findByEmail(organizerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", request.getVenueId()));

        validateEventDates(request.getStartDate(), request.getEndDate());

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .venue(venue)
                .organizer(organizer)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .bannerImage(request.getImageUrl())
                .status(request.getStatus() != null ? request.getStatus() : EventStatus.DRAFT)
                .build();

        event = eventRepository.save(event);

        // Create ticket types if provided
        if (request.getTicketTypes() != null && !request.getTicketTypes().isEmpty()) {
            List<TicketType> ticketTypes = createTicketTypes(event, request.getTicketTypes());
            event.setTicketTypes(ticketTypes);
        }

        return mapToResponse(event);
    }

    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
        return mapToResponse(event);
    }

    public Page<EventResponse> getAllPublishedEvents(Pageable pageable) {
        return eventRepository.findUpcomingEvents(EventStatus.PUBLISHED, LocalDateTime.now(), pageable)
                .map(this::mapToResponse);
    }

    public Page<EventResponse> getOrganizerEvents(String email, Pageable pageable) {
        User organizer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return eventRepository.findByOrganizerId(organizer.getId(), pageable)
                .map(this::mapToResponse);
    }

    public Page<EventResponse> getPopularEvents(Pageable pageable) {
        return eventRepository.findPopularEvents(pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    @CacheEvict(value = { "events", "event", "popularEvents" }, allEntries = true)
    public EventResponse updateEvent(Long id, EventRequest request, String organizerEmail) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        verifyOwnership(event, organizerEmail);
        validateEventDates(request.getStartDate(), request.getEndDate());

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", request.getVenueId()));

        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setVenue(venue);
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
        event.setBannerImage(request.getImageUrl());

        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }

        event = eventRepository.save(event);
        return mapToResponse(event);
    }

    @Transactional
    @CacheEvict(value = { "events", "event", "popularEvents" }, allEntries = true)
    public EventResponse updateEventStatus(Long id, EventStatus status, String organizerEmail) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        verifyOwnership(event, organizerEmail);
        event.setStatus(status);
        event = eventRepository.save(event);
        return mapToResponse(event);
    }

    @Transactional
    @CacheEvict(value = { "events", "event", "popularEvents" }, allEntries = true)
    public void deleteEvent(Long id, String organizerEmail) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        verifyOwnership(event, organizerEmail);
        eventRepository.delete(event);
    }

    @Transactional
    @CacheEvict(value = { "events", "event" }, allEntries = true)
    public String uploadEventBanner(Long eventId, MultipartFile file, String organizerEmail) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        verifyOwnership(event, organizerEmail);

        String imageUrl = fileUploadService.uploadImage(file, "events");
        event.setBannerImage(imageUrl);
        eventRepository.save(event);

        return imageUrl;
    }

    @Transactional
    @CacheEvict(value = "ticketTypes", allEntries = true)
    public TicketTypeResponse addTicketType(Long eventId, TicketTypeRequest request, String organizerEmail) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", eventId));

        verifyOwnership(event, organizerEmail);

        if (ticketTypeRepository.existsByEventIdAndName(eventId, request.getName())) {
            throw new ValidationException("Ticket type with this name already exists for this event");
        }

        TicketType ticketType = TicketType.builder()
                .event(event)
                .name(request.getName())
                .price(request.getPrice())
                .totalQuantity(request.getTotalQuantity())
                .availableQuantity(request.getTotalQuantity())
                .build();

        ticketType = ticketTypeRepository.save(ticketType);
        return mapTicketTypeToResponse(ticketType);
    }

    public List<TicketTypeResponse> getEventTicketTypes(Long eventId) {
        return ticketTypeRepository.findByEventId(eventId).stream()
                .map(this::mapTicketTypeToResponse)
                .collect(Collectors.toList());
    }

    private List<TicketType> createTicketTypes(Event event, List<TicketTypeRequest> requests) {
        List<TicketType> ticketTypes = new ArrayList<>();
        for (TicketTypeRequest request : requests) {
            TicketType ticketType = TicketType.builder()
                    .event(event)
                    .name(request.getName())
                    .price(request.getPrice())
                    .totalQuantity(request.getTotalQuantity())
                    .availableQuantity(request.getTotalQuantity())
                    .build();
            ticketTypes.add(ticketTypeRepository.save(ticketType));
        }
        return ticketTypes;
    }

    private void validateEventDates(LocalDateTime startDate, LocalDateTime endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date must be after start date");
        }
    }

    private void verifyOwnership(Event event, String email) {
        if (!event.getOrganizer().getEmail().equals(email)) {
            throw new UnauthorizedException("You are not authorized to modify this event");
        }
    }

    private EventResponse mapToResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .description(event.getDescription())
                .venue(mapVenueToResponse(event.getVenue()))
                .organizer(mapOrganizerInfo(event.getOrganizer()))
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .imageUrl(event.getBannerImage())
                .status(event.getStatus())
                .ticketTypes(event.getTicketTypes().stream()
                        .map(this::mapTicketTypeToResponse)
                        .collect(Collectors.toList()))
                .createdAt(event.getCreatedAt())
                .build();
    }

    private VenueResponse mapVenueToResponse(Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .city(venue.getCity())
                .capacity(venue.getCapacity())
                .createdAt(venue.getCreatedAt())
                .build();
    }

    private EventResponse.UserInfo mapOrganizerInfo(User organizer) {
        return EventResponse.UserInfo.builder()
                .id(organizer.getId())
                .name(organizer.getName())
                .email(organizer.getEmail())
                .build();
    }

    private TicketTypeResponse mapTicketTypeToResponse(TicketType ticketType) {
        return TicketTypeResponse.builder()
                .id(ticketType.getId())
                .name(ticketType.getName())
                .price(ticketType.getPrice())
                .totalQuantity(ticketType.getTotalQuantity())
                .availableQuantity(ticketType.getAvailableQuantity())
                .build();
    }
}
