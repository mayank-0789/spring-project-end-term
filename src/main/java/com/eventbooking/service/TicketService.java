package com.eventbooking.service;

import com.eventbooking.dto.response.TicketResponse;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.exception.UnauthorizedException;
import com.eventbooking.exception.ValidationException;
import com.eventbooking.model.Ticket;
import com.eventbooking.model.enums.TicketStatus;
import com.eventbooking.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketResponse getTicketByNumber(String ticketNumber, String userEmail) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (!ticket.getBooking().getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You are not authorized to view this ticket");
        }

        return mapToResponse(ticket);
    }

    public List<TicketResponse> getTicketsByBookingId(Long bookingId, String userEmail) {
        List<Ticket> tickets = ticketRepository.findByBookingId(bookingId);

        if (!tickets.isEmpty() && !tickets.get(0).getBooking().getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You are not authorized to view these tickets");
        }

        return tickets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public TicketResponse validateTicket(String ticketNumber) {
        Ticket ticket = ticketRepository.findByTicketNumber(ticketNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        if (ticket.getStatus() == TicketStatus.USED) {
            throw new ValidationException("Ticket has already been used");
        }

        if (ticket.getStatus() == TicketStatus.CANCELLED) {
            throw new ValidationException("Ticket has been cancelled");
        }

        // Mark ticket as used
        ticket.setStatus(TicketStatus.USED);
        ticket = ticketRepository.save(ticket);

        return mapToResponse(ticket);
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .qrCode(ticket.getQrCode())
                .status(ticket.getStatus())
                .eventTitle(ticket.getBooking().getEvent().getTitle())
                .ticketTypeName(ticket.getTicketType().getName())
                .eventStartDate(ticket.getBooking().getEvent().getStartDate())
                .venueName(ticket.getBooking().getEvent().getVenue().getName())
                .createdAt(ticket.getCreatedAt())
                .build();
    }
}
