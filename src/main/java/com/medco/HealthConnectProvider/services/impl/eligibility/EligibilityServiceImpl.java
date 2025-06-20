package com.medco.HealthConnectProvider.services.impl.eligibility;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.groups.ContractDetailEmployeeGroup;
import com.medco.HealthConnectProvider.entity.groups.DependantGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeInsuredGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.group.ContractDetailEmployeeGroupRepository;
import com.medco.HealthConnectProvider.repository.group.DependantGroupRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeInsuredGroupRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.services.eligibility.EligibilityService;
import com.medco.HealthConnectProvider.services.payer.PayerService;
import com.medco.HealthConnectProvider.services.persons.InsuredService;
import com.medco.HealthConnectProvider.ui.request.eligibility.EligibilityCheckRequest;
import com.medco.HealthConnectProvider.ui.response.eligibility.*;
import com.medco.HealthConnectProvider.ui.response.persons.DependantResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredSearchResponse;
import com.medco.HealthConnectProvider.ui.response.persons.MultipleInsuredResponse;
import com.medco.HealthConnectProvider.utils.enums.Relationship;
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
    private EmployeeInsuredGroupRepository employeeInsuredGroupRepository;

    @Autowired
    private DependantRepository dependantRepository;

    @Autowired
    private DependantGroupRepository dependantGroupRepository;

    @Autowired
    private InsuredService insuredService;

    @Autowired
    private ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository;

