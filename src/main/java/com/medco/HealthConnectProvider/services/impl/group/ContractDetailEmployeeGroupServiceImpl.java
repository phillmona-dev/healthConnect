package com.medco.HealthConnectProvider.services.impl.group;


import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.groups.ContractDetailEmployeeGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.group.ContractDetailEmployeeGroupRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.services.group.ContractDetailEmployeeGroupService;
import com.medco.HealthConnectProvider.ui.request.group.ContractDetailEmployeeGroupRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.groups.ContractDetailEmployeeGroupResponse;
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
public class ContractDetailEmployeeGroupServiceImpl implements ContractDetailEmployeeGroupService {

    @Autowired
    private ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository;

    @Autowired
    private ContractDetailRepository contractDetailRepository;

    @Autowired
    private EmployeeDependantGroupRepository employeeDependantGroupRepository;

    @Override
    @Transactional
    public ResponseEntity<?> createContractDetailEmployeeGroup(ContractDetailEmployeeGroupRequest request) {
//        // Validate user access
//        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
//        String payerUuid = userDetails.getPayerUuid();
//
//        // Validate contract detail exists
//        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(request.getContractDetailUuid());
//        if(contractDetail==null){
//            throw new ResourceNotFoundException("Contract Detail", "contractDetailUuid", request.getContractDetailUuid());
//        }
//
//        // Validate contract belongs to payer
//        if (!contractDetail.getContractHeader().getPayer().getPayerUuid().equals(payerUuid)) {
//            throw new BadRequestException("Contract does not belong to this payer");
//        }
//
//        // Validate employee group exists
//        EmployeeDependantGroup employeeGroup = employeeDependantGroupRepository.findByGroupUuid(request.getEmployeeGroupUuid());
//        if (employeeGroup == null) {
//            throw new ResourceNotFoundException("Employee Group", "groupUuid", request.getEmployeeGroupUuid());
//        }
//
//        // Validate employee group belongs to payer
//        if (!employeeGroup.getPayerUuid().equals(payerUuid)) {
//            throw new BadRequestException("Employee group does not belong to this payer");
//        }
//
//        // Check if association already exists
//        boolean exists = contractDetailEmployeeGroupRepository.existsByContractDetailAndEmployeeDependantGroup(
//                contractDetail, employeeGroup);
//        if (exists) {
//            throw new BadRequestException("Association between contract detail and employee group already exists");
//        }
//
//        // Create new association
//        ContractDetailEmployeeGroup association = new ContractDetailEmployeeGroup();
//        association.setContractDetail(contractDetail);
//        association.setEmployeeDependantGroup(employeeGroup);
//        association.setContractDetailUuid(contractDetail.getContractDetailUuid());
//        association.setEmployeeGroupUuid(employeeGroup.getGroupUuid());
//
//        contractDetailEmployeeGroupRepository.save(association);
//
//        return ResponseEntity.ok(new MessageResponse("Contract detail employee group association created successfully"));
        return null;
    }

    @Override
    public List<ContractDetailEmployeeGroupResponse> getContractDetailEmployeeGroupsByContract(String contractUuid) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        List<ContractDetailEmployeeGroup> associations = contractDetailEmployeeGroupRepository.findByContractUuid(contractUuid);

        return associations.stream()
                .filter(a -> a.getEmployeeDependantGroup().getPayerUuid().equals(payerUuid))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContractDetailEmployeeGroupResponse> getContractDetailEmployeeGroupsByContractDetail(String contractDetailUuid) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        List<ContractDetailEmployeeGroup> associations = contractDetailEmployeeGroupRepository.findByContractDetailUuid(contractDetailUuid);

        return associations.stream()
                .filter(a -> a.getEmployeeDependantGroup().getPayerUuid().equals(payerUuid))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ContractDetailEmployeeGroupResponse> getContractDetailEmployeeGroupsByEmployeeGroup(String employeeGroupUuid) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        // Validate employee group belongs to payer
        EmployeeDependantGroup employeeGroup = employeeDependantGroupRepository.findByGroupUuid(employeeGroupUuid);
        if (employeeGroup == null) {
            throw new ResourceNotFoundException("Employee Group", "groupUuid", employeeGroupUuid);
        }

        if (!employeeGroup.getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Employee group does not belong to this payer");
        }

        List<ContractDetailEmployeeGroup> associations = contractDetailEmployeeGroupRepository.findByEmployeeGroupUuid(employeeGroupUuid);

