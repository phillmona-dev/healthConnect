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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExternalInsuranceServiceImpl implements ExternalInsuranceService {

    private final RestTemplate restTemplate;
    private final ExternalApiConfig externalApiConfig;
    private final FailedExternalDispensingService failedDispensingService;
    private static final String DISPENSING_ENDPOINT = "/api/payer/claimconnect/service-provided/synchronizeServiceProvided";

    @Override
    public void sendDispensingToExternalSystem(List<MedicationDispensingItem> dispensingItems,
                                             String packageUuid,
                                             String serviceId,
                                             String dispensingUuid,
                                             String contractHeaderUuid,
                                             org.springframework.web.multipart.MultipartFile attachment) {

        log.info("Sending {} dispensing items to external insurance system", dispensingItems.size());

        try {
            ExternalDispensingRequest payload = buildExternalPayload(dispensingItems, packageUuid, serviceId, dispensingUuid);

            if (attachment != null && !attachment.isEmpty()) {
                sendMultipartToExternalSystem(payload, attachment, dispensingItems, packageUuid, serviceId, dispensingUuid, contractHeaderUuid);
            } else {
                sendPayloadToExternalSystem(payload, dispensingItems, packageUuid, serviceId, dispensingUuid, contractHeaderUuid);
            }

        } catch (Exception e) {
            log.error("Error sending dispensing data to external system", e);

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

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", externalApiConfig.getApiKey());

        HttpEntity<ExternalDispensingRequest> request = new HttpEntity<>(payload, headers);

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

            for (MedicationDispensingItem item : dispensingItems) {
                failedDispensingService.logFailedDispensing(
                    item, packageUuid, serviceId, dispensingUuid, contractHeaderUuid, url, errorMessage, response.getBody());
            }

            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Send multipart/form-data with JSON payload and file attachment to external system
     */
    private void sendMultipartToExternalSystem(ExternalDispensingRequest payload,
                                               org.springframework.web.multipart.MultipartFile attachment,
                                               List<MedicationDispensingItem> dispensingItems,
                                               String packageUuid,
                                               String serviceId,
                                               String dispensingUuid,
                                               String contractHeaderUuid) throws Exception {

        String url = externalApiConfig.getExternalApiBaseUrl() + DISPENSING_ENDPOINT + "/" + contractHeaderUuid;
        log.info("Sending MULTIPART POST request to external insurance system: {} with attachment: {} ({} bytes)",
                url, attachment.getOriginalFilename(), attachment.getSize());

        ObjectMapper mapper = new ObjectMapper();
        String payloadJson = mapper.writeValueAsString(payload);

        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> jsonPart = new HttpEntity<>(payloadJson, jsonHeaders);

        HttpHeaders fileHeaders = new HttpHeaders();
        fileHeaders.setContentType(attachment.getContentType() != null ? MediaType.parseMediaType(attachment.getContentType()) : MediaType.APPLICATION_OCTET_STREAM);
        fileHeaders.setContentDispositionFormData("attachment", attachment.getOriginalFilename());

        ByteArrayResource fileResource = new ByteArrayResource(attachment.getBytes()) {
            @Override
            public String getFilename() {
                return attachment.getOriginalFilename();
            }
        };
        HttpEntity<ByteArrayResource> filePart = new HttpEntity<>(fileResource, fileHeaders);

        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        multipartBody.add("payload", jsonPart);
        multipartBody.add("attachment", filePart);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("X-API-Key", externalApiConfig.getApiKey());

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(multipartBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            log.info("Successfully sent multipart dispensing data to external insurance system. Response: {}",
                    response.getBody());
        } else {
            String errorMessage = String.format("HTTP %s: %s", response.getStatusCode(), response.getBody());
            log.error("Failed to send multipart dispensing data to external insurance system. Status: {}, Response: {}",
                    response.getStatusCode(), response.getBody());

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

        MedicationDispensingItem firstItem = dispensingItems.get(0);

        Double totalPrice = dispensingItems.stream()
                .mapToDouble(item -> item.getTotalPrice() != null ? item.getTotalPrice() : 0.0)
                .sum();

        String insuredUuid = getInsuredUuid(firstItem);
        String dependentUuid = getDependentUuid(firstItem);

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
