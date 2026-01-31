package com.eventbooking.service;

import com.eventbooking.model.Booking;
import com.eventbooking.model.Ticket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${postmark.server-token:}")
    private String postmarkToken;

    @Value("${email.from:noreply@eventbooking.com}")
    private String fromEmail;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Async
    public void sendBookingConfirmation(Booking booking, List<Ticket> tickets) {
        if (postmarkToken.isEmpty()) {
            log.warn("Postmark token not configured, skipping email");
            return;
        }

        String toEmail = booking.getUser().getEmail();
        String subject = "Booking Confirmed - " + booking.getEvent().getTitle();
        String content = buildBookingEmail(booking, tickets);

        sendEmail(toEmail, subject, content);
    }

    @Async
    public void sendBookingCancellation(Booking booking) {
        if (postmarkToken.isEmpty()) {
            log.warn("Postmark token not configured, skipping email");
            return;
        }

        String toEmail = booking.getUser().getEmail();
        String subject = "Booking Cancelled - " + booking.getEvent().getTitle();
        String content = String.format(
                "<h2>Booking Cancelled</h2><p>Your booking %s for %s has been cancelled.</p><p>Amount: ₹%s</p>",
                booking.getBookingReference(),
                booking.getEvent().getTitle(),
                booking.getTotalAmount());

        sendEmail(toEmail, subject, content);
    }

    @Async
    public void sendWelcomeEmail(String email, String name) {
        if (postmarkToken.isEmpty()) {
            log.warn("Postmark token not configured, skipping welcome email");
            return;
        }

        String subject = "Welcome to Event Booking Platform!";
        String content = String.format("""
                <h2>Welcome, %s!</h2>
                <p>Thank you for registering with our Event Booking Platform.</p>
                <p>You can now:</p>
                <ul>
                    <li>Browse upcoming events</li>
                    <li>Book tickets for your favorite events</li>
                    <li>Manage your bookings</li>
                </ul>
                <p>Start exploring events now!</p>
                <p>Best regards,<br>Event Booking Team</p>
                """, name);

        sendEmail(email, subject, content);
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        try {
            String json = String.format("""
                    {
                        "From": "%s",
                        "To": "%s",
                        "Subject": "%s",
                        "HtmlBody": "%s",
                        "MessageStream": "outbound"
                    }
                    """, fromEmail, to, subject,
                    htmlBody.replace("\"", "\\\"").replace("\n", ""));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.postmarkapp.com/email"))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("X-Postmark-Server-Token", postmarkToken)
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Email sent to {}", to);
            } else {
                log.error("Failed to send email: {}", response.body());
            }
        } catch (Exception e) {
            log.error("Error sending email to {}: {}", to, e.getMessage());
        }
    }

    private String buildBookingEmail(Booking booking, List<Ticket> tickets) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h2>Booking Confirmed!</h2>");
        sb.append("<p><strong>Booking Reference:</strong> ").append(booking.getBookingReference()).append("</p>");
        sb.append("<p><strong>Event:</strong> ").append(booking.getEvent().getTitle()).append("</p>");
        sb.append("<p><strong>Date:</strong> ").append(booking.getEvent().getStartDate()).append("</p>");
        sb.append("<p><strong>Venue:</strong> ").append(booking.getEvent().getVenue().getName()).append("</p>");
        sb.append("<p><strong>Tickets:</strong> ").append(booking.getQuantity()).append("</p>");
        sb.append("<p><strong>Total:</strong> ₹").append(booking.getTotalAmount()).append("</p>");
        sb.append("<hr>");
        sb.append("<h3>Your Tickets:</h3>");
        for (Ticket ticket : tickets) {
            sb.append("<p>Ticket #").append(ticket.getTicketNumber()).append("</p>");
        }
        return sb.toString();
    }

    // Direct methods for Kafka consumers (no @Async - Kafka handles async)
    public void sendWelcomeEmailDirect(String email, String name) {
        if (postmarkToken.isEmpty()) {
            log.warn("Postmark token not configured, skipping welcome email");
            return;
        }

        String subject = "Welcome to Event Booking Platform!";
        String content = String.format("""
                <h2>Welcome, %s!</h2>
                <p>Thank you for registering with our Event Booking Platform.</p>
                <p>You can now:</p>
                <ul>
                    <li>Browse upcoming events</li>
                    <li>Book tickets for your favorite events</li>
                    <li>Manage your bookings</li>
                </ul>
                <p>Start exploring events now!</p>
                <p>Best regards,<br>Event Booking Team</p>
                """, name);

        sendEmail(email, subject, content);
    }

    public void sendBookingConfirmationDirect(String toEmail, String bookingRef, String eventTitle,
            String eventDate, String venueName, Integer quantity,
            String totalAmount, String ticketNumbers) {
        if (postmarkToken.isEmpty()) {
            log.warn("Postmark token not configured, skipping email");
            return;
        }

        String subject = "Booking Confirmed - " + eventTitle;
        String content = String.format("""
                <h2>Booking Confirmed!</h2>
                <p><strong>Booking Reference:</strong> %s</p>
                <p><strong>Event:</strong> %s</p>
                <p><strong>Date:</strong> %s</p>
                <p><strong>Venue:</strong> %s</p>
                <p><strong>Tickets:</strong> %d</p>
                <p><strong>Total:</strong> ₹%s</p>
                <hr>
                <h3>Your Tickets:</h3>
                <p>%s</p>
                """, bookingRef, eventTitle, eventDate, venueName, quantity, totalAmount, ticketNumbers);

        sendEmail(toEmail, subject, content);
    }

    public void sendBookingCancellationDirect(String toEmail, String bookingRef,
            String eventTitle, String totalAmount) {
        if (postmarkToken.isEmpty()) {
            log.warn("Postmark token not configured, skipping email");
            return;
        }

        String subject = "Booking Cancelled - " + eventTitle;
        String content = String.format(
                "<h2>Booking Cancelled</h2><p>Your booking %s for %s has been cancelled.</p><p>Amount: ₹%s</p>",
                bookingRef, eventTitle, totalAmount);

        sendEmail(toEmail, subject, content);
    }
}
