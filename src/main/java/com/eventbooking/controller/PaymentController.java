package com.eventbooking.controller;

import com.eventbooking.dto.request.PaymentVerificationRequest;
import com.eventbooking.dto.response.BookingResponse;
import com.eventbooking.dto.response.PaymentOrderResponse;
import com.eventbooking.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order/{bookingReference}")
    public ResponseEntity<PaymentOrderResponse> createOrder(
            @PathVariable String bookingReference,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(paymentService.createPaymentOrder(bookingReference, user.getUsername()));
    }

    @PostMapping("/verify")
    public ResponseEntity<BookingResponse> verify(
            @Valid @RequestBody PaymentVerificationRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(paymentService.verifyPayment(request, user.getUsername()));
    }
}
