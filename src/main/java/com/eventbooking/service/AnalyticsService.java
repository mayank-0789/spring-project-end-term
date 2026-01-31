package com.eventbooking.service;

import com.eventbooking.dto.response.AnalyticsResponse;
import com.eventbooking.exception.ResourceNotFoundException;
import com.eventbooking.model.Payment;
import com.eventbooking.model.User;
import com.eventbooking.repository.EventRepository;
import com.eventbooking.repository.PaymentRepository;
import com.eventbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

        private final EventRepository eventRepository;
        private final PaymentRepository paymentRepository;
        private final UserRepository userRepository;

        public AnalyticsResponse getOrganizerAnalytics(String organizerEmail) {
                User organizer = userRepository.findByEmail(organizerEmail)
                                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

                Long totalEvents = eventRepository.countByOrganizerId(organizer.getId());

                List<Object[]> revenueData = eventRepository.getRevenueByEvent(organizer.getId());
                List<AnalyticsResponse.EventRevenue> revenueByEvent = revenueData.stream()
                                .map(row -> AnalyticsResponse.EventRevenue.builder()
                                                .eventTitle((String) row[0])
                                                .eventDate((LocalDateTime) row[1])
                                                .totalBookings(((Number) row[2]).longValue())
                                                .totalRevenue(row[3] instanceof BigDecimal ? (BigDecimal) row[3]
                                                                : BigDecimal.valueOf(((Number) row[3]).doubleValue()))
                                                .build())
                                .collect(Collectors.toList());

                Long totalBookings = revenueByEvent.stream()
                                .mapToLong(AnalyticsResponse.EventRevenue::getTotalBookings)
                                .sum();

                BigDecimal totalRevenue = revenueByEvent.stream()
                                .map(AnalyticsResponse.EventRevenue::getTotalRevenue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                LocalDateTime startDate = LocalDateTime.now().minusDays(30);
                LocalDateTime endDate = LocalDateTime.now();
                List<Payment> payments = paymentRepository.findSuccessfulPaymentsByDateRange(startDate, endDate);

                Map<LocalDate, List<Payment>> paymentsByDate = payments.stream()
                                .collect(Collectors.groupingBy(p -> p.getPaymentDate().toLocalDate()));

                List<AnalyticsResponse.DailyStats> dailyBookings = new ArrayList<>();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                for (int i = 29; i >= 0; i--) {
                        LocalDate date = LocalDate.now().minusDays(i);
                        List<Payment> dayPayments = paymentsByDate.getOrDefault(date, List.of());

                        dailyBookings.add(AnalyticsResponse.DailyStats.builder()
                                        .date(date.format(formatter))
                                        .bookingCount((long) dayPayments.size())
                                        .revenue(dayPayments.stream()
                                                        .map(Payment::getAmount)
                                                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                                        .build());
                }

                return AnalyticsResponse.builder()
                                .totalEvents(totalEvents)
                                .totalBookings(totalBookings)
                                .totalRevenue(totalRevenue)
                                .revenueByEvent(revenueByEvent)
                                .dailyBookings(dailyBookings)
                                .build();
        }
}
