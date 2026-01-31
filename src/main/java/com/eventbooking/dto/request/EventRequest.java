package com.eventbooking.dto.request;

import com.eventbooking.model.enums.EventStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class EventRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 5000)
    private String description;

    @NotNull
    private Long venueId;

    @NotNull
    @Future
    private LocalDateTime startDate;

    @NotNull
    @Future
    private LocalDateTime endDate;

    private EventStatus status = EventStatus.DRAFT;

    private String imageUrl;

    private List<TicketTypeRequest> ticketTypes;
}
