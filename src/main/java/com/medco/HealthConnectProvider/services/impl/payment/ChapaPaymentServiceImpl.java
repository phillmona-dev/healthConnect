package com.medco.HealthConnectProvider.services.impl.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.payment.PaymentTransaction;
import com.medco.HealthConnectProvider.exception.BankApiException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.payment.PaymentTransactionRepository;
import com.medco.HealthConnectProvider.services.payment.PaymentService;
import com.medco.HealthConnectProvider.ui.request.Payment.WebhookPayload;
import com.medco.HealthConnectProvider.ui.response.Payment.BankApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ChapaPaymentServiceImpl implements PaymentService {

    @Value("${chapa.api.key}")
    private String chapaApiKey;

    @Value("${chapa.api.url}")
    private String chapaApiUrl;



//    @Value("${payment.return-url}")
//    private String returnUrl;

//    @Value("${payment.api_key}")
//    private String api_key;

    @Value("${payment.callback_checkout_url}")
    private String callback_checkout_url;

    @Value("${payment.merchant_id}")
    private String merchant_id;

//


    @Value("${payment.chapa_transfer_request_url}")
    private String chapa_transfer_request_url;

    @Value("${payment.chapa_verify_url}")
    private String chapa_verify_url;

    @Value("${payment.chapa_verify_transfer_url}")
    private String chapa_verify_transfer_url;

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





//    TODO NEW METHODS ABB

    @Override
    public void processWebhookResponse(String payload) {
        WebhookPayload webhookPayload=new WebhookPayload();
        try {
            // Deserialize the payload into a WebhookPayload object
            ObjectMapper objectMapper = new ObjectMapper();
            webhookPayload = objectMapper.readValue(payload, WebhookPayload.class);


            System.out.println("Event: " + webhookPayload.getEvent());
            System.out.println("Transaction Reference: " + webhookPayload.getTx_ref());
            System.out.println("Amount: " + webhookPayload.getAmount());
            System.out.println("Currency: " + webhookPayload.getCurrency());

            // TODO: Add your event processing logic here

//            return "Webhook processed successfully";
        } catch (Exception e) {
            // Handle JSON parsing errors
            System.err.println("Error parsing payload: " + e.getMessage());
//            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid payload");
        }
        if (webhookPayload!=null ){
            String eventType =webhookPayload.getEvent() ;
            switch (eventType) {
                case "charge.success":
                    handleSuccessfulPayment(webhookPayload);
                    break;
                case "charge.failed":
                    handleSuccessfulPayment(webhookPayload);
                    break;
                case "charge.refunded":
                    handleSuccessfulPayment(webhookPayload);
                    break;
                case "charge.reversed":
                    handleSuccessfulPayment(webhookPayload);
                    break;
                case "payout.success":
                    handleSuccessfulPayment(webhookPayload);
                    break;
                case "payout.failed":
                    handleFailedPayout(webhookPayload);
                    break;
                default:
            }}
        System.out.println("the response to the payment through the webhook is :"+ payload);


    }

    private void handleFailedPayout(WebhookPayload webhookPayload) {
//        Payment payment =paymentRepository.findByPaymentUuid((String) payload.getTx_ref()).orElseThrow(()->new RuntimeException("Payment not found"));
//        payment.setPaymentStatus( PaymentStatus.valueOf(payload.getStatus()));
//        payment.setReference( (String) payload.getReference());
//        paymentRepository.save(payment);
//        System.out.println("❌ Payout Failed: " + payload);
//        // TODO: Notify admin and take necessary actions
//        OpenfnUpdateStatus(payload);
    }

    private void handleSuccessfulPayment(WebhookPayload webhookPayload) {
//        Payment payment =paymentRepository.findByTxRef(payload.getTx_ref()).orElseThrow(()->new RuntimeException("Payment not found"));
//        System.out.println("✅ Payment Successful: " + payload);
//        payment.setPaymentStatus( PaymentStatus.valueOf(payload.getStatus()));
//        payment.setReference(  payload.getReference());
//        paymentRepository.save(payment);
//        MonthlyPayments monthlyPayments=payment.getMonthlyPayment();
////        monthlyPayments.setPayed(true);
//        if (Objects.equals(payload.getStatus(), "success"))
//            monthlyPayments.setMonthlyPaymentStatus(MonthlyPaymentStatus.PAID);
//        else monthlyPayments.setMonthlyPaymentStatus(MonthlyPaymentStatus.FAILED);
//
//        monthlyPaymentRepository.save(monthlyPayments);


        // TODO: Update order status in the database
    }

    @Override
    public ResponseEntity<?> getBanks() {
        try {
            BankApiResponse response = WebClient.create()
                    .get()
                    .uri("https://api.chapa.co/v1/banks")
                    .header("Authorization", "Bearer " + chapaApiKey)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse -> {
                        return clientResponse.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new BankApiException(
                                        "API Error: " + clientResponse.statusCode() + " - " + errorBody
                                )));
                    })
                    .bodyToMono(BankApiResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            // Validate response structure
            if (response == null) {
                throw new BankApiException("Empty response from bank API");
            }
            if (response.getData() == null || response.getData().isEmpty()) {
                throw new BankApiException("No bank data available");
            }

            return ResponseEntity.ok(response);

        } catch (WebClientResponseException e) {
            // Handle 4xx/5xx responses
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of(
                            "error", "Bank API request failed",
                            "message", e.getMessage(),
                            "status", e.getStatusCode().value()
                    ));
        } catch (BankApiException e) {
            // Handle our custom validation errors
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Invalid bank data",
                            "message", e.getMessage()
                    ));
        } catch (Exception e) {
            // Catch-all for other errors (network, etc.)
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error", "Failed to fetch banks",
                            "message", e.getMessage()
                    ));
        }
    }
}
