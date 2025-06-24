package com.medco.HealthConnectProvider.services.impl.integration;


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
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import com.medco.HealthConnectProvider.utils.enums.ItemType;
import com.medco.HealthConnectProvider.utils.enums.SourceType;
import com.medco.HealthConnectProvider.utils.enums.Status;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PharmacyIntegrationServiceImpl implements PharmacyIntegrationService {

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

    private Provider validateProvider(String providerUuid) {
        return providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", "providerUuid", providerUuid));
    }

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
    public ResponseEntity<Page<PendingDispensingRecordDTO>> getDispensingRecords(
            String providerUuid, String search, String status, LocalDate startDate, LocalDate endDate,
            String payerUuid, int page, int size, String sortBy, String sortDirection) {

        log.info("Fetching dispensing records with advanced search for provider: {}", providerUuid);

        try {
            providerRepository.findByProviderUuid(providerUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Provider", "providerUuid", providerUuid));

            Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.fromString(sortDirection), sortBy));

            Specification<MedicationDispensing> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();

                predicates.add(cb.equal(root.get("providerUuid"), providerUuid));

                if (status != null && !status.isEmpty()) {
                    predicates.add(cb.equal(root.get("claimStatus"), status));
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

            log.info("Executed query: {}", dispensingRecords);
            log.info("Found {} dispensing records", dispensingRecords.getTotalElements());

            Page<PendingDispensingRecordDTO> dtoPage = dispensingRecords.map(this::convertToDTO);

            Page<PendingDispensingRecordDTO> adjustedPage = new PageImpl<>(
                    dtoPage.getContent(),
                    PageRequest.of(dtoPage.getNumber() + 1, dtoPage.getSize(), dtoPage.getSort()),
                    dtoPage.getTotalElements()
            );

            return ResponseEntity.ok(adjustedPage);
        } catch (ResourceNotFoundException e) {
            log.error("Provider not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching dispensing records", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> addDrugDispensingRecord(DrugDispensingRecordRequest request) {

        try {
            Provider provider = validateProvider(request.getProviderUuid());
            Payer payer = validatePayer(request.getPayerUuid());
            Insured insured = findInsuredPersonFor(request);
            log.info("Insured person found: {}", insured.getFirstName());

            MedicationDispensing dispensingRecord = createDrugDispensingRecord(request, insured, payer, provider);

            List<Drug> drugs = validateDrugs(provider.getProviderUuid(), request.getDrugItems());
            List<MedicationDispensingItem> dispensingItems = createDrugDispensingItems(dispensingRecord, drugs, request.getDrugItems());

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
            if (e.getCause() instanceof ConstraintViolationException) {
                ConstraintViolationException cve = (ConstraintViolationException) e.getCause();
                if (cve.getConstraintViolations().stream().anyMatch(v -> v.getPropertyPath().toString().equals("insurance_coverage"))) {
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
    public ResponseEntity<List<MedicationDispensingDTO>> getMedicationsByBatchCode(String batchCode) {
        List<MedicationDispensing> dispensings = dispensingRepository.findByBatchCode(batchCode);

        if (dispensings.isEmpty()) {
            throw new ResourceNotFoundException("Medications", "batchCode", batchCode);
        }

        List<MedicationDispensingDTO> medicationDTOs = dispensings.stream()
                .map(this::convertToMedicationDispensingDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(medicationDTOs);
    }

    private MedicationDispensingDTO convertToMedicationDispensingDTO(MedicationDispensing dispensing) {
        MedicationDispensingDTO dto = new MedicationDispensingDTO();
        BeanUtils.copyProperties(dispensing,dto);


        // Fetch additional information
        Optional<Provider> providerOptional = providerRepository.findByProviderUuid(dispensing.getProviderUuid());
        providerOptional.ifPresent(provider -> dto.setProviderName(provider.getProviderName()));

        Payer payer = payerRepository.findByPayerUuid(dispensing.getPayerUuid());
        if (payer != null) {
            dto.setPayerName(payer.getPayerName());
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

    private MedicationDispensingDTO.MedicationItemDTO convertToMedicationItemDTO(MedicationDispensingItem item) {
        MedicationDispensingDTO.MedicationItemDTO itemDTO = new MedicationDispensingDTO.MedicationItemDTO();
        BeanUtils.copyProperties(item,itemDTO);

       // itemDTO.setItemType(item.getItemType().toString());

        return itemDTO;
    }

    private Insured findInsuredPersonFor(DrugDispensingRecordRequest request) {
        String patientId = request.getPhone();

        Insured insured = insuredRepository.findByInsuranceId(patientId);

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

        if (insured == null) {
            throw new ResourceNotFoundException("Insured", "patientId", patientId);
        }

        return insured;

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

    private MedicationDispensing createDrugDispensingRecord(DrugDispensingRecordRequest request, Insured insured, Payer payer, Provider provider) {
        MedicationDispensing dispensing = new MedicationDispensing();
        dispensing.setDispensingUuid(UUID.randomUUID().toString());
        dispensing.setProviderUuid(provider.getProviderUuid());
        dispensing.setPayerUuid(payer.getPayerUuid());
        dispensing.setInsuredUuid(insured.getInsuredUuid());
        dispensing.setDispensingDate(request.getDispensingDate());
        dispensing.setPrescriptionNumber(request.getPrescriptionNumber());
        dispensing.setPharmacyTransactionId(request.getPharmacyTransactionId());
        dispensing.setClaimStatus("DRAFT");
        dispensing.setSource(SourceType.INPUT);
        dispensing.setInvoiceNumber(generateInvoiceNumber());

        dispensing.setRecordedAt(LocalDate.now());
        return dispensing;

    }

    private List<MedicationDispensingItem> createDrugDispensingItems(MedicationDispensing dispensingRecord, List<Drug> drugs, List<DrugDispensingRecordRequest.DrugDispensingItemRequest> drugItems) {

        List<MedicationDispensingItem> dispensingItems = new ArrayList<>();
        for (int i = 0; i < drugs.size(); i++) {
            Drug drug = drugs.get(i);
            DrugDispensingRecordRequest.DrugDispensingItemRequest item = drugItems.get(i);

            MedicationDispensingItem dispensingItem = new MedicationDispensingItem();
            dispensingItem.setItemUuid(UUID.randomUUID().toString());
            dispensingItem.setDispensing(dispensingRecord);
            dispensingItem.setMedicationCode(drug.getDrugCode());
            dispensingItem.setMedicationName(drug.getDrugName());
            dispensingItem.setQuantity(item.getQuantity());
            dispensingItem.setTotalPrice(item.getTotalPrice());
            dispensingItem.setFormulation(drug.getFormulation());
            dispensingItem.setRoute(item.getRoute());
            dispensingItem.setItemType(ItemType.DRUG);

            String dosageInstructions = String.format("%s %s for %s", item.getDose(), item.getFrequency(), item.getDuration());
            dispensingItem.setDosageInstructions(dosageInstructions);

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

        Provider provider = providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", "uuid", providerUuid));

        List<MedicationDispensing> dispensingRecords = dispensingRepository.findByDispensingUuidIn(dispensingUuids);

        if (dispensingRecords.size() != dispensingUuids.length) {
            throw new RuntimeException("One or more dispensing records not found");
        }

        BatchRecord batchRecord = null;
        String message = "";

        if (newStatus.equals("SUBMITTED")) {
            for (MedicationDispensing record : dispensingRecords) {
                if (!record.getClaimStatus().equals("DRAFT")) {
                    throw new BadRequestException("Dispensing record " + record.getDispensingUuid() +
                            " is not in DRAFT status");
                }
            }

            // Create batch record
            String payerUuid = dispensingRecords.get(0).getPayerUuid();
            Payer payer = payerRepository.findByPayerUuid(payerUuid);
            if (payer == null) {
                throw new ResourceNotFoundException("Payer", "uuid", payerUuid);
            }

            batchRecord = createBatchRecord(payer, dispensingRecords, null);
            batchRecord = batchRecordRepository.save(batchRecord);

            // Update dispensing records
            for (MedicationDispensing record : dispensingRecords) {
                record.setClaimStatus("SUBMITTED");
                record.setBatchCode(batchRecord.getBatchCode());
            }

            message = dispensingRecords.size() + " dispensing record(s) updated to SUBMITTED status. " +
                    "Batch created with code: " + batchRecord.getBatchCode();

        } else {
            String batchCode = null;
            for (MedicationDispensing record : dispensingRecords) {
                if (!record.getClaimStatus().equals("SUBMITTED")) {
                    throw new BadRequestException("Dispensing record " + record.getDispensingUuid() +
                            " is not in SUBMITTED status");
                }
                if (batchCode == null) {
                    batchCode = record.getBatchCode();
                } else if (!batchCode.equals(record.getBatchCode())) {
                    throw new BadRequestException("All records must belong to the same batch");
                }
            }

            // Update dispensing records
            for (MedicationDispensing record : dispensingRecords) {
                record.setClaimStatus("AUTHORIZED");
            }

            // Update batch record status
            String finalBatchCode = batchCode;
            batchRecord = batchRecordRepository.findByBatchCode(batchCode)
                    .orElseThrow(() -> new ResourceNotFoundException("BatchRecord", "batchCode", finalBatchCode));
            batchRecord.setStatus("AUTHORIZED");
            batchRecordRepository.save(batchRecord);

            message = dispensingRecords.size() + " dispensing record(s) updated to AUTHORIZED status. " +
                    "Batch " + batchCode + " updated to AUTHORIZED status.";
        }

        dispensingRepository.saveAll(dispensingRecords);

        return ResponseEntity.ok(new MessageResponse(message));
    }

    private BatchRecord createBatchRecord(Payer payer, List<MedicationDispensing> dispensingRecords, Claim claim) {
        BatchRecord batchRecord = new BatchRecord();
        batchRecord.setBatchCode(generateBatchCode());
        batchRecord.setPayerName(payer.getPayerName());
        batchRecord.setRequestedOn(LocalDateTime.now());
        batchRecord.setClaimDatingFrom(dispensingRecords.stream()
                .map(MedicationDispensing::getDispensingDate)
                .min(LocalDate::compareTo)
                .orElse(null));
        batchRecord.setClaimDatingTo(dispensingRecords.stream()
                .map(MedicationDispensing::getDispensingDate)
                .max(LocalDate::compareTo)
                .orElse(null));

        // Calculate total amount from dispensing records
        double totalAmount = dispensingRecords.stream()
                .mapToDouble(MedicationDispensing::getTotalAmount)
                .sum();
        batchRecord.setTotalAmount(BigDecimal.valueOf(totalAmount));

        batchRecord.setStatus("PENDING");
        return batchRecord;
    }

    private String generateBatchCode() {
        return "BATCH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    @Transactional
    public ResponseEntity<?> addDispensingRecord(DispensingRecordRequest request) {
        try {
            Provider provider = validateProvider(request.getProviderUuid());
            Payer payer = validatePayer(request.getPayerUuid());
            Insured insured = findInsuredPerson(request);
            logger.info("Insured person found: {}", insured.getFirstName());

            MedicationDispensing dispensingRecord = createDispensingRecord(request, insured, payer, provider);

            List<Servicelist> services = validateServices(provider.getProviderUuid(), request.getMedicationItems());
            List<MedicationDispensingItem> dispensingItems = createDispensingItems(dispensingRecord, services, request.getMedicationItems());

            MedicationDispensing savedRecord = dispensingRepository.save(dispensingRecord);

            dispensingItemRepository.saveAll(dispensingItems);

            updateDispensingRecordTotals(savedRecord, dispensingItems);

            savedRecord = dispensingRepository.save(savedRecord);

            return ResponseEntity.ok(new DispensingRecordResponse(savedRecord));

        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiErrorResponse(e.getMessage()));
        } catch (BadRequestException e) {
            logger.error("Bad request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiErrorResponse(e.getMessage()));
        } catch (DataIntegrityViolationException e) {
            logger.error("Data integrity violation: {}", e.getMessage());
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
            logger.error("Unexpected error in addDispensingRecord: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse("An unexpected error occurred. Please try again later."));
        }
    }

    @Override
    @Transactional
    public ResponseEntity<DispensingResponse> recordMedicationDispensing(KenemaPharmacyDispensingRequest request) {
        log.info("Recording medication dispensing from Kenema pharmacy: {}", request.getIdentifier());

        Provider provider = (Provider) providerRepository.findByProviderName(request.getProviderName())
                .orElseThrow(() -> new ResourceNotFoundException("Provider", "providerName", request.getProviderName()));

        Insured insured = findInsuredPerson(request);
        if (insured == null) {
            throw new ResourceNotFoundException("Insured", "provided identifiers", "Not found");
        }

       // validateUniqueTransaction(request.getMrn());

        EligibilityResponse eligibilityResponse = checkEligibility(insured, provider.getProviderUuid());

        MedicationDispensing dispensing = createDispensingRecord(request, insured, provider, eligibilityResponse);
        MedicationDispensing savedDispensing = dispensingRepository.save(dispensing);

        List<MedicationDispensingItem> items = createDispensingItems(request, savedDispensing);
        dispensingItemRepository.saveAll(items);

        DispensingResponse response = createDispensingResponse(savedDispensing);

        return ResponseEntity.ok(response);
    }


    private Insured findInsuredPerson(KenemaPharmacyDispensingRequest request) {
        if (request.getIdentifier() == null || request.getIdentifier().isEmpty()) {
            throw new BadRequestException("Identifier is required to find the insured person");
        }

        Insured insured = null;

        insured = insuredRepository.findByInsuranceId(request.getIdentifier());

        if (insured == null) {
            insured = insuredRepository.findByEmployeeId(request.getIdentifier());
        }

        if (insured == null) {
            insured = (Insured) insuredRepository.findByIdNumber(request.getIdentifier());
        }

        if (insured == null) {
            insured = insuredRepository.findByNationalId(request.getIdentifier());
        }

        if (insured == null) {
            insured = (Insured) insuredRepository.findByPhone(request.getIdentifier());
        }

        if (insured == null) {
            throw new ResourceNotFoundException("Insured Person", "identifier", request.getIdentifier());
        }

        return insured;
    }


    private EligibilityResponse checkEligibility(Insured insured, String providerUuid) {

        InsuredSearchResponse insuredSearchResponse = convertToInsuredSearchResponse(insured);

        ResponseEntity<EligibilityResponse> eligibilityResponseEntity =
                eligibilityService.checkEligibilityForInsured(providerUuid, insuredSearchResponse, null);

        EligibilityResponse eligibilityResponse = eligibilityResponseEntity.getBody();
        if (eligibilityResponse == null) {
            throw new BadRequestException("Failed to retrieve eligibility information");
        }

        if (!eligibilityResponse.isEligible()) {
            throw new BadRequestException("Patient is not eligible for services: " + eligibilityResponse.getIneligibilityReason());
        }

        return eligibilityResponse;
    }

    private InsuredSearchResponse convertToInsuredSearchResponse(Insured insured) {
        InsuredSearchResponse response = new InsuredSearchResponse();
        response.setInsuredUuid(insured.getInsuredUuid());
        response.setFirstName(insured.getFirstName());
        response.setFatherName(insured.getFatherName());
        response.setGrandFatherName(insured.getGrandFatherName());
        response.setInsuranceId(insured.getInsuranceId());
        response.setEmployeeId(insured.getEmployeeId());
        response.setNationalId(insured.getNationalId());
        response.setPhone(insured.getPhone());
        response.setPayerUuid(insured.getPayerUuid());
        response.setPayerName(insured.getPayer().getPayerName());
        response.setStatus(insured.getStatus());

        return response;
    }

    private MedicationDispensing createDispensingRecord(KenemaPharmacyDispensingRequest request, Insured insured, Provider provider, EligibilityResponse eligibilityResponse) {
        MedicationDispensing dispensing = new MedicationDispensing();

        dispensing.setInvoiceNumber(generateInvoiceNumber());
        dispensing.setDispensingUuid(UUID.randomUUID().toString());
        dispensing.setProviderUuid(provider.getProviderUuid());
        dispensing.setPayerUuid(insured.getPayer().getPayerUuid());
        dispensing.setInsuredUuid(insured.getInsuredUuid());
        dispensing.setPharmacyTransactionId(request.getIdentifier());
        dispensing.setDispensingDate(request.getDispensedDate());
        dispensing.setPrescribingPhysicianName(request.getPhysicianFullName());
        dispensing.setBranchName(request.getProviderBranchName());
        dispensing.setRecordedAt(LocalDate.now());
        dispensing.setSource(SourceType.SYSTEM);
        dispensing.setClaimStatus("DRAFT");

        calculateTotals(dispensing, request, eligibilityResponse);

        return dispensing;
    }

    private void calculateTotals(MedicationDispensing dispensing, KenemaPharmacyDispensingRequest request, EligibilityResponse eligibilityResponse) {
        double totalAmount = request.getTotalPrice();

        double coveragePercentage = 100.0; // Default coverage percentage
        if (eligibilityResponse != null && eligibilityResponse.getRequestedService() != null) {
            coveragePercentage = 100.0 - eligibilityResponse.getRequestedService().getCoPaymentPercentage();
        }

        double insuranceCoverage = totalAmount * (coveragePercentage / 100.0);
        double patientResponsibility = totalAmount - insuranceCoverage;

        dispensing.setTotalAmount(totalAmount);
        dispensing.setPatientResponsibility(patientResponsibility);
        dispensing.setInsuranceCoverage(insuranceCoverage);
    }

    private List<MedicationDispensingItem> createDispensingItems(KenemaPharmacyDispensingRequest request, MedicationDispensing savedDispensing) {
        return request.getPrescriptionDetails().stream()
                .map(item -> createDispensingItem(item, savedDispensing))
                .collect(Collectors.toList());
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
        dispensingItem.setFormulation(item.getRoute());
        dispensingItem.setItemType(ItemType.DRUG);
        return dispensingItem;
    }


    private void calculateCoverageAndResponsibility(MedicationDispensing dispensingRecord, Payer payer, Insured insured) {

        Double totalAmount = dispensingRecord.getTotalAmount();

        if (totalAmount == null) {

            logger.error("Total amount is null for dispensing record: {}", dispensingRecord.getDispensingUuid());

            dispensingRecord.setInsuranceCoverage(0.0);
            dispensingRecord.setPatientResponsibility(0.0);
            return;
        }

        // Example: 80% coverage by insurance, 20% patient responsibility
        double coveragePercentage = 0.8;
        double insuranceCoverage = totalAmount * coveragePercentage;
        double patientResponsibility = totalAmount - insuranceCoverage;

        dispensingRecord.setInsuranceCoverage(insuranceCoverage);
        dispensingRecord.setPatientResponsibility(patientResponsibility);
    }

    private Insured findInsuredPerson(DispensingRecordRequest request) {
        List<Insured> insuredList = new ArrayList<>();

        if (StringUtils.hasText(request.getEmployeeId())) {
            insuredList.addAll(insuredRepository.findByIdNumber(request.getEmployeeId()));
        }

        if (insuredList.isEmpty() && StringUtils.hasText(request.getPhone())) {
            insuredList.addAll(insuredRepository.findByPhone(request.getPhone()));
        }

        if (insuredList.isEmpty()) {
            throw new ResourceNotFoundException("Insured Person", "identifiers",
                    "Employee ID: " + request.getEmployeeId() +
                            ", Phone: " + request.getPhone());
        }

        if (insuredList.size() > 1) {
            logger.warn("Multiple insured persons found for the given identifiers. Count: {}", insuredList.size());

            // Filter by payer UUID
            List<Insured> filteredByPayer = insuredList.stream()
                    .filter(insured -> insured.getPayerUuid().equals(request.getPayerUuid()))
                    .collect(Collectors.toList());

            if (filteredByPayer.size() == 1) {
                logger.info("Single insured person found after filtering by payer UUID.");
                return filteredByPayer.get(0);
            } else if (filteredByPayer.size() > 1) {
                logger.warn("Multiple insured persons found even after filtering by payer UUID. Count: {}", filteredByPayer.size());

                // Additional filtering logic using employeeId (idNumber)
                if (StringUtils.hasText(request.getEmployeeId())) {
                    List<Insured> filteredByEmployeeId = filteredByPayer.stream()
                            .filter(insured -> insured.getIdNumber().equals(request.getEmployeeId()))
                            .collect(Collectors.toList());

                    if (filteredByEmployeeId.size() == 1) {
                        logger.info("Single insured person found after filtering by employee ID.");
                        return filteredByEmployeeId.get(0);
                    }
                }

                // If still multiple results, throw an exception
                throw new BadRequestException("Multiple insured persons found with the given identifiers and payer. Please provide more specific information.");
            } else {
                throw new BadRequestException("No insured person found for the given payer UUID.");
            }
        }

        Insured insured = insuredList.get(0);

        if (!insured.getPayerUuid().equals(request.getPayerUuid())) {
            throw new BadRequestException("Insured person does not belong to the specified payer");
        }

        return insured;
    }

    private MedicationDispensing createDispensingRecord(DispensingRecordRequest request, Insured insured, Payer payer, Provider provider) {
        MedicationDispensing record = new MedicationDispensing();
        record.setDispensingUuid(UUID.randomUUID().toString());
        record.setInsuredUuid(insured.getInsuredUuid());
        record.setPayerUuid(payer.getPayerUuid());
        record.setProviderUuid(provider.getProviderUuid());
        record.setDispensingDate(request.getDispensingDate());
        record.setPrescriptionNumber(request.getPrescriptionNumber());
        record.setPharmacyTransactionId(request.getPharmacyTransactionId());
        record.setClaimStatus("DRAFT");
        record.setSource(SourceType.INPUT);
        record.setInvoiceNumber(generateInvoiceNumber());
        record.setRecordedAt(LocalDate.now());

//        double totalAmount = calculateTotalAmount(request.getMedicationItems());
//        record.setTotalAmount(totalAmount);

        calculateCoverageAndResponsibility(record, payer, insured);

        return record;
    }


    private void updateDispensingRecordTotals(MedicationDispensing record, List<MedicationDispensingItem> items) {
        BigDecimal totalAmount = items.stream()
                .map(item -> BigDecimal.valueOf(item.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        record.setTotalAmount(totalAmount.doubleValue());
        // You might want to calculate insurance coverage and patient responsibility here
        // based on the insured person's policy details
        dispensingRepository.save(record);
    }


    private List<Servicelist> validateServices(String providerUuid, List<?> items) {
        List<Servicelist> services = new ArrayList<>();
        for (Object item : items) {
            String serviceUuid;
            if (item instanceof MedicationDispensingRequest.MedicationItem) {
                serviceUuid = ((MedicationDispensingRequest.MedicationItem) item).getServiceUuid();
            } else if (item instanceof DispensingRecordRequest.DispensingItemRequest) {
                serviceUuid = ((DispensingRecordRequest.DispensingItemRequest) item).getServiceUuid();
            } else {
                throw new IllegalArgumentException("Unsupported item type");
            }

            Servicelist service = servicelistRepository.findByServiceUuidAndProviderProviderUuid(serviceUuid, providerUuid);
            if (service == null) {
                throw new ResourceNotFoundException("Service", "uuid", serviceUuid);
            }
            services.add(service);
        }
        return services;
    }

    private List<MedicationDispensingItem> createDispensingItems(MedicationDispensing dispensingRecord, List<Servicelist> services, List<DispensingRecordRequest.DispensingItemRequest> itemRequests) {
        List<MedicationDispensingItem> dispensingItems = new ArrayList<>();
        for (int i = 0; i < services.size(); i++) {
            Servicelist service = services.get(i);
            DispensingRecordRequest.DispensingItemRequest itemRequest = itemRequests.get(i);

            MedicationDispensingItem item = new MedicationDispensingItem();
            item.setItemUuid(UUID.randomUUID().toString());
            item.setDispensing(dispensingRecord);
            item.setMedicationCode(service.getServiceCode());
            item.setMedicationName(service.getServiceName());
            item.setPrimaryDiagnosis(itemRequest.getPrimaryDiagnosis());
            item.setSecondaryDiagnosis(itemRequest.getSecondaryDiagnosis());
            item.setUnitPrice(service.getPrice());
            item.setTotalPrice(service.getPrice());
            item.setItemType(ItemType.SERVICE);

            dispensingItems.add(item);

        }
        return dispensingItems;
    }


    private PendingDispensingRecordDTO convertToDTO(MedicationDispensing dispensing) {
        PendingDispensingRecordDTO dto = new PendingDispensingRecordDTO();
        dto.setDispensingUuid(dispensing.getDispensingUuid());
        dto.setCreatedAt(dispensing.getRecordedAt());
        dto.setBranchName(dispensing.getBranchName());
        dto.setStatus(Status.valueOf(dispensing.getClaimStatus()));
        dto.setInvoiceNumber(dispensing.getInvoiceNumber());

        Insured insured = insuredRepository.findByInsuredUuid(dispensing.getInsuredUuid());
        if (insured != null) {
            dto.setPatientName(insured.getFirstName() + " " + insured.getFatherName() + " " + insured.getGrandFatherName());
            dto.setInsuranceId(insured.getInsuranceId());
        }

        dto.setDispensingDate(dispensing.getDispensingDate());
        dto.setPrescriptionNumber(dispensing.getPrescriptionNumber());
        dto.setPharmacyTransactionId(dispensing.getPharmacyTransactionId());
        dto.setTotalAmount(dispensing.getTotalAmount());
        dto.setPatientResponsibility(dispensing.getPatientResponsibility());
        dto.setInsuranceCoverage(dispensing.getInsuranceCoverage());
        dto.setPayerUuid(dispensing.getPayerUuid());

        Payer payer = payerRepository.findByPayerUuid(dispensing.getPayerUuid());

        if (payer == null){
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
        Insured insured =  insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null){
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

    //New

    @Override
    @Transactional
    public ResponseEntity<?> authorizeDispensingRecord(String dispensingUuid) {
        MedicationDispensing record = dispensingRepository.findByDispensingUuid(dispensingUuid);
        if (record == null){
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
        if (record == null){
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

        // Update dispensing records to SUBMITTED status
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

        // Perform reconciliation logic here
        claim.setStatus(ClaimStatus.RECONCILED);

        // Fetch associated dispensing records
        List<MedicationDispensing> dispensingRecords = dispensingRepository.findByClaimUuid(claimUuid);

        // Create batch record
        BatchRecord batchRecord = createBatchRecord(claim.getProvidedService().getInsured().getPayer(), dispensingRecords, claim);
        batchRecord.setStatus("RECONCILED");
        batchRecord.setClaim(claim);
        claim.setBatchRecord(batchRecord);

        // Save both entities
        claimRepository.save(claim);
        BatchRecord savedBatchRecord = batchRecordRepository.save(batchRecord);

        createClaimLog(claim, SecurityUtils.getAuthenticatedUser(), ClaimStatus.PAID, ClaimStatus.RECONCILED,
                "Claim reconciled by pharmacy");

        ReconciliationResponse response = new ReconciliationResponse();
        BeanUtils.copyProperties(savedBatchRecord,response);
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