package com.eventbooking.kafka;

import com.eventbooking.config.KafkaConfig;
import com.eventbooking.event.EmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendEmailEvent(EmailEvent event) {
        log.info("Sending email event: {} to {}", event.getType(), event.getToEmail());
        kafkaTemplate.send(KafkaConfig.EMAIL_TOPIC, event.getToEmail(), event);
    }
}
