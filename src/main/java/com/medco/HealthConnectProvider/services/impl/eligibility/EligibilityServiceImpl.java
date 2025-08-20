package com.medco.HealthConnectProvider.services.impl.eligibility;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.groups.ContractDetailEmployeeGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.group.ContractDetailEmployeeGroupRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.ServiceCategoryMappingRepository;
import com.medco.HealthConnectProvider.entity.packageCategory.ServiceCategoryMapping;
import com.medco.HealthConnectProvider.services.eligibility.EligibilityService;
import com.medco.HealthConnectProvider.services.packageCategory.PackageCategoryLimitService;
import com.medco.HealthConnectProvider.services.persons.InsuredService;
import com.medco.HealthConnectProvider.ui.response.packageCategory.CategoryLimitSummaryResponse;
import com.medco.HealthConnectProvider.ui.request.eligibility.EligibilityCheckRequest;
import com.medco.HealthConnectProvider.ui.response.eligibility.*;
import com.medco.HealthConnectProvider.ui.response.persons.DependantResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredSearchResponse;
import com.medco.HealthConnectProvider.ui.response.persons.MultipleInsuredResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Service
public class EligibilityServiceImpl implements EligibilityService {

    private final Logger log = LoggerFactory.getLogger(EligibilityServiceImpl.class);

    @Autowired
    private PayerRepository payerRepository;

    @Autowired
    private InsuredRepository insuredRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private ContractRepository contractHeaderRepository;

    @Autowired
    private ContractDetailRepository contractDetailRepository;

    @Autowired
    private ServicelistRepository servicelistRepository;

    @Autowired
    private EmployeeDependantGroupRepository employeeDependantGroupRepository;

    @Autowired
    private DependantRepository dependantRepository;

    @Autowired
    private InsuredService insuredService;

    @Autowired
    private ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository;

    @Autowired
    private PackageCategoryLimitService packageCategoryLimitService;

    @Autowired
    private ServiceCategoryMappingRepository serviceCategoryMappingRepository;


