package com.eventbooking.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookEvent {
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String status; // SUCCESS, FAILED
}
