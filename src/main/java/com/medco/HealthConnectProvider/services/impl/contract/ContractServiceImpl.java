package com.medco.HealthConnectProvider.services.impl.contract;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserDetailsImpl;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.groups.ContractDetailEmployeeGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.group.ContractDetailEmployeeGroupRepository;
import com.medco.HealthConnectProvider.repository.group.EmployeeDependantGroupRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.service.ServiceRepository;
import com.medco.HealthConnectProvider.services.contract.ContractService;
import com.medco.HealthConnectProvider.ui.request.auth.password.contract.ContractRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.ContractServiceGroupAssignmentRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.group.EmployeeGroupRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractListPayerResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.ui.response.service.ServiceResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
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
    private final ServiceRepository serviceRepository;
    private final PayerRepository payerRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final EmployeeDependantGroupRepository employeeDependantGroupRepository;
    private final ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository;

    @Value("${api.provider.contract}")
    private String providerContractApi;

    @Value("${provider.HostDomain}")
    private String providerHostDomain;

    public ContractServiceImpl(ContractRepository contractRepository, ProviderRepository providerRepository, ServiceRepository serviceRepository, PayerRepository payerRepository, ContractDetailRepository contractDetailRepository, EmployeeDependantGroupRepository employeeDependantGroupRepository, ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository) {
        this.contractRepository = contractRepository;


        this.providerRepository = providerRepository;
        this.serviceRepository = serviceRepository;
        this.payerRepository = payerRepository;
        this.contractDetailRepository = contractDetailRepository;
        this.employeeDependantGroupRepository = employeeDependantGroupRepository;
        this.contractDetailEmployeeGroupRepository = contractDetailEmployeeGroupRepository;
    }

    @Override
    public ResponseEntity<?> createContract(ContractRequest contractRequest) {

        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();

        String preparedBy = userDetails.getUserUuid();
        ContractHeader contract = new ContractHeader();
        BeanUtils.copyProperties(contractRequest, contract);
        contract.setPreparedBy(preparedBy);
        contract.setStatus(Status.PENDING);
        contractRepository.save(contract);

        return ResponseEntity.ok(new MessageResponse("Contract added successfully!"));
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
        UserDetailsImpl userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getInstitutionUuid();
//		List <Contract> payerProviderContractList =contractRepository.findByInstitutionInstitutionUuidAndStatusAndIsDeleted(payerUuid,status,false,pageable);
        List <ContractHeader> payerProviderContractList = (List<ContractHeader>) contractRepository.findByProviderProviderUuidAndStatusAndIsDeleted("c403bb86-a9f3-4429-abe7-d61dd1a07f66",status,false,pageable);
//		return contractListRepository.findPayerProvidersContractAll(payerUuid, searchKey, page, limit, status);
        return getContractListPayerResponse(payerProviderContractList);
    }


    private List<ContractListPayerResponse> getContractListPayerResponse(List<ContractHeader> payerProviderContractList) {
        return payerProviderContractList.stream().map(contract -> {
            ContractListPayerResponse contractListPayerResponse=new ContractListPayerResponse();
            BeanUtils.copyProperties(contract,contractListPayerResponse);
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
         if (contract==null){
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
        Page<com.medco.HealthConnectProvider.entity.services.Service> services;
        if (searchKey != null && !searchKey.isEmpty()) {
            services = serviceRepository.findByProviderAndNameContaining(provider, searchKey, pageable);
        } else {
            services = serviceRepository.findByProvider(provider, pageable);
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

}
