package com.medco.HealthConnectProvider.services.impl.contract;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserDetailsImpl;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.groups.ContractDetailEmployeeGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.group.ContractDetailEmployeeGroupRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.services.contract.ContractService;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractDetailRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRenewalRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractTerminationRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.ContractServiceGroupAssignmentRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.EmployeeGroupRequest;
import com.medco.HealthConnectProvider.ui.request.contract.ContractFilterRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractDetailResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractListPayerResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractResponse;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeGroupResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.ui.response.service.ServiceResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ContractServiceImpl implements ContractService {

    private final ContractRepository contractRepository;
    private final ProviderRepository providerRepository;
    private final ServicelistRepository servicelistRepository;
    private final PayerRepository payerRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final EmployeeDependantGroupRepository employeeDependantGroupRepository;
    private final ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository;

    private final ModelMapper modelMapper;

    @Value("${api.provider.contract}")
    private String providerContractApi;

    @Value("${provider.HostDomain}")
    private String providerHostDomain;

    public ContractServiceImpl(ContractRepository contractRepository, ProviderRepository providerRepository, ServicelistRepository servicelistRepository, PayerRepository payerRepository, ContractDetailRepository contractDetailRepository, EmployeeDependantGroupRepository employeeDependantGroupRepository, ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository, ModelMapper modelMapper) {
        this.contractRepository = contractRepository;


        this.providerRepository = providerRepository;
        this.servicelistRepository = servicelistRepository;
        this.payerRepository = payerRepository;
        this.contractDetailRepository = contractDetailRepository;
        this.employeeDependantGroupRepository = employeeDependantGroupRepository;
        this.contractDetailEmployeeGroupRepository = contractDetailEmployeeGroupRepository;
        this.modelMapper = modelMapper;
    }

    @Transactional
    @Override
    public ResponseEntity<ContractResponse> createContract(ContractRequest contractRequest) {
        // Fetch provider and payer
        Provider provider = providerRepository.findByProviderUuid(contractRequest.getProviderUuid())
                .orElseThrow(() -> new RuntimeException(
                        "Provider not found with uuid: " + contractRequest.getProviderUuid()));

        Payer payer = payerRepository.findByPayerUuid(contractRequest.getPayerUuid());
        if (payer == null){
            throw  new RuntimeException(
                    "Payer not found with uuid: " + contractRequest.getPayerUuid());
        }

        // Create and populate contract
        ContractHeader contract = new ContractHeader();
        modelMapper.map(contractRequest, contract);

        // Set relationships
        contract.setProvider(provider);
        contract.setPayer(payer);

        // Set additional fields
        contract.setStatus(Status.PENDING);
        contract.setStartDate(contractRequest.getBeginDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate());
        contract.setEndDate(contractRequest.getEndDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate());

        // Save the contract
        ContractHeader savedContract = contractRepository.save(contract);

        // Add contract to provider's and payer's collections
        provider.getContractHeaders().add(savedContract);
        payer.getContractHeaders().add(savedContract);

        // Map to response DTO
        ContractResponse response = modelMapper.map(savedContract, ContractResponse.class);

        // Map related entities
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

        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String preparedBy = userDetails.getUserUuid();

        BeanUtils.copyProperties(contractRequest, contract);
        contract.setStatus(Status.PENDING);
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
        // Get the page of contracts
        Page<ContractHeader> contractPage;

        // Default to ACTIVE status if status is null or empty
        Status contractStatus = (status == null || status.isEmpty())
                ? Status.ACTIVE
                : Status.valueOf(status);

        if (searchKey != null && !searchKey.isEmpty()) {
            // Search with the provided key
            contractPage = contractRepository.findByStatusAndIsDeletedAndContractNameContaining(
                    contractStatus, false, searchKey, pageable);
        } else {
            // Get all contracts with the given status
            contractPage = contractRepository.findByStatusAndIsDeleted(
                    contractStatus, false, pageable);
        }

        // Convert the Page<ContractHeader> to List<ContractListPayerResponse>
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

        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();


//		String payerUuid = userDetails.getInstitutionUuid();
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

//	@Override
//	public ResponseEntity<ApiResponse<Void>> approveContract(String contractUuid) {
//		// 1. Validate Contract
//		Contract contract = contractRepository.findByContractUuid(contractUuid)
//				.orElseThrow(() -> new ResourceNotFoundException("Contract", "contractUuid", contractUuid));
//
//		if (Status.ACTIVE.equals(contract.getStatus())) {
//			throw new ResponseStatusException(HttpStatus.CONFLICT,
//					"Contract is already Active");
//		}
//
//		// 2. Get User Context
//		UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
//		String payerUuid = userDetails.getInstitutionUuid();
//		String approverName = String.format("%s %s",
//				userDetails.getFirstName(),
//				userDetails.getFatherName());
//
//		// 3. Prepare Contract Update
//		contract.setApprovedBy(payerUuid);
//		contract.setApprovalDate(Instant.now());
//		contract.setStatus(Status.ACTIVE);
//
//		// 4. Call Provider Service
//		try {
//			ProviderSyncRequest syncRequest = new ProviderSyncRequest(
//					contract.getContractUuid(),
//					contract.getContractName(),
//					contract.getDescription(),
//					contract.getContractCode(),
//					contract.getBeginDate(),
//					contract.getEndDate(),
//					contract.getInstitutionUuid(),
//					approverName,
//					contract.getProviderUuid()
//			);
//
//			ResponseEntity<Void> response = restTemplate.exchange(
//					UriComponentsBuilder.fromHttpUrl(providerHostDomain)
//							.path(providerContractApi)
//							.build()
//							.toUri(),
//					HttpMethod.POST,
//					new HttpEntity<>(syncRequest, createJsonHeaders()),
//					Void.class
//			);
//
//			if (!response.getStatusCode().is2xxSuccessful()) {
//				log.error("Provider sync failed with status: {}", response.getStatusCode());
//				throw new ServiceIntegrationException("Provider service returned: " + response.getStatusCode());
//			}
//
//			// 5. Finalize
//			contractRepository.save(contract);
//			return ResponseEntity.ok(
//					ApiResponse.success("Contract approved and synchronized successfully"));
//
//		} catch (RestClientException e) {
//			log.error("Provider service call failed", e);
//			throw new ServiceIntegrationException("Failed to communicate with provider service");
//		}
//	}
//
//	// Helper Methods
//	private HttpHeaders createJsonHeaders() {
//		HttpHeaders headers = new HttpHeaders();
//		headers.setContentType(MediaType.APPLICATION_JSON);
//		headers.setAccept(List.of(MediaType.APPLICATION_JSON));
//		return headers;
//	}
//
//	// DTOs
//	public record ProviderSyncRequest(
//			String contractUuid,
//			String contractName,
//			String description,
//			String contractCode,
//			Date beginDate,
//			Date endDate,
//			String payerUuid,
//			String addedBy,
//			String providerUuid
//	) {}
//
//	public record ApiResponse<T>(
//			boolean success,
//			String message,
//			T data,
//			Instant timestamp
//	) {
//		public static <T> ApiResponse<T> success(String message) {
//			return new ApiResponse<>(true, message, null, Instant.now());
//		}
//	}

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


    //Filmon

    @Override
    public ResponseEntity<?> getAvailableProvidersForContract(String searchKey, Pageable pageable) {
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        if (payerUuid == null) {
            throw new BadRequestException("User is not associated with any payer");
        }

        // Find providers that don't have active contracts with this payer
        Page<Provider> providers;
        if (searchKey != null && !searchKey.isEmpty()) {
            providers = providerRepository.findAvailableProvidersForPayer(
                    payerUuid, searchKey, Status.ACTIVE.toString(), pageable);
        } else {
            providers = providerRepository.findAvailableProvidersForPayer(
                    payerUuid, "", Status.ACTIVE.toString(), pageable);
        }

        // Map to response DTOs
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
        // Validate provider exists
        Optional<Provider> provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null) {
            throw new ResourceNotFoundException("Provider", "providerUuid", providerUuid);
        }

        // Find services for this provider
        Page<Servicelist> services;
        if (searchKey != null && !searchKey.isEmpty()) {
            services = servicelistRepository.findByProviderAndNameContaining(provider, searchKey, pageable);
        } else {
            services = servicelistRepository.findByProvider(provider, pageable);
        }

        // Map to response DTOs
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
        // Validate contract exists and belongs to the payer
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        // Get payer entity
        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        if (payer == null) {
            throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
        }

        // Create employee groups
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
        // Validate contract exists and belongs to the payer
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        int assignmentCount = 0;

        // Process each assignment
        for (ContractServiceGroupAssignmentRequest assignment : assignments) {
            // Find contract detail
            ContractDetail detail = contractDetailRepository.findByContractDetailUuid(assignment.getContractDetailUuid());
            if (detail == null) {
                throw new ResourceNotFoundException("Contract Detail", "contractDetailUuid", assignment.getContractDetailUuid());
            }

            // Verify detail belongs to this contract
            if (!detail.getContractHeader().getContractHeaderUuid().equals(contractUuid)) {
                throw new BadRequestException("Contract detail does not belong to this contract");
            }

            // Process group assignments
            for (String groupUuid : assignment.getEmployeeGroupUuids()) {
                EmployeeDependantGroup group = employeeDependantGroupRepository.findByGroupUuid(groupUuid);
                if (group == null) {
                    throw new ResourceNotFoundException("Employee Group", "groupUuid", groupUuid);
                }

                // Check if assignment already exists
                boolean exists = contractDetailEmployeeGroupRepository.existsByContractDetailAndEmployeeDependantGroup(detail, group);
                if (!exists) {
                    // Create new assignment
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
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        // Check if service already exists in contract
        boolean serviceExists = contractDetailRepository.existsByContractHeaderContractHeaderUuidAndServicelistServiceUuid(
                contractUuid, detailRequest.getServiceUuid());
        if (serviceExists) {
            throw new BadRequestException("Service already exists in this contract");
        }

        // Get service
        Servicelist service = servicelistRepository.findByServiceUuid(detailRequest.getServiceUuid())
                .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceUuid", detailRequest.getServiceUuid()));

        // Create contract detail
        ContractDetail contractDetail = new ContractDetail();
        contractDetail.setContractHeader(contract);
        contractDetail.setServicelist(service);
        contractDetail.setContractHeaderUuid(contractUuid);
        contractDetail.setServiceUuid(detailRequest.getServiceUuid());
        contractDetail.setNegotiatedPrice(detailRequest.getNegotiatedPrice());
        contractDetail.setStatus(Status.PENDING);

        // Save contract detail
        contractDetail = contractDetailRepository.save(contractDetail);

        // Add employee groups if provided
        if (detailRequest.getEmployeeGroupUuids() != null && !detailRequest.getEmployeeGroupUuids().isEmpty()) {
            for (String groupUuid : detailRequest.getEmployeeGroupUuids()) {
                EmployeeDependantGroup group = employeeDependantGroupRepository.findByGroupUuid(groupUuid);
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
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

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
                EmployeeDependantGroup group = employeeDependantGroupRepository.findByGroupUuid(groupUuid);
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
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

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
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

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
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
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
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

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
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

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
        // Validate contract exists
        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        // Validate user has access to this contract
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        // Validate contract is active
        if (contract.getStatus() != Status.ACTIVE) {
            throw new BadRequestException("Only active contracts can be terminated");
        }

        // Validate termination date
        LocalDate today = LocalDate.now();
        if (terminationRequest.getTerminationDate().isBefore(today)) {
            throw new BadRequestException("Termination date cannot be in the past");
        }

        // Update contract status and termination details
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
        // Validate contract exists
        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        // Validate user has access to this contract
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        // Validate contract is in termination pending state
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



    public ResponseEntity<?> getFilteredContracts(ContractFilterRequest filter, Pageable pageable) {
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

}