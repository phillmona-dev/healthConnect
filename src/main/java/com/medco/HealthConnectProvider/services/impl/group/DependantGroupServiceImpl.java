package com.medco.HealthConnectProvider.services.impl.group;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserDetailsImpl;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.services.group.DependantGroupService;
import com.medco.HealthConnectProvider.ui.request.group.DependantGroupRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.groups.DependantGroupResponse;
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
public class DependantGroupServiceImpl implements DependantGroupService {

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
        if (dependant == null) {
            throw new ResourceNotFoundException("Dependant", "dependantUuid", request.getDependantUuid());
        }

        // Validate dependant belongs to payer (through insured)
        if (!dependant.getInsured().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Dependant does not belong to this payer");
        }

        // Check if association already exists
        boolean associationExists = dependant.getDependantGroups().stream()
                .anyMatch(g -> g.getGroupUuid().equals(group.getGroupUuid()));
        if (associationExists) {
            throw new BadRequestException("Dependant is already in this group");
        }

        // Set the group type to DEPENDANT if not already set
        if (group.getType() == null) {
            group.setType(GroupType.DEPENDANT);
        } else if (group.getType() != GroupType.DEPENDANT && group.getType() != GroupType.BOTH) {
            throw new BadRequestException("This group is not for dependants");
        }

        // Create new association
        group.setDependant(dependant);
        dependant.getDependantGroups().add(group);

        // Save the changes
        employeeDependantGroupRepository.save(group);
        dependantRepository.save(dependant);

        return ResponseEntity.ok(new MessageResponse("Dependant added to group successfully"));
    }

    @Override
    public List<DependantGroupResponse> getGroupsByDependant(String dependantUuid) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        // Validate dependant exists
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
        if (dependant == null) {
            throw new ResourceNotFoundException("Dependant", "dependantUuid", dependantUuid);
        }

        // Validate dependant belongs to payer (through insured)
        if (!dependant.getInsured().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Dependant does not belong to this payer");
        }

        // Get all groups associated with this dependant
        List<EmployeeDependantGroup> groups = dependant.getDependantGroups();

        // Filter out deleted groups and map to response
        return groups.stream()
                .filter(group -> !group.isDeleted())
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

        // Search for dependants in the group
        Page<EmployeeDependantGroup> groupsWithDependants = employeeDependantGroupRepository
                .searchByGroupUuidAndDependantName(groupUuid, search, pageable);

        return groupsWithDependants.map(this::mapToResponse);
    }


    @Override
    @Transactional
    public ResponseEntity<?> removeDependantFromGroup(String dependantUuid, String groupUuid) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        // Validate association exists
        EmployeeDependantGroup group = employeeDependantGroupRepository
                .findByDependant_DependantUuidAndGroupUuid(dependantUuid, groupUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Association", "dependantUuid and groupUuid",
                        dependantUuid + " and " + groupUuid));

        // Validate group belongs to payer
        if (!group.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Group does not belong to this payer");
        }

        // Remove the dependant from the group
        Dependant dependant = group.getDependant();
        if (dependant != null) {
            dependant.getDependantGroups().remove(group);
            group.setDependant(null);
        }

        // Soft delete the group
        group.setDeleted(true);
        employeeDependantGroupRepository.save(group);

        return ResponseEntity.ok(new MessageResponse("Dependant removed from group successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> batchAddDependantToGroup(List<DependantGroupRequest> requests) {
        // Validate user access
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

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
            if (dependant == null) {
                throw new ResourceNotFoundException("Dependant", "dependantUuid", request.getDependantUuid());
            }

            // Validate dependant belongs to payer (through insured)
            if (!dependant.getInsured().getPayerUuid().equals(payerUuid)) {
                throw new BadRequestException("Dependant does not belong to this payer");
            }

            // Check if association already exists
            boolean associationExists = dependant.getDependantGroups().stream()
                    .anyMatch(g -> g.getGroupUuid().equals(group.getGroupUuid()));
            if (!associationExists) {
                // Set the group type to DEPENDANT if not already set
                if (group.getType() == null) {
                    group.setType(GroupType.DEPENDANT);
                } else if (group.getType() != GroupType.DEPENDANT && group.getType() != GroupType.BOTH) {
                    throw new BadRequestException("This group is not for dependants");
                }

                // Create new association
                group.setDependant(dependant);
                dependant.getDependantGroups().add(group);

                // Save the changes
                employeeDependantGroupRepository.save(group);
                dependantRepository.save(dependant);

                addedCount++;
            }
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

        return employeeDependantGroupRepository.countByGroupUuid(groupUuid);
    }

    private DependantGroupResponse mapToResponse(EmployeeDependantGroup group) {
        DependantGroupResponse response = new DependantGroupResponse();
        response.setId(group.getId());
        response.setGroupUuid(group.getGroupUuid());
        response.setGroupName(group.getGroupName());
        response.setGroupDescription(group.getGroupDescription());
        response.setGroupType(group.getType());

        Dependant dependant = group.getDependant();
        if (dependant != null) {
            response.setDependantUuid(dependant.getDependantUuid());
            response.setFirstName(dependant.getFirstName());
            response.setFatherName(dependant.getFatherName());
            response.setGrandFatherName(dependant.getGrandFatherName());
            response.setRelationship(dependant.getRelationship());
            response.setGender(dependant.getGender());
            response.setStatus(dependant.getStatus());

            if (dependant.getInsured() != null) {
                response.setInsuredUuid(dependant.getInsured().getInsuredUuid());
                response.setInsuredName(
                        dependant.getInsured().getFirstName() + " " +
                                dependant.getInsured().getFatherName() + " " +
                                dependant.getInsured().getGrandFatherName()
                );
                response.setInsuranceId(dependant.getInsured().getInsuranceId());
            }
        }

        return response;
    }
}