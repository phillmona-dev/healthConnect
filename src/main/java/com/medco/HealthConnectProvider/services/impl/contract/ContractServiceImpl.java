package com.medco.HealthConnectProvider.services.impl.contract;


import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.groups.ContractDetailEmployeeGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
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
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.services.contract.ContractService;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractDetailRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRenewalRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractTerminationRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.ContractServiceGroupAssignmentRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.EmployeeGroupRequest;
import com.medco.HealthConnectProvider.ui.request.contract.AddInsuredToContractRequest;
import com.medco.HealthConnectProvider.ui.request.contract.ContractFilterRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractDetailResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractListPayerResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.DetailedContractResponse;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeGroupResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.ui.response.service.ServiceResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.HttpEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

import jakarta.validation.Valid;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ProviderRepository providerRepository;
    private final ServicelistRepository servicelistRepository;
    private final PayerRepository payerRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final EmployeeDependantGroupRepository employeeDependantGroupRepository;
    private final ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository;
    private final InsuredRepository insuredRepository;
    private final DependantRepository dependantRepository;

    private final ModelMapper modelMapper;

    @Value("${api.provider.contract}")
    private String providerContractApi;

    @Value("${provider.HostDomain}")
    private String providerHostDomain;

    @Value("${file.upload-dir-payer-logos}")
    private String payerLogosDirectory;

    @Value("${file.upload-dir-provider-logos}")
    private String providerLogosDirectory;

    public ContractServiceImpl(ContractRepository contractRepository, ProviderRepository providerRepository, ServicelistRepository servicelistRepository, PayerRepository payerRepository, ContractDetailRepository contractDetailRepository, EmployeeDependantGroupRepository employeeDependantGroupRepository,
                               ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository, InsuredRepository insuredRepository, DependantRepository dependantRepository, ModelMapper modelMapper) {
        this.contractRepository = contractRepository;


        this.providerRepository = providerRepository;
        this.servicelistRepository = servicelistRepository;
        this.payerRepository = payerRepository;
        this.contractDetailRepository = contractDetailRepository;
        this.employeeDependantGroupRepository = employeeDependantGroupRepository;
        this.contractDetailEmployeeGroupRepository = contractDetailEmployeeGroupRepository;
        this.insuredRepository = insuredRepository;
        this.dependantRepository = dependantRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    @Override
    public ResponseEntity<ContractResponse> createContract(ContractRequest contractRequest) {
        // Fetch provider and payer
        Provider provider = providerRepository.findByProviderUuid(contractRequest.getProviderUuid());
        if (provider == null){
            throw new RuntimeException("Provider not found with uuid: " + contractRequest.getProviderUuid());
        }

        Payer payer = payerRepository.findByPayerUuid(contractRequest.getPayerUuid());
        if (payer == null){
            throw  new RuntimeException(
                    "Payer not found with uuid: " + contractRequest.getPayerUuid());
        }

        ContractHeader contract = new ContractHeader();
        modelMapper.map(contractRequest, contract);

        contract.setProvider(provider);
        contract.setPayer(payer);

        contract.setStatus(Status.ACTIVE);
        contract.setStartDate(contractRequest.getBeginDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate());
        contract.setEndDate(contractRequest.getEndDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate());

        contract.setNegotiatingPrice(contractRequest.getNegotiatingPrice());

        ContractHeader savedContract = contractRepository.save(contract);

        provider.getContractHeaders().add(savedContract);
        payer.getContractHeaders().add(savedContract);

        ContractResponse response = modelMapper.map(savedContract, ContractResponse.class);

        response.setPayerUuid(payer.getPayerUuid());
        response.setPayerName(payer.getPayerName());
        response.setPayerCode(payer.getPayerCode());

        response.setProviderUuid(provider.getProviderUuid());
        response.setProviderName(provider.getProviderName());
        response.setProviderCode(provider.getProviderCode());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);

    }

    @Override
    public ResponseEntity<?> updateContract(String contractUuid, @Valid ContractRequest contractRequest) {

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);

        if (contract == null)
            throw new ResourceNotFoundException("Payer Provider Contract", "contractUuid", contractUuid);

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String preparedBy = userDetails.getUserUuid();

        BeanUtils.copyProperties(contractRequest, contract);
        contract.setStatus(Status.ACTIVE);
        contract.setPreparedBy(preparedBy);
        contractRepository.save(contract);
        return ResponseEntity.ok(new MessageResponse("Contract Updated Successfully!"));
    }


    @Override
    public ContractResponse getContract(String contractUuid) {
        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);

        if (contract == null)
            throw new ResourceNotFoundException("Payer Provider Contract", "contractUuid", contractUuid);

        ContractResponse contractResponse = new ContractResponse();
        BeanUtils.copyProperties(contract, contractResponse);
        contractResponse.setPayerUuid(contract.getPayer().getPayerUuid());
        return contractResponse;

    }

    @Override
    public ResponseEntity<?> deleteContract(String contractUuid) {
        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);

        if (contract == null)
            throw new ResourceNotFoundException("Payer Provider Contract", "contractUuid", contractUuid);
        contract.setDeleted(true);
        contractRepository.save(contract);
        return ResponseEntity.ok(new MessageResponse("Contract deleted successfully!"));
    }

    @Override
    public List<ContractListPayerResponse> getPayerProvidersContractLists(String searchKey, Pageable pageable, String status) {

        Page<ContractHeader> contractPage;

        Status contractStatus = (status == null || status.isEmpty())
                ? Status.ACTIVE
                : Status.valueOf(status);

        if (searchKey != null && !searchKey.isEmpty()) {

            contractPage = contractRepository.findByStatusAndIsDeletedAndContractNameContaining(
                    contractStatus, false, searchKey, pageable);
        } else {

            contractPage = contractRepository.findByStatusAndIsDeleted(
                    contractStatus, false, pageable);
        }

        return getContractListPayerResponse(contractPage.getContent());

    }


    private List<ContractListPayerResponse> getContractListPayerResponse(List<ContractHeader> payerProviderContractList) {
        return payerProviderContractList.stream().map(contract -> {
            ContractListPayerResponse contractListPayerResponse = new ContractListPayerResponse();
            BeanUtils.copyProperties(contract, contractListPayerResponse);
            contractListPayerResponse.setProviderName(contract.getProvider().getProviderName());
            contractListPayerResponse.setProviderPhone(contract.getProvider().getTelephone());
            contractListPayerResponse.setProviderUuid(contract.getProvider().getProviderUuid());
            contractListPayerResponse.setProviderEmail(contract.getProvider().getEmail());
            contractListPayerResponse.setPayerProviderContractUuid(contract.getContractHeaderUuid());
            contractListPayerResponse.setStatus(contract.getStatus().toString());

            return contractListPayerResponse;
        }).toList();
    }

    @Override
    public ResponseEntity<?> approveContract(String contractUuid) {

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null)
            throw new ResourceNotFoundException("Payer Provider Contract", "contractUuid", contractUuid);

        if (Status.ACTIVE.equals(contract.getStatus()))
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Error: Contract is already Active.");

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();


