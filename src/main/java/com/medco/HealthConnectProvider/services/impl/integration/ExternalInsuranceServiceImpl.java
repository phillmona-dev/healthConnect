package com.medco.HealthConnectProvider.services.impl.integration;

import com.medco.HealthConnectProvider.config.ExternalApiConfig;
import com.medco.HealthConnectProvider.dto.integration.ExternalDispensingRequest;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;
import com.medco.HealthConnectProvider.services.integration.ExternalInsuranceService;
import com.medco.HealthConnectProvider.services.integration.FailedExternalDispensingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalInsuranceServiceImpl implements ExternalInsuranceService {

    private final RestTemplate restTemplate;
    private final ExternalApiConfig externalApiConfig;
    private final FailedExternalDispensingService failedDispensingService;

    // External insurance system endpoint for dispensing
    private static final String DISPENSING_ENDPOINT = "/api/payer/claimconnect/service-provided";

    @Override
    public void sendDispensingToExternalSystem(List<MedicationDispensingItem> dispensingItems,
                                             String packageUuid,
                                             String serviceId,
                                             String dispensingUuid,
                                             String contractHeaderUuid) {

        log.info("Sending {} dispensing items to external insurance system", dispensingItems.size());

        try {
            // Build the JSON payload
            ExternalDispensingRequest payload = buildExternalPayload(dispensingItems, packageUuid, serviceId, dispensingUuid);

            // Send to external system
            sendPayloadToExternalSystem(payload, dispensingItems, packageUuid, serviceId, dispensingUuid, contractHeaderUuid);

        } catch (Exception e) {
            log.error("Error sending dispensing data to external system", e);

            // Log failure for all items
            String fullUrl = externalApiConfig.getExternalApiBaseUrl() + DISPENSING_ENDPOINT + "/" + contractHeaderUuid;
            for (MedicationDispensingItem item : dispensingItems) {
                failedDispensingService.logFailedDispensing(
                    item, packageUuid, serviceId, dispensingUuid, contractHeaderUuid,
                    fullUrl, e.getMessage(), null);
            }
        }
    }

    /**
     * Send the JSON payload to external system
     */
    private void sendPayloadToExternalSystem(ExternalDispensingRequest payload,
                                           List<MedicationDispensingItem> dispensingItems,
                                           String packageUuid,
                                           String serviceId,
                                           String dispensingUuid,
                                           String contractHeaderUuid) {

        String url = externalApiConfig.getExternalApiBaseUrl() + DISPENSING_ENDPOINT + "/" + contractHeaderUuid;

        log.info("Sending POST request to external insurance system: {}", url);
        log.debug("Payload: {}", payload);

        // Prepare headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", externalApiConfig.getApiKey());

        HttpEntity<ExternalDispensingRequest> request = new HttpEntity<>(payload, headers);

        // Make the API call
        ResponseEntity<String> response = restTemplate.exchange(
            url,
            HttpMethod.POST,
            request,
            String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("Successfully sent dispensing data to external insurance system. Response: {}",
                    response.getBody());
        } else {
            String errorMessage = String.format("HTTP %s: %s", response.getStatusCode(), response.getBody());
            log.error("Failed to send dispensing data to external insurance system. Status: {}, Response: {}",
                     response.getStatusCode(), response.getBody());

            // Log the failure for retry for all items
            for (MedicationDispensingItem item : dispensingItems) {
                failedDispensingService.logFailedDispensing(
                    item, packageUuid, serviceId, dispensingUuid, contractHeaderUuid, url, errorMessage, response.getBody());
            }

            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Build external dispensing request payload
     */
    private ExternalDispensingRequest buildExternalPayload(List<MedicationDispensingItem> dispensingItems,
                                                         String packageUuid,
                                                         String serviceId,
                                                         String dispensingUuid) {

        if (dispensingItems.isEmpty()) {
            throw new IllegalArgumentException("Dispensing items list cannot be empty");
        }

        // Get the first item to extract common information
        MedicationDispensingItem firstItem = dispensingItems.get(0);

        // Calculate total price
        Double totalPrice = dispensingItems.stream()
                .mapToDouble(item -> item.getTotalPrice() != null ? item.getTotalPrice() : 0.0)
                .sum();

        // Determine insured and dependent UUIDs
        String insuredUuid = getInsuredUuid(firstItem);
        String dependentUuid = getDependentUuid(firstItem);

        // Build items list
        List<ExternalDispensingRequest.ExternalDispensingItem> externalItems = dispensingItems.stream()
                .map(item -> buildExternalItem(item, packageUuid, dispensingUuid))
                .collect(java.util.stream.Collectors.toList());

        return ExternalDispensingRequest.builder()
                .totalPrice(totalPrice)
                .providedDate(firstItem.getDispensing().getDispensingDate() != null ?
                             firstItem.getDispensing().getDispensingDate().toString() : "")
                .serviceProvidedUuid(dispensingUuid)
                .insuredUuid(insuredUuid)
                .dependentUuid(dependentUuid != null ? dependentUuid : "")
                .items(externalItems)
                .build();
    }

    /**
     * Get insured UUID from dispensing item
     */
    private String getInsuredUuid(MedicationDispensingItem item) {
        if (item.getDispensing().getInsured() != null) {
            return item.getDispensing().getInsured().getInsuredUuid();
        } else if (item.getDispensing().getDependant() != null) {
            return item.getDispensing().getDependant().getDependantUuid();
        } else if (item.getDispensing().getInsuredUuid() != null) {
            return item.getDispensing().getInsuredUuid();
        }

        log.warn("No insured UUID found for dispensing item: {}", item.getId());
        return null;
    }

    /**
     * Get dependent UUID from dispensing item
     */
    private String getDependentUuid(MedicationDispensingItem item) {
        if (item.getDispensing().getDependant() != null) {
            return item.getDispensing().getDependant().getDependantUuid();
        }
        return null;
    }

    /**
     * Build external dispensing item from medication dispensing item
     */
    private ExternalDispensingRequest.ExternalDispensingItem buildExternalItem(MedicationDispensingItem item,
                                                                              String packageUuid,
                                                                              String dispensingUuid) {

        return ExternalDispensingRequest.ExternalDispensingItem.builder()
                .serviceId(item.getContractDetail() != null && item.getContractDetail().getServicelist() != null ?
                          item.getContractDetail().getServicelist().getGeneratedServiceId() : "")
                .serviceName(item.getMedicationName() != null ? item.getMedicationName() : "")
                .serviceCode(item.getMedicationCode() != null ? item.getMedicationCode() : "")
                .qty(item.getQuantity() != null ? item.getQuantity().intValue() : 1)
                .totalPrice(item.getTotalPrice() != null ? item.getTotalPrice() : 0.0)
                .recordNumber(dispensingUuid)
                .packageUuid(packageUuid)
                .build();
    }
}
