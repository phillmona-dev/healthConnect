//package com.medco.HealthConnectProvider.services.impl.integration;
//
//import com.medco.HealthConnectProvider.entity.claims.Claim;
//import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
//import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;
//import com.medco.HealthConnectProvider.entity.persons.Insured;
//import com.medco.HealthConnectProvider.entity.payers.Payer;
//import com.medco.HealthConnectProvider.entity.providers.Provider;
//import com.medco.HealthConnectProvider.exception.BadRequestException;
//import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
//import com.medco.HealthConnectProvider.repository.claims.ClaimItemRepository;
//import com.medco.HealthConnectProvider.repository.claims.ClaimRepository;
//import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingItemRepository;
//import com.medco.HealthConnectProvider.repository.integration.MedicationDispensingRepository;
//import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
//import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
//import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
//import com.medco.HealthConnectProvider.services.eligibility.EligibilityService;
//import com.medco.HealthConnectProvider.services.integration.PharmacyIntegrationService;
//import com.medco.HealthConnectProvider.ui.request.eligibility.EligibilityCheckRequest;
//import com.medco.HealthConnectProvider.ui.request.integration.MedicationDispensingRequest;
//import com.medco.HealthConnectProvider.ui.response.eligibility.EligibilityResponse;
//import com.medco.HealthConnectProvider.ui.response.integration.DispensingResponse;
//import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
//import com.medco.HealthConnectProvider.utils.enums.Status;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.ResponseEntity;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.time.LocalDateTime;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.UUID;
//import java.util.stream.Collectors;
//
//@Service
//@Slf4j
//public class PharmacyIntegrationServiceImpl implements PharmacyIntegrationService {
//
//    @Autowired
//    private MedicationDispensingRepository dispensingRepository;
//
//    @Autowired
//    private MedicationDispensingItemRepository dispensingItemRepository;
//
//    @Autowired
//    private ProviderRepository providerRepository;
//
//    @Autowired
//    private PayerRepository payerRepository;
//
//    @Autowired
//    private InsuredRepository insuredRepository;
//
//    @Autowired
//    private ClaimRepository claimRepository;
//
//    @Autowired
//    private ClaimItemRepository claimItemRepository;
//
//    @Autowired
//    private EligibilityService eligibilityService;
//
//    @Override
//    @Transactional
//    public ResponseEntity<DispensingResponse> recordMedicationDispensing(MedicationDispensingRequest request) {
//        log.info("Recording medication dispensing from provider: {}", request.getProviderUuid());
//
//        // Validate provider exists
//        Provider provider = providerRepository.findByProviderUuid(request.getProviderUuid())
//                .orElseThrow(() -> new ResourceNotFoundException("Provider", "providerUuid", request.getProviderUuid()));
//
//        // Validate payer exists
//        Payer payer = payerRepository.findByPayerUuid(request.getPayerUuid());
//        if (payer == null) {
//            throw new ResourceNotFoundException("Payer", "payerUuid", request.getPayerUuid());
//        }
//
//        // Check for duplicate transaction
//        if (dispensingRepository.existsByPharmacyTransactionId(request.getPharmacyTransactionId())) {
//            throw new BadRequestException("This transaction has already been recorded");
//        }
//
//        // Check eligibility
//        EligibilityCheckRequest eligibilityRequest = new EligibilityCheckRequest();
//        eligibilityRequest.setPayerUuid(request.getPayerUuid());
//        eligibilityRequest.setEmployeeId(request.getEmployeeId());
//        eligibilityRequest.setInsuranceId(request.getInsuranceId());
//        eligibilityRequest.setNationalId(request.getNationalId());
//        eligibilityRequest.setPhoneNumber(request.getPhoneNumber());
//
//        ResponseEntity<EligibilityResponse> eligibilityResponseEntity =
//                eligibilityService.checkEligibility(request.getProviderUuid(), eligibilityRequest);
//
//        EligibilityResponse eligibilityResponse = eligibilityResponseEntity.getBody();
//        if (eligibilityResponse == null || !eligibilityResponse.isEligible()) {
//            throw new BadRequestException("Patient is not eligible for services: " +
//                    (eligibilityResponse != null ? eligibilityResponse.getIneligibilityReason() : "Unknown reason"));
//        }
//
//        // Find insured person
//        Insured insured = findInsuredPerson(request, payer);
//
//        // Create dispensing record
//        MedicationDispensing dispensing = new MedicationDispensing();
//        dispensing.setDispensingUuid(UUID.randomUUID().toString());
//        dispensing.setProviderUuid(request.getProviderUuid());
//        dispensing.setPayerUuid(request.getPayerUuid());
//        dispensing.setInsuredUuid(insured.getInsuredUuid());
//        dispensing.setPrescriptionNumber(request.getPrescriptionNumber());
//        dispensing.setPharmacyTransactionId(request.getPharmacyTransactionId());
//        dispensing.setDispensingDate(request.getDispensingDate());
//        dispensing.setPrescribingPhysicianName(request.getPrescribingPhysicianName());
//        dispensing.setPrescribingPhysicianId(request.getPrescribingPhysicianId());
//        dispensing.setPharmacistNotes(request.getPharmacistNotes());
//        dispensing.setRecordedAt(LocalDateTime.now());
//        dispensing.setStatus(Status.ACTIVE);
//        dispensing.setClaimStatus("PENDING"); // Not yet included in a claim
//
//        // Calculate totals
//        double totalAmount = request.getMedicationItems().stream()
//                .mapToDouble(MedicationDispensingRequest.MedicationItem::getTotalPrice)
//                .sum();
//
//        // Apply coverage based on eligibility response
//        double coveragePercentage = 80.0; // Default coverage percentage
//        if (eligibilityResponse.getRequestedService() != null) {
//            coveragePercentage = 100.0 - eligibilityResponse.getRequestedService().getCoPaymentPercentage();
//        }
//
//        double insuranceCoverage = totalAmount * (coveragePercentage / 100.0);
//        double patientResponsibility = totalAmount - insuranceCoverage;
//
//        dispensing.setTotalAmount(totalAmount);
//        dispensing.setPatientResponsibility(patientResponsibility);
//        dispensing.setInsuranceCoverage(insuranceCoverage);
//
//        // Save dispensing record
//        MedicationDispensing savedDispensing = dispensingRepository.save(dispensing);
//
//        // Save medication items
//        List<MedicationDispensingItem> items = request.getMedicationItems().stream()
//                .map(item -> {
//                    MedicationDispensingItem dispensingItem = new MedicationDispensingItem();
//                    dispensingItem.setItemUuid(UUID.randomUUID().toString());
//                    dispensingItem.setDispensing(savedDispensing);
//                    dispensingItem.setMedicationCode(item.getMedicationCode());
//                    dispensingItem.setMedicationName(item.getMedicationName());
//                    dispensingItem.setQuantity(item.getQuantity());
//                    dispensingItem.setUnitOfMeasure(item.getUnitOfMeasure());
//                    dispensingItem.setUnitPrice(item.getUnitPrice());
//                    dispensingItem.setTotalPrice(item.getTotalPrice());
//                    dispensingItem.setDosageInstructions(item.getDosageInstructions());
//                    dispensingItem.setStrength(item.getStrength());
//                    dispensingItem.setFormulation(item.getFormulation());
//                    return dispensingItem;
//                })
//                .collect(Collectors.toList());
//
//        dispensingItemRepository.saveAll(items);
//
//        // Create response
//        DispensingResponse response = new DispensingResponse();
//        response.setDispensingUuid(savedDispensing.getDispensingUuid());
//        response.setStatus("SUCCESS");
//        response.setMessage("Medication dispensing recorded successfully");
//        response.setRecordedAt(savedDispensing.getRecordedAt());
//        response.setTotalAmount(totalAmount);
//        response.setPatientResponsibility(patientResponsibility);
//        response.setInsuranceCoverage(insuranceCoverage);
//
//        return ResponseEntity.ok(response);
//    }
//
//    @Override
//    public ResponseEntity<?> getPendingDispensingRecords(String providerUuid, String patientId, int page, int size) {
//        // Validate provider exists
//        providerRepository.findByProviderUuid(providerUuid)
//                .orElseThrow(() -> new ResourceNotFoundException("Provider", "providerUuid", providerUuid));
//
//        Pageable pageable = PageRequest.of(page, size);
//        Page<MedicationDispensing> dispensingRecords;
//
//        if (patientId != null && !patientId.isEmpty()) {
//            // Find insured person by ID
//            Insured insured = insuredRepository.findByInsuranceId(patientId);
//            if (insured == null) {
//                return ResponseEntity.ok(Page.empty());
//            }
//
//            dispensingRecords = dispensingRepository.findByProviderUuidAndInsuredUuidAndClaimStatus(
//                    providerUuid, insured.getInsuredUuid(), "PENDING", pageable);
//        } else {
//            dispensingRecords = dispensingRepository.findByProviderUuidAndClaimStatus(
//                    providerUuid, "PENDING", pageable);
//        }
//
//        return ResponseEntity.ok(dispensingRecords);
//    }
//
//    @Override
//    @Transactional
//    public ResponseEntity<?> createClaimFromDispensingRecords(String providerUuid, String[] dispensingUuids) {
//        if (dispensingUuids == null || dispensingUuids.length == 0) {
//            throw new BadRequestException("No dispensing records selected");
//        }
//
//        // Validate provider exists
//        Provider provider = providerRepository.findByProviderUuid(providerUuid)
//                .orElseThrow(() -> new ResourceNotFoundException("Provider", "providerUuid", providerUuid));
//
//        // Get all dispensing records
//        List<MedicationDispensing> dispensingRecords = dispensingRepository.findByDispensingUuidIn(dispensingUuids);
//
//        if (dispensingRecords.isEmpty()) {
//            throw new ResourceNotFoundException("Dispensing Records", "uuids", String.join(", ", dispensingUuids));
//        }
//
//        // Validate all records belong to the same provider and payer
//        String payerUuid = dispensingRecords.get(0).getPayerUuid();
//        String insuredUuid = dispensingRecords.get(0).getInsuredUuid();
//
//        for (MedicationDispensing record : dispensingRecords) {
//            if (!record.getProviderUuid().equals(providerUuid)) {
//                throw new BadRequestException("All dispensing records must belong to the same provider");
//            }
//
//            if (!record.getPayerUuid().equals(payerUuid)) {
//                throw new BadRequestException("All dispensing records must belong to the same payer");
//            }
//
//            if (!record.getInsuredUuid().equals(insuredUuid)) {
//                throw new BadRequestException("All dispensing records must belong to the same insured person");
//            }
//
//            if (!"PENDING".equals(record.getClaimStatus())) {
//                throw new BadRequestException("Dispensing record " + record.getDispensingUuid() +
//                        " has already been included in a claim");
//            }
//        }
//
//        // Get payer and insured details
//        Payer payer = payerRepository.findByPayerUuid(payerUuid);
//        if (payer == null) {
//            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
//        }
//
//        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
//        if (insured == null) {
//            throw new ResourceNotFoundException("Insured", "insuredUuid", insuredUuid);
//        }
//
//        // Create new claim
//        Claim claim = new Claim();
//        claim.setClaimUuid(UUID.randomUUID().toString());
//        claim.setProvider(provider);
//        claim.setPayer(payer);
//        claim.setInsured(insured);
//        claim.setClaimDate(LocalDateTime.now());
//        claim.setStatus(ClaimStatus.SUBMITTED);
//
//        // Calculate claim totals
//        double totalAmount = dispensingRecords.stream()
//                .mapToDouble(MedicationDispensing::getTotalAmount)
//                .sum();
//
//        double patientResponsibility = dispensingRecords.stream()
//                .mapToDouble(MedicationDispensing::getPatientResponsibility)
//                .sum();
//
//        double insuranceCoverage = dispensingRecords.stream()
//                .mapToDouble(MedicationDispensing::getInsuranceCoverage)
//                .sum();
//
//        claim.setTotalAmount(totalAmount);
//        claim.setPatientResponsibility(patientResponsibility);
//        claim.setInsuranceCoverage(insuranceCoverage);
//        claim.setClaimType("PHARMACY");
//        claim.setServiceDate(dispensingRecords.get(0).getDispensingDate().toLocalDate());
//
//        // Save claim
//        Claim savedClaim = claimRepository.save(claim);
//
//        // Create claim items from dispensing items
//        List<ClaimItem> claimItems = new ArrayList<>();
//
//        for (MedicationDispensing dispensing : dispensingRecords) {
//            // Get all medication items for this dispensing
//            List<MedicationDispensingItem> medicationItems =
//                    dispensingItemRepository.findByDispensing(dispensing);
//
//            for (MedicationDispensingItem medicationItem : medicationItems) {
//                ClaimItem claimItem = new ClaimItem();
//                claimItem.setItemUuid(UUID.randomUUID().toString());
//                claimItem.setClaim(savedClaim);
//                claimItem.setServiceCode(medicationItem.getMedicationCode());
//                claimItem.setServiceName(medicationItem.getMedicationName());
//                claimItem.setQuantity(medicationItem.getQuantity());
//                claimItem.setUnitPrice(medicationItem.getUnitPrice());
//                claimItem.setTotalPrice(medicationItem.getTotalPrice());
//                claimItem.setItemType("MEDICATION");
//                claimItem.setItemDescription(
//                        medicationItem.getMedicationName() + " " +
//                                (medicationItem.getStrength() != null ? medicationItem.getStrength() : "") + " " +
//                                (medicationItem.getFormulation() != null ? medicationItem.getFormulation() : "")
//                );
//
//                // Calculate coverage based on dispensing record percentages
//                double itemCoveragePercent = dispensing.getInsuranceCoverage() / dispensing.getTotalAmount() * 100;
//                double itemCoverage = medicationItem.getTotalPrice() * (itemCoveragePercent / 100);
//                double itemPatientPortion = medicationItem.getTotalPrice() - itemCoverage;
//
//                claimItem.setInsuranceCoverage(itemCoverage);
//                claimItem.setPatientResponsibility(itemPatientPortion);
//
//                claimItems.add(claimItem);
//            }
//
//            // Update dispensing record to mark it as claimed
//            dispensing.setClaimStatus("CLAIMED");
//            dispensing.setClaimUuid(savedClaim.getClaimUuid());
//            dispensingRepository.save(dispensing);
//        }
//
//        // Save all claim items
//        claimItemRepository.saveAll(claimItems);
//
//        // Return response with claim details
//        return ResponseEntity.ok(savedClaim);
//    }
//
//    /**
//     * Helper method to find an insured person based on the provided identifiers
//     */
//    private Insured findInsuredPerson(MedicationDispensingRequest request, Payer payer) {
//        Insured insured = null;
//
//        // Try to find by insurance ID first
//        if (request.getInsuranceId() != null && !request.getInsuranceId().isEmpty()) {
//            insured = insuredRepository.findByInsuranceId(request.getInsuranceId());
//        }
//
//        // If not found, try employee ID
//        if (insured == null && request.getEmployeeId() != null && !request.getEmployeeId().isEmpty()) {
//            insured = insuredRepository.findByEmployeeId(request.getEmployeeId());
//        }
//
//        // If not found, try national ID
//        if (insured == null && request.getNationalId() != null && !request.getNationalId().isEmpty()) {
//            insured = insuredRepository.findByNationalId(request.getNationalId());
//        }
//
//        // If not found, try phone number
//        if (insured == null && request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
//            insured = insuredRepository.findByPhoneNumber(request.getPhoneNumber());
//        }
//
//        // If still not found, throw exception
//        if (insured == null) {
//            throw new ResourceNotFoundException("Insured Person", "identifiers",
//                    "Insurance ID: " + request.getInsuranceId() +
//                            ", Employee ID: " + request.getEmployeeId() +
//                            ", National ID: " + request.getNationalId() +
//                            ", Phone: " + request.getPhoneNumber());
//        }
//
//        // Verify insured belongs to the specified payer
//        if (!insured.getPayerUuid().equals(payer.getPayerUuid())) {
//            throw new BadRequestException("Insured person does not belong to the specified payer");
//        }
//
//        return insured;
//    }
//}