package com.eventbooking.dto.response;

import com.eventbooking.model.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private String ticketNumber;
    private String qrCode;
    private TicketStatus status;
    private String eventTitle;
    private String ticketTypeName;
    private LocalDateTime eventStartDate;
    private String venueName;
    private LocalDateTime createdAt;
}
