package com.Vencedora.NairaWave.controller;

import com.Vencedora.NairaWave.service.BankTransferService;
import com.Vencedora.NairaWave.service.WebhookVerificationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final BankTransferService bankTransferService;
    private final WebhookVerificationService webhookVerificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostMapping("/paystack")
    public ResponseEntity<String> paystackWebhook(
            @RequestHeader(value = "x-paystack-signature", required = false) String signature,
            @RequestBody String rawBody
    ) {
        if (!webhookVerificationService.verifyPaystackSignature(rawBody, signature)) {
            log.warn("Invalid Paystack webhook signature");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        }

        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String event = root.path("event").asText();
            JsonNode data = root.path("data");

            if ("transfer.success".equals(event)) {
                String reference = data.path("reference").asText();
                String transferCode = data.path("transfer_code").asText();
                bankTransferService.handleWebhookSuccess(reference, transferCode);
                log.info("Webhook: transfer success {}", reference);
            } else if ("transfer.failed".equals(event) || "transfer.reversed".equals(event)) {
                String reference = data.path("reference").asText();
                String transferCode = data.path("transfer_code").asText();
                bankTransferService.handleWebhookFailure(reference, transferCode);
                log.info("Webhook: transfer failed {}", reference);
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage());
            return ResponseEntity.ok("OK");
        }
    }
}