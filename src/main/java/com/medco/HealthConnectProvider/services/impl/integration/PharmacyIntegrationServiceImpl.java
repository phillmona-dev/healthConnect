package com.medco.HealthConnectProvider.services.impl.integration;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserDetailsImpl;
import com.medco.HealthConnectProvider.dto.PendingDispensingRecordDTO;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.claims.ClaimItem;
import com.medco.HealthConnectProvider.entity.claims.ClaimLogs;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.claims.ClaimItemRepository;
import com.medco.HealthConnectProvider.repository.claims.ClaimLogsRepository;
import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingItemRepository;
import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.services.eligibility.EligibilityService;
import com.medco.HealthConnectProvider.services.integration.PharmacyIntegrationService;
import com.medco.HealthConnectProvider.services.persons.InsuredService;
import com.medco.HealthConnectProvider.ui.request.eligibility.EligibilityCheckRequest;
import com.medco.HealthConnectProvider.ui.request.integration.DispensingRecordRequest;
import com.medco.HealthConnectProvider.ui.request.integration.MedicationDispensingRequest;
import com.medco.HealthConnectProvider.ui.response.ApiErrorResponse;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.eligibility.EligibilityResponse;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingRecordResponse;
import com.medco.HealthConnectProvider.ui.response.integration.DispensingResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredSearchResponse;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    @Override
    @Transactional
    public ResponseEntity<DispensingResponse> recordMedicationDispensing(MedicationDispensingRequest request) {
        log.info("Recording medication dispensing from provider: {}", request.getProviderUuid());

        Provider provider = validateProvider(request.getProviderUuid());
        Payer payer = validatePayer(request.getPayerUuid());
        validateUniqueTransaction(request.getPharmacyTransactionId());

        EligibilityResponse eligibilityResponse = checkEligibility(request);

        Insured insured = findInsuredPersonByUuid(eligibilityResponse.getInsuredUuid());

        MedicationDispensing dispensing = createDispensingRecord(request, insured, eligibilityResponse);
        MedicationDispensing savedDispensing = dispensingRepository.save(dispensing);

        List<MedicationDispensingItem> items = createDispensingItems(request, savedDispensing);
        dispensingItemRepository.saveAll(items);

        DispensingResponse response = createDispensingResponse(savedDispensing);

        return ResponseEntity.ok(response);
    }

    private Insured findInsuredPersonByUuid(String insuredUuid) {
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null) {
            throw new ResourceNotFoundException("Insured", "insuredUuid", insuredUuid);
        }
        return insured;
    }

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

    private void validateUniqueTransaction(String pharmacyTransactionId) {
        if (dispensingRepository.existsByPharmacyTransactionId(pharmacyTransactionId)) {
            throw new BadRequestException("This transaction has already been recorded");
        }
    }

    private EligibilityResponse checkEligibility(MedicationDispensingRequest request) {
        EligibilityCheckRequest eligibilityRequest = createEligibilityRequest(request);
        List<InsuredSearchResponse> insuredPersons = insuredService.searchInsuredPersons(
                request.getPhone(), request.getEmployeeId(), request.getInsuranceId(), request.getNationalId());

        if (insuredPersons.isEmpty()) {
            throw new ResourceNotFoundException("Insured Person", "provided identifiers", "Not found");
        }

        if (insuredPersons.size() > 1) {
            throw new BadRequestException("Multiple insured persons found. Please provide more specific information.");
        }

        InsuredSearchResponse insured = insuredPersons.get(0);

        if (!insured.isInsured()) {
            throw new BadRequestException("The person is not currently insured.");
        }

        ResponseEntity<EligibilityResponse> eligibilityResponseEntity =
                eligibilityService.checkEligibilityForInsured(request.getProviderUuid(), insured, eligibilityRequest.getServiceUuid());

        EligibilityResponse eligibilityResponse = eligibilityResponseEntity.getBody();
        if (eligibilityResponse == null) {
            throw new BadRequestException("Failed to retrieve eligibility information");
        }

        if (!eligibilityResponse.isEligible()) {
            throw new BadRequestException("Patient is not eligible for services: " + eligibilityResponse.getIneligibilityReason());
        }

        if (eligibilityResponse.getInsuredUuid() == null) {
            eligibilityResponse.setInsuredUuid(insured.getInsuredUuid());
        }

        return eligibilityResponse;
    }

    private EligibilityResponse processEligibilityResponse(ResponseEntity<EligibilityResponse> eligibilityResponseEntity) {
        EligibilityResponse eligibilityResponse = eligibilityResponseEntity.getBody();
        if (eligibilityResponse == null || !eligibilityResponse.isEligible()) {
            throw new BadRequestException("Patient is not eligible for services: " +
                    (eligibilityResponse != null ? eligibilityResponse.getIneligibilityReason() : "Unknown reason"));
        }
        return eligibilityResponse;
    }

    private InsuredSearchResponse selectInsuredPerson(List<InsuredSearchResponse> insuredPersons, String payerUuid) {
        return insuredPersons.stream()
                .filter(insured -> insured.getPayerUuid().equals(payerUuid))
                .findFirst()
                .orElse(null);
    }

    private EligibilityCheckRequest createEligibilityRequest(MedicationDispensingRequest request) {
        EligibilityCheckRequest eligibilityRequest = new EligibilityCheckRequest();
        //eligibilityRequest.setPayerUuid(request.getPayerUuid());
        eligibilityRequest.setEmployeeId(request.getEmployeeId());
        eligibilityRequest.setInsuranceId(request.getInsuranceId());
        eligibilityRequest.setNationalId(request.getNationalId());
        eligibilityRequest.setPhoneNumber(request.getPhone());
        return eligibilityRequest;
    }

    private MedicationDispensing createDispensingRecord(MedicationDispensingRequest request, Insured insured, EligibilityResponse eligibilityResponse) {
        MedicationDispensing dispensing = new MedicationDispensing();

        dispensing.setInvoiceNumber(generateInvoiceNumber());
        dispensing.setDispensingUuid(UUID.randomUUID().toString());
        dispensing.setProviderUuid(request.getProviderUuid());
        dispensing.setPayerUuid(request.getPayerUuid());
        dispensing.setInsuredUuid(insured.getInsuredUuid());
        dispensing.setPrescriptionNumber(request.getPrescriptionNumber());
        dispensing.setPharmacyTransactionId(request.getPharmacyTransactionId());
        dispensing.setDispensingDate(request.getDispensingDate());
        dispensing.setPrescribingPhysicianName(request.getPrescribingPhysicianName());
        dispensing.setPrescribingPhysicianId(request.getPrescribingPhysicianId());
        dispensing.setBranchName(request.getBranchName());
        dispensing.setPharmacistNotes(request.getPharmacistNotes());
        dispensing.setRecordedAt(LocalDate.now());
        dispensing.setClaimStatus("DRAFT");

        calculateTotals(dispensing, request, eligibilityResponse);

        return dispensing;

    }

    private void calculateTotals(MedicationDispensing dispensing, Object request, EligibilityResponse eligibilityResponse) {
        double totalAmount;
        List<?> medicationItems;

        if (request instanceof MedicationDispensingRequest) {
            medicationItems = ((MedicationDispensingRequest) request).getMedicationItems();
            totalAmount = ((MedicationDispensingRequest) request).getMedicationItems().stream()
                    .mapToDouble(item -> ((MedicationDispensingRequest.MedicationItem) item).getTotalPrice())
                    .sum();
        } else if (request instanceof DispensingRecordRequest) {
            medicationItems = ((DispensingRecordRequest) request).getMedicationItems();
            totalAmount = ((DispensingRecordRequest) request).getMedicationItems().stream()
                    .mapToDouble(item -> ((DispensingRecordRequest.DispensingItemRequest) item).getTotalPrice())
                    .sum();
        } else {
            throw new IllegalArgumentException("Unsupported request type");
        }

        double coveragePercentage = 80.0; // Default coverage percentage
        if (eligibilityResponse != null && eligibilityResponse.getRequestedService() != null) {
            coveragePercentage = 100.0 - eligibilityResponse.getRequestedService().getCoPaymentPercentage();
        }

        double insuranceCoverage = totalAmount * (coveragePercentage / 100.0);
        double patientResponsibility = totalAmount - insuranceCoverage;

        dispensing.setTotalAmount(totalAmount);
        dispensing.setPatientResponsibility(patientResponsibility);
        dispensing.setInsuranceCoverage(insuranceCoverage);
    }

    private List<MedicationDispensingItem> createDispensingItems(MedicationDispensingRequest request, MedicationDispensing savedDispensing) {
        return request.getMedicationItems().stream()
                .map(item -> createDispensingItem(item, savedDispensing))
                .collect(Collectors.toList());
    }


    private MedicationDispensingItem createDispensingItem(MedicationDispensingRequest.MedicationItem item, MedicationDispensing savedDispensing) {
        MedicationDispensingItem dispensingItem = new MedicationDispensingItem();
        dispensingItem.setItemUuid(UUID.randomUUID().toString());
        dispensingItem.setDispensing(savedDispensing);
        dispensingItem.setMedicationCode(item.getMedicationCode());
        dispensingItem.setMedicationName(item.getMedicationName());
        dispensingItem.setQuantity(item.getQuantity());
        dispensingItem.setUnitOfMeasure(item.getUnitOfMeasure());
        dispensingItem.setUnitPrice(item.getUnitPrice());
        dispensingItem.setTotalPrice(item.getTotalPrice());
        dispensingItem.setDosageInstructions(item.getDosageInstructions());
        dispensingItem.setStrength(item.getStrength());
        dispensingItem.setFormulation(item.getFormulation());
        return dispensingItem;
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
            String providerUuid, String phone, String status, LocalDate startDate, LocalDate endDate,
            String medicationName, String patientName, int page, int size, String sortBy, String sortDirection) {

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

                if (medicationName != null && !medicationName.isEmpty()) {
                    predicates.add(cb.like(cb.lower(root.join("items").get("medicationName")), "%" + medicationName.toLowerCase() + "%"));
                }

                if (patientName != null && !patientName.isEmpty()) {
                    Join<MedicationDispensing, Insured> insuredJoin = root.join("insured", JoinType.LEFT);
                    predicates.add(
                            cb.or(
                                    cb.like(cb.lower(insuredJoin.get("firstName")), "%" + patientName.toLowerCase() + "%"),
                                    cb.like(cb.lower(insuredJoin.get("lastName")), "%" + patientName.toLowerCase() + "%")
                            )
                    );
                }

                if (phone != null && !phone.isEmpty()) {
                    Join<MedicationDispensing, Insured> insuredJoin = root.join("insured", JoinType.LEFT);
                    predicates.add(cb.equal(insuredJoin.get("phone"), phone));
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
    public ResponseEntity<?> updateDispensingRecordsStatus(String providerUuid, String newStatus, String[] dispensingUuids) {
        if (dispensingUuids == null || dispensingUuids.length == 0) {
            throw new BadRequestException("No dispensing records selected");
        }

        if (!newStatus.equals("AUTHORIZED") && !newStatus.equals("SUBMITTED")) {
            throw new BadRequestException("Invalid new status. Must be either AUTHORIZED or SUBMITTED");
        }

        Provider provider = providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", "uuid", providerUuid));

        List<MedicationDispensing> dispensingRecords = dispensingRepository.findByDispensingUuidIn(dispensingUuids);

        if (dispensingRecords.size() != dispensingUuids.length) {
            throw new RuntimeException("One or more dispensing records not found");
        }

        String expectedCurrentStatus = newStatus.equals("AUTHORIZED") ? "DRAFT" : "AUTHORIZED";

        for (MedicationDispensing record : dispensingRecords) {
            if (!record.getClaimStatus().equals(expectedCurrentStatus)) {
                throw new BadRequestException("Dispensing record " + record.getDispensingUuid() +
                        " is not in " + expectedCurrentStatus + " status");
            }
        }

        Claim claim = null;
        if (newStatus.equals("SUBMITTED")) {

            String payerUuid = dispensingRecords.get(0).getPayerUuid();
            Payer payer = payerRepository.findByPayerUuid(payerUuid);
            if (payer == null){
                throw new ResourceNotFoundException("Payer", "uuid", payerUuid);
            }

            String insuredUuid = dispensingRecords.get(0).getInsuredUuid();
            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null){
                throw new ResourceNotFoundException("Insured", "uuid", insuredUuid);
            }

            claim = createClaim(provider, payer, insured, dispensingRecords);
            claim = claimRepository.save(claim);
        }

        for (MedicationDispensing record : dispensingRecords) {
            record.setClaimStatus(newStatus);
            if (claim != null) {
                record.setClaimUuid(claim.getClaimUuid());
            }
        }

        dispensingRepository.saveAll(dispensingRecords);

        if (claim != null) {
            List<ClaimItem> claimItems = createClaimItems(dispensingRecords, claim);
            claimItemRepository.saveAll(claimItems);
        }

        String message = dispensingRecords.size() + " dispensing record(s) updated to " + newStatus + " status";
        if (claim != null) {
            message += " and claim created with UUID: " + claim.getClaimUuid();
        }

        return ResponseEntity.ok(new MessageResponse(message));
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
                if (cve.getConstraintViolations().contains("insurance_coverage")) {
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

    private void calculateCoverageAndResponsibility(MedicationDispensing dispensingRecord, Payer payer, Insured insured) {

        Double totalAmount = dispensingRecord.getTotalAmount();

        if (totalAmount == null) {

            logger.error("Total amount is null for dispensing record: {}", dispensingRecord.getDispensingUuid());

            dispensingRecord.setInsuranceCoverage(0.0);
            dispensingRecord.setPatientResponsibility(0.0);
            return;
        }

        // Example: 80% coverage by insurance, 20% patient responsibility
        double coveragePercentage = 0.8; // This should be determined based on the payer and insured's policy
        double insuranceCoverage = totalAmount * coveragePercentage;
        double patientResponsibility = totalAmount - insuranceCoverage;

        dispensingRecord.setInsuranceCoverage(insuranceCoverage);
        dispensingRecord.setPatientResponsibility(patientResponsibility);
    }

    private Insured findInsuredPerson(DispensingRecordRequest request) {
        Insured insured = insuredRepository.findByPhone(request.getPhone());
        if (insured == null) {
            throw new ResourceNotFoundException("Insured Person", "phone", request.getPhone());
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
        record.setInvoiceNumber(generateInvoiceNumber());
        record.setRecordedAt(LocalDate.now());

        // Calculate total amount
        double totalAmount = calculateTotalAmount(request.getMedicationItems());
        record.setTotalAmount(totalAmount);

        // Calculate insurance coverage and patient responsibility
        calculateCoverageAndResponsibility(record, payer, insured);

        return record;
    }

    private List<MedicationDispensingItem> createDispensingItems(DispensingRecordRequest request, MedicationDispensing dispensingRecord) {
        List<MedicationDispensingItem> items = new ArrayList<>();
        for (DispensingRecordRequest.DispensingItemRequest itemRequest : request.getMedicationItems()) {
            Servicelist service = servicelistRepository.findByServiceUuid(itemRequest.getServiceUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "uuid", itemRequest.getServiceUuid()));

            MedicationDispensingItem item = new MedicationDispensingItem();
            item.setItemUuid(UUID.randomUUID().toString());
            item.setDispensing(dispensingRecord);
            item.setMedicationCode(service.getServiceCode());
            item.setMedicationName(service.getServiceName());
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(service.getPrice());
            item.setTotalPrice(service.getPrice() * itemRequest.getQuantity());
            items.add(item);
        }
        return items;
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


    private Insured findInsuredPerson(MedicationDispensingRequest request) {

        Insured insured = insuredRepository.findByInsuranceId(request.getInsuranceId());

        if (insured == null && request.getEmployeeId() != null) {
            insured = insuredRepository.findByEmployeeId(request.getEmployeeId());
        }

        // If not found, try national ID
        if (insured == null && request.getNationalId() != null) {
            insured = insuredRepository.findByNationalId(request.getNationalId());
        }

        // If not found, try phone number
        if (insured == null && request.getPhone() != null) {
            insured = insuredRepository.findByPhone(request.getPhone());
        }

        return insured;
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

    private MedicationDispensing createDispensingRecord(Provider provider, Payer payer, Insured insured, MedicationDispensingRequest request) {
        MedicationDispensing dispensing = new MedicationDispensing();
        dispensing.setDispensingUuid(UUID.randomUUID().toString());
        dispensing.setProviderUuid(provider.getProviderUuid());
        dispensing.setPayerUuid(payer.getPayerUuid());
        dispensing.setInsuredUuid(insured.getInsuredUuid());
        dispensing.setDispensingDate(request.getDispensingDate());
        dispensing.setPrescriptionNumber(request.getPrescriptionNumber());
        dispensing.setPharmacyTransactionId(request.getPharmacyTransactionId());
        dispensing.setTotalAmount(calculateTotalAmount(request.getMedicationItems()));
        dispensing.setClaimStatus("DRAFT");
        // Set other fields as needed
        return dispensing;
    }


    private List<MedicationDispensingItem> createDispensingItems(MedicationDispensing dispensing, List<Servicelist> services, List<?> items) {
        List<MedicationDispensingItem> dispensingItems = new ArrayList<>();
        for (int i = 0; i < services.size(); i++) {
            Servicelist service = services.get(i);
            Object item = items.get(i);

            MedicationDispensingItem dispensingItem = new MedicationDispensingItem();
            dispensingItem.setItemUuid(UUID.randomUUID().toString()); // Set a unique UUID for each item
            dispensingItem.setDispensing(dispensing);
            dispensingItem.setMedicationCode(service.getServiceCode());
            dispensingItem.setMedicationName(service.getServiceName());

            if (item instanceof MedicationDispensingRequest.MedicationItem) {
                MedicationDispensingRequest.MedicationItem medicationItem = (MedicationDispensingRequest.MedicationItem) item;
                dispensingItem.setQuantity(medicationItem.getQuantity());
                dispensingItem.setDosageInstructions(medicationItem.getDosageInstructions());
                dispensingItem.setStrength(medicationItem.getStrength());
                dispensingItem.setFormulation(medicationItem.getFormulation());
            } else if (item instanceof DispensingRecordRequest.DispensingItemRequest) {
                DispensingRecordRequest.DispensingItemRequest dispensingItemRequest = (DispensingRecordRequest.DispensingItemRequest) item;
                dispensingItem.setQuantity(dispensingItemRequest.getQuantity());
                // Set other fields if available in DispensingItemRequest
            } else {
                throw new IllegalArgumentException("Unsupported item type");
            }

            // Ensure quantity is not null
            if (dispensingItem.getQuantity() == null) {
                throw new IllegalArgumentException("Quantity cannot be null for medication item: " + dispensingItem.getMedicationName());
            }

            dispensingItem.setUnitOfMeasure(service.getUnitOfMeasure());
            dispensingItem.setUnitPrice(service.getPrice());
            dispensingItem.setTotalPrice(dispensingItem.getQuantity() * service.getPrice());
            dispensingItems.add(dispensingItem);
        }
        return dispensingItems;
    }

    private double calculateTotalAmount(List<?> items) {
        return items.stream()
                .mapToDouble(item -> {
                    String serviceUuid;
                    double quantity;
                    if (item instanceof MedicationDispensingRequest.MedicationItem) {
                        MedicationDispensingRequest.MedicationItem medicationItem = (MedicationDispensingRequest.MedicationItem) item;
                        serviceUuid = medicationItem.getServiceUuid();
                        quantity = medicationItem.getQuantity();
                    } else if (item instanceof DispensingRecordRequest.DispensingItemRequest) {
                        DispensingRecordRequest.DispensingItemRequest dispensingItem = (DispensingRecordRequest.DispensingItemRequest) item;
                        serviceUuid = dispensingItem.getServiceUuid();
                        quantity = dispensingItem.getQuantity();
                    } else {
                        throw new IllegalArgumentException("Unsupported item type");
                    }

                    Servicelist service = servicelistRepository.findByServiceUuid(serviceUuid)
                            .orElseThrow(() -> new ResourceNotFoundException("Service", "uuid", serviceUuid));
                    return service.getPrice() * quantity;
                })
                .sum();
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

        List<MedicationDispensingItem> items = dispensingItemRepository.findByDispensing(dispensing);
        dto.setMedicationItems(items.stream().map(this::convertToItemDTO).collect(Collectors.toList()));

        return dto;
    }

    private String generateInvoiceNumber() {
        int randomNum = 100000000 + new Random().nextInt(900000000);
        return "IN-" + randomNum;
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

    private Insured findInsuredPerson(String patientId) {

        Insured insured = insuredRepository.findByInsuranceId(patientId);

        if (insured == null) {
            insured = insuredRepository.findByEmployeeId(patientId);
        }

        if (insured == null) {
            insured = insuredRepository.findByNationalId(patientId);
        }

        if (insured == null) {
            insured = insuredRepository.findByPhone(patientId);
        }

        return insured;
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
        claim.setProvider(provider);
        claim.setPayer(payer);
        claim.setInsuredPerson(insured);
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

    /**
     * Helper method to find an insured person based on the provided identifiers
     */
    private Insured findInsuredPerson(MedicationDispensingRequest request, Payer payer) {
        Insured insured = null;

        if (request.getInsuranceId() != null && !request.getInsuranceId().isEmpty()) {
            insured = insuredRepository.findByInsuranceId(request.getInsuranceId());
        }

        if (insured == null && request.getEmployeeId() != null && !request.getEmployeeId().isEmpty()) {
            insured = insuredRepository.findByEmployeeId(request.getEmployeeId());
        }

        if (insured == null && request.getNationalId() != null && !request.getNationalId().isEmpty()) {
            insured = insuredRepository.findByNationalId(request.getNationalId());
        }

        if (insured == null && request.getPhone() != null && !request.getPhone().isEmpty()) {
            insured = insuredRepository.findByPhone(request.getPhone());
        }

        if (insured == null) {
            throw new ResourceNotFoundException("Insured Person", "identifiers",
                    "Insurance ID: " + request.getInsuranceId() +
                            ", Employee ID: " + request.getEmployeeId() +
                            ", National ID: " + request.getNationalId() +
                            ", Phone: " + request.getPhone());
        }

        if (!insured.getPayerUuid().equals(payer.getPayerUuid())) {
            throw new BadRequestException("Insured person does not belong to the specified payer");
        }

        return insured;
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

        // Ensure all records are in AUTHORIZED status
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
    public ResponseEntity<?> reconcilePayment(String claimUuid) {
        Claim claim = claimRepository.findByClaimUuid(claimUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Claim", "claimUuid", claimUuid));

        if (!claim.getStatus().equals(ClaimStatus.PAID.toString())) {
            throw new BadRequestException("Only paid claims can be reconciled");
        }

        // Perform reconciliation logic here
        // This could involve updating the claim status, creating a reconciliation record, etc.

        claim.setStatus(ClaimStatus.RECONCILED);
        claimRepository.save(claim);

        createClaimLog(claim, SecurityUtils.getAuthenticatedUser(), ClaimStatus.PAID, ClaimStatus.RECONCILED,
                "Claim reconciled by pharmacy");

        return ResponseEntity.ok(new MessageResponse("Claim reconciled successfully"));
    }

    private void createClaimLog(Claim claim, UserDetailsImpl user, ClaimStatus previousStatus, ClaimStatus newStatus, String comment) {
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