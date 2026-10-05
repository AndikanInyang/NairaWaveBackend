package com.Vencedora.NairaWave.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.Vencedora.NairaWave.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private static final Logger log = LoggerFactory.getLogger(CatalogController.class);

    @Value("${paystack.secret-key:}")
    private String paystackSecretKey;

    @Value("${paystack.base-url:https://api.paystack.co}")
    private String paystackBaseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // In-memory cache to only call Paystack once
    private List<Map<String, String>> cachedBanks = null;
    private long cacheExpiry = 0;

    @GetMapping("/banks")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> banks() {
        long now = System.currentTimeMillis();

        // Returns cached banks if valid (cache for 1 hour)
        if (cachedBanks != null && now < cacheExpiry) {
            return ResponseEntity.ok(ApiResponse.success("OK", cachedBanks));
        }

        // Fetches official live bank list from Paystack
        if (paystackSecretKey != null && !paystackSecretKey.isBlank()) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setBearerAuth(paystackSecretKey);
                HttpEntity<Void> entity = new HttpEntity<>(headers);

                String url = paystackBaseUrl + "/bank?country=nigeria&perPage=100";
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                JsonNode root = objectMapper.readTree(response.getBody());
                if (root.path("status").asBoolean()) {
                    List<Map<String, String>> bankList = new ArrayList<>();
                    for (JsonNode b : root.path("data")) {
                        bankList.add(Map.of(
                                "name", b.path("name").asText(),
                                "code", b.path("code").asText()
                        ));
                    }

                    // Sort alphabetically
                    bankList.sort(Comparator.comparing(m -> m.get("name")));

                    cachedBanks = bankList;
                    cacheExpiry = now + (60 * 60 * 1000L); // 1 hour
                    return ResponseEntity.ok(ApiResponse.success("Banks retrieved from Paystack", cachedBanks));
                }
            } catch (Exception e) {
                log.warn("Failed to fetch banks from Paystack, falling back to static list: {}", e.getMessage());
            }
        }

        // Fallback list
        List<Map<String, String>> fallback = List.of(
                Map.of("name", "Access Bank", "code", "044"),
                Map.of("name", "Citibank Nigeria", "code", "023"),
                Map.of("name", "Ecobank Nigeria", "code", "050"),
                Map.of("name", "Fidelity Bank", "code", "070"),
                Map.of("name", "First Bank of Nigeria", "code", "011"),
                Map.of("name", "First City Monument Bank (FCMB)", "code", "214"),
                Map.of("name", "Guaranty Trust Bank (GTBank)", "code", "058"),
                Map.of("name", "Heritage Bank", "code", "030"),
                Map.of("name", "Keystone Bank", "code", "082"),
                Map.of("name", "Kuda Bank", "code", "50211"),
                Map.of("name", "Moniepoint MFB", "code", "50515"),
                Map.of("name", "OPay Digital Services (Paycom)", "code", "999992"),
                Map.of("name", "PalmPay", "code", "999991"),
                Map.of("name", "Polaris Bank", "code", "076"),
                Map.of("name", "Providus Bank", "code", "101"),
                Map.of("name", "Stanbic IBTC Bank", "code", "221"),
                Map.of("name", "Standard Chartered Bank", "code", "068"),
                Map.of("name", "Sterling Bank", "code", "232"),
                Map.of("name", "Suntrust Bank", "code", "100"),
                Map.of("name", "TAJ Bank", "code", "302"),
                Map.of("name", "Union Bank of Nigeria", "code", "032"),
                Map.of("name", "United Bank for Africa (UBA)", "code", "033"),
                Map.of("name", "Unity Bank", "code", "215"),
                Map.of("name", "Wema Bank", "code", "035"),
                Map.of("name", "Zenith Bank", "code", "057")
        );

        return ResponseEntity.ok(ApiResponse.success("OK", fallback));
    }
}