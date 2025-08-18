package com.medco.HealthConnectProvider.services.impl.integration;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.ui.request.drug.DrugDispensingRecordEditRequest;
import com.medco.HealthConnectProvider.ui.request.integration.DispensingRecordEditRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingDetailResponse;
import com.medco.HealthConnectProvider.utils.enums.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.dto.MedicationDispensingDTO;
import com.medco.HealthConnectProvider.dto.PendingDispensingRecordDTO;
import com.medco.HealthConnectProvider.entity.claims.BatchRecord;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.claims.ClaimItem;
import com.medco.HealthConnectProvider.entity.claims.ClaimLogs;
import com.medco.HealthConnectProvider.entity.drug.Drug;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.BatchRecordRepository;
import com.medco.HealthConnectProvider.repository.claims.ClaimItemRepository;
import com.medco.HealthConnectProvider.repository.claims.ClaimLogsRepository;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.drug.DrugRepository;
import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingItemRepository;
import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.services.eligibility.EligibilityService;
import com.medco.HealthConnectProvider.services.integration.PharmacyIntegrationService;
import com.medco.HealthConnectProvider.services.persons.InsuredService;
import com.medco.HealthConnectProvider.ui.BatchCodeInfo;
import com.medco.HealthConnectProvider.ui.request.drug.DrugDispensingRecordRequest;
import com.medco.HealthConnectProvider.ui.request.integration.DispensingRecordRequest;
import com.medco.HealthConnectProvider.ui.request.integration.KenemaPharmacyDispensingRequest;
import com.medco.HealthConnectProvider.ui.request.integration.MedicationDispensingRequest;
import com.medco.HealthConnectProvider.ui.response.ApiErrorResponse;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.claims.ReconciliationResponse;
import com.medco.HealthConnectProvider.ui.response.eligibility.EligibilityResponse;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingRecordResponse;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredSearchResponse;
import com.medco.HealthConnectProvider.dto.integration.KenemaIntegrationConfig;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class PharmacyIntegrationServiceImpl implements PharmacyIntegrationService {

    @Value("${file.upload-dir-payer-logos}")
    private String payerLogosDirectory;

    @Value("${file.upload-dir-provider-logos}")
    private String providerLogosDirectory;

    private static final Logger logger = LoggerFactory.getLogger(PharmacyIntegrationServiceImpl.class);

    private final SecurityUtils securityUtils;

    @Autowired
    public PharmacyIntegrationServiceImpl(SecurityUtils securityUtils) {
        this.securityUtils = securityUtils;
    }

    @Autowired
    private MedicationDispensingRepository dispensingRepository;

    @Autowired
    private MedicationDispensingItemRepository dispensingItemRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private PayerRepository payerRepository;

    @Autowired
    private InsuredRepository insuredRepository;

    @Autowired
    private ClaimRepository claimRepository;

    @Autowired
    private ClaimItemRepository claimItemRepository;

    @Autowired
    private EligibilityService eligibilityService;

    @Autowired
    private ClaimLogsRepository claimLogsRepository;

    @Autowired
    private InsuredService insuredService;

    @Autowired
    private ServicelistRepository servicelistRepository;

    @Autowired
    private BatchRecordRepository batchRecordRepository;

    @Autowired
    private DrugRepository drugRepository;

    @Autowired
    private DependantRepository dependantRepository;

    @Autowired
    private ContractDetailRepository contractDetailRepository;
    @Autowired
    private ContractRepository contractHeaderRepository;

    @PersistenceContext
    private EntityManager entityManager;

    // New dependencies for package usage and external insurance
    @Autowired
    private com.medco.HealthConnectProvider.services.packageCategory.PackageCategoryUsageService packageUsageService;

    @Autowired
    private com.medco.HealthConnectProvider.services.integration.ExternalInsuranceService externalInsuranceService;


    private Payer validatePayer(String payerUuid) {
        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }
        return payer;
    }

    private DispensingResponse createDispensingResponse(MedicationDispensing savedDispensing) {
        DispensingResponse response = new DispensingResponse();
        response.setDispensingUuid(savedDispensing.getDispensingUuid());
        response.setStatus("SUCCESS");
        response.setMessage("Medication dispensing recorded successfully");
        response.setRecordedAt(savedDispensing.getRecordedAt());
        response.setTotalAmount(savedDispensing.getTotalAmount());
        response.setPatientResponsibility(savedDispensing.getPatientResponsibility());
        response.setInsuranceCoverage(savedDispensing.getInsuranceCoverage());
        return response;
    }

    @Override
    public ResponseEntity<PagedResponse<PendingDispensingRecordDTO>> getDispensingRecords(
            String providerUuid, String search, String status, LocalDate startDate, LocalDate endDate,
            String payerUuid, int page, int size, String sortBy, String sortDirection) {

        log.info("Fetching dispensing records with advanced search for provider: {}", providerUuid);

        try {
            Provider provider = providerRepository.findByProviderUuid(providerUuid);
            if (provider == null) {
                throw new ResourceNotFoundException("Provider", "providerUuid", providerUuid);
            }

            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

            Specification<MedicationDispensing> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                predicates.add(cb.equal(root.get("providerUuid"), providerUuid));
                //TODO CHANGING THE CLAIM STATUS TO STATUS
                if (status != null && !status.isEmpty()) {
                    predicates.add(cb.equal(root.get("status"), status));
                }

                if (startDate != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("dispensingDate"), startDate));
                }

                if (endDate != null) {
                    predicates.add(cb.lessThan(root.get("dispensingDate"), endDate.plusDays(1)));
                }

                if (search != null && !search.isEmpty()) {
                    String searchLower = "%" + search.toLowerCase() + "%";
                    Join<MedicationDispensing, Insured> insuredJoin = root.join("insured", JoinType.LEFT);
                    Join<MedicationDispensing, MedicationDispensingItem> itemsJoin = root.join("items", JoinType.LEFT);

                    predicates.add(cb.or(
                            cb.like(cb.lower(insuredJoin.get("firstName")), searchLower),
                            cb.like(cb.lower(insuredJoin.get("lastName")), searchLower),
                            cb.like(cb.lower(insuredJoin.get("phone")), searchLower),
                            cb.like(cb.lower(itemsJoin.get("medicationName")), searchLower)
                    ));
                }

                if (payerUuid != null && !payerUuid.isEmpty()) {
                    predicates.add(cb.equal(root.get("payerUuid"), payerUuid));
                }

                return cb.and(predicates.toArray(new Predicate[0]));

            };

            Page<MedicationDispensing> dispensingRecords = dispensingRepository.findAll(spec, pageable);

            List<PendingDispensingRecordDTO> dtoList = dispensingRecords.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            PagedResponse<PendingDispensingRecordDTO> pagedResponse = new PagedResponse<>(
                    dtoList,
                    dispensingRecords.getNumber() + 1,
                    dispensingRecords.getSize(),
                    dispensingRecords.getTotalElements(),
                    dispensingRecords.getTotalPages(),
                    dispensingRecords.isLast()
            );

            return ResponseEntity.ok(pagedResponse);
        } catch (ResourceNotFoundException e) {
            log.error("Provider not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching dispensing records", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    public ResponseEntity<?> removeDispensingFromBatch(String dispensingUuid) {

        MedicationDispensing dispensing = dispensingRepository.findByDispensingUuid(dispensingUuid);
        if (dispensing == null) {
            throw new ResourceNotFoundException("Dispensing", "uuid", dispensingUuid);
        }
        if (dispensing.getBatchRecord() == null) {
            throw new BadRequestException("This dispensing record is not associated with any batch.");
        }

        BatchRecord batchRecord = dispensing.getBatchRecord();

        batchRecord.getMedicationDispensing().remove(dispensing);
        dispensing.setBatchRecord(null);
        dispensing.setBatchCode(null);

        dispensing.setClaimStatus("DRAFT");
        dispensing.setStatus(MedicationStatus.DRAFT);

        dispensingRepository.save(dispensing);
        batchRecordRepository.save(batchRecord);

        if (batchRecord.getMedicationDispensing().isEmpty()) {
            batchRecord.setStatus(String.valueOf(Status.INACTIVE));
            batchRecordRepository.save(batchRecord);
        }

        return ResponseEntity.ok(new MessageResponse("Dispensing record removed from batch and status changed to DRAFT"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> addDrugDispensingRecord(DrugDispensingRecordRequest request) {

        try {

            Provider provider = validateProvider(request.getProviderUuid());
            Payer payer = validatePayer(request.getPayerUuid());
            ImmutablePair<Insured, Dependant> insuredDependantPair = findInsuredPersonFor(request);
            Insured insured = insuredDependantPair.getLeft();
            Dependant dependant = insuredDependantPair.getRight();

            log.info("Insured person found: {}", insured.getFirstName());

            if (dependant != null) {
                log.info("Dependant found: {}", dependant.getFirstName());
            }

            ContractHeader activeContract = contractHeaderRepository.findActiveContractByPayerUuid(payer.getPayerUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Active contract", "payer", payer.getPayerUuid()));

            MedicationDispensing dispensingRecord = createDrugDispensingRecord(request, insured, dependant, payer, provider);

            List<Drug> drugs = validateDrugs(provider.getProviderUuid(), request.getDrugItems());
            List<MedicationDispensingItem> dispensingItems = createDrugDispensingItems(dispensingRecord, drugs, request.getDrugItems(), payer, activeContract);

            MedicationDispensing savedRecord = dispensingRepository.save(dispensingRecord);

            dispensingItemRepository.saveAll(dispensingItems);

            updateDispensingRecordTotals(savedRecord, dispensingItems);

            savedRecord = dispensingRepository.save(savedRecord);

            return ResponseEntity.ok(new DispensingRecordResponse(savedRecord));

        } catch (ResourceNotFoundException e) {
            log.error("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiErrorResponse(e.getMessage()));
        } catch (BadRequestException e) {
            log.error("Bad request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.error("Data integrity violation: {}", e.getMessage());
            String errorMessage = "A conflict occurred while saving the record. ";
            if (e.getCause() instanceof ConstraintViolationException cve) {
                if (cve.getConstraintViolations().stream()
                        .anyMatch(v -> v.getPropertyPath().toString().equals("insurance_coverage"))) {
                    errorMessage += "Insurance coverage cannot be null.";
                } else {
                    errorMessage += "Please check for duplicate entries or missing required fields.";
                }
            }

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiErrorResponse(errorMessage));
        } catch (Exception e) {
            log.error("Unexpected error in addDrugDispensingRecord: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse("An unexpected error occurred. Please try again later."));
        }
    }

    @Override
    public ResponseEntity<PagedResponse<MedicationDispensingDTO>> getMedicationsByBatchCode(String batchCode, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<MedicationDispensing> dispensingsPage = dispensingRepository.findByBatchCode(batchCode, pageable);

        if (dispensingsPage.isEmpty()) {
            throw new ResourceNotFoundException("Medications", "batchCode", batchCode);
        }

        List<MedicationDispensingDTO> medicationDTOs = dispensingsPage.getContent().stream()
                .map(this::convertToMedicationDispensingDTO)
                .collect(Collectors.toList());

        PagedResponse<MedicationDispensingDTO> pagedResponse = new PagedResponse<>(
                medicationDTOs,
                dispensingsPage.getNumber(),
                dispensingsPage.getSize(),
                dispensingsPage.getTotalElements(),
                dispensingsPage.getTotalPages(),
                dispensingsPage.isLast()
        );

        return ResponseEntity.ok(pagedResponse);

    }

    @Override
    public ResponseEntity<DispensingDetailResponse> getDispensingDetail(String dispensingUuid) {
        MedicationDispensing dispensing = dispensingRepository.findByDispensingUuid(dispensingUuid);
        if (dispensing == null) {
            throw new ResourceNotFoundException("Dispensing", "uuid", dispensingUuid);
        }

        DispensingDetailResponse response = new DispensingDetailResponse();
        BeanUtils.copyProperties(dispensing, response);

        List<MedicationDispensingItem> items = dispensingItemRepository.findByDispensing(dispensing);

        items.forEach(item -> {
            if (item.getContractDetail() != null) {
                item.getContractDetail().getContractDetailUuid();

                if (item.getContractDetail().getContractHeader() != null) {
                    item.getContractDetail().getContractHeader().getContractHeaderUuid();
                }
            }
        });

        response.setItems(items.stream().map(this::convertToItemDetail).collect(Collectors.toList()));

        return ResponseEntity.ok(response);

    }

    @Transactional
    @Override
    public ResponseEntity<?> editDispensingRecord(String dispensingUuid, DispensingRecordEditRequest editRequest) {
        log.info("[DISPENSING-EDIT] Starting edit for dispensing record: {}", dispensingUuid);
        try {
            // 1. Authentication and basic validation
            UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
            log.debug("[DISPENSING-EDIT] Authenticated user: {}", userDetails.getUsername());

            // 2. Find the dispensing record
            MedicationDispensing dispensing = dispensingRepository.findByDispensingUuid(dispensingUuid);
            if (dispensing == null) {
                log.error("[DISPENSING-EDIT] Dispensing record not found with UUID: {}", dispensingUuid);
                throw new ResourceNotFoundException("Dispensing Record", "uuid", dispensingUuid);
            }

            // 3. Set claim association from request
            if (editRequest.getClaimUuid() != null) {
                log.info("[DISPENSING-EDIT] Associating dispensing with claim: {}", editRequest.getClaimUuid());
                dispensing.setClaimUuid(editRequest.getClaimUuid());
            } else {
                log.warn("[DISPENSING-EDIT] No claimUuid provided in request");
            }

            // 4. Status change validation
            String originalStatus = dispensing.getClaimStatus();
            log.debug("[DISPENSING-EDIT] Original status: {}", originalStatus);

            if (!Arrays.asList("DRAFT", "REJECTED", "RESUBMITTED").contains(originalStatus)) {
                log.warn("[DISPENSING-EDIT] Invalid status for editing: {}", originalStatus);
                throw new BadRequestException("Only dispensing records in DRAFT, REJECTED, or RESUBMITTED status can be edited");
            }

            // 5. Handle status changes
            if (editRequest.getClaimStatus() != null) {
                log.debug("[DISPENSING-EDIT] Requested status change to: {}", editRequest.getClaimStatus());
                if (!"RESUBMITTED".equals(editRequest.getClaimStatus())) {
                    log.warn("[DISPENSING-EDIT] Invalid status change requested: {}", editRequest.getClaimStatus());
                    throw new BadRequestException("Only status change to RESUBMITTED is allowed");
                }
                if (!"REJECTED".equals(originalStatus)) {
                    log.warn("[DISPENSING-EDIT] Attempt to resubmit non-REJECTED record: {}", originalStatus);
                    throw new BadRequestException("Only REJECTED records can be changed to RESUBMITTED");
                }
                dispensing.setClaimStatus(editRequest.getClaimStatus());
                log.info("[DISPENSING-EDIT] Changed status from {} to RESUBMITTED", originalStatus);
            } else if (Arrays.asList("REJECTED", "RESUBMITTED").contains(originalStatus)) {
                dispensing.setClaimStatus("DRAFT");
                log.info("[DISPENSING-EDIT] Changed status from {} to DRAFT", originalStatus);
            }

            // 6. Validate provider
            Provider provider = providerRepository.findByProviderUuid(userDetails.getProviderUuid());
            if (provider == null) {
                log.error("[DISPENSING-EDIT] Provider not found: {}", userDetails.getProviderUuid());
                throw new ResourceNotFoundException("Provider", "uuid", userDetails.getProviderUuid());
            }
            log.debug("[DISPENSING-EDIT] Validated provider: {}", provider.getProviderName());

            // 7. Validate insured and dependant
            Insured insured = validateInsured(editRequest.getInsuredUuid());
            Dependant dependant = validateDependant(editRequest.getDependantUuid());
            log.debug("[DISPENSING-EDIT] Validated insured and dependant");

            // 8. Validate contract
            Payer payer = insured.getPayer();
            ContractHeader activeContract = contractHeaderRepository.findActiveContractByProviderProviderUuidAndPayerPayerUuid(
                            provider.getProviderUuid(), payer.getPayerUuid())
                    .orElseThrow(() -> {
                        log.error("[DISPENSING-EDIT] Active contract not found for provider {} and payer {}",
                                provider.getProviderUuid(), payer.getPayerUuid());
                        return new ResourceNotFoundException("Active contract", "payer", payer.getPayerUuid());
                    });
            log.debug("[DISPENSING-EDIT] Validated active contract");

            // 9. Update dispensing details
            updateDispensingRecordDetails(dispensing, editRequest, insured, dependant);
            List<MedicationDispensingItem> updatedItems = updateDispensingItems(
                    dispensing, editRequest.getMedicationItems(), provider, payer, activeContract);
            updateDispensingRecordTotals(dispensing, updatedItems);

            // 10. Save the updated dispensing
            MedicationDispensing savedRecord = dispensingRepository.save(dispensing);
            log.info("[DISPENSING-EDIT] Successfully saved dispensing record with claimUuid: {}", savedRecord.getClaimUuid());

            // 11. Trigger auto-resubmission if conditions met
            if (savedRecord.getClaimUuid() != null && "RESUBMITTED".equals(savedRecord.getClaimStatus())) {
                log.info("[DISPENSING-EDIT] Triggering auto-resubmission for claim: {}", savedRecord.getClaimUuid());
                checkAndResubmitClaim(savedRecord.getClaimUuid(), userDetails, savedRecord.getDispensingUuid());
            } else {
                log.info("[DISPENSING-EDIT] Auto-resubmission not triggered. ClaimUuid: {}, Status: {}",
                        savedRecord.getClaimUuid(), savedRecord.getClaimStatus());
            }

            return ResponseEntity.ok(new DispensingRecordResponse(savedRecord));

        } catch (ResourceNotFoundException e) {
            log.error("[DISPENSING-ERROR] Resource not found: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiErrorResponse(e.getMessage()));
        } catch (BadRequestException e) {
            log.error("[DISPENSING-ERROR] Bad request: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            log.error("[DISPENSING-ERROR] Data integrity violation: {}", e.getMessage(), e);
            String errorMessage = "A conflict occurred while updating the record. Please check for duplicate entries or missing required fields.";
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ApiErrorResponse(errorMessage));
        } catch (Exception e) {
            log.error("[DISPENSING-ERROR] Unexpected error in editDispensingRecord: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse("An unexpected error occurred. Please try again later."));
        }
    }

    private void checkAndResubmitClaim(String claimUuid, UserPrincipal userDetails, String currentDispensingUuid) {
        log.info("[AUTO-RESUBMIT] INITIATING for claim: {}", claimUuid);

        try {
            Claim claim = claimRepository.findByClaimUuid(claimUuid)
                    .orElseThrow(() -> {
                        log.error("[AUTO-RESUBMIT] Claim not found: {}", claimUuid);
                        return new ResourceNotFoundException("Claim", "uuid", claimUuid);
                    });
            log.debug("[AUTO-RESUBMIT] Current claim status: {}", claim.getStatus());

            List<MedicationDispensing> dispensings = dispensingRepository.findByClaimUuid(claimUuid);
            log.info("[AUTO-RESUBMIT] Found {} existing dispensing records for claim", dispensings.size());

            boolean includesCurrent = dispensings.stream()
                    .anyMatch(d -> d.getDispensingUuid().equals(currentDispensingUuid));

            if (!includesCurrent) {
                log.debug("[AUTO-RESUBMIT] Adding current dispensing to evaluation");
                MedicationDispensing currentDispensing = dispensingRepository.findByDispensingUuid(currentDispensingUuid);
                if (currentDispensing != null) {
                    dispensings.add(currentDispensing);
                    log.debug("[AUTO-RESUBMIT] Current dispensing status: {}", currentDispensing.getClaimStatus());
                }
            }

            dispensings.forEach(d ->
                    log.debug("[AUTO-RESUBMIT] Dispensing {} status: {}", d.getDispensingUuid(), d.getClaimStatus()));

            boolean anyResubmitted = dispensings.stream()
                    .anyMatch(d -> "RESUBMITTED".equals(d.getClaimStatus()));
            boolean anyRejected = dispensings.stream()
                    .anyMatch(d -> "REJECTED".equals(d.getClaimStatus()));

            log.info("[AUTO-RESUBMIT] Conditions - Any RESUBMITTED: {}, Any REJECTED: {}",
                    anyResubmitted, anyRejected);

            if (anyResubmitted && !anyRejected) {
                log.info("[AUTO-RESUBMIT] ELIGIBLE for auto-resubmission");

                if (claim.getStatus() != ClaimStatus.RESUBMITTED) {
                    log.info("[AUTO-RESUBMIT] Updating claim status from {} to RESUBMITTED", claim.getStatus());

                    claim.setStatus(ClaimStatus.RESUBMITTED);
                    claim.addLog(new ClaimLogs(claim, ClaimStatus.RESUBMITTED,
                            "Auto-resubmitted because all dispensing records are either RESUBMITTED or DRAFT"));

                    Claim updatedClaim = claimRepository.save(claim);
                    log.info("[AUTO-RESUBMIT] SUCCESSFULLY updated claim {} to status: {}",
                            claimUuid, updatedClaim.getStatus());
                } else {
                    log.info("[AUTO-RESUBMIT] Claim already in RESUBMITTED status - no change needed");
                }
            } else {
                log.info("[AUTO-RESUBMIT] NOT ELIGIBLE - Requires at least one RESUBMITTED and zero REJECTED");
                if (!anyResubmitted) {
                    log.info("[AUTO-RESUBMIT] No RESUBMITTED dispensing records found");
                }
                if (anyRejected) {
                    log.info("[AUTO-RESUBMIT] REJECTED dispensing records present - blocking auto-resubmission");
                }
            }
        } catch (Exception e) {
            log.error("[AUTO-RESUBMIT] ERROR processing claim {}: {}", claimUuid, e.getMessage(), e);
        }
    }

    private Dependant validateDependant(String dependantUuid) {
        if (dependantUuid == null || dependantUuid.isEmpty()) {
            return null;
        }
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
        if (dependant == null) {
            throw new ResourceNotFoundException("Dependant", "uuid", dependantUuid);
        }
        return dependant;
    }

    private void updateDispensingRecordDetails(MedicationDispensing dispensing, DispensingRecordEditRequest editRequest, Insured insured, Dependant dependant) {
        dispensing.setInsured(insured);
        dispensing.setDependant(dependant);
        dispensing.setPrimaryDiagnosis(editRequest.getPrimaryDiagnosis());
        dispensing.setSecondaryDiagnosis(editRequest.getSecondaryDiagnosis());
    }

    @Transactional
    private List<MedicationDispensingItem> updateDispensingItems(MedicationDispensing dispensing,
                                                                 List<DispensingRecordEditRequest.DispensingItemEditRequest> itemRequests,
                                                                 Provider provider, Payer payer, ContractHeader activeContract) {
        log.info("Updating dispensing items for dispensing record: {}", dispensing.getDispensingUuid());

        entityManager.clear();

        dispensing = dispensingRepository.findByDispensingUuid(dispensing.getDispensingUuid());

        Map<String, MedicationDispensingItem> existingItemsMap = dispensing.getItems().stream()
                .collect(Collectors.toMap(MedicationDispensingItem::getItemUuid, Function.identity(), (item1, item2) -> item1));

        List<MedicationDispensingItem> updatedItems = new ArrayList<>();

        for (DispensingRecordEditRequest.DispensingItemEditRequest itemRequest : itemRequests) {
            log.debug("Processing item request with UUID: {}", itemRequest.getItemUuid());
            MedicationDispensingItem item;
            if (itemRequest.getItemUuid() != null && existingItemsMap.containsKey(itemRequest.getItemUuid())) {
                item = existingItemsMap.get(itemRequest.getItemUuid());
                log.debug("Updating existing item: {}", item.getItemUuid());
            } else {
                item = new MedicationDispensingItem();
                item.setItemUuid(UUID.randomUUID().toString());
                log.debug("Creating new item with UUID: {}", item.getItemUuid());
            }

            updateDispensingItem(item, itemRequest, dispensing, provider, payer, activeContract);
            updatedItems.add(item);

        }

        Set<String> requestedItemUuids = itemRequests.stream()
                .map(DispensingRecordEditRequest.DispensingItemEditRequest::getItemUuid)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        dispensing.getItems().removeIf(item -> !requestedItemUuids.contains(item.getItemUuid()));
        dispensing.getItems().clear();
        dispensing.getItems().addAll(updatedItems);

        log.info("Updated {} dispensing items for dispensing record: {}", updatedItems.size(), dispensing.getDispensingUuid());
        return updatedItems;
    }

    private void updateDispensingItem(MedicationDispensingItem item, DispensingRecordEditRequest.DispensingItemEditRequest itemRequest,
                                      MedicationDispensing dispensing, Provider provider, Payer payer, ContractHeader activeContract) {
        log.debug("Updating dispensing item: {}", item.getItemUuid());
        item.setDispensing(dispensing);
        item.setQuantity((double) itemRequest.getQuantity());
        item.setRemark(itemRequest.getRemark());
        item.setItemType(ItemType.valueOf(itemRequest.getItemType()));

        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(itemRequest.getContractDetailUuid());
        if (contractDetail == null) {
            throw new ResourceNotFoundException("ContractDetail", "uuid", itemRequest.getContractDetailUuid());
        }

        item.setContractDetail(contractDetail);

        if (item.getItemType() == ItemType.DRUG) {
            Drug drug = contractDetail.getDrug();
            if (drug == null) {
                throw new ResourceNotFoundException("Drug", "contractDetail", contractDetail.getContractDetailUuid());
            }
            item.setMedicationCode(drug.getDrugCode());
            item.setMedicationName(drug.getDrugName());
        } else if (item.getItemType() == ItemType.SERVICE) {
            Servicelist service = contractDetail.getServicelist();
            if (service == null) {
                throw new ResourceNotFoundException("Service", "contractDetail", contractDetail.getContractDetailUuid());
            }
            item.setMedicationCode(service.getServiceCode());
            item.setMedicationName(service.getServiceName());
        } else {
            throw new BadRequestException("Invalid item type: " + itemRequest.getItemType());
        }

        item.setUnitPrice(itemRequest.getPrice());
        item.setTotalPrice(itemRequest.getPrice() * itemRequest.getQuantity());
        log.debug("Dispensing item updated: {}", item.getItemUuid());
    }


    @Override
    @Transactional
    public ResponseEntity<?> editDrugDispensingRecord(String dispensingUuid, DrugDispensingRecordEditRequest editRequest) {

        MedicationDispensing dispensing = dispensingRepository.findByDispensingUuid(dispensingUuid);
        if (dispensing == null) {
            throw new ResourceNotFoundException("Dispensing Record", "uuid", dispensingUuid);
        }

        if (!"DRAFT".equals(dispensing.getClaimStatus())) {
            throw new BadRequestException("Only dispensing records in DRAFT status can be edited");
        }

        dispensing.setDispensingDate(editRequest.getDispensingDate());
        dispensing.setPrescriptionNumber(editRequest.getPrescriptionNumber());
        dispensing.setPharmacyTransactionId(editRequest.getPharmacyTransactionId());
        dispensing.setPharmacistNotes(editRequest.getPharmacistNotes());
        dispensing.setPrimaryDiagnosis(editRequest.getPrimaryDiagnosis());
        dispensing.setSecondaryDiagnosis(editRequest.getSecondaryDiagnosis());
        dispensing.setPrescribingPhysicianName(editRequest.getPrescribingPhysicianName());
        dispensing.setPrescribingPhysicianId(editRequest.getPrescribingPhysicianId());
        dispensing.setBranchName(editRequest.getBranchName());

        List<MedicationDispensingItem> items = dispensingItemRepository.findByDispensing(dispensing);
        for (DrugDispensingRecordEditRequest.DrugDispensingItemEditRequest itemEdit : editRequest.getMedicationItems()) {
            MedicationDispensingItem item = items.stream()
                    .filter(i -> i.getItemUuid().equals(itemEdit.getItemUuid()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Dispensing Item", "uuid", itemEdit.getItemUuid()));

            Drug drug = drugRepository.findByDrugUuid(itemEdit.getDrugUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Drug", "uuid", itemEdit.getDrugUuid()));

            item.setMedicationCode(drug.getDrugCode());
            item.setMedicationName(drug.getDrugName());
            item.setQuantity(itemEdit.getQuantity());
            item.setTotalPrice(itemEdit.getTotalPrice());
            item.setRoute(itemEdit.getRoute());
            item.setItemType(itemEdit.getItemType());

            String dosageInstructions = String.format("%s %s for %s", itemEdit.getDose(), itemEdit.getFrequency(), itemEdit.getDuration());
            item.setDosageInstructions(dosageInstructions);
        }

        double totalAmount = items.stream().mapToDouble(MedicationDispensingItem::getTotalPrice).sum();
        dispensing.setTotalAmount(totalAmount);

        calculateCoverageAndResponsibility(dispensing, dispensing.getInsured().getPayer(), dispensing.getInsured());

        dispensingRepository.save(dispensing);
        dispensingItemRepository.saveAll(items);

        return ResponseEntity.ok(new MessageResponse("Drug dispensing record updated successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateServiceClaimStatus(String medicationDispensingUuid, String newStatus, String remark) {
        MedicationDispensing medicationDispensing = dispensingRepository.findByDispensingUuid(medicationDispensingUuid);
        if (medicationDispensing == null) {
            throw new ResourceNotFoundException("MedicationDispensing", "uuid", medicationDispensingUuid);
        }

        BatchRecord batchRecord = medicationDispensing.getBatchRecord();
        if (batchRecord == null) {
            throw new BadRequestException("This medication dispensing is not associated with any batch.");
        }

        medicationDispensing.setClaimStatus(newStatus);
        medicationDispensing.setRemark(remark);
        dispensingRepository.save(medicationDispensing);

        boolean allSameStatus = batchRecord.getMedicationDispensing().stream()
                .allMatch(md -> newStatus.equals(md.getClaimStatus()));

        if (allSameStatus) {
            Claim claim = batchRecord.getClaim();
            if (claim != null) {
                claim.setStatus(ClaimStatus.valueOf(newStatus));
                claimRepository.save(claim);
            }
        }

        return ResponseEntity.ok("Claim status updated successfully");

    }

    private DispensingDetailResponse.DispensingItemDetail convertToItemDetail(MedicationDispensingItem item) {
        DispensingDetailResponse.DispensingItemDetail detail = new DispensingDetailResponse.DispensingItemDetail();
        BeanUtils.copyProperties(item, detail);
        detail.setItemType(String.valueOf(item.getItemType()));

        if (item.getContractDetail() != null) {
            detail.setContractDetailUuid(item.getContractDetail().getContractDetailUuid());

            if (item.getContractDetail().getContractHeader() != null) {
                detail.setContractHeaderUuid(item.getContractDetail().getContractHeader().getContractHeaderUuid());
            }
        }

        return detail;
    }

    private MedicationDispensingDTO convertToMedicationDispensingDTO(MedicationDispensing dispensing) {
        MedicationDispensingDTO dto = new MedicationDispensingDTO();
        BeanUtils.copyProperties(dispensing, dto);

        Provider provider = providerRepository.findByProviderUuid(dispensing.getProviderUuid());

        if (provider != null) {

            dto.setProviderName(provider.getProviderName());
            dto.setProviderPhoneNumber(provider.getTelephone());
            String providerLogoPath = provider.getLogoPath();
            if (providerLogoPath == null || providerLogoPath.isEmpty()) {
                log.warn("Provider {} has no logo path set", provider.getProviderUuid());
            }
            String providerLogo = getLogoBase64(providerLogoPath, providerLogosDirectory);
            dto.setProviderLogoBase64(providerLogo);
            log.info("Provider logo: {}", providerLogo != null ? "Set" : "Null");

        }

        Payer payer = payerRepository.findByPayerUuid(dispensing.getPayerUuid());

        if (payer != null) {
            dto.setPayerName(payer.getPayerName());
            dto.setPayerPhoneNumber(payer.getTelephone());
            String payerLogo = getLogoBase64(payer.getLogoPath(), payerLogosDirectory);
            dto.setPayerLogoBase64(payerLogo);
            log.info("Payer logo: {}", payerLogo != null ? "Set" : "Null");
        }

        Insured insured = insuredRepository.findByInsuredUuid(dispensing.getInsuredUuid());
        if (insured != null) {
            dto.setInsuredName(insured.getFirstName() + " " + insured.getFatherName() + " " + insured.getGrandFatherName());
            dto.setInsuranceId(insured.getInsuranceId());
        }

        List<MedicationDispensingDTO.MedicationItemDTO> itemDTOs = dispensing.getItems().stream()
                .map(this::convertToMedicationItemDTO)
                .collect(Collectors.toList());
        dto.setMedicationItems(itemDTOs);

        return dto;
    }

    private String getLogoBase64(String logoPath, String logoDirectory) {
        if (logoPath == null || logoPath.isEmpty()) {
            log.info("Logo path is null or empty");
            return getDefaultLogoBase64();
        }

        try {
            String fullPath = logoDirectory + "/" + logoPath;
            log.info("Attempting to read logo from: {}", fullPath);
            File logoFile = new File(fullPath);

            if (logoFile.exists() && logoFile.isFile()) {
                byte[] fileContent = Files.readAllBytes(logoFile.toPath());
                String base64Logo = Base64.getEncoder().encodeToString(fileContent);
                String contentType = determineContentType(fullPath);
                log.info("Logo found and encoded. Content type: {}", contentType);
                return "data:" + contentType + ";base64," + base64Logo;
            } else {
                log.warn("Logo file does not exist or is not a file: {}", fullPath);
                return getDefaultLogoBase64();
            }
        } catch (IOException e) {
            log.error("Error reading logo file: {}", logoPath, e);
            return getDefaultLogoBase64();
        }
    }

    private String determineContentType(String filePath) {
        try {
            String contentType = Files.probeContentType(Paths.get(filePath));
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            log.error("Error determining content type for file: " + filePath, e);
            return "application/octet-stream";
        }
    }

    private String getDefaultLogoBase64() {
        return null;
    }

    private MedicationDispensingDTO.MedicationItemDTO convertToMedicationItemDTO(MedicationDispensingItem item) {
        MedicationDispensingDTO.MedicationItemDTO itemDTO = new MedicationDispensingDTO.MedicationItemDTO();
        BeanUtils.copyProperties(item, itemDTO);

        // itemDTO.setItemType(item.getItemType().toString());

        return itemDTO;

    }

    private ImmutablePair<Insured, Dependant> findInsuredPersonFor(DrugDispensingRecordRequest request) {
        String patientId = request.getPhone();
        Insured insured = null;
        Dependant dependant = null;

        insured = insuredRepository.findByInsuranceId(patientId);
        if (insured == null) {
            insured = insuredRepository.findByEmployeeId(patientId);
        }
        if (insured == null) {
            insured = insuredRepository.findByNationalId(patientId);
        }
        if (insured == null) {
            List<Insured> insuredList = (List<Insured>) insuredRepository.findByPhone(patientId);
            if (!insuredList.isEmpty()) {
                insured = insuredList.get(0);
            }
        }

        if (insured != null && request.getDependantUuid() != null && !request.getDependantUuid().isEmpty()) {
            dependant = insured.getDependants().stream()
                    .filter(d -> d.getDependantUuid().equals(request.getDependantUuid()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Dependant", "uuid", request.getDependantUuid()));
        }

        if (insured == null) {
            dependant = dependantRepository.findByPhone(patientId);
            if (dependant != null) {
                insured = dependant.getInsured();
            }
        }

        if (insured == null) {
            throw new ResourceNotFoundException("Insured or Dependant", "patientId", patientId);
        }

        return new ImmutablePair<>(insured, dependant);
    }

    private List<Drug> validateDrugs(String providerUuid, List<DrugDispensingRecordRequest.DrugDispensingItemRequest> drugItems) {
        List<String> drugUuids = drugItems.stream().map(DrugDispensingRecordRequest.DrugDispensingItemRequest::getDrugUuid).collect(Collectors.toList());

        List<Drug> drugs = drugRepository.findByDrugUuidIn(drugUuids);

        if (drugs.size() != drugUuids.size()) {
            throw new RuntimeException("One or more drugs not found");
        }

        drugs.forEach(drug -> {
            if (!drug.getProvider().getProviderUuid().equals(providerUuid)) {
                throw new BadRequestException("Drug " + drug.getDrugName() + " does not belong to the specified provider");
            }
        });

        return drugs;
    }

    private MedicationDispensing createDrugDispensingRecord(DrugDispensingRecordRequest request, Insured insured, Dependant dependant, Payer payer, Provider provider) {
        MedicationDispensing dispensing = new MedicationDispensing();
        dispensing.setDispensingUuid(UUID.randomUUID().toString());
        dispensing.setProviderUuid(provider.getProviderUuid());

        dispensing.setInsured(insured);
        dispensing.setInsuredUuid(insured.getInsuredUuid());

        if (dependant != null) {
            dispensing.setDependant(dependant);
        }

        dispensing.setPayerUuid(payer.getPayerUuid());
        dispensing.setDispensingDate(request.getDispensingDate());
        dispensing.setPrescriptionNumber(request.getPrescriptionNumber());
        dispensing.setPharmacyTransactionId(request.getPharmacyTransactionId());
        dispensing.setClaimStatus(ClaimStatus.DRAFT.toString());
        dispensing.setSource(SourceType.INPUT);
        dispensing.setStatus(MedicationStatus.DRAFT);
        dispensing.setInvoiceNumber(generateInvoiceNumber());

        dispensing.setRecordedAt(LocalDate.now());
        return dispensing;
    }

    private List<MedicationDispensingItem> createDrugDispensingItems(MedicationDispensing dispensingRecord, List<Drug> drugs, List<DrugDispensingRecordRequest.DrugDispensingItemRequest> drugItems, Payer payer, ContractHeader activeContract) {
        List<MedicationDispensingItem> dispensingItems = new ArrayList<>();
        Insured insured = dispensingRecord.getInsured();
        String contractHeaderUuid = activeContract.getContractHeaderUuid();

        for (int i = 0; i < drugs.size(); i++) {
            Drug drug = drugs.get(i);
            DrugDispensingRecordRequest.DrugDispensingItemRequest item = drugItems.get(i);

            MedicationDispensingItem dispensingItem = new MedicationDispensingItem();
            dispensingItem.setItemUuid(UUID.randomUUID().toString());
            dispensingItem.setDispensing(dispensingRecord);
            dispensingItem.setMedicationCode(drug.getDrugCode());
            dispensingItem.setMedicationName(drug.getDrugName());
            dispensingItem.setQuantity((double) item.getQuantity().intValue());
            dispensingItem.setTotalPrice(item.getTotalPrice());
            dispensingItem.setFormulation(drug.getFormulation());
            dispensingItem.setRoute(item.getRoute());
            dispensingItem.setItemType(ItemType.DRUG);

            String dosageInstructions = String.format("%s %s for %s", item.getDose(), item.getFrequency(), item.getDuration());
            dispensingItem.setDosageInstructions(dosageInstructions);

            ContractDetail contractDetail = findContractDetail(contractHeaderUuid, drug.getDrugUuid(), insured);
            if (contractDetail == null) {
                throw new ResourceNotFoundException("ContractDetail", "drug", drug.getDrugUuid());
            }

            dispensingItem.setContractDetail(contractDetail);

            dispensingItems.add(dispensingItem);
        }

        return dispensingItems;
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateDispensingRecordsStatus(String providerUuid, String newStatus, String[] dispensingUuids) {

        if (dispensingUuids == null || dispensingUuids.length == 0) {
            throw new BadRequestException("No dispensing records selected");
        }

        if (!newStatus.equals("SUBMITTED") && !newStatus.equals("AUTHORIZED")) {
            throw new BadRequestException("Invalid new status. Must be either SUBMITTED or AUTHORIZED");
        }

        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null) {
            throw new ResourceNotFoundException("Provider", "uuid", providerUuid);
        }

        List<MedicationDispensing> dispensingRecords = dispensingRepository.findByDispensingUuidIn(dispensingUuids);

        if (dispensingRecords.size() != dispensingUuids.length) {
            throw new RuntimeException("One or more dispensing records not found");
        }

        BatchRecord batchRecord = null;
        String message = "";

        if (newStatus.equals("SUBMITTED")) {
            for (MedicationDispensing record : dispensingRecords) {
                if (!record.getStatus().equals(MedicationStatus.valueOf("DRAFT"))) {
                    throw new BadRequestException("Dispensing record " + record.getDispensingUuid() +
                            " is not in DRAFT status");
                }
            }

            String payerUuid = dispensingRecords.get(0).getPayerUuid();
            Payer payer = payerRepository.findByPayerUuid(payerUuid);
            if (payer == null) {
                throw new ResourceNotFoundException("Payer", "uuid", payerUuid);
            }

            batchRecord = createBatchRecord(payer, dispensingRecords, null);
            batchRecord = batchRecordRepository.save(batchRecord);

            for (MedicationDispensing record : dispensingRecords) {
                record.setClaimStatus("SUBMITTED");
                record.setStatus(MedicationStatus.SUBMITTED);
                record.setBatchCode(batchRecord.getBatchCode());
                record.setBatchRecord(batchRecord);
            }

            message = dispensingRecords.size() + " dispensing record(s) updated to SUBMITTED status. " +
                    "Batch created with code: " + batchRecord.getBatchCode();

        }

        dispensingRepository.saveAll(dispensingRecords);

        return ResponseEntity.ok(new MessageResponse(message));

    }

    private BatchRecord createBatchRecord(Payer payer, List<MedicationDispensing> dispensingRecords, Claim claim) {

        BatchRecord batchRecord = new BatchRecord();

        String providerUuid = dispensingRecords.get(0).getProviderUuid();
        Provider provider = providerRepository.findByProviderUuid(providerUuid);

        BatchCodeInfo batchCodeInfo = generateBatchCode(provider);
        batchRecord.setBatchCode(batchCodeInfo.getBatchCode());
        batchRecord.setBatchNumber(batchCodeInfo.getBatchNumber());

        batchRecord.setPayerName(payer.getPayerName());
        batchRecord.setRequestedOn(LocalDate.now());
        batchRecord.setClaimDatingFrom(dispensingRecords.stream()
                .map(MedicationDispensing::getDispensingDate)
                .min(LocalDate::compareTo)
                .orElse(null));
        batchRecord.setClaimDatingTo(dispensingRecords.stream()
                .map(MedicationDispensing::getDispensingDate)
                .max(LocalDate::compareTo)
                .orElse(null));

        double totalAmount = dispensingRecords.stream()
                .mapToDouble(MedicationDispensing::getTotalAmount)
                .sum();
        batchRecord.setTotalAmount(totalAmount);

        long uniqueDispensingCount = dispensingRecords.stream()
                .map(MedicationDispensing::getDispensingUuid)
                .distinct()
                .count();
        batchRecord.setNumberOfClaims((double) uniqueDispensingCount);

        batchRecord.setStatus("SUBMITTED");
        batchRecord.setMedicationDispensing(dispensingRecords);

        if (claim != null) {
            batchRecord.setClaim(claim);
            batchRecord.setClaimUuid(claim.getClaimUuid());
        }

        return batchRecord;
    }

    private BatchCodeInfo generateBatchCode(Provider provider) {
        String providerPrefix = provider.getProviderName().substring(0, Math.min(provider.getProviderName().length(), 3)).toUpperCase();

        Long maxBatchNumber = batchRecordRepository.findMaxBatchNumber();
        Long newBatchNumber = (maxBatchNumber == null) ? 1L : maxBatchNumber + 1;

        String batchNumberString = String.format("%07d", newBatchNumber);

        String batchCode = "CL-" + providerPrefix + "-" + batchNumberString;

        return new BatchCodeInfo(batchCode, newBatchNumber);
    }

    @Override
    @Transactional
    public ResponseEntity<?> addDispensingRecord(DispensingRecordRequest request) {
        try {
            log.info("Starting to add dispensing record");
            UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

            Provider provider = providerRepository.findByProviderUuid(userDetails.getProviderUuid());
            if (provider == null) {
                throw new ResourceNotFoundException("Provider", "uuid", userDetails.getProviderUuid());
            }

            Insured insured = insuredRepository.findByInsuredUuid(request.getInsuredUuid());
            if (insured == null) {
                throw new ResourceNotFoundException("Insured", "uuid", request.getInsuredUuid());
            }

            Payer payer = insured.getPayer();

            ContractHeader contract = contractHeaderRepository.findByContractHeaderUuid(request.getContractHeaderUuid());
            if (contract == null) {
                throw new ResourceNotFoundException("Contract", "uuid", request.getContractHeaderUuid());
            }
            if (contract.getStatus() != Status.ACTIVE) {
                throw new BadRequestException("The specified contract is not active");
            }

            MedicationDispensing dispensingRecord = createDispensingRecord(request, provider, insured, payer);

            List<Servicelist> services = validateServices(provider.getProviderUuid(), request.getMedicationItems(), contract);
            log.info("Services validated. Count: {}", services.size());

            List<MedicationDispensingItem> dispensingItems = createDispensingItems(
                    dispensingRecord,
                    services,
                    request.getMedicationItems(),
                    payer,
                    contract,
                    request
            );

            updateDispensingRecordTotals(dispensingRecord, dispensingItems);
            MedicationDispensing savedRecord = dispensingRepository.save(dispensingRecord);
            dispensingItemRepository.saveAll(dispensingItems);

            handlePostDispensingProcessing(request, savedRecord, dispensingItems);

            return ResponseEntity.ok(new DispensingRecordResponse(savedRecord));

        } catch (Exception e) {
            log.error("Error in addDispensingRecord: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse("An unexpected error occurred. Please try again later."));
        }
    }

    /**
     * Handle post-dispensing processing based on insurance type
     */
    private void handlePostDispensingProcessing(DispensingRecordRequest request,
                                                MedicationDispensing savedRecord,
                                                List<MedicationDispensingItem> dispensingItems) {

        log.info("Starting post-dispensing processing. IsInsurance: {}", request.getIsInsurance());

        try {
            if (Boolean.TRUE.equals(request.getIsInsurance())) {
                handleInsuranceProcessing(request, savedRecord, dispensingItems);
            } else {
                handleNonInsuranceProcessing(savedRecord, dispensingItems);
            }
        } catch (Exception e) {
            log.error("Error in post-dispensing processing", e);
        }
    }

    /**
     * Handle processing for insurance payers - send to external system
     */
    private void handleInsuranceProcessing(DispensingRecordRequest request,
                                           MedicationDispensing savedRecord,
                                           List<MedicationDispensingItem> dispensingItems) {

        log.info("Processing insurance dispensing for external system");
        log.info("Dispensing items count: {}", dispensingItems != null ? dispensingItems.size() : "null");

        if (dispensingItems == null || dispensingItems.isEmpty()) {
            log.error("Cannot process insurance - dispensing items list is empty or null");
            return;
        }

        try {
            String serviceId = getServiceId(request, dispensingItems);

            if (serviceId != null) {
                log.info("Sending {} dispensing items to external insurance system with serviceId: {}",
                        dispensingItems.size(), serviceId);

                externalInsuranceService.sendDispensingToExternalSystem(
                        dispensingItems,
                        request.getPackageUuid(),
                        serviceId,
                        savedRecord.getDispensingUuid(),
                        request.getContractHeaderUuid()
                );
                log.info("Successfully sent dispensing data to external insurance system");
            } else {
                log.warn("Could not determine serviceId for external system integration");
            }

        } catch (Exception e) {
            log.error("Error sending data to external insurance system", e);
        }
    }

    /**
     * Handle processing for non-insurance payers - reduce package usage
     */
    private void handleNonInsuranceProcessing(MedicationDispensing savedRecord,
                                              List<MedicationDispensingItem> dispensingItems) {

        log.info("Processing non-insurance dispensing - reducing package usage");

        try {
            String insuredUuid = getInsuredUuidFromDispensing(savedRecord);

            for (MedicationDispensingItem item : dispensingItems) {
                if (item.getContractDetail() != null) {
                    packageUsageService.recordServiceConsumption(
                            insuredUuid,
                            item.getContractDetail().getContractDetailUuid(),
                            BigDecimal.valueOf(item.getTotalPrice()),
                            item.getQuantity(),
                            savedRecord.getCreatedAt(),
                            null,
                            savedRecord.getDispensingUuid(),
                            "Medication dispensing consumption"
                    );

                    log.info("Recorded package usage for service: {}, amount: {}",
                            item.getContractDetail().getContractDetailUuid(), item.getTotalPrice());
                }
            }

        } catch (Exception e) {
            log.error("Error recording package usage for non-insurance dispensing", e);
        }
    }

    /**
     * Get serviceId from medication items or derive from contractDetailUuid
     * Now supports serviceId at item level
     */
    private String getServiceId(DispensingRecordRequest request, List<MedicationDispensingItem> dispensingItems) {

        if (request.getMedicationItems() != null && !request.getMedicationItems().isEmpty()) {
            DispensingRecordRequest.DispensingItemRequest firstItemRequest = request.getMedicationItems().get(0);

            if (firstItemRequest.getServiceId() != null && !firstItemRequest.getServiceId().isEmpty()) {
                log.info("Using serviceId from first medication item: {}", firstItemRequest.getServiceId());
                return firstItemRequest.getServiceId();
            }
        }

        if (!dispensingItems.isEmpty()) {
            MedicationDispensingItem firstItem = dispensingItems.get(0);
            if (firstItem.getContractDetail() != null &&
                    firstItem.getContractDetail().getServicelist() != null) {

                log.info("Using serviceId from first dispensing item's contract detail: {}",
                        firstItem.getContractDetail().getServicelist().getGeneratedServiceId());
                return firstItem.getContractDetail().getServicelist().getGeneratedServiceId();
            }
        }

        log.warn("Could not determine serviceId from request or dispensing items");
        return null;
    }

    /**
     * Get insured UUID from dispensing record
     */
    private String getInsuredUuidFromDispensing(MedicationDispensing savedRecord) {
        if (savedRecord.getInsured() != null) {
            return savedRecord.getInsured().getInsuredUuid();
        } else if (savedRecord.getInsuredUuid() != null) {
            return savedRecord.getInsuredUuid();
        }

        log.warn("Could not determine insured UUID from dispensing record: {}", savedRecord.getDispensingUuid());
        return null;
    }

    /**
     * Get or resolve contractDetailUuid from serviceId if not provided
     * Now supports serviceId at item level
     */
    private String getContractDetailUuid(DispensingRecordRequest.DispensingItemRequest item,
                                         ContractHeader contract,
                                         String fallbackServiceId) {

        if (item.getContractDetailUuid() != null && !item.getContractDetailUuid().isEmpty()) {
            return item.getContractDetailUuid();
        }

        String serviceIdToUse;
        if (item.getServiceId() != null && !item.getServiceId().isEmpty()) {
            serviceIdToUse = item.getServiceId();
            log.info("Using serviceId from item: {}", serviceIdToUse);
        } else if (fallbackServiceId != null && !fallbackServiceId.isEmpty()) {
            serviceIdToUse = fallbackServiceId;
            log.info("Using fallback serviceId: {}", serviceIdToUse);
        } else {
            serviceIdToUse = null;
        }

        if (serviceIdToUse != null) {
            log.info("ContractDetailUuid not provided, searching using serviceId: {}", serviceIdToUse);

            Servicelist service = servicelistRepository.findAll().stream()
                    .filter(s -> serviceIdToUse.equals(s.getGeneratedServiceId()))
                    .findFirst()
                    .orElse(null);

            if (service != null) {
                ContractDetail contractDetail = contractDetailRepository.findAll().stream()
                        .filter(cd -> cd.getContractHeader().getId().equals(contract.getId()) &&
                                cd.getServicelist() != null &&
                                cd.getServicelist().getId().equals(service.getId()))
                        .findFirst()
                        .orElse(null);

                if (contractDetail != null) {
                    log.info("Found contractDetailUuid: {} for serviceId: {}",
                            contractDetail.getContractDetailUuid(), serviceIdToUse);
                    return contractDetail.getContractDetailUuid();
                } else {
                    log.warn("No contract detail found for serviceId: {} in contract: {}",
                            serviceIdToUse, contract.getContractHeaderUuid());
                }
            } else {
                log.warn("No service found with generatedServiceId: {}", serviceIdToUse);
            }
        }

        return null;
    }

    private Provider validateProvider(String providerUuid) {
        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null) {
            throw new ResourceNotFoundException("Provider", "uuid", providerUuid);
        }

        return provider;
    }

    @Override
    @Transactional
    public ResponseEntity<DispensingResponse> recordMedicationDispensing(KenemaPharmacyDispensingRequest request) {

        log.info("Recording medication dispensing for insured UUID: {}", request.getInsuredUuid());

        if (StringUtils.isBlank(request.getInsuredUuid())) {
            throw new BadRequestException("insuredUuid is required");
        }

        Provider provider = providerRepository.findByProviderNameContainingIgnoreCase("kenema")
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider",
                        "name containing 'kenema'",
                        request.getProviderName()
                ));
        log.info("Kenema provider found: {}", provider.getProviderName());

        Insured insured = insuredRepository.findByInsuredUuid(request.getInsuredUuid());
        if (insured == null) {
            throw new ResourceNotFoundException(
                    "Insured",
                    "insuredUuid",
                    request.getInsuredUuid()
            );
        }
        log.info("Found insured: {} {}", insured.getFirstName(), insured.getFatherName());

        EligibilityResponse eligibilityResponse = checkEligibility(insured, provider.getProviderUuid());
        log.info("Eligibility check response: {}", eligibilityResponse);

        MedicationDispensing dispensing = createDispensingRecord(request, insured, provider, eligibilityResponse);
        log.info("Dispensing record created: {}", dispensing);

        MedicationDispensing savedDispensing = dispensingRepository.save(dispensing);
        log.info("Dispensing record saved: {}", savedDispensing.getDispensingUuid());

        List<MedicationDispensingItem> items = createDispensingItems(request, savedDispensing);
        log.info("Dispensing items created: {}", items.size());

        List<MedicationDispensingItem> savedItems = dispensingItemRepository.saveAll(items);
        log.info("Dispensing items saved: {}", savedItems.size());

        handleKenemaPackageAndExternalIntegration(request, savedItems, savedDispensing);

        DispensingResponse response = createDispensingResponse(savedDispensing);
        log.info("Dispensing response created: {}", response);

        return ResponseEntity.ok(response);

    }

    /**
     * Handle external API integration for Kenema pharmacy dispensing
     * Gets additional fields from your system instead of external request
     */
    private void handleKenemaPackageAndExternalIntegration(KenemaPharmacyDispensingRequest request,
                                                           List<MedicationDispensingItem> dispensingItems,
                                                           MedicationDispensing dispensing) {

        log.info("Handling external integration for Kenema dispensing: {}", dispensing.getDispensingUuid());

        KenemaIntegrationConfig config = resolveKenemaIntegrationConfig(request, dispensing);
        if (config.isInsurance()) {
            log.info("Processing insurance payer - sending to external system");
            handleKenemaExternalInsuranceIntegration(request, dispensingItems, dispensing, config);
        } else {
            log.info("Non-insurance payer - no external integration needed for pharmacy");
            // For pharmacy, we don't handle package management
        }
    }

    /**
     * Handle external insurance system integration for Kenema dispensing
     */
    private void handleKenemaExternalInsuranceIntegration(KenemaPharmacyDispensingRequest request,
                                                          List<MedicationDispensingItem> dispensingItems,
                                                          MedicationDispensing dispensing,
                                                          KenemaIntegrationConfig config) {

        try {
            if (config.getContractHeaderUuid() == null || config.getContractHeaderUuid().isEmpty()) {
                log.error("ContractHeaderUuid is required for insurance integration");
                throw new IllegalArgumentException("ContractHeaderUuid is required for insurance payers");
            }

            String serviceId = getServiceIdForKenemaIntegration(config, dispensingItems);

            if (serviceId != null) {
                log.info("Sending Kenema dispensing to external insurance system with serviceId: {}", serviceId);

                externalInsuranceService.sendDispensingToExternalSystem(
                        dispensingItems,
                        config.getPackageUuid(),
                        serviceId,
                        dispensing.getDispensingUuid(),
                        config.getContractHeaderUuid()
                );

                log.info("Successfully sent Kenema dispensing to external insurance system");
            } else {
                log.warn("No serviceId found for Kenema integration - skipping external integration");
            }

        } catch (Exception e) {
            log.error("Error sending Kenema dispensing to external insurance system", e);
            // Don't throw exception to avoid breaking the main dispensing flow
            // The failure will be logged by the external insurance service for retry
        }
    }

    /**
     * Resolve Kenema integration configuration from your system
     * This method should be customized to get the required fields from your system
     */
    private KenemaIntegrationConfig resolveKenemaIntegrationConfig(KenemaPharmacyDispensingRequest request,
                                                                   MedicationDispensing dispensing) {

        // TODO: Implement logic to get these fields from your system
        // This is where you would query your database/configuration to get:
        // - contractHeaderUuid
        // - isInsurance flag
        // - packageUuid
        // - dependantUuid
        // - serviceId

        // For now, using default logic based on payer type
        String payerUuid = dispensing.getPayerUuid();
        Payer payer = payerRepository.findByPayerUuid(payerUuid);

        boolean isInsurance = false;
        String contractHeaderUuid = null;
        String packageUuid = null;
        String dependantUuid = null;
        String serviceId = null;

        if (payer != null) {
            isInsurance = isInsurancePayer(payer);

            if (isInsurance) {
                contractHeaderUuid = getContractHeaderUuidForPayer(dispensing.getProviderUuid(), payerUuid);
                packageUuid = getPackageUuidForInsured(dispensing.getInsuredUuid());
                serviceId = getDefaultServiceIdForPharmacy();
            }
        }

        return KenemaIntegrationConfig.builder()
                .contractHeaderUuid(contractHeaderUuid)
                .isInsurance(isInsurance)
                .packageUuid(packageUuid)
                .dependantUuid(dependantUuid)
                .serviceId(serviceId)
                .payerUuid(payerUuid)
                .payerType(payer != null ? payer.getPayerName() : null)
                .build();
    }

    /**
     * Get serviceId for Kenema integration from config or dispensing items
     */
    private String getServiceIdForKenemaIntegration(KenemaIntegrationConfig config,
                                                    List<MedicationDispensingItem> dispensingItems) {

        if (config.getServiceId() != null && !config.getServiceId().isEmpty()) {
            return config.getServiceId();
        }

        if (!dispensingItems.isEmpty()) {
            MedicationDispensingItem firstItem = dispensingItems.get(0);
            if (firstItem.getContractDetail() != null &&
                    firstItem.getContractDetail().getServicelist() != null) {
                return firstItem.getContractDetail().getServicelist().getGeneratedServiceId();
            }
        }

        return null;
    }

    private boolean isInsurancePayer(Payer payer) {
        if (payer == null) {
            return false;
        }
        return payer.isInsurance();
    }

    /**
     * Get contract header UUID for provider-payer combination
     */
    private String getContractHeaderUuidForPayer(String providerUuid, String payerUuid) {
        try {
            List<ContractHeader> activeContracts = contractHeaderRepository
                    .findActiveContractsBetweenProviderAndPayer(providerUuid, payerUuid, Status.ACTIVE);

            if (!activeContracts.isEmpty()) {
                return activeContracts.get(0).getContractHeaderUuid();
            }
        } catch (Exception e) {
            log.warn("Error getting contract for provider {} and payer {}: {}",
                    providerUuid, payerUuid, e.getMessage());
        }
        return null;
    }

    /**
     * Check if service validation should be performed
     * Skip validation if any medication item has serviceId provided
     */
    private boolean shouldValidateServices(List<DispensingRecordRequest.DispensingItemRequest> medicationItems) {
        if (medicationItems == null || medicationItems.isEmpty()) {
            return true; // Validate if no items
        }

        boolean hasServiceId = medicationItems.stream()
                .anyMatch(item -> item.getServiceId() != null && !item.getServiceId().trim().isEmpty());

        if (hasServiceId) {
            log.info("ServiceId found in medication items - skipping service validation");
            return false;
        }

        log.info("No serviceId found in medication items - performing service validation");
        return true;
    }

    /**
     * Resolve service from item request when service validation is skipped
     */
    private Servicelist resolveServiceFromItemRequest(DispensingRecordRequest.DispensingItemRequest itemRequest,
                                                      String providerUuid) {

        // Priority 1: Use serviceId if provided
        if (itemRequest.getServiceId() != null && !itemRequest.getServiceId().trim().isEmpty()) {
            log.info("Resolving service using serviceId: {}", itemRequest.getServiceId());

            Servicelist service = servicelistRepository.findByGeneratedServiceIdAndProviderProviderUuid(
                    itemRequest.getServiceId(), providerUuid);

            if (service != null) {
                log.info("Service resolved successfully: {}", service.getServiceName());
                return service;
            } else {
                log.error("Service not found for serviceId: {} and provider: {}",
                        itemRequest.getServiceId(), providerUuid);
                return null;
            }
        }

        // Priority 2: Use contractDetailUuid to get service
        if (itemRequest.getContractDetailUuid() != null && !itemRequest.getContractDetailUuid().trim().isEmpty()) {
            log.info("Resolving service using contractDetailUuid: {}", itemRequest.getContractDetailUuid());

            ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(
                    itemRequest.getContractDetailUuid());

            if (contractDetail != null && contractDetail.getServicelist() != null) {
                log.info("Service resolved from contract detail: {}", contractDetail.getServicelist().getServiceName());
                return contractDetail.getServicelist();
            } else {
                log.error("Contract detail not found or has no service for contractDetailUuid: {}",
                        itemRequest.getContractDetailUuid());
                return null;
            }
        }

        log.error("Cannot resolve service - neither serviceId nor contractDetailUuid provided");
        return null;
    }

    /**
     * Get package UUID for insured person
     * Customize this based on your package management system
     */
    private String getPackageUuidForInsured(String insuredUuid) {
        // TODO: Implement logic to get package UUID for insured person
        // This could involve:
        // - Querying package assignments
        // - Getting active package for insured
        // - Default package based on insured type

        log.info("Getting package UUID for insured: {}", insuredUuid);
        // Return null for now - customize based on your package system
        return null;
    }

    /**
     * Get default service ID for pharmacy dispensing
     * Customize this based on your service configuration
     */
    private String getDefaultServiceIdForPharmacy() {
        // TODO: Implement logic to get default service ID for pharmacy
        // This could be:
        // - A configured default pharmacy service
        // - A general medication dispensing service
        // - Based on provider type

        log.info("Getting default service ID for pharmacy");
        // Return null for now - customize based on your service setup
        return null;
    }


    private Insured selectInsured(List<Insured> insuredList, String identifierType, String identifier) {
        if (insuredList.size() == 1) {
            return insuredList.get(0);
        } else {

            log.warn("Multiple insured persons found with the same {} : {}",
                    identifierType, insuredList.stream().map(Insured::getInsuredUuid).collect(Collectors.joining(", ")));

            throw new BadRequestException("Multiple insured persons found with the same " + identifierType +
                    " (" + identifier + "). Please use a more specific identifier.");
        }
    }

    private EligibilityResponse checkEligibility(Insured insured, String providerUuid) {
        log.info("Checking eligibility for insured: {}, provider: {}", insured.getInsuredUuid(), providerUuid);

        InsuredSearchResponse insuredSearchResponse = convertToInsuredSearchResponse(insured);
        log.info("Converted InsuredSearchResponse: {}", insuredSearchResponse);

        try {
            log.info("Calling eligibilityService.checkEligibilityForInsured");
            ResponseEntity<EligibilityResponse> eligibilityResponseEntity =
                    eligibilityService.checkEligibilityForInsured(providerUuid, insuredSearchResponse, null);
            log.info("Received eligibility response entity: {}", eligibilityResponseEntity);

            if (eligibilityResponseEntity == null) {
                log.error("Eligibility response entity is null");
                throw new BadRequestException("Failed to retrieve eligibility information: null response");
            }

            EligibilityResponse eligibilityResponse = eligibilityResponseEntity.getBody();
            log.info("Extracted eligibility response body: {}", eligibilityResponse);

            if (eligibilityResponse == null) {
                log.error("Eligibility response body is null");
                throw new BadRequestException("Failed to retrieve eligibility information: null body");
            }

            if (!eligibilityResponse.isEligible()) {
                log.warn("Patient is not eligible. Reason: {}", eligibilityResponse.getIneligibilityReason());
                throw new BadRequestException("Patient is not eligible for services: " + eligibilityResponse.getIneligibilityReason());
            }

            log.info("Eligibility check completed successfully");
            return eligibilityResponse;
        } catch (Exception e) {
            log.error("Error during eligibility check", e);
            throw new BadRequestException("Error during eligibility check: " + e.getMessage());
        }
    }

    private InsuredSearchResponse convertToInsuredSearchResponse(Insured insured) {
        InsuredSearchResponse response = new InsuredSearchResponse();
        response.setInsuredUuid(insured.getInsuredUuid());
        response.setFirstName(insured.getFirstName());
        response.setFatherName(insured.getFatherName());
        response.setGrandFatherName(insured.getGrandFatherName());
        //response.setInsuranceId(insured.getInsuranceId());
        response.setEmployeeId(insured.getEmployeeId());
        response.setNationalId(insured.getNationalId());
        response.setPhone(insured.getPhone());
        response.setPayerUuid(insured.getPayerUuid());
        response.setPayerName(insured.getPayer().getPayerName());
        response.setStatus(insured.getStatus());

        return response;
    }

    private MedicationDispensing createDispensingRecord(KenemaPharmacyDispensingRequest request,
                                                        Insured insured,
                                                        Provider provider,
                                                        EligibilityResponse eligibilityResponse) {

        MedicationDispensing dispensing = new MedicationDispensing();
        dispensing.setInvoiceNumber(generateInvoiceNumber());
        dispensing.setDispensingUuid(UUID.randomUUID().toString());
        dispensing.setProviderUuid(provider.getProviderUuid());
        dispensing.setPayerUuid(insured.getPayer().getPayerUuid());
        dispensing.setInsuredUuid(insured.getInsuredUuid());
        dispensing.setDispensingDate(request.getDispensedDate());
        dispensing.setPrescribingPhysicianName(request.getPhysicianFullName());
        dispensing.setBranchName(request.getProviderBranchName());
        dispensing.setRecordedAt(LocalDate.now());
        dispensing.setSource(SourceType.SYSTEM);
        dispensing.setClaimStatus("DRAFT");
        dispensing.setStatus(MedicationStatus.DRAFT);

        double calculatedTotal = request.getPrescriptionDetails().stream()
                .mapToDouble(detail -> detail.getPrice() * detail.getQuantity())
                .sum();

        dispensing.setTotalAmount(calculatedTotal);

        calculateTotals(dispensing, eligibilityResponse);

        return dispensing;
    }

    private void calculateTotals(MedicationDispensing dispensing, EligibilityResponse eligibilityResponse) {
        double totalAmount = dispensing.getTotalAmount();

        double coveragePercentage = 100.0;

        if (eligibilityResponse != null && eligibilityResponse.getRequestedService() != null) {
            coveragePercentage = 100.0 - eligibilityResponse.getRequestedService().getCoPaymentPercentage();
        }

        double insuranceCoverage = totalAmount * (coveragePercentage / 100.0);
        double patientResponsibility = totalAmount - insuranceCoverage;

        dispensing.setInsuranceCoverage(insuranceCoverage);
        dispensing.setPatientResponsibility(patientResponsibility);

        log.info("Financial Breakdown - Total: {}, Coverage: {}%, Insurance Pays: {}, Patient Pays: {}",
                totalAmount, coveragePercentage, insuranceCoverage, patientResponsibility);
    }

    private List<MedicationDispensingItem> createDispensingItems(KenemaPharmacyDispensingRequest request, MedicationDispensing savedDispensing) {
        List<MedicationDispensingItem> items = new ArrayList<>();

        double totalAmount = 0;

        for (KenemaPharmacyDispensingRequest.PrescriptionDetail prescriptionDetail : request.getPrescriptionDetails()) {
            MedicationDispensingItem item = createDispensingItem(prescriptionDetail, savedDispensing);

            double totalPrice = item.getQuantity() * item.getUnitPrice();
            item.setTotalPrice(totalPrice);

            totalAmount += totalPrice;

            items.add(item);
        }

        savedDispensing.setTotalAmount(totalAmount);
        return items;
    }

    private Drug findOrCreateDrug(KenemaPharmacyDispensingRequest.PrescriptionDetail prescriptionDetail) {
        Optional<Drug> existingDrug = drugRepository.findByDrugName(prescriptionDetail.getMedicationName());

        if (existingDrug.isPresent()) {
            return existingDrug.get();
        } else {
            Drug newDrug = new Drug();
            newDrug.setDrugName(prescriptionDetail.getMedicationName());
            newDrug.setDosage(prescriptionDetail.getDosage().toString());
            newDrug.setRoute(prescriptionDetail.getRoute());
            newDrug.setPrice(prescriptionDetail.getPrice());
            newDrug.setStatus(Status.ACTIVE);

            return drugRepository.save(newDrug);
        }
    }

    private MedicationDispensingItem createDispensingItem(KenemaPharmacyDispensingRequest.PrescriptionDetail item, MedicationDispensing savedDispensing) {
        MedicationDispensingItem dispensingItem = new MedicationDispensingItem();
        dispensingItem.setItemUuid(UUID.randomUUID().toString());
        dispensingItem.setDispensing(savedDispensing);
        dispensingItem.setMedicationName(item.getMedicationName());
        dispensingItem.setQuantity(item.getQuantity().doubleValue());
        dispensingItem.setUnitOfMeasure(item.getUnitOfMeasure());
        dispensingItem.setUnitPrice(item.getPrice());
        dispensingItem.setTotalPrice(item.getPrice() * item.getQuantity());
        dispensingItem.setDosageInstructions(item.getDosage() + " " + item.getFrequency() + " for " + item.getDuration());
        dispensingItem.setRoute(item.getRoute());
        dispensingItem.setItemType(ItemType.DRUG);

        List<ContractHeader> activeContracts = contractHeaderRepository.findActiveContractsBetweenProviderAndPayer(
                savedDispensing.getProviderUuid(), savedDispensing.getPayerUuid(), Status.ACTIVE);

        if (activeContracts.isEmpty()) {
            throw new ResourceNotFoundException("Active contract", "provider and payer",
                    savedDispensing.getProviderUuid() + " and " + savedDispensing.getPayerUuid());
        }

        ContractHeader activeContract = activeContracts.get(0);

        ContractDetail contractDetail = resolveKenemaContractDetail(item, activeContract, savedDispensing);

        dispensingItem.setContractDetail(contractDetail);

        return dispensingItem;
    }

    /**
     * Resolve contract detail for Kenema prescription using drug-based resolution
     * Since external system doesn't provide serviceId or contractDetailUuid, we use drug-based approach
     */
    private ContractDetail resolveKenemaContractDetail(KenemaPharmacyDispensingRequest.PrescriptionDetail item,
                                                       ContractHeader activeContract,
                                                       MedicationDispensing savedDispensing) {

        log.info("Resolving contract detail using drug-based resolution for medication: {}", item.getMedicationName());

        Drug drug = findOrCreateDrug(item);

        ContractDetail contractDetail = contractDetailRepository.findByContractHeaderAndDrug(activeContract, drug)
                .orElseGet(() -> {
                    log.info("Creating new contract detail for drug: {}", drug.getDrugName());
                    ContractDetail newDetail = new ContractDetail();
                    newDetail.setContractHeader(activeContract);
                    newDetail.setContractHeaderUuid(activeContract.getContractHeaderUuid());
                    newDetail.setDrug(drug);
                    newDetail.setDrugUuid(drug.getDrugUuid());
                    newDetail.setNegotiatedPrice(item.getPrice());
                    newDetail.setStatus(Status.ACTIVE);
                    newDetail.setItemType("DRUG");
                    newDetail.setContractDetailUuid(UUID.randomUUID().toString());
                    return contractDetailRepository.save(newDetail);
                });

        return contractDetail;
    }


    private void calculateCoverageAndResponsibility(MedicationDispensing dispensingRecord, Payer payer, Insured insured) {

        Double totalAmount = dispensingRecord.getTotalAmount();

        if (totalAmount == null) {

            logger.error("Total amount is null for dispensing record: {}", dispensingRecord.getDispensingUuid());

            dispensingRecord.setInsuranceCoverage(0.0);
            dispensingRecord.setPatientResponsibility(0.0);
            return;
        }

        // Example: 100% coverage by insurance, 0% patient responsibility
        double coveragePercentage = 1.0;
        double insuranceCoverage = totalAmount * coveragePercentage;
        double patientResponsibility = totalAmount - insuranceCoverage;

        dispensingRecord.setInsuranceCoverage(insuranceCoverage);
        dispensingRecord.setPatientResponsibility(patientResponsibility);
    }


    private MedicationDispensing createDispensingRecord(DispensingRecordRequest request, Provider provider, Insured insured, Payer payer) {

        MedicationDispensing record = new MedicationDispensing();

        record.setDispensingUuid(UUID.randomUUID().toString());
        record.setInsured(insured);
        record.setInsuredUuid(insured.getInsuredUuid());
        record.setPayerUuid(payer.getPayerUuid());
        record.setProviderUuid(provider.getProviderUuid());
        record.setClaimStatus("DRAFT");
        record.setStatus(MedicationStatus.DRAFT);
        record.setSource(SourceType.INPUT);
        record.setInvoiceNumber(generateInvoiceNumber());
        record.setDispensingDate(LocalDate.now());
        record.setRecordedAt(LocalDate.now());
        record.setPharmacyTransactionId(insured.getIdNumber());
        record.setPrimaryDiagnosis(request.getPrimaryDiagnosis());
        record.setSecondaryDiagnosis(request.getSecondaryDiagnosis());

        if (request.getDependantUuid() != null && !request.getDependantUuid().isEmpty()) {
            Dependant dependant = insured.getDependants().stream()
                    .filter(d -> d.getDependantUuid().equals(request.getDependantUuid()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Dependant", "uuid", request.getDependantUuid()));
            record.setDependant(dependant);
        }

        return record;
    }

    private void updateDispensingRecordTotals(MedicationDispensing dispensingRecord, List<MedicationDispensingItem> items) {
        BigDecimal totalAmount = items.stream()
                .map(item -> BigDecimal.valueOf(item.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        dispensingRecord.setTotalAmount(totalAmount.doubleValue());

        //calculate insurance coverage and patient responsibility
        //TODO
        // let's assume a fixed 100% coverage
        BigDecimal insuranceCoverage = totalAmount;
        BigDecimal patientResponsibility = BigDecimal.ZERO;

        dispensingRecord.setInsuranceCoverage(totalAmount.doubleValue());
        dispensingRecord.setPatientResponsibility(patientResponsibility.doubleValue());
    }

    private List<Servicelist> validateServices(String providerUuid, List<DispensingRecordRequest.DispensingItemRequest> items, ContractHeader activeContract) {
        List<Servicelist> services = new ArrayList<>();
        log.info("Validating services for contract: {}", activeContract.getContractHeaderUuid());
        log.info("Number of contract details: {}", activeContract.getContractDetails().size());

        for (DispensingRecordRequest.DispensingItemRequest item : items) {
            Servicelist service;

            // Priority 1: Use serviceId if provided
            if (item.getServiceId() != null && !item.getServiceId().trim().isEmpty()) {
                log.info("Validating item with serviceId: {}", item.getServiceId());

                service = servicelistRepository.findByGeneratedServiceIdAndProviderProviderUuid(
                        item.getServiceId(), providerUuid);

                if (service == null) {
                    log.error("Service not found for serviceId: {} and providerUuid: {}", item.getServiceId(), providerUuid);
                    throw new ResourceNotFoundException("Service", "serviceId", item.getServiceId());
                }

                // Validate that this service is covered in the contract
                boolean isServiceInContract = activeContract.getContractDetails().stream()
                        .anyMatch(cd -> cd.getServiceUuid().equals(service.getServiceUuid()));

                if (!isServiceInContract) {
                    log.error("Service with serviceId: {} is not covered in contract: {}",
                            item.getServiceId(), activeContract.getContractHeaderUuid());
                    throw new ResourceNotFoundException("Service", "serviceId in contract", item.getServiceId());
                }

                log.info("Found Service by serviceId: {}", service.getServiceName());

            }
            // Priority 2: Fallback to contractDetailUuid if serviceId not provided
            else if (item.getContractDetailUuid() != null && !item.getContractDetailUuid().trim().isEmpty()) {
                log.info("Validating item with contractDetailUuid: {}", item.getContractDetailUuid());

                ContractDetail contractDetail = activeContract.getContractDetails().stream()
                        .filter(cd -> cd.getContractDetailUuid().equals(item.getContractDetailUuid()))
                        .findFirst()
                        .orElseThrow(() -> {
                            log.error("ContractDetail not found for uuid: {}", item.getContractDetailUuid());
                            return new ResourceNotFoundException("ContractDetail", "uuid", item.getContractDetailUuid());
                        });

                log.info("Found ContractDetail with serviceUuid: {}", contractDetail.getServiceUuid());

                service = servicelistRepository.findByServiceUuidAndProviderProviderUuid(
                        contractDetail.getServiceUuid(), providerUuid);

                if (service == null) {
                    log.error("Service not found for uuid: {} and providerUuid: {}", contractDetail.getServiceUuid(), providerUuid);
                    throw new ResourceNotFoundException("Service", "uuid", contractDetail.getServiceUuid());
                }

                log.info("Found Service by contractDetailUuid: {}", service.getServiceName());
            }
            // Error: Neither serviceId nor contractDetailUuid provided
            else {
                service = null;
                log.error("Neither serviceId nor contractDetailUuid provided for item");
                throw new IllegalArgumentException("Either serviceId or contractDetailUuid must be provided for each medication item");
            }

            services.add(service);
        }
        return services;
    }

    private List<MedicationDispensingItem> createDispensingItems(MedicationDispensing dispensingRecord,
                                                                 List<Servicelist> services,
                                                                 List<DispensingRecordRequest.DispensingItemRequest> itemRequests,
                                                                 Payer payer,
                                                                 ContractHeader activeContract,
                                                                 DispensingRecordRequest request) {
        List<MedicationDispensingItem> dispensingItems = new ArrayList<>();
        List<String> uncoveredItems = new ArrayList<>();

        log.info("Starting to create dispensing items for dispensing record: {}", dispensingRecord.getDispensingUuid());
        log.info("Number of item requests: {}", itemRequests.size());

        for (int i = 0; i < itemRequests.size(); i++) {
            DispensingRecordRequest.DispensingItemRequest itemRequest = itemRequests.get(i);

            // Get service from validated list or resolve from serviceId
            Servicelist service = null;
            if (!services.isEmpty() && i < services.size()) {
                service = services.get(i);
                log.info("Processing item request {} of {} with validated service", i + 1, itemRequests.size());
                log.info("Service: {}, Quantity: {}, Price: {}", service.getServiceName(), itemRequest.getQuantity(), itemRequest.getPrice());
            } else {
                // Service validation was skipped, resolve service from serviceId
                service = resolveServiceFromItemRequest(itemRequest, dispensingRecord.getProviderUuid());
                log.info("Processing item request {} of {} with serviceId resolution", i + 1, itemRequests.size());
                if (service != null) {
                    log.info("Resolved Service: {}, Quantity: {}, Price: {}", service.getServiceName(), itemRequest.getQuantity(), itemRequest.getPrice());
                }
            }

            try {
                // Skip processing if service couldn't be resolved
                if (service == null) {
                    log.error("Could not resolve service for item request {}", i + 1);
                    log.error("Item request details - ServiceId: {}, ContractDetailUuid: {}, ItemType: {}",
                            itemRequest.getServiceId(), itemRequest.getContractDetailUuid(), itemRequest.getItemType());
                    uncoveredItems.add("Item " + (i + 1) + ": Could not resolve service");
                    continue;
                }

                ContractDetail contractDetail = null;

                String contractDetailUuid = getContractDetailUuid(itemRequest, activeContract, null);

                if (contractDetailUuid != null) {
                    contractDetail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
                    log.info("Contract detail found using contractDetailUuid: {}", contractDetailUuid);
                } else {
                    Servicelist finalService = service;
                    contractDetail = contractDetailRepository.findByContractHeaderAndServiceUuid(activeContract, service.getServiceUuid())
                            .orElseThrow(() -> new ResourceNotFoundException("ContractDetail", "serviceUuid", finalService.getServiceUuid()));
                    log.info("Contract detail found using service UUID: {}", service.getServiceUuid());
                }

                if (contractDetail == null) {
                    throw new ResourceNotFoundException("ContractDetail", "contractDetailUuid or serviceUuid",
                            contractDetailUuid != null ? contractDetailUuid : service.getServiceUuid());
                }

                if (!isItemCoveredForInsured(contractDetail, dispensingRecord.getInsured())) {
                    uncoveredItems.add(service.getServiceUuid());
                    log.warn("Item not covered for insured: {}", service.getServiceUuid());
                    //continue;
                }

                MedicationDispensingItem item = new MedicationDispensingItem();
                item.setItemUuid(UUID.randomUUID().toString());
                item.setDispensing(dispensingRecord);
                item.setContractDetail(contractDetail);
                item.setQuantity((double) itemRequest.getQuantity());
                item.setUnitPrice(contractDetail.getNegotiatedPrice());
                item.setTotalPrice(itemRequest.getPrice() * itemRequest.getQuantity());
                item.setMedicationName(service.getServiceName());
                item.setMedicationCode(service.getServiceCode());
                item.setRemark(itemRequest.getRemark());
                item.setItemType(ItemType.SERVICE);

                log.info("Created MedicationDispensingItem: {}", item.getItemUuid());
                log.info("Item details - Name: {}, Quantity: {}, Unit Price: {}, Total Price: {}",
                        item.getMedicationName(), item.getQuantity(), item.getUnitPrice(), item.getTotalPrice());

                dispensingItems.add(item);
                dispensingRecord.getItems().add(item);

                log.info("Added item to dispensing record and dispensingItems list");
            } catch (ResourceNotFoundException e) {
                log.error("Contract detail not found for service: {}", service.getServiceUuid(), e);
            }
        }

        if (!uncoveredItems.isEmpty()) {
            log.warn("Some items are not covered for the insured: {}", String.join(", ", uncoveredItems));
        }

        log.info("Finished creating dispensing items. Total items requested: {}, Total items created: {}, Items skipped: {}",
                itemRequests.size(), dispensingItems.size(), uncoveredItems.size());
        log.info("Dispensing record now has {} items", dispensingRecord.getItems().size());

        if (dispensingItems.isEmpty()) {
            log.error("WARNING: No dispensing items were created! This will cause issues with external processing.");
            log.error("Uncovered items: {}", uncoveredItems);
        }

        return dispensingItems;
    }

    private boolean isItemCoveredForInsured(ContractDetail contractDetail, Insured insured) {
        log.info("Checking coverage for insured: {}", insured.getInsuredUuid());
        log.info("Contract detail: {}", contractDetail.getContractDetailUuid());
        log.info("Insured's EmployeeDependantGroup: {}", insured.getEmployeeDependantGroup() != null ? insured.getEmployeeDependantGroup().getGroupUuid() : "null");

        boolean covered = contractDetail.getEmployeeDependantGroups().stream()
                .anyMatch(group -> {
                    boolean groupMatch = group.getInsureds().contains(insured) || insured.getEmployeeDependantGroup().equals(group);
                    log.info("Group: {}, Contains insured: {}, Matches insured's group: {}",
                            group.getGroupUuid(),
                            group.getInsureds().contains(insured),
                            insured.getEmployeeDependantGroup().equals(group));
                    return groupMatch;
                });

        log.info("Item covered: {}", covered);
        return covered;
    }

    private ContractDetail findContractDetail(String contractHeaderUuid, String serviceUuid, Insured insured) {

        if (insured.getEmployeeDependantGroup() != null) {
            ContractDetail detail = contractDetailRepository.findByContractHeaderUuidAndServiceUuidAndEmployeeDependantGroups(
                    contractHeaderUuid, serviceUuid, insured.getEmployeeDependantGroup());
            if (detail != null) {
                return detail;
            }
        }

        return contractDetailRepository.findByContractHeaderUuidAndServiceUuid(contractHeaderUuid, serviceUuid);
    }

    private PendingDispensingRecordDTO convertToDTO(MedicationDispensing dispensing) {

        PendingDispensingRecordDTO dto = new PendingDispensingRecordDTO();

        dto.setDispensingUuid(dispensing.getDispensingUuid());
        dto.setCreatedAt(dispensing.getRecordedAt());
        dto.setBranchName(dispensing.getBranchName());
        dto.setStatus(dispensing.getStatus());
        dto.setInvoiceNumber(dispensing.getInvoiceNumber());
        dto.setDispensingDate(dispensing.getDispensingDate());
        dto.setPrescriptionNumber(dispensing.getPrescriptionNumber());
        dto.setPharmacyTransactionId(dispensing.getPharmacyTransactionId());
        dto.setTotalAmount(dispensing.getTotalAmount());
        dto.setPatientResponsibility(dispensing.getPatientResponsibility());
        dto.setInsuranceCoverage(dispensing.getInsuranceCoverage());
        dto.setPayerUuid(dispensing.getPayerUuid());
        dto.setStatus(dispensing.getStatus());
        dto.setClaimStatus(ClaimStatus.valueOf(dispensing.getClaimStatus()));
        dto.setSource(dispensing.getSource());

        Insured insured = null;

        if (dispensing.getInsured() != null) {
            insured = dispensing.getInsured();
        } else if (dispensing.getInsuredUuid() != null) {
            insured = insuredRepository.findByInsuredUuid(dispensing.getInsuredUuid());
        }

        if (insured != null) {
            String fullName = Stream.of(insured.getFirstName(), insured.getFatherName(), insured.getGrandFatherName())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
            dto.setPatientName(fullName.trim());
            dto.setInsuranceId(insured.getEmployeeId() != null ? insured.getEmployeeId() : insured.getInsuranceId());
        } else {
            dto.setPatientName("Unknown");
            dto.setInsuranceId("N/A");
        }

        Payer payer = payerRepository.findByPayerUuid(dispensing.getPayerUuid());
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "uuid", dispensing.getPayerUuid());
        }
        dto.setPayerName(payer.getPayerName());

        List<MedicationDispensingItem> items = dispensingItemRepository.findByDispensing(dispensing);
        dto.setMedicationItems(items.stream().map(this::convertToItemDTO).collect(Collectors.toList()));

        return dto;
    }

    private String generateInvoiceNumber() {
        int randomNum = 100000000 + new Random().nextInt(900000000);
        return "CR-" + randomNum;
    }

    private PendingDispensingRecordDTO.MedicationItemDTO convertToItemDTO(MedicationDispensingItem item) {
        PendingDispensingRecordDTO.MedicationItemDTO itemDTO = new PendingDispensingRecordDTO.MedicationItemDTO();
        itemDTO.setMedicationName(item.getMedicationName());
        itemDTO.setQuantity(item.getQuantity());
        itemDTO.setUnitOfMeasure(item.getUnitOfMeasure());
        itemDTO.setUnitPrice(item.getUnitPrice());
        itemDTO.setTotalPrice(item.getTotalPrice());
        return itemDTO;
    }

    @Override
    @Transactional
    public ResponseEntity<?> createClaimFromDispensingRecords(String providerUuid, String[] dispensingUuids) {
        if (dispensingUuids == null || dispensingUuids.length == 0) {
            throw new BadRequestException("No dispensing records selected");
        }

        Provider provider = validateProvider(providerUuid);

        List<MedicationDispensing> dispensingRecords = fetchAndValidateDispensingRecords(providerUuid, dispensingUuids);

        Payer payer = validatePayer(dispensingRecords.get(0).getPayerUuid());
        Insured insured = validateInsured(dispensingRecords.get(0).getInsuredUuid());

        Claim claim = createClaim(provider, payer, insured, dispensingRecords);
        Claim savedClaim = claimRepository.save(claim);

        List<ClaimItem> claimItems = createClaimItems(dispensingRecords, savedClaim);
        claimItemRepository.saveAll(claimItems);

        updateDispensingRecords(dispensingRecords, savedClaim);

        return ResponseEntity.ok(savedClaim);
    }

    private Insured validateInsured(String insuredUuid) {
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null) {
            throw new RuntimeException("Insured not found");
        }

        return insured;
    }

    private List<MedicationDispensing> fetchAndValidateDispensingRecords(String providerUuid, String[] dispensingUuids) {
        List<MedicationDispensing> dispensingRecords = dispensingRepository.findByDispensingUuidIn(dispensingUuids);

        if (dispensingRecords.isEmpty()) {
            throw new ResourceNotFoundException("Dispensing Records", "uuids", String.join(", ", dispensingUuids));
        }

        String payerUuid = dispensingRecords.get(0).getPayerUuid();
        String insuredUuid = dispensingRecords.get(0).getInsuredUuid();

        for (MedicationDispensing record : dispensingRecords) {
            if (!record.getProviderUuid().equals(providerUuid)) {
                throw new BadRequestException("All dispensing records must belong to the same provider");
            }

            if (!record.getPayerUuid().equals(payerUuid)) {
                throw new BadRequestException("All dispensing records must belong to the same payer");
            }

            if (!record.getInsuredUuid().equals(insuredUuid)) {
                throw new BadRequestException("All dispensing records must belong to the same insured person");
            }

            if (!"DRAFT".equals(record.getClaimStatus())) {
                throw new BadRequestException("Dispensing record " + record.getDispensingUuid() +
                        " has already been included in a claim");
            }
        }

        return dispensingRecords;
    }

    private Claim createClaim(Provider provider, Payer payer, Insured insured, List<MedicationDispensing> dispensingRecords) {
        Claim claim = new Claim();
        claim.setClaimUuid(UUID.randomUUID().toString());

        claim.setSubmissionDate(LocalDateTime.now());
        claim.setStatus(ClaimStatus.SUBMITTED);

        double totalAmount = dispensingRecords.stream().mapToDouble(MedicationDispensing::getTotalAmount).sum();
        claim.setTotalAmount(BigDecimal.valueOf(totalAmount));
        claim.setClaimType("PHARMACY");
        claim.setServiceDate(dispensingRecords.get(0).getDispensingDate());

        return claim;
    }

    private List<ClaimItem> createClaimItems(List<MedicationDispensing> dispensingRecords, Claim savedClaim) {
        List<ClaimItem> claimItems = new ArrayList<>();

        for (MedicationDispensing dispensing : dispensingRecords) {
            List<MedicationDispensingItem> medicationItems = dispensingItemRepository.findByDispensing(dispensing);

            for (MedicationDispensingItem medicationItem : medicationItems) {
                ClaimItem claimItem = createClaimItem(medicationItem, savedClaim, dispensing);
                claimItems.add(claimItem);
            }
        }

        return claimItems;
    }

    private ClaimItem createClaimItem(MedicationDispensingItem medicationItem, Claim savedClaim, MedicationDispensing dispensing) {
        ClaimItem claimItem = new ClaimItem();
        claimItem.setItemUuid(UUID.randomUUID().toString());
        claimItem.setClaim(savedClaim);
        claimItem.setServiceCode(medicationItem.getMedicationCode());
        claimItem.setServiceName(medicationItem.getMedicationName());
        claimItem.setQuantity(medicationItem.getQuantity());
        claimItem.setUnitPrice(medicationItem.getUnitPrice());
        claimItem.setTotalPrice(medicationItem.getTotalPrice());
        claimItem.setItemType("MEDICATION");
        claimItem.setItemDescription(
                medicationItem.getMedicationName() + " " +
                        (medicationItem.getStrength() != null ? medicationItem.getStrength() : "") + " " +
                        (medicationItem.getFormulation() != null ? medicationItem.getFormulation() : "")
        );

        double itemCoveragePercent = dispensing.getInsuranceCoverage() / dispensing.getTotalAmount() * 100;
        double itemCoverage = medicationItem.getTotalPrice() * (itemCoveragePercent / 100);
        double itemPatientPortion = medicationItem.getTotalPrice() - itemCoverage;

        claimItem.setInsuranceCoverage(itemCoverage);
        claimItem.setPatientResponsibility(itemPatientPortion);

        return claimItem;

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDispensingRecords(List<MedicationDispensing> dispensingRecords, Claim savedClaim) {
        for (MedicationDispensing dispensing : dispensingRecords) {
            try {
                int updatedRows = dispensingRepository.updateClaimStatus(
                        dispensing.getDispensingUuid(),
                        "CLAIMED",
                        savedClaim.getClaimUuid()
                );

                if (updatedRows == 0) {
                    throw new OptimisticLockingFailureException("Dispensing record " + dispensing.getDispensingUuid() + " was updated by another transaction");
                }
            } catch (OptimisticLockingFailureException e) {
                log.error("Optimistic locking failure for dispensing record: " + dispensing.getDispensingUuid(), e);
                throw new BadRequestException("Unable to update dispensing record " + dispensing.getDispensingUuid() + ". Please try again.");
            }
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> authorizeDispensingRecord(String dispensingUuid) {
        MedicationDispensing record = dispensingRepository.findByDispensingUuid(dispensingUuid);
        if (record == null) {
            throw new ResourceNotFoundException("Dispensing Record", "uuid", dispensingUuid);
        }

        if (!"DRAFT".equals(record.getClaimStatus())) {
            throw new BadRequestException("Dispensing record " + dispensingUuid + " is not in DRAFT status");
        }

        record.setClaimStatus("AUTHORIZED");
        dispensingRepository.save(record);

        return ResponseEntity.ok(new MessageResponse("Dispensing record has been authorized"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> authorizeDispensingRecords(String[] dispensingUuids) {
        if (dispensingUuids == null || dispensingUuids.length == 0) {
            throw new BadRequestException("No dispensing records selected");
        }

        List<MedicationDispensing> dispensingRecords = dispensingRepository.findByDispensingUuidIn(dispensingUuids);

        for (MedicationDispensing record : dispensingRecords) {
            if (!"DRAFT".equals(record.getClaimStatus())) {
                throw new BadRequestException("Dispensing record " + record.getDispensingUuid() +
                        " is not in DRAFT status");
            }
            record.setClaimStatus("AUTHORIZED");
        }

        dispensingRepository.saveAll(dispensingRecords);

        return ResponseEntity.ok(new MessageResponse("Selected dispensing records have been authorized"));
    }


    @Override
    @Transactional
    public ResponseEntity<?> createClaimFromAuthorizedRecord(String providerUuid, String dispensingUuid) {
        Provider provider = validateProvider(providerUuid);

        MedicationDispensing record = dispensingRepository.findByDispensingUuid(dispensingUuid);
        if (record == null) {
            throw new ResourceNotFoundException("Dispensing Record", "uuid", dispensingUuid);
        }

        if (!"AUTHORIZED".equals(record.getClaimStatus())) {
            throw new BadRequestException("Dispensing record " + dispensingUuid + " is not in AUTHORIZED status");
        }

        Payer payer = validatePayer(record.getPayerUuid());
        Insured insured = validateInsured(record.getInsuredUuid());

        Claim claim = createClaim(provider, payer, insured, Collections.singletonList(record));
        Claim savedClaim = claimRepository.save(claim);

        List<ClaimItem> claimItems = createClaimItems(Collections.singletonList(record), savedClaim);
        claimItemRepository.saveAll(claimItems);

        record.setClaimStatus("SUBMITTED");
        record.setClaimUuid(savedClaim.getClaimUuid());
        dispensingRepository.save(record);

        return ResponseEntity.ok(savedClaim);

    }

    @Override
    @Transactional
    public ResponseEntity<?> createClaimFromAuthorizedRecords(String providerUuid, String[] dispensingUuids) {
        if (dispensingUuids == null || dispensingUuids.length == 0) {
            throw new BadRequestException("No dispensing records selected");
        }

        Provider provider = validateProvider(providerUuid);

        List<MedicationDispensing> dispensingRecords = fetchAndValidateDispensingRecords(providerUuid, dispensingUuids);

        for (MedicationDispensing record : dispensingRecords) {
            if (!"AUTHORIZED".equals(record.getClaimStatus())) {
                throw new BadRequestException("Dispensing record " + record.getDispensingUuid() +
                        " is not in AUTHORIZED status");
            }
        }

        Payer payer = validatePayer(dispensingRecords.get(0).getPayerUuid());
        Insured insured = validateInsured(dispensingRecords.get(0).getInsuredUuid());

        Claim claim = createClaim(provider, payer, insured, dispensingRecords);
        Claim savedClaim = claimRepository.save(claim);

        List<ClaimItem> claimItems = createClaimItems(dispensingRecords, savedClaim);
        claimItemRepository.saveAll(claimItems);

        for (MedicationDispensing record : dispensingRecords) {
            record.setClaimStatus("SUBMITTED");
            record.setClaimUuid(savedClaim.getClaimUuid());
        }
        dispensingRepository.saveAll(dispensingRecords);

        return ResponseEntity.ok(savedClaim);

    }

    @Override
    @Transactional
    public ResponseEntity<ReconciliationResponse> reconcilePayment(String claimUuid) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        if (!claim.getStatus().equals(ClaimStatus.PAID)) {
            throw new BadRequestException("Only paid claims can be reconciled");
        }

        claim.setStatus(ClaimStatus.RECONCILED);

        List<MedicationDispensing> dispensingRecords = dispensingRepository.findByClaimUuid(claimUuid);

        BatchRecord batchRecord = createBatchRecord(claim.getBatchRecord().getMedicationDispensing().get(0).getInsured().getPayer(), dispensingRecords, claim);
        batchRecord.setStatus("RECONCILED");
        batchRecord.setClaim(claim);
        claim.setBatchRecord(batchRecord);

        claimRepository.save(claim);
        BatchRecord savedBatchRecord = batchRecordRepository.save(batchRecord);

        createClaimLog(claim, SecurityUtils.getAuthenticatedUser(), ClaimStatus.PAID, ClaimStatus.RECONCILED,
                "Claim reconciled by pharmacy");

        ReconciliationResponse response = new ReconciliationResponse();
        BeanUtils.copyProperties(savedBatchRecord, response);
        response.setMessage("Claim reconciled successfully");

        return ResponseEntity.ok(response);

    }

    private void createClaimLog(Claim claim, UserPrincipal user, ClaimStatus previousStatus, ClaimStatus newStatus, String comment) {

        ClaimLogs log = new ClaimLogs();
        log.setLogUuid(UUID.randomUUID().toString());
        log.setClaim(claim);
        log.setActionByUuid(user.getUserUuid());
        log.setActionByName(user.getFirstName() + " " + user.getFatherName());
        log.setActionByRole(user.getAuthorities().iterator().next().getAuthority());
        log.setComment(comment);
        log.setActionDate(Instant.now());
        log.setActionStatus(newStatus.toString());
        log.setPreviousStatus(previousStatus.toString());
        claimLogsRepository.save(log);

    }

}