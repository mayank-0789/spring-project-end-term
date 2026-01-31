package com.eventbooking.service;

import com.eventbooking.dto.request.PaymentVerificationRequest;
import com.eventbooking.dto.request.PaymentWebhookEvent;
import com.eventbooking.dto.response.BookingResponse;
import com.eventbooking.dto.response.PaymentOrderResponse;
import com.eventbooking.exception.*;
import com.eventbooking.model.Booking;
import com.eventbooking.model.Payment;
import com.eventbooking.model.enums.BookingStatus;
import com.eventbooking.model.enums.PaymentStatus;
import com.eventbooking.repository.BookingRepository;
import com.eventbooking.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Transactional
    public PaymentOrderResponse createPaymentOrder(String bookingReference, String userEmail) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You are not authorized to pay for this booking");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ValidationException("Booking is not in pending state");
        }

        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BookingExpiredException(bookingReference);
        }

        try {
            // Create Razorpay order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", booking.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue()); // Amount
                                                                                                               // in
                                                                                                               // paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", bookingReference);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            // Create or update payment record
            Payment payment = booking.getPayment();
            if (payment == null) {
                payment = Payment.builder()
                        .booking(booking)
                        .amount(booking.getTotalAmount())
                        .status(PaymentStatus.PENDING)
                        .build();
            }
            payment.setRazorpayOrderId(razorpayOrder.get("id"));
            payment = paymentRepository.save(payment);

            return PaymentOrderResponse.builder()
                    .razorpayOrderId(razorpayOrder.get("id"))
                    .amount(booking.getTotalAmount())
                    .currency("INR")
                    .bookingReference(bookingReference)
                    .razorpayKeyId(razorpayKeyId)
                    .build();

        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order: {}", e.getMessage());
            throw new PaymentFailedException("Failed to create payment order: " + e.getMessage());
        }
    }

    @Transactional
    public BookingResponse verifyPayment(PaymentVerificationRequest request, String userEmail) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        Booking booking = payment.getBooking();

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You are not authorized to verify this payment");
        }

        try {
            // Verify signature
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());

            boolean isValid = Utils.verifyPaymentSignature(attributes, razorpayKeySecret);

            if (!isValid) {
                payment.setStatus(PaymentStatus.FAILED);
                paymentRepository.save(payment);
                throw new PaymentFailedException("Payment verification failed - invalid signature");
            }

            // Update payment
            payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
            payment.setRazorpaySignature(request.getRazorpaySignature());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);

            // Confirm booking
            return bookingService.confirmBooking(booking.getId());

        } catch (RazorpayException e) {
            log.error("Error verifying payment: {}", e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentFailedException("Payment verification failed: " + e.getMessage());
        }
    }

    public Payment getPaymentByBookingReference(String bookingReference) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));
        return booking.getPayment();
    }

    @Transactional
    public void handleWebhook(PaymentWebhookEvent event) {
        log.info("Processing webhook for order: {}, status: {}", event.getRazorpayOrderId(), event.getStatus());

        Payment payment = paymentRepository.findByRazorpayOrderId(event.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found for order: " + event.getRazorpayOrderId()));

        // Avoid processing already completed payments
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            log.info("Payment already processed successfully, skipping");
            return;
        }

        if ("SUCCESS".equals(event.getStatus())) {
            payment.setRazorpayPaymentId(event.getRazorpayPaymentId());
            payment.setStatus(PaymentStatus.SUCCESS);
            payment.setPaymentDate(LocalDateTime.now());
            paymentRepository.save(payment);

            // Confirm booking and generate tickets
            Booking booking = payment.getBooking();
            if (booking.getStatus() == BookingStatus.PENDING) {
                bookingService.confirmBooking(booking.getId());
                log.info("Booking {} confirmed via webhook", booking.getBookingReference());
            }
        } else if ("FAILED".equals(event.getStatus())) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment marked as failed for order: {}", event.getRazorpayOrderId());
        }
    }
}
