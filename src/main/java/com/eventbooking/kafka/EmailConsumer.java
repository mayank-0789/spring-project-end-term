package com.eventbooking.kafka;

import com.eventbooking.config.KafkaConfig;
import com.eventbooking.event.EmailEvent;
import com.eventbooking.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailConsumer {

    private final EmailService emailService;

    @KafkaListener(topics = KafkaConfig.EMAIL_TOPIC, groupId = "email-group")
    public void consumeEmailEvent(EmailEvent event) {
        log.info("Consumed email event: {} for {}", event.getType(), event.getToEmail());

        try {
            switch (event.getType()) {
                case WELCOME -> emailService.sendWelcomeEmailDirect(
                        event.getToEmail(),
                        event.getUserName());
                case BOOKING_CONFIRMATION -> emailService.sendBookingConfirmationDirect(
                        event.getToEmail(),
                        event.getBookingReference(),
                        event.getEventTitle(),
                        event.getEventDate(),
                        event.getVenueName(),
                        event.getQuantity(),
                        event.getTotalAmount(),
                        event.getTicketNumbers());
                case BOOKING_CANCELLATION -> emailService.sendBookingCancellationDirect(
                        event.getToEmail(),
                        event.getBookingReference(),
                        event.getEventTitle(),
                        event.getTotalAmount());
            }
        } catch (Exception e) {
            log.error("Failed to process email event: {}", e.getMessage());
        }
    }
}
