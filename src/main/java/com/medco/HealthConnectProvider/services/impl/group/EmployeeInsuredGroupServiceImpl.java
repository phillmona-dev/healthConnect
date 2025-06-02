package com.medco.HealthConnectProvider.services.impl.group;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserDetailsImpl;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeInsuredGroup;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeInsuredGroupRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.group.EmployeeInsuredGroupService;
import com.medco.HealthConnectProvider.ui.request.group.EmployeeInsuredGroupRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeInsuredGroupResponse;
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
    private EmployeeInsuredGroupRepository employeeInsuredGroupRepository;

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
        if (insured == insured){
            throw new ResourceNotFoundException("Insured Person", "insuredUuid", request.getInsuredUuid());
        }

        // Validate insured belongs to payer
        if (!insured.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Insured person does not belong to this payer");
        }

        // Check if association already exists
        boolean exists = employeeInsuredGroupRepository.existsByInsuredAndEmployeeDependantGroup(insured, group);
        if (exists) {
            throw new BadRequestException("Insured person is already in this group");
        }

        // Create new association
        EmployeeInsuredGroup association = new EmployeeInsuredGroup();
        association.setInsured(insured);
        association.setEmployeeDependantGroup(group);
        association.setEmployeeInsuredUuid(insured.getInsuredUuid());
        association.setGroupUuid(group.getGroupUuid());

        employeeInsuredGroupRepository.save(association);

        return ResponseEntity.ok(new MessageResponse("Insured person added to group successfully"));
    }

    @Override
    public List<EmployeeInsuredGroupResponse> getGroupsByInsured(String insuredUuid) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        // Validate insured exists
        Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
        if(insured == insured){
            throw new ResourceNotFoundException("Insured Person", "insuredUuid", insuredUuid);
        }

        // Validate insured belongs to payer
        if (!insured.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Insured person does not belong to this payer");
        }

        List<EmployeeInsuredGroup> associations = employeeInsuredGroupRepository.findByEmployeeInsuredUuid(insuredUuid);

        return associations.stream()
                .filter(a -> !a.isDeleted())
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

        Page<EmployeeInsuredGroup> associations = employeeInsuredGroupRepository
                .searchByGroupAndInsuredName(groupUuid, search, pageable);

        return associations.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ResponseEntity<?> removeInsuredFromGroup(String insuredUuid, String groupUuid) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        // Validate association exists
        EmployeeInsuredGroup association = employeeInsuredGroupRepository
                .findByEmployeeInsuredUuidAndGroupUuid(insuredUuid, groupUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Association", "insuredUuid and groupUuid", 
                        insuredUuid + " and " + groupUuid));

        // Validate association belongs to payer
        if (!association.getEmployeeDependantGroup().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Association does not belong to this payer");
        }

        // Soft delete
        association.setDeleted(true);
        employeeInsuredGroupRepository.save(association);

        return ResponseEntity.ok(new MessageResponse("Insured person removed from group successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> batchAddInsuredToGroup(List<EmployeeInsuredGroupRequest> requests) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        List<EmployeeInsuredGroup> associations = new ArrayList<>();
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
            boolean exists = employeeInsuredGroupRepository.existsByInsuredAndEmployeeDependantGroup(insured, group);
            if (!exists) {
                // Create new association
                EmployeeInsuredGroup association = new EmployeeInsuredGroup();
                association.setInsured(insured);
                association.setEmployeeDependantGroup(group);
                association.setEmployeeInsuredUuid(insured.getInsuredUuid());
                association.setGroupUuid(group.getGroupUuid());

                associations.add(association);
                addedCount++;
            }
        }

        if (!associations.isEmpty()) {
            employeeInsuredGroupRepository.saveAll(associations);
        }

        return ResponseEntity.ok(new MessageResponse("Added " + addedCount + " insured persons to groups"));
    }

    @Override
    public Long getInsuredCountByGroup(String groupUuid) {
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

        return employeeInsuredGroupRepository.countByGroupUuid(groupUuid);
    }

    private EmployeeInsuredGroupResponse mapToResponse(EmployeeInsuredGroup association) {
        EmployeeInsuredGroupResponse response = new EmployeeInsuredGroupResponse();
        response.setId(association.getId());
        response.setInsuredUuid(association.getEmployeeInsuredUuid());
        response.setGroupUuid(association.getGroupUuid());
        
        // Add insured information
        if (association.getInsured() != null) {
            response.setFirstName(association.getInsured().getFirstName());
            response.setFatherName(association.getInsured().getFatherName());
            response.setGrandFatherName(association.getInsured().getGrandFatherName());
            response.setInsuranceId(association.getInsured().getInsuranceId());
            response.setPhone(association.getInsured().getPhone());
            response.setGender(association.getInsured().getGender());
            response.setStatus(association.getInsured().getStatus());
        }
        
        // Add group information
        if (association.getEmployeeDependantGroup() != null) {
            response.setGroupName(association.getEmployeeDependantGroup().getGroupName());
            response.setGroupDescription(association.getEmployeeDependantGroup().getGroupDescription());
            response.setGroupType(association.getEmployeeDependantGroup().getType());
        }
        
        return response;
    }
}