//    @Override
//    public ResponseEntity<?> checkEligibility(String providerUuid, EligibilityCheckRequest request) {
//        Provider provider = providerRepository.findByProviderUuid(providerUuid)
//                .orElseThrow(() -> new ResourceNotFoundException("Provider", "providerUuid", providerUuid));
//
//        List<InsuredSearchResponse> insuredList = insuredService.searchInsuredPersons(
//                request.getPhoneNumber(), request.getEmployeeId(), request.getInsuranceId(), request.getNationalId());
//
//        if (insuredList.isEmpty()) {
//            throw new ResourceNotFoundException("Insured Person", "provided identifiers", "Not found");
//        }
//
//        if (insuredList.size() > 1) {
//            // Return the list of insured persons for selection
//            return ResponseEntity.ok(new MultipleInsuredResponse(insuredList));
//        }
//
//        // If only one insured person is found, proceed with eligibility check
//        return checkEligibilityForInsured(provider, insuredList.get(0), request.getServiceUuid());
//    }

    @Override
    public ResponseEntity<EligibilityResponse> checkEligibilityForInsured(String providerUuid, InsuredSearchResponse insured, String serviceUuid) {
        // Create a new EligibilityCheckRequest from the InsuredSearchResponse
        EligibilityCheckRequest request = new EligibilityCheckRequest();
        request.setInsuranceId(insured.getInsuranceId());
        request.setEmployeeId(insured.getEmployeeId());
        request.setNationalId(insured.getNationalId());
        request.setPhoneNumber(insured.getPhone());
        request.setServiceUuid(serviceUuid);

        // Perform the eligibility check
        Provider provider = providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", "providerUuid", providerUuid));

        Payer payer = payerRepository.findByPayerUuid(insured.getPayerUuid());
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", insured.getPayerUuid());
        }

        Insured insuredPerson = insuredRepository.findByInsuredUuid(insured.getInsuredUuid());
        if (insuredPerson == null) {
            throw new ResourceNotFoundException("Insured", "insuredUuid", insured.getInsuredUuid());
        }

        ContractHeader contract = contractHeaderRepository.findActiveContractBetweenProviderAndPayer(
                providerUuid, payer.getPayerUuid(), Status.ACTIVE);
        if (contract == null) {
            throw new BadRequestException("No active contract exists between this provider and payer");
        }

        // Check if insured person's policy is active
        boolean isPolicyActive = insuredPerson.getStatus() == Status.ACTIVE &&
                (insuredPerson.getPolicyStartDate() == null || LocalDate.now().isAfter(insuredPerson.getPolicyStartDate())) &&
                (insuredPerson.getPolicyEndDate() == null || LocalDate.now().isBefore(insuredPerson.getPolicyEndDate()));

        // Build eligibility response
        EligibilityResponse response = new EligibilityResponse();

        // Set insured person details
        BeanUtils.copyProperties(insured, response);
        response.setPayerUuid(payer.getPayerUuid());

        // Set policy details
        response.setPolicyNumber(insuredPerson.getPolicyNumber());
        response.setPolicyStartDate(insuredPerson.getPolicyStartDate());
        response.setPolicyEndDate(insuredPerson.getPolicyEndDate());
        response.setPolicyActive(isPolicyActive);

        // Get groups the insured belongs to
        List<GroupMembershipResponse> groupResponses = getInsuredGroups(insuredPerson);
        response.setGroups(groupResponses);

        // Set dependents if any
        if (insured.getDependants() != null && !insured.getDependants().isEmpty()) {
            response.setDependents(insured.getDependants().stream()
                    .map(this::mapDependantResponseToDependentEligibilityResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setDependents(new ArrayList<>());
        }

        // Check specific service eligibility if requested
        if (serviceUuid != null && !serviceUuid.isEmpty()) {
            response.setRequestedService(checkServiceEligibility(
                    serviceUuid,
                    contract,
                    provider,
                    insuredPerson,
                    groupResponses));
        } else {
            response.setRequestedService(null);
        }

        // Determine overall eligibility
        boolean isEligible = isPolicyActive && insuredPerson.getStatus() == Status.ACTIVE;
        response.setEligible(isEligible);

        if (!isEligible) {
            if (!isPolicyActive) {
                response.setIneligibilityReason("Policy is not active or has expired");
            } else if (insuredPerson.getStatus() != Status.ACTIVE) {
                response.setIneligibilityReason("Insured person's status is not active");
            }
        }

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<?> checkEligibility(String identifier) {

        List<InsuredSearchResponse> insuredList = insuredService.searchInsuredPersons(identifier);

        if (insuredList.isEmpty()) {

            throw new ResourceNotFoundException("Insured Person", "identifier", identifier);

        }

        if (insuredList.size() > 1) {
            return ResponseEntity.ok(new MultipleInsuredResponse(insuredList));
        }

        return checkEligibilityForInsured(insuredList.get(0));
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
        // You might need to implement a method to get the groups for an insured person
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
        // This is a placeholder implementation; replace with your actual logic
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

        ContractHeader contract = contractHeaderRepository.findActiveContractBetweenProviderAndPayer(
                provider.getProviderUuid(), payer.getPayerUuid(), Status.ACTIVE);
        if (contract == null) {
            throw new BadRequestException("No active contract exists between this provider and payer");
        }

        // Check if insured person's policy is active
        boolean isPolicyActive = insured.getStatus() == Status.ACTIVE &&
                (insured.getPolicyStartDate() == null || LocalDate.now().isAfter(insured.getPolicyStartDate())) &&
                (insured.getPolicyEndDate() == null || LocalDate.now().isBefore(insured.getPolicyEndDate()));

        // Build eligibility response
        EligibilityResponse response = new EligibilityResponse();

        // Set insured person details
        BeanUtils.copyProperties(insuredResponse, response);
        response.setPayerUuid(payer.getPayerUuid());
        response.setPhoneNumber(insuredResponse.getPhone());
        response.setBirthDate(insuredResponse.getBirthDate());

        // Set policy details
        response.setPolicyNumber(insured.getPolicyNumber());
        response.setPolicyStartDate(insured.getPolicyStartDate());
        response.setPolicyEndDate(insured.getPolicyEndDate());
        response.setPolicyActive(isPolicyActive);

        // Get groups the insured belongs to
        List<GroupMembershipResponse> groupResponses = getInsuredGroups(insured);
        response.setGroups(groupResponses);

        // Set dependents if any
        if (insuredResponse.getDependants() != null && !insuredResponse.getDependants().isEmpty()) {
            response.setDependents(insuredResponse.getDependants().stream()
                    .map(this::mapDependantResponseToDependentEligibilityResponse)
                    .collect(Collectors.toList()));
        } else {
            response.setDependents(new ArrayList<>());
        }

        // Check specific service eligibility if requested
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

        // Determine overall eligibility
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
        // Get all groups for this insured
        List<EmployeeInsuredGroup> employeeGroups = employeeInsuredGroupRepository
                .findByEmployeeInsuredUuidAndIsDeleted(insured.getInsuredUuid(), false);

        return employeeGroups.stream()
                .map(group -> {
                    EmployeeDependantGroup employeeGroup = group.getEmployeeDependantGroup();

                    GroupMembershipResponse response = new GroupMembershipResponse();
                    response.setGroupUuid(employeeGroup.getGroupUuid());
                    response.setGroupName(employeeGroup.getGroupName());
                    response.setGroupDescription(employeeGroup.getGroupDescription());
                    response.setGroupType(employeeGroup.getType() != null ? employeeGroup.getType().toString() : "Employee");
                    response.setGroupStatus(employeeGroup.getStatus());

                    return response;
                })
                .collect(Collectors.toList());
    }

    private List<DependentEligibilityResponse> getDependentsEligibility(List<Dependant> dependants) {
        return dependants.stream()
                .map(dependant -> {
                    DependentEligibilityResponse response = new DependentEligibilityResponse();
                    response.setDependantUuid(dependant.getDependantUuid());
                    response.setFirstName(dependant.getFirstName());
                    response.setFatherName(dependant.getFatherName());
                    response.setGrandFatherName(dependant.getGrandFatherName());
                    response.setRelationship(dependant.getRelationship() != null ?
                            Relationship.valueOf(dependant.getRelationship().toString()) : null);
                    response.setStatus(dependant.getStatus());

                    // Get groups for this dependant
                    List<DependantGroup> dependantGroups = dependantGroupRepository
                            .findByDependantUuidAndIsDeleted(dependant.getDependantUuid(), false);

                    List<GroupMembershipResponse> groupResponses = dependantGroups.stream()
                            .map(group -> {
                                EmployeeDependantGroup employeeGroup = group.getEmployeeDependantGroup();

                                GroupMembershipResponse groupResponse = new GroupMembershipResponse();
                                groupResponse.setGroupUuid(employeeGroup.getGroupUuid());
                                groupResponse.setGroupName(employeeGroup.getGroupName());
                                groupResponse.setGroupDescription(employeeGroup.getGroupDescription());
                                groupResponse.setGroupType(employeeGroup.getType() != null ?
                                        employeeGroup.getType().toString() : "Dependant");
                                groupResponse.setGroupStatus(employeeGroup.getStatus());

                                return groupResponse;
                            })
                            .collect(Collectors.toList());

                    response.setGroups(groupResponses);

                    return response;
                })
                .collect(Collectors.toList());
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
            return calculateCoverage(response, bestContractDetail, contractHeader);
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

        // If no group-specific contract detail found, use a general one if available
        if (bestContractDetail == null) {
            bestContractDetail = findGeneralContractDetail(contractDetails);
        }

        // If still no contract detail found, use the first one as fallback
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
            ContractHeader contractHeader) {
        response.setCovered(true);
        response.setPrice(bestContractDetail.getNegotiatedPrice());

        Double coPaymentPercentage = contractHeader.getCoPaymentPercentage() != null ?
                contractHeader.getCoPaymentPercentage() : 20.0; // Default 20%

        response.setCoPaymentPercentage(coPaymentPercentage);

        BigDecimal price = bestContractDetail.getNegotiatedPrice() != null ?
                bestContractDetail.getNegotiatedPrice() : BigDecimal.ZERO;
        BigDecimal coPaymentPercentageBD = BigDecimal.valueOf(coPaymentPercentage / 100.0);
        BigDecimal coPaymentAmount = price.multiply(coPaymentPercentageBD);
        response.setCoPaymentAmount(coPaymentAmount);
        response.setInsuranceCoverage(price.subtract(coPaymentAmount));

        // Set applied group info if available
//        if (bestContractDetail.getAppliedGroupUuid() != null) {
//            response.setAppliedGroupUuid(bestContractDetail.getAppliedGroupUuid());
//            response.setAppliedGroupName(bestContractDetail.getAppliedGroupName());
//        }

        return response;
    }
}