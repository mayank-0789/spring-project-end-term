package com.eventbooking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class BookingRequest {

    @NotNull
    private Long eventId;

    @NotNull
    private Long ticketTypeId;

    @NotNull
    @Positive
    private Integer quantity;
}
