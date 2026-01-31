package com.eventbooking.controller;

import com.eventbooking.config.RazorpayConfig;
import com.eventbooking.dto.request.PaymentWebhookEvent;
import com.eventbooking.service.PaymentService;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final RazorpayConfig razorpayConfig;

    @PostMapping("/razorpay")
    public ResponseEntity<Map<String, String>> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Received Razorpay webhook");

        try {
            // Verify signature if webhook secret is configured
            String webhookSecret = razorpayConfig.getWebhookSecret();
            if (webhookSecret != null && !webhookSecret.isEmpty() && signature != null) {
                boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
                if (!isValid) {
                    log.warn("Invalid Razorpay webhook signature");
                    Map<String, String> response = new HashMap<>();
                    response.put("error", "Invalid signature");
                    return ResponseEntity.badRequest().body(response);
                }
            }

            JSONObject webhookPayload = new JSONObject(payload);
            String event = webhookPayload.optString("event", "");

            log.info("Razorpay event: {}", event);

            if ("payment.captured".equals(event) || "order.paid".equals(event)) {
                JSONObject paymentEntity = webhookPayload
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String razorpayOrderId = paymentEntity.getString("order_id");
                String razorpayPaymentId = paymentEntity.getString("id");

                PaymentWebhookEvent webhookEvent = PaymentWebhookEvent.builder()
                        .razorpayOrderId(razorpayOrderId)
                        .razorpayPaymentId(razorpayPaymentId)
                        .status("SUCCESS")
                        .build();

                paymentService.handleWebhook(webhookEvent);
                log.info("Payment webhook processed successfully for order: {}", razorpayOrderId);

            } else if ("payment.failed".equals(event)) {
                JSONObject paymentEntity = webhookPayload
                        .getJSONObject("payload")
                        .getJSONObject("payment")
                        .getJSONObject("entity");

                String razorpayOrderId = paymentEntity.getString("order_id");
                String razorpayPaymentId = paymentEntity.optString("id", "");

                PaymentWebhookEvent webhookEvent = PaymentWebhookEvent.builder()
                        .razorpayOrderId(razorpayOrderId)
                        .razorpayPaymentId(razorpayPaymentId)
                        .status("FAILED")
                        .build();

                paymentService.handleWebhook(webhookEvent);
                log.info("Payment failure processed for order: {}", razorpayOrderId);
            }

            Map<String, String> response = new HashMap<>();
            response.put("status", "ok");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing Razorpay webhook: {}", e.getMessage());
            Map<String, String> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
