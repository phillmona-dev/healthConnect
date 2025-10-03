package com.medco.HealthConnectProvider.services.impl.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medco.HealthConnectProvider.dto.integration.ExternalClaimRejectionResponse;
import com.medco.HealthConnectProvider.entity.integration.DispensingRejection;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import com.medco.HealthConnectProvider.repository.integration.DispensingRejectionRepository;
import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingRepository;
import com.medco.HealthConnectProvider.services.integration.DispensingRejectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class DispensingRejectionServiceImpl implements DispensingRejectionService {

    private final DispensingRejectionRepository rejectionRepository;
    private final MedicationDispensingRepository medicationDispensingRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void processExternalRejectionResponse(ExternalClaimRejectionResponse rejectionResponse) {
        log.info("Processing external rejection response for batch: {}", rejectionResponse.getBatchCode());

        try {
            // Process rejected dispensing records
            if (rejectionResponse.getRejectedDispensing() != null && !rejectionResponse.getRejectedDispensing().isEmpty()) {
                log.info("Processing {} rejected dispensing records", rejectionResponse.getRejectedDispensing().size());
                for (ExternalClaimRejectionResponse.DispensingRejectionDetail rejection : rejectionResponse.getRejectedDispensing()) {
                    processIndividualRejection(rejectionResponse, rejection);
                }
            } else {
                log.warn("No rejected dispensing records found in response for batch: {}", rejectionResponse.getBatchCode());
            }

            log.info("Successfully processed rejection response for batch: {}", rejectionResponse.getBatchCode());

        } catch (Exception e) {
            log.error("Error processing external rejection response for batch: {}", rejectionResponse.getBatchCode(), e);
            throw new RuntimeException("Failed to process rejection response", e);
        }
    }

    private void processIndividualRejection(ExternalClaimRejectionResponse response,
                                          ExternalClaimRejectionResponse.DispensingRejectionDetail rejection) {
        try {
            // Fetch the dispensing record to get missing information
            MedicationDispensing dispensing = medicationDispensingRepository
                    .findByDispensingUuid(rejection.getDispensingUuid());

            if (dispensing == null) {
                log.error("Dispensing record not found for UUID: {}", rejection.getDispensingUuid());
                return;
            }

            // Get contract UUID from the first dispensing item (all items should have same contract)
            String contractUuid = response.getContractUuid();
            if (contractUuid == null && !dispensing.getItems().isEmpty()) {
                contractUuid = dispensing.getItems().get(0).getContractDetail().getContractHeaderUuid();
                log.debug("Retrieved contract UUID from dispensing item: {}", contractUuid);
            }

            if (contractUuid == null) {
                log.error("Contract UUID is null for dispensing: {}", rejection.getDispensingUuid());
                throw new IllegalStateException("Contract UUID is required but not found");
            }

            // Get provider UUID and claim UUID from dispensing if not in response
            String providerUuid = response.getProviderUuid() != null ?
                    response.getProviderUuid() : dispensing.getProviderUuid();
            String claimUuid = response.getClaimUuid() != null ?
                    response.getClaimUuid() : dispensing.getClaimUuid();

            // Create rejection record
            DispensingRejection rejectionEntity = DispensingRejection.builder()
                    .dispensingUuid(rejection.getDispensingUuid())
                    .claimUuid(claimUuid)
                    .batchCode(response.getBatchCode())
                    .contractUuid(contractUuid)
                    .providerUuid(providerUuid)
                    .rejectionCode(rejection.getRejectionCode())
                    .rejectionReason(rejection.getRejectionReason())
                    .rejectionCategory(rejection.getRejectionCategory())
                    .rejectedAmount(rejection.getRejectedAmount())
                    .rejectedAt(rejection.getRejectedAt() != null ? rejection.getRejectedAt() : LocalDateTime.now())
                    .reviewerComments(rejection.getReviewerComments())
                    .canResubmit(rejection.getCanResubmit() != null ? rejection.getCanResubmit() : false)
                    .requiredDocuments(serializeRequiredDocuments(rejection.getRequiredDocuments()))
                    .rejectionStatus("ACTIVE")
                    .externalResponsePayload(serializeToJson(response))
                    .build();

            rejectionRepository.save(rejectionEntity);

            // Update dispensing status
            updateDispensingStatusForRejection(rejection.getDispensingUuid(), "REJECTED");

            log.info("Processed rejection for dispensing: {} with reason: {}",
                    rejection.getDispensingUuid(), rejection.getRejectionReason());

        } catch (Exception e) {
            log.error("Error processing individual rejection for dispensing: {}", rejection.getDispensingUuid(), e);
            throw new RuntimeException("Failed to process rejection for dispensing: " + rejection.getDispensingUuid(), e);
        }
    }



    @Override
    public List<DispensingRejection> getRejectionsByDispensingUuid(String dispensingUuid) {
        return rejectionRepository.findByDispensingUuid(dispensingUuid);
    }

    @Override
    public List<DispensingRejection> getRejectionsByClaimUuid(String claimUuid) {
        return rejectionRepository.findByClaimUuid(claimUuid);
    }

    @Override
    public List<DispensingRejection> getRejectionsByBatchCode(String batchCode) {
        return rejectionRepository.findByBatchCode(batchCode);
    }

    @Override
    public List<DispensingRejection> getRejectionsByProvider(String providerUuid) {
        return rejectionRepository.findByProviderUuid(providerUuid);
    }

    @Override
    public List<DispensingRejection> getRejectionsByStatus(String status) {
        return rejectionRepository.findByRejectionStatus(status);
    }

    @Override
    public List<DispensingRejection> getRejectionsByCategory(String category) {
        return rejectionRepository.findByRejectionCategory(category);
    }

    @Override
    public List<DispensingRejection> getResubmittableRejections() {
        return rejectionRepository.findResubmittableRejections();
    }

    @Override
    public List<DispensingRejection> getRejectionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return rejectionRepository.findByDateRange(startDate, endDate);
    }

    @Override
    public boolean isDispensingRejected(String dispensingUuid) {
        return rejectionRepository.isDispensingRejected(dispensingUuid);
    }

    @Override
    @Transactional
    public void markRejectionAsResolved(String rejectionUuid, String resolvedBy, String resolutionComments) {
        Optional<DispensingRejection> rejectionOpt = rejectionRepository.findByRejectionUuid(rejectionUuid);
        if (rejectionOpt.isPresent()) {
            DispensingRejection rejection = rejectionOpt.get();
            rejection.setRejectionStatus("RESOLVED");
            rejection.setResolvedAt(LocalDateTime.now());
            rejection.setResolvedBy(resolvedBy);
            rejection.setResolutionComments(resolutionComments);
            rejectionRepository.save(rejection);

            log.info("Marked rejection {} as resolved by {}", rejectionUuid, resolvedBy);
        }
    }

    @Override
    @Transactional
    public void markRejectionAsResubmitted(String rejectionUuid, String resubmittedBy, String resubmissionComments) {
        Optional<DispensingRejection> rejectionOpt = rejectionRepository.findByRejectionUuid(rejectionUuid);
        if (rejectionOpt.isPresent()) {
            DispensingRejection rejection = rejectionOpt.get();
            rejection.setRejectionStatus("RESUBMITTED");
            rejection.setResubmittedAt(LocalDateTime.now());
            rejection.setResubmittedBy(resubmittedBy);
            rejection.setResubmissionComments(resubmissionComments);
            rejectionRepository.save(rejection);

            // Update dispensing status
            updateDispensingStatusForRejection(rejection.getDispensingUuid(), "RESUBMITTED");

            log.info("Marked rejection {} as resubmitted by {}", rejectionUuid, resubmittedBy);
        }
    }

    @Override
    @Transactional
    public void appealRejection(String rejectionUuid, String appealedBy, String appealComments) {
        Optional<DispensingRejection> rejectionOpt = rejectionRepository.findByRejectionUuid(rejectionUuid);
        if (rejectionOpt.isPresent()) {
            DispensingRejection rejection = rejectionOpt.get();
            rejection.setRejectionStatus("APPEALED");
            rejection.setResolutionComments(appealComments);
            rejection.setResolvedBy(appealedBy);
            rejection.setResolvedAt(LocalDateTime.now());
            rejectionRepository.save(rejection);

            log.info("Appealed rejection {} by {}", rejectionUuid, appealedBy);
        }
    }

    @Override
    public RejectionStatistics getRejectionStatistics() {
        long totalRejections = rejectionRepository.count();
        long activeRejections = rejectionRepository.countByRejectionStatus("ACTIVE");
        long resolvedRejections = rejectionRepository.countByRejectionStatus("RESOLVED");
        long resubmittedRejections = rejectionRepository.countByRejectionStatus("RESUBMITTED");
        long appealedRejections = rejectionRepository.countByRejectionStatus("APPEALED");

        Map<String, Long> rejectionsByCategory = new HashMap<>();
        rejectionsByCategory.put("ELIGIBILITY", rejectionRepository.countByRejectionCategory("ELIGIBILITY"));
        rejectionsByCategory.put("COVERAGE", rejectionRepository.countByRejectionCategory("COVERAGE"));
        rejectionsByCategory.put("DOCUMENTATION", rejectionRepository.countByRejectionCategory("DOCUMENTATION"));
        rejectionsByCategory.put("DUPLICATE", rejectionRepository.countByRejectionCategory("DUPLICATE"));
        rejectionsByCategory.put("FRAUD", rejectionRepository.countByRejectionCategory("FRAUD"));

        return new RejectionStatistics(totalRejections, activeRejections, resolvedRejections, 
                                     resubmittedRejections, appealedRejections, rejectionsByCategory);
    }

    @Override
    public Map<String, Long> getRejectionStatsByProvider(String providerUuid, LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = rejectionRepository.getRejectionStatsByProvider(providerUuid, startDate, endDate);
        Map<String, Long> stats = new HashMap<>();
        
        for (Object[] result : results) {
            String category = (String) result[0];
            Long count = (Long) result[1];
            stats.put(category, count);
        }
        
        return stats;
    }

    @Override
    @Transactional
    public void updateDispensingStatusForRejection(String dispensingUuid, String rejectionStatus) {
        MedicationDispensing dispensing = medicationDispensingRepository
                .findByDispensingUuid(dispensingUuid);

        if (dispensing != null) {
            dispensing.setClaimStatus(rejectionStatus);
            dispensing.setUpdatedAt(LocalDateTime.now());
            medicationDispensingRepository.save(dispensing);

            log.info("Updated dispensing {} status to {}", dispensingUuid, rejectionStatus);
        } else {
            log.warn("Dispensing not found for status update: {}", dispensingUuid);
        }
    }

    @Override
    @Transactional
    public void processBatchRejections(String batchCode, List<ExternalClaimRejectionResponse.DispensingRejectionDetail> rejections) {
        log.info("Processing batch rejections for batch: {} with {} rejections", batchCode, rejections.size());

        for (ExternalClaimRejectionResponse.DispensingRejectionDetail rejection : rejections) {
            ExternalClaimRejectionResponse mockResponse = ExternalClaimRejectionResponse.builder()
                    .batchCode(batchCode)
                    .build();
            processIndividualRejection(mockResponse, rejection);
        }
    }

    @Override
    public DispensingRejection getRejectionByUuid(String rejectionUuid) {
        return rejectionRepository.findByRejectionUuid(rejectionUuid).orElse(null);
    }

    private String serializeRequiredDocuments(List<String> documents) {
        try {
            return documents != null ? objectMapper.writeValueAsString(documents) : null;
        } catch (Exception e) {
            log.error("Error serializing required documents", e);
            return null;
        }
    }

    private String serializeToJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("Error serializing object to JSON", e);
            return null;
        }
    }
}