        return associations.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ContractDetailEmployeeGroupResponse> searchContractDetailEmployeeGroups(
            String contractUuid, String search, Pageable pageable) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        Page<ContractDetailEmployeeGroup> associations = contractDetailEmployeeGroupRepository
                .searchByContractAndGroupName(contractUuid, search, pageable);

        return associations.map(this::mapToResponse);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteContractDetailEmployeeGroup(String contractDetailUuid, String employeeGroupUuid) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        // Validate association exists
        ContractDetailEmployeeGroup association = contractDetailEmployeeGroupRepository
                .findByContractDetailUuidAndEmployeeGroupUuid(contractDetailUuid, employeeGroupUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Association", "contractDetailUuid and employeeGroupUuid", 
                        contractDetailUuid + " and " + employeeGroupUuid));

        // Validate association belongs to payer
        if (!association.getEmployeeDependantGroup().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Association does not belong to this payer");
        }

        contractDetailEmployeeGroupRepository.deleteByContractDetailUuidAndEmployeeGroupUuid(
                contractDetailUuid, employeeGroupUuid);

        return ResponseEntity.ok(new MessageResponse("Contract detail employee group association deleted successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> batchCreateContractDetailEmployeeGroups(String employeeGroupUuid ,ContractDetailEmployeeGroupRequest request) {
        // Validate user access
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();
        EmployeeDependantGroup employeeDependantGroup=employeeDependantGroupRepository.findByGroupUuid(employeeGroupUuid);

        List<ContractDetail >contractDetails=contractDetailRepository.findByContractDetailUuidIn(request.getContractDetailUuid());
        List<ContractDetailEmployeeGroup>associations=new ArrayList<>();
        int createdCount=0;
        for (ContractDetail contractDetail : contractDetails) {
            // Validate contract detail exists



            // Validate contract belongs to payer
            if (!contractDetail.getContractHeader().getPayer().getPayerUuid().equals(payerUuid)) {
                throw new BadRequestException("Contract does not belong to this payer");
            }



            // Validate employee group belongs to payer
            if (!employeeDependantGroup.getPayerUuid().equals(payerUuid)) {
                throw new BadRequestException("Employee group does not belong to this payer");
            }

            // Check if association already exists
            boolean exists = contractDetailEmployeeGroupRepository.existsByContractDetailAndEmployeeDependantGroup(
                    contractDetail, employeeDependantGroup);
            if (!exists) {
                // Create new association
                ContractDetailEmployeeGroup association = new ContractDetailEmployeeGroup();
                association.setContractDetail(contractDetail);
                association.setEmployeeDependantGroup(employeeDependantGroup);
                association.setContractDetailUuid(contractDetail.getContractDetailUuid());
                association.setEmployeeGroupUuid(employeeDependantGroup.getGroupUuid());

                associations.add(association);
                createdCount++;
            }
        }

        if (!associations.isEmpty()) {
            contractDetailEmployeeGroupRepository.saveAll(associations);
        }

        return ResponseEntity.ok(new MessageResponse("Created " + createdCount + " contract detail employee group associations"));

    }

    private ContractDetailEmployeeGroupResponse mapToResponse(ContractDetailEmployeeGroup association) {
        ContractDetailEmployeeGroupResponse response = new ContractDetailEmployeeGroupResponse();
        response.setId(association.getId());
        response.setContractDetailUuid(association.getContractDetailUuid());
        response.setEmployeeGroupUuid(association.getEmployeeGroupUuid());
        
        // Add contract detail information
        if (association.getContractDetail() != null) {
            response.setServiceName(association.getContractDetail().getServicelist().getServiceName());
            response.setServiceCode(association.getContractDetail().getServicelist().getServiceCode());
            response.setServicePrice(association.getContractDetail().getNegotiatedPrice());
            response.setContractName(association.getContractDetail().getContractHeader().getContractName());
            response.setContractCode(association.getContractDetail().getContractHeader().getContractCode());
        }
        
        // Add employee group information
        if (association.getEmployeeDependantGroup() != null) {
            response.setGroupName(association.getEmployeeDependantGroup().getGroupName());
            response.setGroupDescription(association.getEmployeeDependantGroup().getGroupDescription());
            response.setEstimatedMembers(association.getEmployeeDependantGroup().getEstimatedMembers());
        }
        
        return response;
    }
}