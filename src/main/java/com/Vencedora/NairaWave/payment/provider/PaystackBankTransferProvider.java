package com.Vencedora.NairaWave.payment.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.Vencedora.NairaWave.exception.BadRequestException;
import com.Vencedora.NairaWave.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "bank.provider", havingValue = "paystack")
public class PaystackBankTransferProvider implements BankTransferProvider {

    private static final Logger log = LoggerFactory.getLogger(PaystackBankTransferProvider.class);

    @Value("${paystack.secret-key}")
    private String secretKey;

    @Value("${paystack.base-url:https://api.paystack.co}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpHeaders headers() {
        HttpHeaders h = new HttpHeaders();
        h.setBearerAuth(secretKey);
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    @Override
    public NameEnquiryResult resolveAccount(String bankCode, String accountNumber) {
        if (accountNumber == null || accountNumber.length() != 10) {
            throw new BadRequestException("Account number must be 10 digits");
        }

        String url = baseUrl + "/bank/resolve?account_number=" + accountNumber + "&bank_code=" + bankCode;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            if (!root.path("status").asBoolean()) {
                String msg = root.path("message").asText("Account resolution failed");
                throw new ResourceNotFoundException(msg);
            }

            JsonNode data = root.path("data");
            String accountName = data.path("account_name").asText();
            String bankName = data.path("bank_name").asText("");

            return new NameEnquiryResult(accountName, bankCode, accountNumber, bankName);

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            log.error("Paystack resolve failed [{}]: {}", e.getStatusCode(), responseBody);
            try {
                JsonNode errNode = objectMapper.readTree(responseBody);
                String msg = errNode.path("message").asText("Could not resolve account name for this bank");
                throw new BadRequestException(msg);
            } catch (Exception parseEx) {
                throw new BadRequestException("Could not resolve account name. Please check account number and bank.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Paystack resolve unexpected error: {}", e.getMessage());
            throw new BadRequestException("Account resolution error: " + e.getMessage());
        }
    }

    @Override
    public String createRecipient(String bankCode, String accountNumber, String accountName) {
        String url = baseUrl + "/transferrecipient";

        Map<String, Object> body = Map.of(
                "type", "nuban",
                "name", accountName,
                "account_number", accountNumber,
                "bank_code", bankCode,
                "currency", "NGN"
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers()), String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            if (!root.path("status").asBoolean()) {
                throw new BadRequestException(root.path("message").asText("Failed to create recipient"));
            }

            return root.path("data").path("recipient_code").asText();

        } catch (RestClientException e) {
            log.error("Paystack create recipient failed: {}", e.getMessage());
            throw new BadRequestException("Could not create transfer recipient");
        } catch (Exception e) {
            throw new BadRequestException("Recipient creation error: " + e.getMessage());
        }
    }

    @Override
    public ProviderTransferResult initiateTransfer(BankTransferCommand command) {
        String url = baseUrl + "/transfer";

        long amountInKobo = command.amount().multiply(new BigDecimal("100")).longValue();

        Map<String, Object> body = Map.of(
                "source", "balance",
                "reason", command.narration() != null ? command.narration() : "Transfer",
                "amount", amountInKobo,
                "recipient", command.recipientCode(),
                "reference", command.clientReference()
        );

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body, headers()), String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            if (!root.path("status").asBoolean()) {
                String msg = root.path("message").asText("Transfer initiation failed");
                return new ProviderTransferResult(null, "FAILED", msg);
            }

            JsonNode data = root.path("data");
            String transferCode = data.path("transfer_code").asText();
            String status = data.path("status").asText("pending").toUpperCase();

            return new ProviderTransferResult(transferCode, status, "Transfer initiated");

        } catch (RestClientException e) {
            log.error("Paystack transfer failed: {}", e.getMessage());
            return new ProviderTransferResult(null, "FAILED", "Provider unreachable");
        } catch (Exception e) {
            return new ProviderTransferResult(null, "FAILED", e.getMessage());
        }
    }

    @Override
    public ProviderTransferResult queryTransfer(String providerReference) {
        String url = baseUrl + "/transfer/" + providerReference;

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers()), String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());

            if (!root.path("status").asBoolean()) {
                return new ProviderTransferResult(providerReference, "FAILED",
                        root.path("message").asText("Query failed"));
            }

            String status = root.path("data").path("status").asText("pending").toUpperCase();
            return new ProviderTransferResult(providerReference, status, "OK");

        } catch (Exception e) {
            log.error("Paystack query failed: {}", e.getMessage());
            return new ProviderTransferResult(providerReference, "PENDING", "Query error");
        }
    }
}