package com.medco.HealthConnectProvider.services.impl.group;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserDetailsImpl;
import com.medco.HealthConnectProvider.entity.groups.DependantGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.group.DependantGroupRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.services.group.DependantGroupService;
import com.medco.HealthConnectProvider.ui.request.group.DependantGroupRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.groups.DependantGroupResponse;
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
public class DependantGroupServiceImpl implements DependantGroupService {

    @Autowired
    private DependantGroupRepository dependantGroupRepository;

    @Autowired
    private EmployeeDependantGroupRepository employeeDependantGroupRepository;

    @Autowired
    private DependantRepository dependantRepository;

    @Override
    @Transactional
    public ResponseEntity<?> addDependantToGroup(DependantGroupRequest request) {
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

        // Validate dependant exists
        Dependant dependant = dependantRepository.findByDependantUuid(request.getDependantUuid());
        if (dependant == null){
            throw new ResourceNotFoundException("Dependant", "dependantUuid", request.getDependantUuid());
        }

        // Validate dependant belongs to payer (through insured)
        if (!dependant.getInsured().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Dependant does not belong to this payer");
        }

        // Check if association already exists
        boolean exists = dependantGroupRepository.existsByDependantUuidAndGroupUuid(
                request.getDependantUuid(), request.getGroupUuid());
        if (exists) {
            throw new BadRequestException("Dependant is already in this group");
        }

        // Create new association
        DependantGroup association = new DependantGroup();
        association.setDependant(dependant);
        association.setEmployeeDependantGroup(group);
        association.setDependantUuid(dependant.getDependantUuid());
        association.setGroupUuid(group.getGroupUuid());

        dependantGroupRepository.save(association);

        return ResponseEntity.ok(new MessageResponse("Dependant added to group successfully"));
    }

    @Override
    public List<DependantGroupResponse> getGroupsByDependant(String dependantUuid) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        // Validate dependant exists
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
        if (dependant == null){
            throw new ResourceNotFoundException("Dependant", "dependantUuid", dependantUuid);
        }

        // Validate dependant belongs to payer (through insured)
        if (!dependant.getInsured().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Dependant does not belong to this payer");
        }

        List<DependantGroup> associations = dependantGroupRepository.findByDependantUuid(dependantUuid);

        return associations.stream()
                .filter(a -> !a.isDeleted())
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<DependantGroupResponse> getDependantsByGroup(String groupUuid, String search, Pageable pageable) {
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

        Page<DependantGroup> associations = dependantGroupRepository
                .searchByGroupAndDependantName(groupUuid, search, pageable);

        return associations.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ResponseEntity<?> removeDependantFromGroup(String dependantUuid, String groupUuid) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        // Validate association exists
        DependantGroup association = dependantGroupRepository
                .findByDependantUuidAndGroupUuid(dependantUuid, groupUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Association", "dependantUuid and groupUuid", 
                        dependantUuid + " and " + groupUuid));

        // Validate association belongs to payer
        if (!association.getEmployeeDependantGroup().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Association does not belong to this payer");
        }

        // Soft delete
        association.setDeleted(true);
        dependantGroupRepository.save(association);

        return ResponseEntity.ok(new MessageResponse("Dependant removed from group successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> batchAddDependantToGroup(List<DependantGroupRequest> requests) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        List<DependantGroup> associations = new ArrayList<>();
        int addedCount = 0;

        for (DependantGroupRequest request : requests) {
            // Validate group exists
            EmployeeDependantGroup group = employeeDependantGroupRepository.findByGroupUuid(request.getGroupUuid());
            if (group == null) {
                throw new ResourceNotFoundException("Employee Group", "groupUuid", request.getGroupUuid());
            }

            // Validate group belongs to payer
            if (!group.getPayerUuid().equals(payerUuid)) {
                throw new BadRequestException("Group does not belong to this payer");
            }

            // Validate dependant exists
            Dependant dependant = dependantRepository.findByDependantUuid(request.getDependantUuid());
            if (dependant == null){
                throw new ResourceNotFoundException("Dependant", "dependantUuid", request.getDependantUuid());
            }

            // Validate dependant belongs to payer (through insured)
            if (!dependant.getInsured().getPayerUuid().equals(payerUuid)) {
                throw new BadRequestException("Dependant does not belong to this payer");
            }

            // Check if association already exists
            boolean exists = dependantGroupRepository.existsByDependantUuidAndGroupUuid(
                    request.getDependantUuid(), request.getGroupUuid());
            if (!exists) {
                // Create new association
                DependantGroup association = new DependantGroup();
                association.setDependant(dependant);
                association.setEmployeeDependantGroup(group);
                association.setDependantUuid(dependant.getDependantUuid());
                association.setGroupUuid(group.getGroupUuid());

                associations.add(association);
                addedCount++;
            }
        }

        if (!associations.isEmpty()) {
            dependantGroupRepository.saveAll(associations);
        }

        return ResponseEntity.ok(new MessageResponse("Added " + addedCount + " dependants to groups"));
    }

    @Override
    public Long getDependantCountByGroup(String groupUuid) {
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

        return dependantGroupRepository.countByGroupUuid(groupUuid);
    }

    private DependantGroupResponse mapToResponse(DependantGroup association) {
        DependantGroupResponse response = new DependantGroupResponse();
        response.setId(association.getId());
        response.setDependantUuid(association.getDependantUuid());
        response.setGroupUuid(association.getGroupUuid());
        
        // Add dependant information
        if (association.getDependant() != null) {
            response.setFirstName(association.getDependant().getFirstName());
            response.setFatherName(association.getDependant().getFatherName());
            response.setGrandFatherName(association.getDependant().getGrandFatherName());
            response.setRelationship(association.getDependant().getRelationship());
            response.setGender(association.getDependant().getGender());
            response.setStatus(association.getDependant().getStatus());
            
            // Add insured information if available
            if (association.getDependant().getInsured() != null) {
                response.setInsuredUuid(association.getDependant().getInsured().getInsuredUuid());
                response.setInsuredName(
                    association.getDependant().getInsured().getFirstName() + " " +
                    association.getDependant().getInsured().getFatherName() + " " +
                    association.getDependant().getInsured().getGrandFatherName()
                );
                response.setInsuranceId(association.getDependant().getInsured().getInsuranceId());
            }
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