package com.eventbooking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsResponse {
    private Long totalEvents;
    private Long totalBookings;
    private BigDecimal totalRevenue;
    private List<EventRevenue> revenueByEvent;
    private List<DailyStats> dailyBookings;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EventRevenue {
        private String eventTitle;
        private LocalDateTime eventDate;
        private Long totalBookings;
        private BigDecimal totalRevenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        private String date;
        private Long bookingCount;
        private BigDecimal revenue;
    }
}
