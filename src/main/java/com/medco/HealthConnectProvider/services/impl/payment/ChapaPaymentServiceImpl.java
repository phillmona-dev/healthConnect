package com.medco.HealthConnectProvider.services.impl.payment;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.payment.PaymentTransaction;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.payment.PaymentTransactionRepository;
import com.medco.HealthConnectProvider.services.payment.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class ChapaPaymentServiceImpl implements PaymentService {

    @Value("${chapa.api.key}")
    private String chapaApiKey;

    @Value("${chapa.api.url}")
    private String chapaApiUrl;

    private final ClaimRepository claimRepository;
    private final RestTemplate restTemplate;
    private final PaymentTransactionRepository paymentTransactionRepository;

    public ChapaPaymentServiceImpl(ClaimRepository claimRepository, RestTemplate restTemplate, PaymentTransactionRepository paymentTransactionRepository) {
        this.claimRepository = claimRepository;
        this.restTemplate = restTemplate;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Override
    public PaymentTransaction initiatePayment(String claimUuid, BigDecimal amount, String currency) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        // Create a new PaymentTransaction
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionUuid(UUID.randomUUID().toString());
        transaction.setClaim(claim);
        transaction.setAmount(amount);
        transaction.setCurrency(currency);
        transaction.setStatus("PENDING");

        // Prepare the request to Chapa
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + chapaApiKey);

        // Create the request body (adjust according to Chapa's API requirements)
        String requestBody = String.format(
                "{\"amount\":\"%s\",\"currency\":\"%s\",\"tx_ref\":\"%s\",\"callback_url\":\"https://yourwebsite.com/callback\"}",
                amount, currency, transaction.getTransactionUuid()
        );

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

        // Make the API call to Chapa
        ResponseEntity<String> response = restTemplate.postForEntity(chapaApiUrl + "/transaction/initialize", request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            // Parse the response and update the transaction (you'll need to adjust this based on Chapa's response format)
            String chapaTransactionId = ""; // Extract from response
            String checkoutUrl = ""; // Extract from response

            transaction.setChapaTransactionId(chapaTransactionId);
            transaction.setCheckoutUrl(checkoutUrl);

            // Save the transaction
            return paymentTransactionRepository.save(transaction);
        } else {
            // Handle error
            throw new RuntimeException("Failed to initiate payment with Chapa");
        }
    }

    @Override
    public boolean verifyPayment(String transactionId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + chapaApiKey);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                chapaApiUrl + "/transaction/verify/" + transactionId,
                HttpMethod.GET,
                request,
                Map.class
        );

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            String status = (String) data.get("status");
            return "success".equalsIgnoreCase(status);
        }

        return false;
    }

    @Override
    public PaymentTransaction getPaymentStatus(String transactionId) {
        PaymentTransaction transaction = paymentTransactionRepository.findByChapaTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (verifyPayment(transactionId)) {
            transaction.setStatus("COMPLETED");
        } else {
            transaction.setStatus("FAILED");
        }

        return paymentTransactionRepository.save(transaction);
    }
}
