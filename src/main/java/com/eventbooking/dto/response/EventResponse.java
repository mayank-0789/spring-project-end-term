package com.eventbooking.dto.response;

import com.eventbooking.model.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private Long id;
    private String title;
    private String description;
    private VenueResponse venue;
    private UserInfo organizer;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private EventStatus status;
    private String imageUrl;
    private List<TicketTypeResponse> ticketTypes;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String name;
        private String email;
    }
}
