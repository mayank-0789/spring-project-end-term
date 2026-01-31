package com.eventbooking.controller;

import com.eventbooking.dto.response.AnalyticsResponse;
import com.eventbooking.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<AnalyticsResponse> getDashboard(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok(analyticsService.getOrganizerAnalytics(user.getUsername()));
    }
}
