package com.medco.HealthConnectProvider.services.impl.group;


import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.services.group.EmployeeDependantGroupService;
import com.medco.HealthConnectProvider.ui.request.group.EmployeeDependantGroupRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeDependantGroupResponse;
import com.medco.HealthConnectProvider.utils.enums.GroupType;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EmployeeDependantGroupServiceImpl implements EmployeeDependantGroupService {

    @Autowired
    private EmployeeDependantGroupRepository groupRepository;

    @Autowired
    private PayerRepository payerRepository;

    @Override
    @Transactional
    public ResponseEntity<EmployeeDependantGroupResponse> createGroup(EmployeeDependantGroupRequest request) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        // Validate payer exists
        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        // Check if group name already exists for this payer
        if (groupRepository.existsByGroupNameAndPayerUuid(request.getGroupName(), payerUuid)) {
            throw new BadRequestException("Group with name '" + request.getGroupName() + "' already exists");
        }

        // Create new group
        EmployeeDependantGroup group = new EmployeeDependantGroup();
        group.setGroupName(request.getGroupName());
        group.setGroupDescription(request.getGroupDescription());
        group.setEstimatedMembers(request.getEstimatedMembers());
        group.setType(request.getType() != null ? request.getType() : GroupType.BOTH);
        group.setStatus(Status.ACTIVE);
        group.setPayer(payer);
        group.setPayerUuid(payerUuid);
        group.setGroupUuid(UUID.randomUUID().toString());

        // Save group
        EmployeeDependantGroup savedGroup = groupRepository.save(group);

        // Map to response
        EmployeeDependantGroupResponse response = mapToResponse(savedGroup);
        
        return ResponseEntity.ok(response);
    }

    @Override
    public EmployeeDependantGroupResponse getGroupByUuid(String groupUuid) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        // Find group
        EmployeeDependantGroup group = groupRepository.findByGroupUuid(groupUuid);
        if (group == null) {
            throw new ResourceNotFoundException("Group", "groupUuid", groupUuid);
        }

        // Validate group belongs to payer
        if (!group.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Group does not belong to this payer");
        }

        // Map to response
        return mapToResponse(group);
    }

    @Override
    public Page<EmployeeDependantGroupResponse> listGroups(String search, Pageable pageable) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        // Get groups with search
        Page<EmployeeDependantGroup> groups = groupRepository.findByPayerUuidAndGroupNameContainingIgnoreCase(
                payerUuid, search, pageable);

        // Map to response
        return groups.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ResponseEntity<EmployeeDependantGroupResponse> updateGroup(String groupUuid, EmployeeDependantGroupRequest request) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        // Find group
        EmployeeDependantGroup group = groupRepository.findByGroupUuid(groupUuid);
        if (group == null) {
            throw new ResourceNotFoundException("Group", "groupUuid", groupUuid);
        }

        // Validate group belongs to payer
        if (!group.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Group does not belong to this payer");
        }

        // Check if new name conflicts with existing group
        if (!group.getGroupName().equals(request.getGroupName()) && 
                groupRepository.existsByGroupNameAndPayerUuid(request.getGroupName(), payerUuid)) {
            throw new BadRequestException("Group with name '" + request.getGroupName() + "' already exists");
        }

        // Update group
        group.setGroupName(request.getGroupName());
        group.setGroupDescription(request.getGroupDescription());
        group.setEstimatedMembers(request.getEstimatedMembers());
        if (request.getType() != null) {
            group.setType(request.getType());
        }
        if (request.getStatus() != null) {
            group.setStatus(request.getStatus());
        }

        // Save updated group
        EmployeeDependantGroup updatedGroup = groupRepository.save(group);

        // Map to response
        EmployeeDependantGroupResponse response = mapToResponse(updatedGroup);
        
        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteGroup(String groupUuid) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        // Find group
        EmployeeDependantGroup group = groupRepository.findByGroupUuid(groupUuid);
        if (group == null) {
            throw new ResourceNotFoundException("Group", "groupUuid", groupUuid);
        }

        // Validate group belongs to payer
        if (!group.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Group does not belong to this payer");
        }

        // Soft delete
        group.setDeleted(true);
        groupRepository.save(group);

        return ResponseEntity.ok(new MessageResponse("Group deleted successfully"));
    }

    // Helper method to map entity to response
    private EmployeeDependantGroupResponse mapToResponse(EmployeeDependantGroup group) {
        EmployeeDependantGroupResponse response = new EmployeeDependantGroupResponse();
        BeanUtils.copyProperties(group, response);
        
        // Add additional fields if needed
        response.setPayerName(group.getPayer() != null ? group.getPayer().getPayerName() : null);
        
        return response;
    }
}