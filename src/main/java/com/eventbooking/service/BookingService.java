package com.eventbooking.service;

import com.eventbooking.dto.request.BookingRequest;
import com.eventbooking.dto.response.BookingResponse;
import com.eventbooking.dto.response.TicketResponse;
import com.eventbooking.exception.*;
import com.eventbooking.model.*;
import com.eventbooking.model.enums.BookingStatus;
import com.eventbooking.model.enums.EventStatus;
import com.eventbooking.model.enums.TicketStatus;
import com.eventbooking.repository.*;
import com.eventbooking.util.QRCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final QRCodeGenerator qrCodeGenerator;
    private final EmailService emailService;

    private static final int BOOKING_EXPIRY_MINUTES = 10;

    @Transactional
    public BookingResponse createBooking(BookingRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", request.getEventId()));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new ValidationException("Event is not available for booking");
        }

        if (event.getStartDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Event has already started");
        }

        // Lock ticket type for concurrent access
        TicketType ticketType = ticketTypeRepository.findByIdWithLock(request.getTicketTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Ticket type", request.getTicketTypeId()));

        if (!ticketType.getEvent().getId().equals(event.getId())) {
            throw new ValidationException("Ticket type does not belong to this event");
        }

        if (ticketType.getAvailableQuantity() < request.getQuantity()) {
            throw new InsufficientTicketsException(ticketType.getAvailableQuantity(), request.getQuantity());
        }

        // Reserve tickets
        ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() - request.getQuantity());
        ticketTypeRepository.save(ticketType);

        // Calculate total amount
        BigDecimal totalAmount = ticketType.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // Create booking
        Booking booking = Booking.builder()
                .event(event)
                .ticketType(ticketType)
                .user(user)
                .quantity(request.getQuantity())
                .totalAmount(totalAmount)
                .bookingReference(generateBookingReference())
                .status(BookingStatus.PENDING)
                .expiresAt(LocalDateTime.now().plusMinutes(BOOKING_EXPIRY_MINUTES))
                .build();

        booking = bookingRepository.save(booking);
        return mapToResponse(booking);
    }

    public BookingResponse getBookingByReference(String bookingReference, String userEmail) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You are not authorized to view this booking");
        }

        return mapToResponse(booking);
    }

    public Page<BookingResponse> getUserBookings(String userEmail, Pageable pageable) {
        return bookingRepository.findByUserEmail(userEmail, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public BookingResponse confirmBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ValidationException("Booking is not in pending state");
        }

        if (booking.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BookingExpiredException(booking.getBookingReference());
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setExpiresAt(null);

        // Generate tickets
        List<Ticket> tickets = generateTickets(booking);
        booking.setTickets(tickets);

        booking = bookingRepository.save(booking);

        // Extract values before transaction ends (to avoid lazy loading in async)
        String toEmail = booking.getUser().getEmail();
        String bookingRef = booking.getBookingReference();
        String eventTitle = booking.getEvent().getTitle();
        String eventDate = booking.getEvent().getStartDate().toString();
        String venueName = booking.getEvent().getVenue().getName();
        Integer quantity = booking.getQuantity();
        String totalAmount = booking.getTotalAmount().toString();
        String ticketNumbers = tickets.stream()
                .map(Ticket::getTicketNumber)
                .collect(Collectors.joining(", "));

        // Send confirmation email using direct method (avoids entity access in async)
        emailService.sendBookingConfirmationDirect(toEmail, bookingRef, eventTitle,
                eventDate, venueName, quantity, totalAmount, ticketNumbers);

        return mapToResponse(booking);
    }

    @Transactional
    public BookingResponse cancelBooking(String bookingReference, String userEmail) {
        Booking booking = bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found"));

        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("You are not authorized to cancel this booking");
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ValidationException("Booking is already cancelled");
        }

        // Restore ticket availability
        TicketType ticketType = booking.getTicketType();
        ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() + booking.getQuantity());
        ticketTypeRepository.save(ticketType);

        booking.setStatus(BookingStatus.CANCELLED);

        // Cancel associated tickets
        booking.getTickets().forEach(ticket -> ticket.setStatus(TicketStatus.CANCELLED));

        booking = bookingRepository.save(booking);

        // Send cancellation email
        emailService.sendBookingCancellation(booking);

        return mapToResponse(booking);
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void expireOldBookings() {
        List<Booking> expiredBookings = bookingRepository
                .findByStatusAndExpiresAtBefore(BookingStatus.PENDING, LocalDateTime.now());

        for (Booking booking : expiredBookings) {
            // Restore ticket availability
            TicketType ticketType = booking.getTicketType();
            ticketType.setAvailableQuantity(ticketType.getAvailableQuantity() + booking.getQuantity());
            ticketTypeRepository.save(ticketType);

            booking.setStatus(BookingStatus.EXPIRED);
            bookingRepository.save(booking);
            log.info("Expired booking: {}", booking.getBookingReference());
        }
    }

    private List<Ticket> generateTickets(Booking booking) {
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < booking.getQuantity(); i++) {
            String ticketNumber = generateTicketNumber();
            String qrContent = String.format("TICKET:%s|EVENT:%d|BOOKING:%s",
                    ticketNumber, booking.getEvent().getId(), booking.getBookingReference());
            String qrCode = qrCodeGenerator.generateQRCodeBase64(qrContent);

            Ticket ticket = Ticket.builder()
                    .booking(booking)
                    .ticketType(booking.getTicketType())
                    .ticketNumber(ticketNumber)
                    .qrCode(qrCode)
                    .status(TicketStatus.ACTIVE)
                    .build();

            tickets.add(ticketRepository.save(ticket));
        }
        return tickets;
    }

    private String generateBookingReference() {
        return "BK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateTicketNumber() {
        return "TKT" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }

    private BookingResponse mapToResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .event(mapEventSummary(booking.getEvent()))
                .ticketType(mapTicketTypeSummary(booking.getTicketType()))
                .quantity(booking.getQuantity())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .expiresAt(booking.getExpiresAt())
                .payment(booking.getPayment() != null ? mapPaymentSummary(booking.getPayment()) : null)
                .tickets(booking.getTickets().stream()
                        .map(this::mapTicketToResponse)
                        .collect(Collectors.toList()))
                .createdAt(booking.getCreatedAt())
                .build();
    }

    private BookingResponse.EventSummary mapEventSummary(Event event) {
        return BookingResponse.EventSummary.builder()
                .id(event.getId())
                .title(event.getTitle())
                .startDate(event.getStartDate())
                .venueName(event.getVenue().getName())
                .build();
    }

    private BookingResponse.TicketTypeSummary mapTicketTypeSummary(TicketType ticketType) {
        return BookingResponse.TicketTypeSummary.builder()
                .id(ticketType.getId())
                .name(ticketType.getName())
                .price(ticketType.getPrice())
                .build();
    }

    private BookingResponse.PaymentSummary mapPaymentSummary(Payment payment) {
        return BookingResponse.PaymentSummary.builder()
                .id(payment.getId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .build();
    }

    private TicketResponse mapTicketToResponse(Ticket ticket) {
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
