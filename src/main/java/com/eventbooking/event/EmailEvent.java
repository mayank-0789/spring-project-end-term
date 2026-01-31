package com.eventbooking.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent {
    private EmailType type;
    private String toEmail;
    private String userName;

    // For booking emails
    private String bookingReference;
    private String eventTitle;
    private String eventDate;
    private String venueName;
    private Integer quantity;
    private String totalAmount;
    private String ticketNumbers;

    public enum EmailType {
        WELCOME,
        BOOKING_CONFIRMATION,
        BOOKING_CANCELLATION
    }
}
