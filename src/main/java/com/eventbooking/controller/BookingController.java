package com.eventbooking.controller;

import com.eventbooking.dto.request.BookingRequest;
import com.eventbooking.dto.response.BookingResponse;
import com.eventbooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> create(
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.createBooking(request, user.getUsername()));
    }

    @GetMapping("/{bookingReference}")
    public ResponseEntity<BookingResponse> getByReference(
            @PathVariable String bookingReference,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bookingService.getBookingByReference(bookingReference, user.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<BookingResponse>> getMyBookings(
            @AuthenticationPrincipal UserDetails user, Pageable pageable) {
        return ResponseEntity.ok(bookingService.getUserBookings(user.getUsername(), pageable));
    }

    @DeleteMapping("/{bookingReference}")
    public ResponseEntity<BookingResponse> cancel(
            @PathVariable String bookingReference,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(bookingService.cancelBooking(bookingReference, user.getUsername()));
    }
}
