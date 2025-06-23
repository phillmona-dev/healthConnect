package com.medco.HealthConnectProvider.services.impl.group;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserDetailsImpl;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.group.EmployeeInsuredGroupService;
import com.medco.HealthConnectProvider.ui.request.group.EmployeeInsuredGroupRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeInsuredGroupResponse;
import com.medco.HealthConnectProvider.utils.enums.GroupType;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EmployeeInsuredGroupServiceImpl implements EmployeeInsuredGroupService {

    @Autowired
    private EmployeeDependantGroupRepository employeeDependantGroupRepository;

    @Autowired
    private InsuredRepository insuredRepository;


    @Override
    @Transactional
    public ResponseEntity<?> addInsuredToGroup(EmployeeInsuredGroupRequest request) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        // Validate group exists
        EmployeeDependantGroup group = employeeDependantGroupRepository.findByGroupUuid(request.getGroupUuid());
        if (group == null) {
            throw new ResourceNotFoundException("Employee Group", "groupUuid", request.getGroupUuid());
        }

        // Validate group belongs to payer
        if (!group.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Group does not belong to this payer");
        }

        // Validate insured exists
        Insured insured = insuredRepository.findByInsuredUuid(request.getInsuredUuid());
        if (insured == null) {
            throw new ResourceNotFoundException("Insured Person", "insuredUuid", request.getInsuredUuid());
        }

        // Validate insured belongs to payer
        if (!insured.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Insured person does not belong to this payer");
        }

        // Check if association already exists
        if (group.getInsured() != null && group.getInsured().getInsuredUuid().equals(insured.getInsuredUuid())) {
            throw new BadRequestException("Insured person is already in this group");
        }

        // Set the group type to EMPLOYEE if not already set
        if (group.getType() == null) {
            group.setType(GroupType.EMPLOYEE);
        } else if (group.getType() != GroupType.EMPLOYEE) {
            throw new BadRequestException("This group is not for employees");
        }

        // Create new association
        group.setInsured(insured);
        group.setDependant(null); // Ensure no dependant is associated with this group

        // Add the group to the insured's list of groups
        insured.getEmployeeInsuredGroups().add(group);

        // Save the changes
        employeeDependantGroupRepository.save(group);
        insuredRepository.save(insured);

        return ResponseEntity.ok(new MessageResponse("Insured person added to group successfully"));
    }


    @Override
    public List<EmployeeInsuredGroupResponse> getGroupsByInsured(String insuredUuid) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        // Validate insured exists
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if (insured == null) {
            throw new ResourceNotFoundException("Insured Person", "insuredUuid", insuredUuid);
        }

        // Validate insured belongs to payer
        if (!insured.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Insured person does not belong to this payer");
        }

        // Find all groups associated with the insured person
        List<EmployeeDependantGroup> groups = employeeDependantGroupRepository.findByInsuredAndType(insured, GroupType.EMPLOYEE);

        return groups.stream()
                .filter(group -> !group.isDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<EmployeeInsuredGroupResponse> getInsuredByGroup(String groupUuid, String search, Pageable pageable) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        // Validate group exists
        EmployeeDependantGroup group = employeeDependantGroupRepository.findByGroupUuid(groupUuid);
        if (group == null) {
            throw new ResourceNotFoundException("Employee Group", "groupUuid", groupUuid);
        }

        // Validate group belongs to payer
        if (!group.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Group does not belong to this payer");
        }

        // Search for insured persons in the group
        Page<EmployeeDependantGroup> groups = employeeDependantGroupRepository
                .searchByGroupUuidAndInsuredName(groupUuid, search, pageable);

        return groups.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ResponseEntity<?> removeInsuredFromGroup(String insuredUuid, String groupUuid) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        // Validate group exists
        EmployeeDependantGroup group = employeeDependantGroupRepository.findByGroupUuid(groupUuid);
        if (group == null) {
            throw new ResourceNotFoundException("Employee Group", "groupUuid", groupUuid);
        }

        // Validate group belongs to payer
        if (!group.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Group does not belong to this payer");
        }

        // Validate insured exists and is associated with the group
        if (group.getInsured() == null || !group.getInsured().getInsuredUuid().equals(insuredUuid)) {
            throw new ResourceNotFoundException("Association", "insuredUuid and groupUuid",
                    insuredUuid + " and " + groupUuid);
        }

        // Remove the insured from the group
        group.setInsured(null);
        group.setType(null); // Reset the group type if needed

        // Save the changes
        employeeDependantGroupRepository.save(group);

        return ResponseEntity.ok(new MessageResponse("Insured person removed from group successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> batchAddInsuredToGroup(List<EmployeeInsuredGroupRequest> requests) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        int addedCount = 0;

        for (EmployeeInsuredGroupRequest request : requests) {
            // Validate group exists
            EmployeeDependantGroup group = employeeDependantGroupRepository.findByGroupUuid(request.getGroupUuid());
            if (group == null) {
                throw new ResourceNotFoundException("Employee Group", "groupUuid", request.getGroupUuid());
            }

            // Validate group belongs to payer
            if (!group.getPayerUuid().equals(payerUuid)) {
                throw new BadRequestException("Group does not belong to this payer");
            }

            // Validate insured exists
            Insured insured = insuredRepository.findByInsuredUuid(request.getInsuredUuid());
            if (insured == null){
                throw new ResourceNotFoundException("Insured Person", "insuredUuid", request.getInsuredUuid());
            }

            // Validate insured belongs to payer
            if (!insured.getPayerUuid().equals(payerUuid)) {
                throw new BadRequestException("Insured person does not belong to this payer");
            }

            // Check if association already exists
            if (group.getInsured() == null || !group.getInsured().getInsuredUuid().equals(insured.getInsuredUuid())) {
                // Create new association
                group.setInsured(insured);
                group.setType(GroupType.EMPLOYEE);

                // Add the group to the insured's list of groups
                insured.getEmployeeInsuredGroups().add(group);

                employeeDependantGroupRepository.save(group);
                insuredRepository.save(insured);

                addedCount++;
            }
        }

        return ResponseEntity.ok(new MessageResponse("Added " + addedCount + " insured persons to groups"));
    }

    @Override
    public Long getInsuredCountByGroup(String groupUuid) {

        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        EmployeeDependantGroup group = employeeDependantGroupRepository.findByGroupUuid(groupUuid);
        if (group == null) {
            throw new ResourceNotFoundException("Employee Group", "groupUuid", groupUuid);
        }

        if (!group.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Group does not belong to this payer");
        }

        return employeeDependantGroupRepository.countByGroupUuidAndInsuredIsNotNull(groupUuid);
    }

    private EmployeeInsuredGroupResponse mapToResponse(EmployeeDependantGroup group) {
        EmployeeInsuredGroupResponse response = new EmployeeInsuredGroupResponse();
        response.setId(group.getId());
        response.setGroupUuid(group.getGroupUuid());
        response.setGroupName(group.getGroupName());
        response.setGroupDescription(group.getGroupDescription());
        response.setGroupType(group.getType());

        if (group.getInsured() != null) {
            response.setInsuredUuid(group.getInsured().getInsuredUuid());
            response.setFirstName(group.getInsured().getFirstName());
            response.setFatherName(group.getInsured().getFatherName());
            response.setGrandFatherName(group.getInsured().getGrandFatherName());
            response.setInsuranceId(group.getInsured().getInsuranceId());
            response.setPhone(group.getInsured().getPhone());
            response.setGender(group.getInsured().getGender());
            response.setStatus(group.getInsured().getStatus());
        }

        return response;
    }
}