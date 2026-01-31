package com.eventbooking.controller;

import com.eventbooking.dto.response.TicketResponse;
import com.eventbooking.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/{ticketNumber}")
    public ResponseEntity<TicketResponse> getByNumber(
            @PathVariable String ticketNumber,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ticketService.getTicketByNumber(ticketNumber, user.getUsername()));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<TicketResponse>> getByBooking(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(ticketService.getTicketsByBookingId(bookingId, user.getUsername()));
    }

    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    @PostMapping("/{ticketNumber}/validate")
    public ResponseEntity<TicketResponse> validate(@PathVariable String ticketNumber) {
        return ResponseEntity.ok(ticketService.validateTicket(ticketNumber));
    }
}
