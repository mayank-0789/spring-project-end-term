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

    @Value("${sendgrid.api-key:}")
    private String sendgridApiKey;

    @Value("${email.from:noreply@eventbooking.com}")
    private String fromEmail;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Async
    public void sendBookingConfirmation(Booking booking, List<Ticket> tickets) {
        if (sendgridApiKey.isEmpty()) {
            log.warn("SendGrid API key not configured, skipping email");
            return;
        }

        String toEmail = booking.getUser().getEmail();
        String subject = "Booking Confirmed - " + booking.getEvent().getTitle();
        String content = buildBookingEmail(booking, tickets);

        sendEmail(toEmail, subject, content);
    }

    @Async
    public void sendBookingCancellation(Booking booking) {
        if (sendgridApiKey.isEmpty()) {
            log.warn("SendGrid API key not configured, skipping email");
            return;
        }

        String toEmail = booking.getUser().getEmail();
        String subject = "Booking Cancelled - " + booking.getEvent().getTitle();
        String content = String.format(
                "Your booking %s for %s has been cancelled. Amount: ₹%s",
                booking.getBookingReference(),
                booking.getEvent().getTitle(),
                booking.getTotalAmount());

        sendEmail(toEmail, subject, content);
    }

    private void sendEmail(String to, String subject, String content) {
        try {
            String json = String.format("""
                    {
                        "personalizations": [{"to": [{"email": "%s"}]}],
                        "from": {"email": "%s"},
                        "subject": "%s",
                        "content": [{"type": "text/html", "value": "%s"}]
                    }
                    """, to, fromEmail, subject, content.replace("\"", "\\\"").replace("\n", "<br>"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.sendgrid.com/v3/mail/send"))
                    .header("Authorization", "Bearer " + sendgridApiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
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
}