//		String payerUuid = userDetails.getPayerUuid();
        String approver = userDetails.getUserUuid();
        String fullName = userDetails.getFirstName() + " " + userDetails.getFatherName();

        LocalDateTime now = LocalDateTime.now();
        Date date = Date.from(now.atZone(ZoneId.systemDefault()).toInstant());

        contract.setApprovedBy(approver);
        contract.setApprovalDate(date);
        contract.setStatus(Status.APPROVED);

        RestTemplate restTemplate = new RestTemplate();
        String url = providerHostDomain + "/" + providerContractApi;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        JSONObject requestBody = new JSONObject();
        requestBody.put("contractHeaderUuid", contract.getContractHeaderUuid());
        requestBody.put("contractName", contract.getContractName());
        requestBody.put("description", contract.getDescription());
        requestBody.put("contractCode", contract.getContractCode());
        requestBody.put("startDate", contract.getStartDate());
        requestBody.put("endDate", contract.getEndDate());
        requestBody.put("payerUuid", contract.getPayer().getPayerUuid());
        requestBody.put("addedBy", fullName);
        requestBody.put("payerName", "contract.getPayerName()");
        requestBody.put("payerPhone", "payer.getTelephone()");
        requestBody.put("providerUuid", contract.getProvider().getProviderUuid());
        requestBody.put("status", "PENDING");

        HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