    @Override
    public ResponseEntity<EligibilityResponse> checkEligibilityForInsured(String providerUuid, InsuredSearchResponse insured, String serviceUuid) {
        log.info("Starting eligibility check for insured: {}, provider: {}, service: {}", insured.getInsuredUuid(), providerUuid, serviceUuid);

        try {
            EligibilityCheckRequest request = new EligibilityCheckRequest();
            request.setInsuranceId(insured.getInsuranceId());
            request.setEmployeeId(insured.getEmployeeId());
            request.setNationalId(insured.getNationalId());
            request.setPhoneNumber(insured.getPhone());
            request.setServiceUuid(serviceUuid);

            log.info("Validating provider: {}", providerUuid);
            Provider provider = providerRepository.findByProviderUuid(providerUuid);
            if (provider == null) {
                throw new ResourceNotFoundException("Provider", "providerUuid", providerUuid);
            }

            log.info("Validating payer: {}", insured.getPayerUuid());
            Payer payer = payerRepository.findByPayerUuid(insured.getPayerUuid());
            if (payer == null) {
                throw new ResourceNotFoundException("Payer", "payerUuid", insured.getPayerUuid());
            }

            log.info("Validating insured person: {}", insured.getInsuredUuid());
            Insured insuredPerson = insuredRepository.findByInsuredUuid(insured.getInsuredUuid());
            if (insuredPerson == null) {
                throw new ResourceNotFoundException("Insured", "insuredUuid", insured.getInsuredUuid());
            }

            log.info("Checking for active contract between provider and payer");
            List<ContractHeader> activeContracts = contractHeaderRepository.findActiveContractsBetweenProviderAndPayer(
                    providerUuid, payer.getPayerUuid(), Status.ACTIVE);

            if (activeContracts.isEmpty()) {
                throw new BadRequestException("No active contract exists between this provider and payer");
            }

            ContractHeader contract = activeContracts.get(0);
            log.info("Found active contract: {}", contract.getContractHeaderUuid());

            boolean isPolicyActive = insuredPerson.getStatus() == Status.ACTIVE &&
                    (insuredPerson.getPolicyStartDate() == null || LocalDate.now().isAfter(insuredPerson.getPolicyStartDate())) &&
                    (insuredPerson.getPolicyEndDate() == null || LocalDate.now().isBefore(insuredPerson.getPolicyEndDate()));

            log.info("Creating eligibility response");
            EligibilityResponse response = new EligibilityResponse();
            BeanUtils.copyProperties(insured, response);
            response.setPayerUuid(payer.getPayerUuid());
            response.setPolicyNumber(insuredPerson.getPolicyNumber());
            response.setPolicyStartDate(insuredPerson.getPolicyStartDate());
            response.setPolicyEndDate(insuredPerson.getPolicyEndDate());
            response.setPolicyActive(isPolicyActive);

            log.info("Getting insured groups");
            List<GroupMembershipResponse> groupResponses = getInsuredGroups(insuredPerson);
            response.setGroups(groupResponses);

            if (insured.getDependants() != null && !insured.getDependants().isEmpty()) {
                log.info("Processing dependants");
                response.setDependents(insured.getDependants().stream()
                        .map(this::mapDependantResponseToDependentEligibilityResponse)
                        .collect(Collectors.toList()));
            } else {
                response.setDependents(new ArrayList<>());
            }

            if (serviceUuid != null && !serviceUuid.isEmpty()) {
                log.info("Checking service eligibility for service: {}", serviceUuid);
                response.setRequestedService(checkServiceEligibility(
                        serviceUuid,
                        contract,
                        provider,
                        insuredPerson,
                        groupResponses));
            } else {
                response.setRequestedService(null);
            }

            boolean isEligible = isPolicyActive && insuredPerson.getStatus() == Status.ACTIVE;
            response.setEligible(isEligible);

            if (!isEligible) {
                log.warn("Insured person is not eligible");
                if (!isPolicyActive) {
                    response.setIneligibilityReason("Policy is not active or has expired");
                } else if (insuredPerson.getStatus() != Status.ACTIVE) {
                    response.setIneligibilityReason("Insured person's status is not active");
                }
            }

            log.info("Eligibility check completed successfully");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error during eligibility check", e);
            throw new BadRequestException("Error during eligibility check: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> checkEligibility(String identifier) {

        MultipleInsuredResponse multiple = insuredService.searchInsuredPersons(identifier);

        if (multiple == null || multiple.isEmpty()) {
            throw new ResourceNotFoundException("Insured Person", "identifier", identifier);
        }

        // Customize the message based on result count for this flow
        if (multiple.size() == 1) {
            multiple.setMessage("One insured person found.");
        } else {
            multiple.setMessage("Multiple insured persons found. Please select one.");
        }

        return ResponseEntity.ok(multiple);

    }


    private ResponseEntity<EligibilityResponse> checkEligibilityForInsured(InsuredSearchResponse insured) {
        EligibilityResponse response = new EligibilityResponse();

        BeanUtils.copyProperties(insured, response);
        response.setPhoneNumber(insured.getPhone());
        response.setStatus(insured.getStatus() != null ? Status.valueOf(insured.getStatus().toString()) : null);

        boolean isPolicyActive = isPolicyActive(insured);
        response.setPolicyActive(isPolicyActive);
        response.setEligible(isPolicyActive && insured.getStatus() == Status.ACTIVE);

        if (!isPolicyActive) {
            response.setIneligibilityReason("Policy is not active or has expired");
        } else if (insured.getStatus() != Status.ACTIVE) {
            response.setIneligibilityReason("Insured person's status is not active");
        }

        // Set groups if available
        //  implement a method to get the groups for an insured person
        // response.setGroups(getInsuredGroups(insured));

        if (insured.getDependants() != null) {
            response.setDependents(insured.getDependants().stream()
                    .map(this::mapDependantResponseToDependentEligibilityResponse)
                    .collect(Collectors.toList()));
        }

        return ResponseEntity.ok(response);
    }


    private boolean isPolicyActive(InsuredSearchResponse insured) {
        // Implement the logic to check if the policy is active
        // This is a placeholder implementation
        return insured.getStatus() == Status.ACTIVE;
    }

    private ResponseEntity<EligibilityResponse> checkEligibilityForInsured(Provider provider, InsuredSearchResponse insuredResponse, String serviceUuid) {
        Insured insured = insuredRepository.findByInsuredUuid(insuredResponse.getInsuredUuid());
        if (insured == null) {
            throw new ResourceNotFoundException("Insured", "insuredUuid", insuredResponse.getInsuredUuid());
        }

        Payer payer = payerRepository.findByPayerName(insuredResponse.getPayerName());
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerName", insuredResponse.getPayerName());
        }

        ContractHeader contract = (ContractHeader) contractHeaderRepository.findActiveContractsBetweenProviderAndPayer(
                provider.getProviderUuid(), payer.getPayerUuid(), Status.ACTIVE);
        if (contract == null) {
            throw new BadRequestException("No active contract exists between this provider and payer");
        }

        boolean isPolicyActive = insured.getStatus() == Status.ACTIVE &&
                (insured.getPolicyStartDate() == null || LocalDate.now().isAfter(insured.getPolicyStartDate())) &&
                (insured.getPolicyEndDate() == null || LocalDate.now().isBefore(insured.getPolicyEndDate()));

        EligibilityResponse response = new EligibilityResponse();

        BeanUtils.copyProperties(insuredResponse, response);
        response.setPayerUuid(payer.getPayerUuid());
        response.setPhoneNumber(insuredResponse.getPhone());
        response.setBirthDate(insuredResponse.getBirthDate());

        response.setPolicyNumber(insured.getPolicyNumber());
        response.setPolicyStartDate(insured.getPolicyStartDate());
        response.setPolicyEndDate(insured.getPolicyEndDate());
        response.setPolicyActive(isPolicyActive);

        List<GroupMembershipResponse> groupResponses = getInsuredGroups(insured);
        response.setGroups(groupResponses);

        if (insuredResponse.getDependants() != null && !insuredResponse.getDependants().isEmpty()) {
            response.setDependents(insuredResponse.getDependants().stream()
                    .map(this::mapDependantResponseToDependentEligibilityResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setDependents(new ArrayList<>());
        }

        if (serviceUuid != null && !serviceUuid.isEmpty() && !serviceUuid.equals("string")) {
            try {
                UUID.fromString(serviceUuid);
                response.setRequestedService(checkServiceEligibility(
                        serviceUuid,
                        contract,
                        provider,
                        insured,
                        groupResponses));
            } catch (IllegalArgumentException e) {
                log.warn("Invalid serviceUuid provided: {}", serviceUuid);
                response.setRequestedService(null);
            }
        } else {
            response.setRequestedService(null);
        }

        boolean isEligible = isPolicyActive && insured.getStatus() == Status.ACTIVE;
        response.setEligible(isEligible);

        if (!isEligible) {
            if (!isPolicyActive) {
                response.setIneligibilityReason("Policy is not active or has expired");
            } else if (insured.getStatus() != Status.ACTIVE) {
                response.setIneligibilityReason("Insured person's status is not active");
            }
        }

        return ResponseEntity.ok(response);
    }

    private DependentEligibilityResponse mapDependantResponseToDependentEligibilityResponse(DependantResponse dependantResponse) {
        DependentEligibilityResponse dependentEligibilityResponse = new DependentEligibilityResponse();
        BeanUtils.copyProperties(dependantResponse, dependentEligibilityResponse);
        dependentEligibilityResponse.setEligible(dependantResponse.getStatus() == Status.ACTIVE);
        dependentEligibilityResponse.setBirthDate(dependantResponse.getBirthDate());
        dependentEligibilityResponse.setRelationship(dependantResponse.getRelationship());
        return dependentEligibilityResponse;
    }

    private Insured findInsuredPerson(EligibilityCheckRequest request, Payer payer) {
        Insured insured = null;
        String foundBy = "";

        if (request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            insured = insuredRepository.findByPhoneAndPayer(request.getPhoneNumber(), payer.getPayerUuid());
            if (insured != null) {
                foundBy = "phone number";
            }
        }

        if (insured == null && request.getEmployeeId() != null && !request.getEmployeeId().isEmpty()) {
            insured = insuredRepository.findByEmployeeIdAndPayer(request.getEmployeeId(), payer.getPayerUuid());
            if (insured != null) {
                foundBy = "employee ID";
            }
        }

        if (insured == null && request.getInsuranceId() != null && !request.getInsuranceId().isEmpty()) {
            insured = insuredRepository.findByInsuranceIdAndPayer(request.getInsuranceId(), payer.getPayerUuid());
            if (insured != null) {
                foundBy = "insurance ID";
            }
        }

        if (insured == null && request.getNationalId() != null && !request.getNationalId().isEmpty()) {
            insured = insuredRepository.findByNationalIdAndPayer(request.getNationalId(), payer.getPayerUuid());
            if (insured != null) {
                foundBy = "national ID";
            }
        }

        if (insured == null) {
            throw new ResourceNotFoundException("Insured Person", "identifiers",
                    "Phone: " + request.getPhoneNumber() +
                            ", Employee ID: " + request.getEmployeeId() +
                            ", Insurance ID: " + request.getInsuranceId() +
                            ", National ID: " + request.getNationalId());
        }

        log.info("Insured person found by: {}", foundBy);

        return insured;
    }

    private List<GroupMembershipResponse> getInsuredGroups(Insured insured) {
        List<EmployeeDependantGroup> employeeGroups = employeeDependantGroupRepository
                .findByInsuredsAndIsDeleted(insured, false);

        return employeeGroups.stream()
                .map(this::mapToGroupMembershipResponse)
                .collect(Collectors.toList());
    }

    private GroupMembershipResponse mapToGroupMembershipResponse(EmployeeDependantGroup group) {
        GroupMembershipResponse response = new GroupMembershipResponse();
        response.setGroupUuid(group.getGroupUuid());
        response.setGroupName(group.getGroupName());
        response.setGroupDescription(group.getGroupDescription());
        response.setGroupType(group.getType() != null ? group.getType().toString() : "Employee");
        response.setGroupStatus(group.getStatus());
        return response;
    }


    private ServiceEligibilityResponse checkServiceEligibility(
            String serviceUuid,
            ContractHeader contractHeader,
            Provider provider,
            Insured insured,
            List<GroupMembershipResponse> insuredGroups) {

        Servicelist service = findService(serviceUuid);
        ServiceEligibilityResponse response = initializeServiceEligibilityResponse(service);

        List<ContractDetail> contractDetails = findContractDetails(contractHeader, serviceUuid);

        if (contractDetails.isEmpty()) {
            return handleUncoveredService(response, service);
        }

        Set<String> insuredGroupUuids = getInsuredGroupUuids(insuredGroups);
        ContractDetail bestContractDetail = findBestContractDetail(contractDetails, insuredGroupUuids);

        if (bestContractDetail != null) {
            return calculateCoverage(response, bestContractDetail, contractHeader, insured);
        } else {
            return handleUncoveredService(response, service);
        }
    }

    private Servicelist findService(String serviceUuid) {
        try {
            return servicelistRepository.findByServiceUuid(serviceUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceUuid", serviceUuid));
        } catch (Exception e) {
            log.error("Error finding service with UUID: {}", serviceUuid, e);
            throw new ResourceNotFoundException("Service", "serviceUuid", serviceUuid);
        }
    }
    private ServiceEligibilityResponse initializeServiceEligibilityResponse(Servicelist service) {
        ServiceEligibilityResponse response = new ServiceEligibilityResponse();
        response.setServiceUuid(service.getServiceUuid());
        response.setServiceCode(service.getServiceCode());
        response.setServiceName(service.getServiceName());
        response.setCategory(service.getServiceCategory());
        response.setSubCategory(service.getServiceSubCategory());
        return response;
    }

    private List<ContractDetail> findContractDetails(ContractHeader contractHeader, String serviceUuid) {
        return contractDetailRepository
                .findByContractHeaderAndService(contractHeader.getContractHeaderUuid(), serviceUuid);
    }

    private ServiceEligibilityResponse handleUncoveredService(ServiceEligibilityResponse response, Servicelist service) {
        response.setCovered(false);
        response.setPrice(BigDecimal.valueOf(service.getPrice()));
        response.setCoPaymentAmount(BigDecimal.valueOf(service.getPrice()));
        response.setCoPaymentPercentage(100.0);
        response.setInsuranceCoverage(BigDecimal.valueOf(0.0));
        return response;
    }

    private Set<String> getInsuredGroupUuids(List<GroupMembershipResponse> insuredGroups) {
        return insuredGroups.stream()
                .map(GroupMembershipResponse::getGroupUuid)
                .collect(Collectors.toSet());
    }

    private ContractDetail findBestContractDetail(List<ContractDetail> contractDetails, Set<String> insuredGroupUuids) {
        ContractDetail bestContractDetail = null;
        String appliedGroupUuid = null;
        String appliedGroupName = null;

        // First, try to find a contract detail specifically for one of the insured's groups
        for (ContractDetail detail : contractDetails) {
            List<ContractDetailEmployeeGroup> groupAssociations = contractDetailEmployeeGroupRepository
                    .findByContractDetailUuid(detail.getContractDetailUuid());

            for (ContractDetailEmployeeGroup groupAssociation : groupAssociations) {
                String groupUuid = groupAssociation.getEmployeeDependantGroup().getGroupUuid();

                if (insuredGroupUuids.contains(groupUuid)) {
                    if (isBetterContractDetail(detail, bestContractDetail)) {
                        bestContractDetail = detail;
                        appliedGroupUuid = groupUuid;
                        appliedGroupName = groupAssociation.getEmployeeDependantGroup().getGroupName();
                    }
                }
            }
        }

        if (bestContractDetail == null) {
            bestContractDetail = findGeneralContractDetail(contractDetails);
        }

        if (bestContractDetail == null && !contractDetails.isEmpty()) {
            bestContractDetail = contractDetails.get(0);
        }

        return bestContractDetail;
    }

    private boolean isBetterContractDetail(ContractDetail newDetail, ContractDetail currentBest) {
        return currentBest == null ||
                (newDetail.getNegotiatedPrice() != null && currentBest.getNegotiatedPrice() != null &&
                        newDetail.getNegotiatedPrice().compareTo(currentBest.getNegotiatedPrice()) < 0);
    }

    private ContractDetail findGeneralContractDetail(List<ContractDetail> contractDetails) {
        return contractDetails.stream()
                .filter(detail -> contractDetailEmployeeGroupRepository
                        .findByContractDetailUuid(detail.getContractDetailUuid()).isEmpty())
                .min(Comparator.comparing(ContractDetail::getNegotiatedPrice,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    private ServiceEligibilityResponse calculateCoverage(
            ServiceEligibilityResponse response,
            ContractDetail bestContractDetail,
            ContractHeader contractHeader,
            Insured insured) {
        response.setCovered(true);
        response.setPrice(BigDecimal.valueOf(bestContractDetail.getNegotiatedPrice()));

        Double coPaymentPercentage = contractHeader.getCoPaymentPercentage() != null ?
                contractHeader.getCoPaymentPercentage() : 20.0; // Default 20%

        response.setCoPaymentPercentage(coPaymentPercentage);

        BigDecimal price = bestContractDetail.getNegotiatedPrice() != null ?
                BigDecimal.valueOf(bestContractDetail.getNegotiatedPrice()) : BigDecimal.ZERO;
        BigDecimal coPaymentPercentageBD = BigDecimal.valueOf(coPaymentPercentage / 100.0);
        BigDecimal coPaymentAmount = price.multiply(coPaymentPercentageBD);
        response.setCoPaymentAmount(coPaymentAmount);
        response.setInsuranceCoverage(price.subtract(coPaymentAmount));

        // Add package category limit information
        addCategoryLimitInfo(response, bestContractDetail, insured, price);

        // Set applied group info if available
//        if (bestContractDetail.getAppliedGroupUuid() != null) {
//            response.setAppliedGroupUuid(bestContractDetail.getAppliedGroupUuid());
//            response.setAppliedGroupName(bestContractDetail.getAppliedGroupName());
//        }

        return response;
    }

    private void addCategoryLimitInfo(ServiceEligibilityResponse response,
                                     ContractDetail contractDetail,
                                     Insured insured,
                                     BigDecimal serviceAmount) {
        try {
            // Get category mappings for this service
            List<ServiceCategoryMapping> mappings = serviceCategoryMappingRepository
                    .findActiveByContractDetailUuid(contractDetail.getContractDetailUuid());

            if (mappings.isEmpty()) {
                response.setHasAvailableLimit(true);
                response.setCategoryLimits(new ArrayList<>());
                return;
            }

            List<ServiceEligibilityResponse.CategoryLimitInfo> categoryLimits = new ArrayList<>();
            List<String> limitWarnings = new ArrayList<>();

            for (ServiceCategoryMapping mapping : mappings) {
                if (!mapping.isConsumesFromLimit()) {
                    continue;
                }

                // Get category limit summary for this insured person
                CategoryLimitSummaryResponse summary = packageCategoryLimitService
                        .getCategoryLimitSummary(insured.getInsuredUuid(),
                                               contractDetail.getContractHeaderUuid());

                // Find the specific category limit
                summary.getCategoryLimits().stream()
                        .filter(limit -> limit.getCategoryUuid().equals(mapping.getPackageCategory().getCategoryUuid()))
                        .findFirst()
                        .ifPresent(limit -> {
                            ServiceEligibilityResponse.CategoryLimitInfo info =
                                    new ServiceEligibilityResponse.CategoryLimitInfo();
                            info.setCategoryUuid(limit.getCategoryUuid());
                            info.setCategoryName(limit.getCategoryName());
                            info.setCategoryCode(limit.getCategoryCode());
                            info.setLimitValue(limit.getLimitValue());
                            info.setUsedAmount(limit.getUsedAmount());
                            info.setRemainingAmount(limit.getRemainingAmount());
                            info.setPeriodType(limit.getPeriodType());
                            info.setExpired(limit.isExpired());
                            info.setUtilizationPercentage(limit.getUtilizationPercentage());

                            categoryLimits.add(info);

                            // Check if service amount would exceed remaining limit
                            if (limit.getRemainingAmount().compareTo(serviceAmount) < 0) {
                                limitWarnings.add(String.format(
                                        "Insufficient limit in category '%s' (Remaining: %s, Required: %s)",
                                        limit.getCategoryName(),
                                        limit.getRemainingAmount(),
                                        serviceAmount));
                            }
                        });
            }

            // Determine if all limits are available
            boolean hasAvailableLimit = limitWarnings.isEmpty();

            response.setCategoryLimits(categoryLimits);
            response.setHasAvailableLimit(hasAvailableLimit);
            response.setLimitWarning(limitWarnings.isEmpty() ? null : String.join("; ", limitWarnings));

        } catch (Exception e) {
            log.warn("Error checking category limits for service: {}", contractDetail.getServiceUuid(), e);
            response.setHasAvailableLimit(true);
            response.setCategoryLimits(new ArrayList<>());
            response.setLimitWarning("Unable to verify category limits");
        }
    }
}