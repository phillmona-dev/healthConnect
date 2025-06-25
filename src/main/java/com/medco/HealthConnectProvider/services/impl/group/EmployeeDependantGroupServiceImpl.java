package com.medco.HealthConnectProvider.services.impl.group;


import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.group.EmployeeDependantGroupService;
import com.medco.HealthConnectProvider.ui.request.group.EmployeeDependantGroupRequest;
import com.medco.HealthConnectProvider.ui.request.group.GroupMembersRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeDependantGroupResponse;
import com.medco.HealthConnectProvider.ui.response.groups.GroupMembersAndServicesResponse;
import com.medco.HealthConnectProvider.ui.response.persons.DependantResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PagedResponse;
import com.medco.HealthConnectProvider.utils.enums.GroupType;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class EmployeeDependantGroupServiceImpl implements EmployeeDependantGroupService {


    private final EmployeeDependantGroupRepository groupRepository;

    private final PayerRepository payerRepository;
    private final InsuredRepository insuredRepository;
    private final DependantRepository dependantRepository;
    private final ContractDetailRepository contractDetailRepository;


    public EmployeeDependantGroupServiceImpl(EmployeeDependantGroupRepository groupRepository, PayerRepository payerRepository, InsuredRepository insuredRepository, DependantRepository dependantRepository, ContractDetailRepository contractDetailRepository) {
        this.groupRepository = groupRepository;
        this.payerRepository = payerRepository;
        this.insuredRepository = insuredRepository;
        this.dependantRepository = dependantRepository;
        this.contractDetailRepository = contractDetailRepository;
    }

    @Override
    @Transactional
    public ResponseEntity<EmployeeDependantGroupResponse> createGroup(String payerUuid,EmployeeDependantGroupRequest request) {
//        // Validate user access
//        System.out.println("in the service impl");
//        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
//        System.out.println("userUuid"+userDetails.getUserUuid());
//        if (userDetails.getPayerUuid()==null)throw new BadRequestException("Allowed only for payers");
//        String payerUuid = userDetails.getPayerUuid();

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
    public GroupMembersAndServicesResponse getGroupByUuid(String groupUuid) {
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
        GroupMembersAndServicesResponse response=new GroupMembersAndServicesResponse();
        BeanUtils.copyProperties(group,response);
        response.setInsuredResponses(mapToInsuredResponse(new ArrayList<>(group.getInsureds())));
        response.setDependantResponses(mapToDependantResponse(new ArrayList<>(group.getDependants())));

        return response;
    }

    private List<DependantResponse> mapToDependantResponse(List<Dependant> dependants) {
        List<DependantResponse> dependantResponses=new ArrayList<>();
        for (Dependant dependant:dependants){
            DependantResponse dependantResponse=new DependantResponse();
            BeanUtils.copyProperties(dependant,dependantResponse);
            dependantResponses.add(dependantResponse);

        }
        return dependantResponses;
    }

    private List<InsuredResponse> mapToInsuredResponse(List<Insured> insureds) {
        List<InsuredResponse> insuredResponses=new ArrayList<>();
        for (Insured insured:insureds){
            InsuredResponse insuredResponse=new InsuredResponse();
            BeanUtils.copyProperties(insured,insuredResponse);
           insuredResponses.add(insuredResponse);

        }
        return insuredResponses;
    }


    @Override
    public Page<EmployeeDependantGroupResponse> listGroups(String search, Pageable pageable) {

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        Page<EmployeeDependantGroup> groups = groupRepository.findByPayerUuidAndGroupNameContainingIgnoreCase(
                payerUuid, search, pageable);

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

    @Override
    public PagedResponse<EmployeeDependantGroupResponse> payerGroups(String payerUUid, String search, Pageable pageable) {
//        Payer payer=payerRepository.findByPayerUuid(payerUUid);
       Page <EmployeeDependantGroup> employeeDependantGroup =groupRepository.findByPayerPayerUuid(payerUUid,pageable);
        System.out.println("size"+employeeDependantGroup.getSize());
       List<EmployeeDependantGroupResponse>responseList=employeeDependantGroup.stream().map(employeeDependantGroup1 -> {
           EmployeeDependantGroupResponse employeeDependantGroupResponse=new EmployeeDependantGroupResponse();
            BeanUtils.copyProperties(employeeDependantGroup1,employeeDependantGroupResponse);
            return employeeDependantGroupResponse;
       }).toList();
//        if (payer==null)throw new BadRequestException("payer no found");
//        List<EmployeeDependantGroupResponse>employeeDependantGroupResponses=new ArrayList<>();
//        for (EmployeeDependantGroup employeeDependantGroup:payer.getEmployeeDependantGroups()){
//
//            EmployeeDependantGroupResponse employeeDependantGroupResponse=new EmployeeDependantGroupResponse();
//            BeanUtils.copyProperties(employeeDependantGroup,employeeDependantGroupResponse);
//            employeeDependantGroupResponses.add(employeeDependantGroupResponse);
//        }
        PagedResponse<EmployeeDependantGroupResponse> pagedResponses = new PagedResponse<>();
        pagedResponses.setContent(responseList);
        pagedResponses.setPage(employeeDependantGroup.getNumber() + 1);
        pagedResponses.setPerPage(employeeDependantGroup.getSize());
        pagedResponses.setTotalElements(employeeDependantGroup.getTotalElements());
        pagedResponses.setTotalPages(employeeDependantGroup.getTotalPages());
        pagedResponses.setHasNext(employeeDependantGroup.hasNext());
        pagedResponses.setHasPrevious(employeeDependantGroup.hasPrevious());

        return pagedResponses;
//        return PagedResponse employeeDependantGroupResponses;

    }

    @Override
    public ResponseEntity<?> addMembersToGroup(String groupUuid, GroupMembersRequest request) {
        EmployeeDependantGroup employeeDependantGroup=groupRepository.findByGroupUuid(groupUuid);
        if (request.isInsured()) {
            List<Insured>insuredList=insuredRepository.findByInsuredUuidIn(request.getInsuredUuids());
            Set<Insured> insuredSet = new HashSet<>(insuredList);
            employeeDependantGroup.getInsureds().addAll(insuredSet);
            groupRepository.save(employeeDependantGroup);

        }
        else {
            List<Dependant>dependantList=dependantRepository.findByDependantUuidIn(request.getDependantUuids());
            Set<Dependant> dependantSet = new HashSet<>(dependantList);
            employeeDependantGroup.getDependants().addAll(dependantSet);
            groupRepository.save(employeeDependantGroup);
        }
        return ResponseEntity.ok("members successfully added to the group");
    }

    @Override
    public ResponseEntity<?> addServicesToGroup(String groupUuid, List<String> services) {

        return null;
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