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
import com.medco.HealthConnectProvider.ui.request.eligibility.EligibilityCheckRequest;
import com.medco.HealthConnectProvider.ui.response.eligibility.*;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EligibilityServiceImpl implements EligibilityService {

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
    private ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository;

    @Override
    public ResponseEntity<EligibilityResponse> checkEligibility(String providerUuid, EligibilityCheckRequest request) {
        // Validate provider exists
        Provider provider = providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Provider", "providerUuid", providerUuid));

        // Validate payer exists
        Payer payer = payerRepository.findByPayerUuid(request.getPayerUuid());
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", request.getPayerUuid());
        }

        // Validate contract exists between provider and payer
        ContractHeader contract = contractHeaderRepository.findActiveContractBetweenProviderAndPayer(
                providerUuid, request.getPayerUuid(), Status.ACTIVE);
        if (contract == null) {
            throw new BadRequestException("No active contract exists between this provider and payer");
        }

        // Find insured person by one of the provided identifiers
        Insured insured = findInsuredPerson(request, payer);

        // Check if insured person's policy is active
        boolean isPolicyActive = insured.getStatus() == Status.ACTIVE &&
                LocalDate.now().isAfter(insured.getPolicyStartDate()) &&
                LocalDate.now().isBefore(insured.getPolicyEndDate());

        // Build eligibility response
        EligibilityResponse response = new EligibilityResponse();

        // Set insured person details
        response.setInsuredUuid(insured.getInsuredUuid());
        response.setEmployeeId(insured.getEmployeeId());
        response.setFirstName(insured.getFirstName());
        response.setFatherName(insured.getFatherName());
        response.setGrandFatherName(insured.getGrandFatherName());
        response.setInsuranceId(insured.getInsuranceId());
        response.setNationalId(insured.getNationalId());
        response.setPhoneNumber(insured.getPhone());
        response.setStatus(insured.getStatus());

        // Set payer details
        response.setPayerUuid(payer.getPayerUuid());
        response.setPayerName(payer.getPayerName());

        // Set policy details
        response.setPolicyNumber(insured.getPolicyNumber());
        response.setPolicyStartDate(insured.getPolicyStartDate());
        response.setPolicyEndDate(insured.getPolicyEndDate());
        response.setPolicyActive(isPolicyActive);

        // Get groups the insured belongs to
        List<GroupMembershipResponse> groupResponses = getInsuredGroups(insured);
        response.setGroups(groupResponses);

        // Set dependents if any
        if (insured.getDependants() != null && !insured.getDependants().isEmpty()) {
            response.setDependents(getDependentsEligibility(insured.getDependants()));
        } else {
            response.setDependents(new ArrayList<>());
        }

        // Check specific service eligibility if requested
        if (request.getServiceUuid() != null && !request.getServiceUuid().isEmpty()) {
            response.setRequestedService(checkServiceEligibility(
                    request.getServiceUuid(),
                    contract,
                    provider,
                    insured,
                    groupResponses));
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

    private Insured findInsuredPerson(EligibilityCheckRequest request, Payer payer) {
        Insured insured = null;

        // Try to find by employee ID
        if (request.getEmployeeId() != null && !request.getEmployeeId().isEmpty()) {
            insured = insuredRepository.findByEmployeeIdAndPayer(request.getEmployeeId(), payer.getPayerUuid());
        }

        // If not found, try by insurance ID
        if (insured == null && request.getInsuranceId() != null && !request.getInsuranceId().isEmpty()) {
            insured = insuredRepository.findByInsuranceIdAndPayer(request.getInsuranceId(), payer.getPayerUuid());
        }

        // If not found, try by national ID
        if (insured == null && request.getNationalId() != null && !request.getNationalId().isEmpty()) {
            insured = insuredRepository.findByNationalIdAndPayer(request.getNationalId(), payer.getPayerUuid());
        }

        // If still not found, try by phone number
        if (insured == null && request.getPhoneNumber() != null && !request.getPhoneNumber().isEmpty()) {
            insured = insuredRepository.findByPhoneAndPayer(request.getPhoneNumber(), payer.getPayerUuid());
        }

        // If not found by any identifier, throw exception
        if (insured == null) {
            throw new ResourceNotFoundException("Insured Person", "identifiers",
                    "Employee ID: " + request.getEmployeeId() +
                            ", Insurance ID: " + request.getInsuranceId() +
                            ", National ID: " + request.getNationalId() +
                            ", Phone: " + request.getPhoneNumber());
        }

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
                            dependant.getRelationship().toString() : null);
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

        // Find the service
        Servicelist service = servicelistRepository.findByServiceUuid(serviceUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceUuid", serviceUuid));

        ServiceEligibilityResponse response = new ServiceEligibilityResponse();
        response.setServiceUuid(service.getServiceUuid());
        response.setServiceCode(service.getServiceCode());
        response.setServiceName(service.getServiceName());
        response.setCategory(service.getServiceCategory());
        response.setSubCategory(service.getServiceSubCategory());

        // Find contract details for this service under the contract
        List<ContractDetail> contractDetails = contractDetailRepository
                .findByContractHeaderAndService(contractHeader.getContractHeaderUuid(), serviceUuid);

        if (contractDetails.isEmpty()) {
            // Service not covered under this contract
            response.setCovered(false);
            response.setPrice(service.getPrice());
            response.setCoPaymentAmount(service.getPrice());
            response.setCoPaymentPercentage(100.0);
            response.setInsuranceCoverage(BigDecimal.valueOf(0.0));
            return response;
        }

        // Get the group UUIDs for the insured person
        Set<String> insuredGroupUuids = insuredGroups.stream()
                .map(GroupMembershipResponse::getGroupUuid)
                .collect(Collectors.toSet());

        // Find the best contract detail for this service based on group membership
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
                    // Found a contract detail for one of the insured's groups
                    if (bestContractDetail == null ||
                            (detail.getNegotiatedPrice() != null && bestContractDetail.getNegotiatedPrice() != null &&
                                    detail.getNegotiatedPrice().compareTo(bestContractDetail.getNegotiatedPrice())<0)) {
                        bestContractDetail = detail;
                        appliedGroupUuid = groupUuid;
                        appliedGroupName = groupAssociation.getEmployeeDependantGroup().getGroupName();
                    }
                }
            }
        }

        // If no group-specific contract detail found, use a general one if available
        if (bestContractDetail == null) {
            for (ContractDetail detail : contractDetails) {
                List<ContractDetailEmployeeGroup> groupAssociations = contractDetailEmployeeGroupRepository
                        .findByContractDetailUuid(detail.getContractDetailUuid());

                if (groupAssociations.isEmpty()) {
                    // This is a general contract detail not tied to any specific group
                    if (bestContractDetail == null ||
                            (detail.getNegotiatedPrice() != null && bestContractDetail.getNegotiatedPrice() != null &&
                                    detail.getNegotiatedPrice().compareTo(bestContractDetail.getNegotiatedPrice())<0)) {
                        bestContractDetail = detail;
                    }
                }
            }
        }

        // If still no contract detail found, use the first one as fallback
        if (bestContractDetail == null && !contractDetails.isEmpty()) {
            bestContractDetail = contractDetails.get(0);
        }

        // Calculate coverage based on the best contract detail
        if (bestContractDetail != null) {
            response.setCovered(true);
            response.setPrice(bestContractDetail.getNegotiatedPrice());

            // Get co-payment percentage from contract header or use default
            Double coPaymentPercentage = contractHeader.getCoPaymentPercentage() != null ?
                    contractHeader.getCoPaymentPercentage() : 20.0; // Default 20%

            response.setCoPaymentPercentage(coPaymentPercentage);

            // Calculate co-payment amount
            BigDecimal price = bestContractDetail.getNegotiatedPrice() != null ?
                    bestContractDetail.getNegotiatedPrice() : BigDecimal.ZERO;
            BigDecimal coPaymentPercentageBD = BigDecimal.valueOf(coPaymentPercentage / 100.0);
            BigDecimal coPaymentAmount = price.multiply(coPaymentPercentageBD);
            response.setCoPaymentAmount(coPaymentAmount);
            response.setInsuranceCoverage(price.subtract(coPaymentAmount));

            if (appliedGroupUuid != null) {
                response.setAppliedGroupUuid(appliedGroupUuid);
                response.setAppliedGroupName(appliedGroupName);
            }
        } else {

            response.setCovered(false);
            response.setPrice(service.getPrice());
            response.setCoPaymentAmount(service.getPrice());
            response.setCoPaymentPercentage(100.0);
            response.setInsuranceCoverage(BigDecimal.valueOf(0.0));
        }

        return response;
    }
}