//		TODO AUTHENTICATION , ERROR HANDLING
        if (response == null)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Error: Contract approval and synchronization failed"));

        if (response.getStatusCode() == HttpStatus.OK) {
            contractRepository.save(contract);
            return ResponseEntity.ok(new MessageResponse("Contract Approved and synchronized successfully "));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new MessageResponse("Error: Contract approval and synchronization failed"));

    }

    @Override
    public List<ContractListPayerResponse> getProvidersContractLists(String providerUuid, String searchKey, int page,
                                                                     int limit, Status status) {
//		return contractListRepository.findProvidersContracs(providerUuid, searchKey, page, limit, status);
        return null;
    }

    @Override
    public ResponseEntity<?> payerAgreementResponse(String payerProviderContractUuid, String status, String remark) {
        ContractHeader contract = contractRepository.findByContractHeaderUuid(payerProviderContractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Payer Provider Contract", "contractUuid", payerProviderContractUuid);
        }
        contract.setStatus(Status.valueOf(status));
        contract.setRemark(remark);
        contractRepository.save(contract);
        return ResponseEntity.ok().build();

    }

    @Override
    public ResponseEntity<?> getAvailableProvidersForContract(String searchKey, Pageable pageable) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (payerUuid == null) {
            throw new BadRequestException("User is not associated with any payer");
        }

        Page<Provider> providers;
        if (searchKey != null && !searchKey.isEmpty()) {
            providers = providerRepository.findAvailableProvidersForPayer(
                    payerUuid, searchKey, Status.ACTIVE.toString(), pageable);
        } else {
            providers = providerRepository.findAvailableProvidersForPayer(
                    payerUuid, "", Status.ACTIVE.toString(), pageable);
        }

        List<ProviderResponse> responseList = providers.getContent().stream()
                .map(provider -> {
                    ProviderResponse response = new ProviderResponse();
                    BeanUtils.copyProperties(provider, response);
                    return response;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new PagedResponse<>(
                responseList,
                providers.getNumber(),
                providers.getSize(),
                providers.getTotalElements(),
                providers.getTotalPages(),
                providers.isLast()
        ));
    }

    @Override
    public ResponseEntity<?> getAvailableServicesForProvider(String providerUuid, String searchKey, Pageable pageable) {

        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null) {
            throw new ResourceNotFoundException("Provider", "providerUuid", providerUuid);
        }

        Page<Servicelist> services;
        if (searchKey != null && !searchKey.isEmpty()) {
            services = servicelistRepository.findByProviderAndNameContaining(Optional.of(provider), searchKey, pageable);
        } else {
            services = servicelistRepository.findByProvider(Optional.of(provider), pageable);
        }

        List<ServiceResponse> responseList = services.getContent().stream()
                .map(service -> {
                    ServiceResponse response = new ServiceResponse();
                    BeanUtils.copyProperties(service, response);
                    return response;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(new PagedResponse<>(
                responseList,
                services.getNumber(),
                services.getSize(),
                services.getTotalElements(),
                services.getTotalPages(),
                services.isLast()
        ));
    }

    @Override
    @Transactional
    public ResponseEntity<?> addEmployeeGroupsToContract(String contractUuid, List<EmployeeGroupRequest> groups) {

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        List<EmployeeDependantGroup> createdGroups = new ArrayList<>();
        for (EmployeeGroupRequest groupRequest : groups) {
            EmployeeDependantGroup group = new EmployeeDependantGroup();
            group.setGroupName(groupRequest.getGroupName());
            group.setGroupDescription(groupRequest.getGroupDescription());
            group.setEstimatedMembers(groupRequest.getEstimatedMembers());
            group.setPayer(payer);

            createdGroups.add(employeeDependantGroupRepository.save(group));
        }

        return ResponseEntity.ok(new MessageResponse("Added " + createdGroups.size() + " employee groups to contract"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> assignServicesToEmployeeGroups(String contractUuid,
                                                            List<ContractServiceGroupAssignmentRequest> assignments) {

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        int assignmentCount = 0;

        for (ContractServiceGroupAssignmentRequest assignment : assignments) {

            ContractDetail detail = contractDetailRepository.findByContractDetailUuid(assignment.getContractDetailUuid());
            if (detail == null) {
                throw new ResourceNotFoundException("Contract Detail", "contractDetailUuid", assignment.getContractDetailUuid());
            }

            if (!detail.getContractHeader().getContractHeaderUuid().equals(contractUuid)) {
                throw new BadRequestException("Contract detail does not belong to this contract");
            }

            for (String groupUuid : assignment.getEmployeeGroupUuids()) {
                EmployeeDependantGroup group = (EmployeeDependantGroup) employeeDependantGroupRepository.findByGroupUuid(groupUuid);
                if (group == null) {
                    throw new ResourceNotFoundException("Employee Group", "groupUuid", groupUuid);
                }

                boolean exists = contractDetailEmployeeGroupRepository.existsByContractDetailAndEmployeeDependantGroup(detail, group);
                if (!exists) {

                    ContractDetailEmployeeGroup linkage = new ContractDetailEmployeeGroup();
                    linkage.setContractDetail(detail);
                    linkage.setEmployeeDependantGroup(group);
                    linkage.setContractDetailUuid(detail.getContractDetailUuid());
                    linkage.setEmployeeGroupUuid(group.getGroupUuid());

                    contractDetailEmployeeGroupRepository.save(linkage);
                    assignmentCount++;
                }
            }
        }

        return ResponseEntity.ok(new MessageResponse("Created " + assignmentCount + " service-group assignments"));
    }


    //Filmon

    @Override
    @Transactional
    public ResponseEntity<?> addServiceToContract(String contractUuid, @Valid ContractDetailRequest detailRequest) {
        // Validate contract exists and belongs to the payer
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();
        System.out.println("payer uuid "+payerUuid);
        System.out.println( "in the add service to contract ");
        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        boolean serviceExists = contractDetailRepository.existsByContractHeaderContractHeaderUuidAndServicelistServiceUuid(
                contractUuid, detailRequest.getServiceUuid());
        if (serviceExists) {
            throw new BadRequestException("Service already exists in this contract");
        }

        Servicelist service = servicelistRepository.findByServiceUuid(detailRequest.getServiceUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceUuid", detailRequest.getServiceUuid()));

        ContractDetail contractDetail = new ContractDetail();
        contractDetail.setContractHeader(contract);
        contractDetail.setServicelist(service);
        contractDetail.setContractHeaderUuid(contractUuid);
        contractDetail.setServiceUuid(detailRequest.getServiceUuid());
        contractDetail.setNegotiatedPrice(detailRequest.getNegotiatedPrice());
        contractDetail.setStatus(Status.PENDING);

        contractDetail = contractDetailRepository.save(contractDetail);

        // Add employee groups if provided
        if (detailRequest.getEmployeeGroupUuids() != null && !detailRequest.getEmployeeGroupUuids().isEmpty()) {
            for (String groupUuid : detailRequest.getEmployeeGroupUuids()) {
                EmployeeDependantGroup group = (EmployeeDependantGroup) employeeDependantGroupRepository.findByGroupUuid(groupUuid);
                if (group != null) {
                    contractDetail.addEmployeeDependantGroup(group);
                }
            }
            contractDetailRepository.save(contractDetail);
        }

        return ResponseEntity.ok(new MessageResponse("Service added to contract successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateContractDetail(String contractDetailUuid, @Valid ContractDetailRequest detailRequest) {
        // Validate contract detail exists
        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
        if (contractDetail == null) {
            throw new ResourceNotFoundException("Contract Detail", "contractDetailUuid", contractDetailUuid);
        }

        // Validate user has access to this contract
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!contractDetail.getContractHeader().getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        // Update negotiated price
        contractDetail.setNegotiatedPrice(detailRequest.getNegotiatedPrice());

        // Update service if changed
        if (!contractDetail.getServiceUuid().equals(detailRequest.getServiceUuid())) {
            Servicelist service = servicelistRepository.findByServiceUuid(detailRequest.getServiceUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceUuid", detailRequest.getServiceUuid()));

            contractDetail.setServicelist(service);
            contractDetail.setServiceUuid(detailRequest.getServiceUuid());
        }

        // Update employee groups
        if (detailRequest.getEmployeeGroupUuids() != null) {
            // Clear existing groups
            contractDetail.getEmployeeDependantGroups().clear();

            // Add new groups
            for (String groupUuid : detailRequest.getEmployeeGroupUuids()) {
                EmployeeDependantGroup group = (EmployeeDependantGroup) employeeDependantGroupRepository.findByGroupUuid(groupUuid);
                if (group != null) {
                    contractDetail.addEmployeeDependantGroup(group);
                }
            }
        }

        contractDetailRepository.save(contractDetail);
        return ResponseEntity.ok(new MessageResponse("Contract detail updated successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> removeServiceFromContract(String contractDetailUuid) {
        // Validate contract detail exists
        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
        if (contractDetail == null) {
            throw new ResourceNotFoundException("Contract Detail", "contractDetailUuid", contractDetailUuid);
        }

        // Validate user has access to this contract
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!contractDetail.getContractHeader().getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        // Soft delete the contract detail
        contractDetail.setDeleted(true);
        contractDetailRepository.save(contractDetail);

        return ResponseEntity.ok(new MessageResponse("Service removed from contract successfully"));
    }

    @Override
    public List<ContractDetailResponse> getContractDetails(String contractUuid, Pageable pageable) {
        // Validate contract exists
        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        // Get contract details
        List<ContractDetail> details = contractDetailRepository.findByContractHeaderContractHeaderUuid(contractUuid);

        // Map to response DTOs
        return details.stream().map(detail -> {
            ContractDetailResponse response = new ContractDetailResponse();
            response.setContractDetailUuid(detail.getContractDetailUuid());
            response.setServiceUuid(detail.getServiceUuid());
            response.setServiceName(detail.getServicelist().getServiceName());
            response.setServiceCode(detail.getServicelist().getServiceCode());
            response.setServiceCategory(detail.getServicelist().getServiceCategory());
            response.setServiceSubCategory(detail.getServicelist().getServiceSubCategory());
            response.setNegotiatedPrice(detail.getNegotiatedPrice());
            response.setDefaultPrice(detail.getServicelist().getDefaultPrice());
            response.setStatus(detail.getStatus().toString());

            // Map assigned groups
            response.setAssignedGroups(detail.getEmployeeDependantGroups().stream()
                    .map(group -> {
                        EmployeeGroupResponse groupResponse = new EmployeeGroupResponse();
                        groupResponse.setGroupUuid(group.getGroupUuid());
                        groupResponse.setGroupName(group.getGroupName());
                        groupResponse.setGroupDescription(group.getGroupDescription());
                        groupResponse.setEstimatedMembers(group.getEstimatedMembers());
                        return groupResponse;
                    }).collect(Collectors.toList()));

            return response;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> submitContractForApproval(String contractUuid) {
        // Validate contract exists
        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        // Validate user has access to this contract
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        // Validate contract has details
        List<ContractDetail> details = contractDetailRepository.findByContractHeaderContractHeaderUuid(contractUuid);
        if (details.isEmpty()) {
            throw new BadRequestException("Contract must have at least one service before submission");
        }

        // Update contract status
        contract.setStatus(Status.PENDING_APPROVAL);
        contractRepository.save(contract);

        // TODO: Send notification to approvers

        return ResponseEntity.ok(new MessageResponse("Contract submitted for approval successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> reviewContract(String contractUuid, String reviewerComments, boolean approved) {
        // Validate contract exists
        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        // Validate contract is in pending approval state
        if (contract.getStatus() != Status.PENDING_APPROVAL) {
            throw new BadRequestException("Contract is not pending approval");
        }

        // Get current user
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String reviewerUuid = userDetails.getUserUuid();

        // Update contract status based on approval decision
        if (approved) {
            contract.setStatus(Status.APPROVED);
            contract.setApprovedBy(reviewerUuid);
            contract.setApprovalDate(new Date());
        } else {
            contract.setStatus(Status.REJECTED);
            contract.setRemark(reviewerComments);
        }

        contractRepository.save(contract);

        // TODO: Send notification to contract creator

        return ResponseEntity.ok(new MessageResponse(
                approved ? "Contract approved successfully" : "Contract rejected"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> initiateContractRenewal(String contractUuid, @Valid ContractRenewalRequest renewalRequest) {
        // Validate contract exists
        ContractHeader originalContract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (originalContract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        // Validate user has access to this contract
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!originalContract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        // Validate dates
        if (renewalRequest.getEndDate().isBefore(renewalRequest.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

        // Create new contract as a renewal
        ContractHeader renewalContract = new ContractHeader();
        renewalContract.setContractName(originalContract.getContractName() + " (Renewal)");
        renewalContract.setContractDescription(originalContract.getContractDescription());
        renewalContract.setContractNumber(originalContract.getContractNumber() + "-R");
        renewalContract.setContractCode(originalContract.getContractCode() + "-R");
        renewalContract.setStartDate(renewalRequest.getStartDate());
        renewalContract.setEndDate(renewalRequest.getEndDate());
        renewalContract.setStatus(Status.DRAFT);
        renewalContract.setPreparedBy(userDetails.getUserUuid());
        renewalContract.setPayer(originalContract.getPayer());
        renewalContract.setProvider(originalContract.getProvider());
        renewalContract.setRemark(renewalRequest.getRenewalNotes());

        contractRepository.save(renewalContract);

        // Copy contract details if requested
        if (renewalRequest.isCopyExistingTerms()) {
            List<ContractDetail> originalDetails = contractDetailRepository.findByContractHeaderContractHeaderUuid(contractUuid);

            for (ContractDetail originalDetail : originalDetails) {
                ContractDetail newDetail = new ContractDetail();
                newDetail.setContractHeader(renewalContract);
                newDetail.setServicelist(originalDetail.getServicelist());
                newDetail.setContractHeaderUuid(renewalContract.getContractHeaderUuid());
                newDetail.setServiceUuid(originalDetail.getServiceUuid());
                newDetail.setNegotiatedPrice(originalDetail.getNegotiatedPrice());
                newDetail.setStatus(Status.PENDING);

                ContractDetail savedDetail = contractDetailRepository.save(newDetail);

                // Copy employee group assignments
                for (EmployeeDependantGroup group : originalDetail.getEmployeeDependantGroups()) {
                    savedDetail.addEmployeeDependantGroup(group);
                }

                contractDetailRepository.save(savedDetail);
            }
        }

        return ResponseEntity.ok(new MessageResponse("Contract renewal initiated successfully. New contract UUID: "
                + renewalContract.getContractHeaderUuid()));
    }

    @Override
    @Transactional
    public ResponseEntity<?> cancelRenewal(String renewalUuid) {
        // Validate renewal contract exists
        ContractHeader renewalContract = contractRepository.findByContractHeaderUuid(renewalUuid);
        if (renewalContract == null) {
            throw new ResourceNotFoundException("Contract", "renewalUuid", renewalUuid);
        }

        // Validate user has access to this contract
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!renewalContract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        // Validate contract is in draft or pending approval state
        if (renewalContract.getStatus() != Status.DRAFT && renewalContract.getStatus() != Status.PENDING_APPROVAL) {
            throw new BadRequestException("Only draft or pending approval contracts can be cancelled");
        }

        // Delete the renewal contract
        renewalContract.setDeleted(true);
        contractRepository.save(renewalContract);

        // Delete associated contract details
        List<ContractDetail> details = contractDetailRepository.findByContractHeaderContractHeaderUuid(renewalUuid);
        for (ContractDetail detail : details) {
            detail.setDeleted(true);
            contractDetailRepository.save(detail);
        }

        return ResponseEntity.ok(new MessageResponse("Contract renewal cancelled successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> terminateContract(String contractUuid, ContractTerminationRequest terminationRequest) {

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        if (contract.getStatus() != Status.ACTIVE) {
            throw new BadRequestException("Only active contracts can be terminated");
        }

        LocalDate today = LocalDate.now();
        if (terminationRequest.getTerminationDate().isBefore(today)) {
            throw new BadRequestException("Termination date cannot be in the past");
        }

        contract.setStatus(Status.TERMINATION_PENDING);
        contract.setTerminationDate(Date.from(terminationRequest.getTerminationDate()
                .atStartOfDay(ZoneId.systemDefault()).toInstant()));
        contract.setTerminationReason(terminationRequest.getTerminationReason());
        contract.setTerminationNotes(terminationRequest.getAdditionalNotes());
        contract.setTerminatedBy(userDetails.getUserUuid());
        contract.setTerminationRequestDate(new Date());

        contractRepository.save(contract);

        // TODO: Send notification to provider about termination

        return ResponseEntity.ok(new MessageResponse("Contract termination request submitted successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> withdrawTermination(String contractUuid) {

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        if (contract.getStatus() != Status.TERMINATION_PENDING) {
            throw new BadRequestException("Only contracts with pending termination can have termination withdrawn");
        }

        // Reset termination details
        contract.setStatus(Status.ACTIVE);
        contract.setTerminationDate(null);
        contract.setTerminationReason(null);
        contract.setTerminationNotes(null);
        contract.setTerminatedBy(null);
        contract.setTerminationRequestDate(null);

        contractRepository.save(contract);

        // TODO: Send notification to provider about termination withdrawal

        return ResponseEntity.ok(new MessageResponse("Contract termination request withdrawn successfully"));
    }

    @Override
    public ResponseEntity<?> getFilteredContracts(ContractFilterRequest filter, Pageable pageable, int page) {

        if (page > 0) {
            page = page - 1;
        }

        Page<ContractHeader> contractPage = contractRepository.findFilteredContracts(filter, pageable);

        Page<ContractResponse> responsePage = contractPage.map(contract -> {
            ContractResponse response = new ContractResponse();
            BeanUtils.copyProperties(contract, response);

            // Explicitly map related entities to avoid null values
            if (contract.getPayer() != null) {
                response.setPayerUuid(contract.getPayer().getPayerUuid());
                response.setPayerName(contract.getPayer().getPayerName());
                response.setPayerCode(contract.getPayer().getPayerCode());
            }

            if (contract.getProvider() != null) {
                response.setProviderUuid(contract.getProvider().getProviderUuid());
                response.setProviderName(contract.getProvider().getProviderName());
                response.setProviderCode(contract.getProvider().getProviderCode());
            }

            // Ensure dates are properly mapped
            response.setStartDate(contract.getStartDate());
            response.setEndDate(contract.getEndDate());

            // Map other potentially null fields with defaults if needed
            response.setContractNumber(contract.getContractNumber() != null ?
                    contract.getContractNumber() : "");
            response.setContractDescription(contract.getContractDescription() != null ?
                    contract.getContractDescription() : "");
            response.setRemark(contract.getRemark() != null ?
                    contract.getRemark() : "");
            response.setDescription(contract.getDescription() != null ?
                    contract.getDescription() : "");

            // Set status explicitly
            response.setStatus(contract.getStatus());

            return response;
        });

        return ResponseEntity.ok(responsePage);
    }

    @Override
    public DetailedContractResponse getDetailedContract(String contractHeaderUuid, String userType) {
        ContractHeader contractHeader = contractRepository.findByContractHeaderUuid(contractHeaderUuid);
        if (contractHeader == null) {
            throw new ResourceNotFoundException("Contract", "contractHeaderUuid", contractHeaderUuid);
        }

        DetailedContractResponse response = new DetailedContractResponse();

        response.setContractHeaderUuid(contractHeader.getContractHeaderUuid());
        response.setContractNumber(contractHeader.getContractNumber());
        response.setContractName(contractHeader.getContractName());
        response.setContractDescription(contractHeader.getContractDescription());
        response.setStartDate(contractHeader.getStartDate());
        response.setEndDate(contractHeader.getEndDate());
        response.setStatus(contractHeader.getStatus().toString());
        response.setPayerName(contractHeader.getPayer().getPayerName());
        response.setProviderName(contractHeader.getProvider().getProviderName());
        response.setCoPaymentPercentage(contractHeader.getCoPaymentPercentage());

        // Set logos based on userType
        if ("payer".equalsIgnoreCase(userType)) {
            response.setPayerLogoBase64(getBase64FromPath(contractHeader.getPayer().getLogoPath(), "payer"));
            response.setProviderLogoBase64(""); // Payers don't need provider logo
        } else if ("provider".equalsIgnoreCase(userType)) {
            response.setProviderLogoBase64(getBase64FromPath(contractHeader.getProvider().getLogoPath(), "provider"));
            response.setPayerLogoBase64(""); // Providers don't need payer logo
        } else {
            // Handle invalid userType
            throw new BadRequestException("Invalid user type: " + userType);
        }

        response.setContractDetails(contractHeader.getContractDetails().stream()
                .map(this::mapContractDetail)
                .collect(Collectors.toList()));

        response.setInsured(contractHeader.getInsured().stream()
                .map(this::mapInsured)
                .collect(Collectors.toList()));

        return response;
    }

    private String getBase64FromPath(String logoPath, String logoType) {
        if (logoPath == null || logoPath.isEmpty()) {
            return "";
        }

        try {
            String baseDirectory;
            if ("payer".equalsIgnoreCase(logoType)) {
                baseDirectory = payerLogosDirectory;
            } else if ("provider".equalsIgnoreCase(logoType)) {
                baseDirectory = providerLogosDirectory;
            } else {
                log.error("Invalid logo type: {}", logoType);
                return "";
            }

            Path path = Paths.get(baseDirectory, logoPath);
            if (!Files.exists(path)) {
                log.error("Logo file does not exist at path: {}", path.toString());
                return "";
            }

            byte[] imageBytes = Files.readAllBytes(path);
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
            String contentType = determineContentType(logoPath);
            return "data:" + contentType + ";base64," + base64Image;
        } catch (IOException e) {
            log.error("Error reading logo: {}", e.getMessage(), e);
            return "";
        }
    }

    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            default -> "application/octet-stream";
        };
    }

    @Override
    @Transactional
    public ResponseEntity<?> addInsuredToContract(String contractUuid, AddInsuredToContractRequest request) {

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        List<Insured> insuredList = insuredRepository.findByInsuredUuidIn(request.getInsuredUuids());
        for (Insured insured : insuredList) {
            if (!insured.getPayer().getPayerUuid().equals(payerUuid)) {
                throw new BadRequestException("Insured " + insured.getInsuredUuid() + " does not belong to this payer");
            }
            contract.addInsured(insured);
        }

        if (request.getDependants() != null) {
            for (AddInsuredToContractRequest.DependantRequest dependantRequest : request.getDependants()) {
                Insured insured = insuredRepository.findByInsuredUuid(dependantRequest.getInsuredUuid());
                if (insured == null || !insured.getPayer().getPayerUuid().equals(payerUuid)) {
                    throw new BadRequestException("Invalid insured UUID: " + dependantRequest.getInsuredUuid());
                }

                Dependant dependant = dependantRepository.findByDependantUuid(dependantRequest.getDependantUuid());
                if (dependant == null || !dependant.getInsured().equals(insured)) {
                    throw new BadRequestException("Invalid dependant UUID: " + dependantRequest.getDependantUuid());
                }

                contract.addDependant(dependant);
            }
        }

        contractRepository.save(contract);

        return ResponseEntity.ok(new MessageResponse("Insured and dependants added to contract successfully"));
    }
    private DetailedContractResponse.ContractDetailResponse mapContractDetail(ContractDetail detail) {
        DetailedContractResponse.ContractDetailResponse response = new DetailedContractResponse.ContractDetailResponse();
        response.setContractDetailUuid(detail.getContractDetailUuid());
        response.setServiceUuid(detail.getServiceUuid());
        response.setServiceName(detail.getServicelist().getServiceName());
        response.setNegotiatedPrice(detail.getNegotiatedPrice().doubleValue());
        response.setEmployeeDependantGroups(detail.getEmployeeDependantGroups().stream()
                .map(EmployeeDependantGroup::getGroupName)
                .collect(Collectors.toList()));
        return response;
    }

    private DetailedContractResponse.InsuredResponse mapInsured(Insured insured) {
        DetailedContractResponse.InsuredResponse response = new DetailedContractResponse.InsuredResponse();
        response.setInsuredUuid(insured.getInsuredUuid());
        response.setFullName(insured.getFirstName() + " " + insured.getFatherName());
        response.setMembershipNumber(insured.getIdNumber());
        response.setDependants(insured.getDependants().stream()
                .map(this::mapDependant)
                .collect(Collectors.toList()));
        return response;
    }

    private DetailedContractResponse.DependantResponse mapDependant(Dependant dependant) {
        DetailedContractResponse.DependantResponse response = new DetailedContractResponse.DependantResponse();
        response.setDependantUuid(dependant.getDependantUuid());
        response.setFullName(dependant.getFirstName() + " " + dependant.getFatherName());
        response.setRelationshipType(dependant.getRelationship());
        return response;
    }

}