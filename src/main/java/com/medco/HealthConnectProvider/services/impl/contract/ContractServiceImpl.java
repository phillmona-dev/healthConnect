package com.medco.HealthConnectProvider.services.impl.contract;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.drug.Drug;
import com.medco.HealthConnectProvider.entity.groups.ContractDetailEmployeeGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceAlreadyExistsException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.drug.DrugRepository;
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
import com.medco.HealthConnectProvider.ui.request.auth.password.group.EmployeeGroupRequest;
import com.medco.HealthConnectProvider.ui.request.contract.AddInsuredToContractRequest;
import com.medco.HealthConnectProvider.ui.request.contract.ContractFilterRequest;
import com.medco.HealthConnectProvider.ui.request.contract.ContractStatusUpdateRequest;
import com.medco.HealthConnectProvider.ui.request.contract.CreateActiveContractRequest;
import com.medco.HealthConnectProvider.ui.response.ApiErrorResponse;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.*;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeGroupResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.ui.response.service.ServiceResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final DrugRepository drugRepository;

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
                               ContractDetailEmployeeGroupRepository contractDetailEmployeeGroupRepository, InsuredRepository insuredRepository, DependantRepository dependantRepository, DrugRepository drugRepository, ModelMapper modelMapper) {

        this.contractRepository = contractRepository;
        this.providerRepository = providerRepository;
        this.servicelistRepository = servicelistRepository;
        this.payerRepository = payerRepository;
        this.contractDetailRepository = contractDetailRepository;
        this.employeeDependantGroupRepository = employeeDependantGroupRepository;
        this.contractDetailEmployeeGroupRepository = contractDetailEmployeeGroupRepository;
        this.insuredRepository = insuredRepository;
        this.dependantRepository = dependantRepository;
        this.drugRepository = drugRepository;
        this.modelMapper = modelMapper;

    }

    @Transactional
    @Override
    public ResponseEntity<ContractResponse> createContract(ContractRequest contractRequest) {

        Payer payer = payerRepository.findByPayerUuid(contractRequest.getPayerUuid());
        if (payer == null){
            throw new ResourceNotFoundException("Payer", "payerUuid", contractRequest.getPayerUuid());
        }

        Provider provider = providerRepository.findByProviderUuid(contractRequest.getProviderUuid());
        if (provider == null){
            throw new ResourceNotFoundException("Provider", "providerUuid", contractRequest.getProviderUuid());
        }

        String contractName = payer.getPayerName() + " - " + provider.getThreeDigitAcronym();
        String uniqueContractName = generateUniqueContractName(contractName);
        String contractCode = generateContractCode();

        ContractHeader contract = new ContractHeader();
        modelMapper.map(contractRequest, contract);

        contract.setPayer(payer);
        contract.setProvider(provider);
        contract.setStatus(Status.PENDING);
        contract.setContractName(uniqueContractName);
        contract.setContractCode(contractCode);
        contract.setStartDate(contractRequest.getBeginDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        contract.setEndDate(contractRequest.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        ContractHeader savedContract = contractRepository.save(contract);

        if (contractRequest.getContractItems() != null && !contractRequest.getContractItems().isEmpty()) {
            for (ContractRequest.ContractItemRequest itemRequest : contractRequest.getContractItems()) {
                ContractDetail contractDetail = new ContractDetail();
                contractDetail.setContractHeader(savedContract);
                contractDetail.setContractHeaderUuid(savedContract.getContractHeaderUuid());
                contractDetail.setNegotiatedPrice(itemRequest.getNegotiatedPrice());
                contractDetail.setStatus(Status.ACTIVE);

                if ("DRUG".equalsIgnoreCase(itemRequest.getItemType())) {
                    Drug drug = drugRepository.findByDrugUuid(itemRequest.getItemUuid())
                            .orElseThrow(() -> new ResourceNotFoundException("Drug", "drugUuid", itemRequest.getItemUuid()));
                    contractDetail.setDrug(drug);
                    contractDetail.setDrugUuid(drug.getDrugUuid());
                    contractDetail.setServiceUuid(null);
                } else if ("SERVICE".equalsIgnoreCase(itemRequest.getItemType())) {
                    Servicelist service = servicelistRepository.findByServiceNameAndProvider(itemRequest.getServiceName(), provider)
                            .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceName", itemRequest.getServiceName()));
                    contractDetail.setServicelist(service);
                    contractDetail.setServiceUuid(service.getServiceUuid());
                    contractDetail.setDrugUuid(null);
                } else {
                    throw new BadRequestException("Invalid item type: " + itemRequest.getItemType());
                }

                contractDetailRepository.save(contractDetail);

            }
        }

        ContractResponse response = new ContractResponse();
        modelMapper.map(savedContract, response);

        return ResponseEntity.ok(response);

    }

    private String generateContractCode() {
        LocalDate currentDate = LocalDate.now();
        String datePart = currentDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        return "CT" + datePart;
    }

    @Transactional
    @Override
    public ResponseEntity<?> updateContract(String contractUuid, @Valid ContractRequest contractRequest) {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        logger.info("Updating contract with UUID: {}", contractUuid);
        logger.debug("Contract update request: {}", contractRequest);

        try {
            ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);

            if (contract == null) {
                logger.error("Contract not found with UUID: {}", contractUuid);
                throw new ResourceNotFoundException("Payer Provider Contract", "contractUuid", contractUuid);
            }

            UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
            String preparedBy = userDetails.getUserUuid();

            contract.setStartDate(contractRequest.getBeginDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            contract.setEndDate(contractRequest.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            contract.setStatus(contractRequest.getStatus());
            contract.setPreparedBy(preparedBy);
            contract.setDescription(contractRequest.getDescription());

            logger.info("Updated contract header details. New status: {}, Start date: {}, End date: {}",
                    contract.getStatus(), contract.getStartDate(), contract.getEndDate());

            if (contractRequest.getContractItems() != null && !contractRequest.getContractItems().isEmpty()) {
                logger.info("Processing {} contract items", contractRequest.getContractItems().size());

                Map<String, ContractDetail> existingDetails = new HashMap<>();
                for (ContractDetail detail : contract.getContractDetails()) {
                    String key = (detail.getDrugUuid() != null) ? detail.getDrugUuid() : detail.getServicelist().getServiceName();
                    existingDetails.put(key, detail);
                }

                Set<String> updatedItems = new HashSet<>();

                for (ContractRequest.ContractItemRequest itemRequest : contractRequest.getContractItems()) {
                    logger.debug("Processing item: {}", itemRequest);

                    String itemKey = ("DRUG".equalsIgnoreCase(itemRequest.getItemType())) ? itemRequest.getItemUuid() : itemRequest.getServiceName();
                    ContractDetail contractDetail = existingDetails.get(itemKey);
                    if (contractDetail == null) {
                        logger.info("Creating new contract detail for item: {}", itemKey);
                        contractDetail = new ContractDetail();
                        contractDetail.setContractHeader(contract);
                        contractDetail.setContractHeaderUuid(contract.getContractHeaderUuid());
                        contract.getContractDetails().add(contractDetail);
                    } else {
                        logger.info("Updating existing contract detail for item: {}", itemKey);
                    }

                    updateContractDetail(contractDetail, itemRequest, contractRequest.getStatus());
                    updatedItems.add(itemKey);
                }

                int removedCount = 0;
                Iterator<ContractDetail> iterator = contract.getContractDetails().iterator();
                while (iterator.hasNext()) {
                    ContractDetail detail = iterator.next();
                    String itemKey = (detail.getDrugUuid() != null) ? detail.getDrugUuid() : detail.getServicelist().getServiceName();
                    if (!updatedItems.contains(itemKey)) {
                        logger.info("Removing contract detail with item: {}", itemKey);
                        iterator.remove();
                        removedCount++;
                    }
                }
                logger.info("Removed {} contract details not present in the update request", removedCount);

            } else {
                logger.info("No contract items provided in the update request. Removing all existing items.");
                contract.getContractDetails().clear();
            }

            ContractHeader updatedContract = contractRepository.save(contract);
            logger.info("Contract updated successfully. Contract UUID: {}", updatedContract.getContractHeaderUuid());

            ContractResponse response = new ContractResponse();
            modelMapper.map(updatedContract, response);
            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found while updating contract", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Unexpected error occurred while updating contract", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiErrorResponse("An unexpected error occurred while updating the contract."));
        }
    }

    private void updateContractDetail(ContractDetail contractDetail, ContractRequest.ContractItemRequest itemRequest, Status status) {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        logger.debug("Updating contract detail: {}", contractDetail);
        contractDetail.setNegotiatedPrice(itemRequest.getNegotiatedPrice());
        contractDetail.setStatus(status);

        try {
            if ("DRUG".equalsIgnoreCase(itemRequest.getItemType())) {
                Drug drug = drugRepository.findByDrugUuid(itemRequest.getItemUuid())
                        .orElseThrow(() -> new ResourceNotFoundException("Drug", "drugUuid", itemRequest.getItemUuid()));
                contractDetail.setDrug(drug);
                contractDetail.setDrugUuid(drug.getDrugUuid());
                contractDetail.setServiceUuid(null);
                contractDetail.setServicelist(null);
                logger.info("Updated contract detail with drug. Drug UUID: {}", drug.getDrugUuid());
            } else if ("SERVICE".equalsIgnoreCase(itemRequest.getItemType())) {
                Servicelist service = servicelistRepository.findByServiceName(itemRequest.getServiceName())
                        .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceName", itemRequest.getServiceName()));
                contractDetail.setServicelist(service);
                contractDetail.setServiceUuid(service.getServiceUuid());
                contractDetail.setDrugUuid(null);
                contractDetail.setDrug(null);
                logger.info("Updated contract detail with service. Service Name: {}", service.getServiceName());
            } else {
                logger.error("Invalid item type: {}", itemRequest.getItemType());
                throw new BadRequestException("Invalid item type: " + itemRequest.getItemType());
            }
        } catch (ResourceNotFoundException e) {
            logger.error("Resource not found while updating contract detail", e);
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error occurred while updating contract detail", e);
            throw new RuntimeException("An unexpected error occurred while updating the contract detail.", e);
        }
    }

    @Override
    public ContractResponse getContract(String contractUuid, String userType, String searchKey) {

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);

        if (contract == null)
            throw new ResourceNotFoundException("Payer Provider Contract", "contractUuid", contractUuid);

        ContractResponse contractResponse = new ContractResponse();
        BeanUtils.copyProperties(contract, contractResponse);

        contractResponse.setPayerUuid(contract.getPayer().getPayerUuid());
        contractResponse.setPayerName(contract.getPayer().getPayerName());
        contractResponse.setPayerCode(contract.getPayer().getPayerCode());

        contractResponse.setProviderUuid(contract.getProvider().getProviderUuid());
        contractResponse.setProviderName(contract.getProvider().getProviderName());
        contractResponse.setProviderCode(contract.getProvider().getProviderCode());

        if ("payer".equalsIgnoreCase(userType)) {

            contractResponse.setPayerLogoBase64(getBase64FromPath(contract.getPayer().getLogoPath(), "payer"));
            contractResponse.setProviderLogoBase64("");

            contractResponse.setPayerAddress(contract.getPayer().getAddress1()+ "," + contract.getPayer().getAddress2() + "," + contract.getPayer().getAddress3());
            contractResponse.setPayerTelephone(contract.getPayer().getTelephone());
            contractResponse.setPayerContactEmail(contract.getPayer().getEmail());

            contractResponse.setProviderAddress("");
            contractResponse.setProviderTelephone("");
            contractResponse.setProviderContactEmail("");
        } else if ("provider".equalsIgnoreCase(userType)) {

            contractResponse.setProviderLogoBase64(getBase64FromPath(contract.getProvider().getLogoPath(), "provider"));
            contractResponse.setPayerLogoBase64("");

            contractResponse.setProviderAddress(contract.getProvider().getAddress1()+ "," + contract.getProvider().getAddress2() + "," + contract.getProvider().getAddress3());
            contractResponse.setProviderTelephone(contract.getProvider().getTelephone());
            contractResponse.setProviderContactEmail(contract.getProvider().getEmail());

            contractResponse.setPayerAddress("");
            contractResponse.setPayerTelephone("");
            contractResponse.setPayerContactEmail("");

        } else {
            throw new BadRequestException("Invalid user type: " + userType);
        }

        List<ContractResponse.ContractDetailSummary> contractDetails = contract.getContractDetails().stream()
                .map(this::mapContractDetailSummary)
                .collect(Collectors.toList());
        contractResponse.setContractDetails(contractDetails);

        long totalServices = contractDetails.stream().filter(detail -> "SERVICE".equals(detail.getItemType())).count();
        long totalDrugs = contractDetails.stream().filter(detail -> "DRUG".equals(detail.getItemType())).count();

        contractResponse.setTotalServices((int) totalServices);
        contractResponse.setTotalDrugs((int) totalDrugs);

        List<ContractResponse.InsuredSummary> insuredSummaries = contract.getInsured().stream()
                .map(this::mapInsuredSummary)
                .map(summary -> filterDependants(summary, searchKey))
                .filter(summary -> matchesSearchCriteria(summary, searchKey) || !summary.getDependants().isEmpty())
                .collect(Collectors.toList());

        contractResponse.setInsuredSummaries(insuredSummaries);
        contractResponse.setTotalInsured(insuredSummaries.size());
        contractResponse.setTotalDependants((int) insuredSummaries.stream()
                .mapToLong(summary -> summary.getDependants().size())
                .sum());

        contractResponse.setContractNumber(Optional.ofNullable(contractResponse.getContractNumber()).orElse(""));
        contractResponse.setContractDescription(Optional.ofNullable(contractResponse.getContractDescription()).orElse(""));
        contractResponse.setRemark(Optional.ofNullable(contractResponse.getRemark()).orElse(""));
        contractResponse.setDescription(Optional.ofNullable(contractResponse.getDescription()).orElse(""));

        return contractResponse;

    }

    @Transactional
    @Override
    public ResponseEntity<List<ContractResponse>> createKenemaContracts(List<String> payerUuids) {

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String providerUuid = userDetails.getProviderUuid();

        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null || !provider.getProviderName().toLowerCase().contains("kenema")) {
            throw new BadRequestException("This operation is only allowed for Kenema providers");
        }

        List<ContractResponse> contractResponses = new ArrayList<>();

        for (String payerUuid : payerUuids) {
            Payer payer = payerRepository.findByPayerUuid(payerUuid);
            if (payer == null) {
                throw new ResourceNotFoundException("Payer", "payerUuid", payerUuid);
            }

            String contractName = payer.getPayerName() + " - " + provider.getThreeDigitAcronym();
            String uniqueContractName = generateUniqueContractName(contractName);
            String contractCode = generateContractCode();

            LocalDate startDate = LocalDate.now();
            LocalDate endDate = startDate.plusYears(1);

            ContractHeader contract = ContractHeader.builder()
                    .contractName(uniqueContractName)
                    .contractCode(contractCode)
                    .payer(payer)
                    .provider(provider)
                    .status(Status.ACTIVE)
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            ContractHeader savedContract = contractRepository.save(contract);

            Optional<Provider> optionalProvider = Optional.of(provider);
            Page<Servicelist> providerServicesPage = servicelistRepository.findByProvider(optionalProvider, Pageable.unpaged());
            List<Servicelist> providerServices = providerServicesPage.getContent();

            for (Servicelist service : providerServices) {
                ContractDetail contractDetail = ContractDetail.builder()
                        .contractHeader(savedContract)
                        .contractHeaderUuid(savedContract.getContractHeaderUuid())
                        .servicelist(service)
                        .serviceUuid(service.getServiceUuid())
                        .negotiatedPrice(service.getNegotiatedPrice())
                        .status(Status.ACTIVE)
                        .build();
                contractDetailRepository.save(contractDetail);
            }

            List<Drug> providerDrugs = drugRepository.findByProvider(provider);
            for (Drug drug : providerDrugs) {
                ContractDetail contractDetail = ContractDetail.builder()
                        .contractHeader(savedContract)
                        .contractHeaderUuid(savedContract.getContractHeaderUuid())
                        .drug(drug)
                        .drugUuid(drug.getDrugUuid())
                        .negotiatedPrice(drug.getPrice())
                        .status(Status.ACTIVE)
                        .build();
                contractDetailRepository.save(contractDetail);
            }

            ContractResponse response = new ContractResponse();
            modelMapper.map(savedContract, response);
            contractResponses.add(response);
        }

        return ResponseEntity.ok(contractResponses);
    }

    @Override
    public ContractNewResponse createActiveContract(CreateActiveContractRequest request) {
        if (request.getContractUuid() != null &&
                contractRepository.existsByContractHeaderUuid(request.getContractUuid())) {
            throw new ResourceAlreadyExistsException("Contract", "contractUuid", request.getContractUuid());
        }

        Payer payer = payerRepository.findByPayerUuid(request.getPayerUuid());
        if (payer == null) {
            payer = createNewPayer(request);
        }

        Provider provider = providerRepository.findByProviderUuid(request.getProviderUuid());
        if (provider == null) {
            throw new ResourceNotFoundException("Provider", "providerUuid", request.getProviderUuid());
        }

        boolean contractExists = contractRepository.existsByPayerAndProviderAndIsDeletedFalse(payer, provider);
        if (contractExists) {
            throw new ResourceAlreadyExistsException(
                    "Contract already exists for this payer and provider combination",
                    "payerName", request.getPayerName(),
                    "providerName", provider.getProviderName()
            );
        }

        ContractHeader contract = new ContractHeader();
        modelMapper.map(request, contract);

        contract.setContractHeaderUuid(request.getContractUuid());
        contract.setPayer(payer);
        contract.setProvider(provider);
        contract.setStatus(Status.ACTIVE);
        contract.setContractName(generateUniqueContractName(request.getContractName()));
        contract.setContractCode(request.getContractCode());
        contract.setStartDate(request.getBeginDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        contract.setEndDate(request.getEndDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());

        ContractHeader savedContract = contractRepository.save(contract);

        ContractNewResponse response = new ContractNewResponse();
        modelMapper.map(savedContract, response);

        return response;
    }

    private Payer createNewPayer(CreateActiveContractRequest request) {

        Payer newPayer = new Payer();

        newPayer.setPayerUuid(request.getPayerUuid());
        newPayer.setPayerName(request.getPayerName());
        newPayer.setTelephone(request.getPayerPhone());
        newPayer.setEmail(request.getPayerEmail());
        newPayer.setCategory(request.getPayerCategory());
        newPayer.setAddress2(request.getPayerSubCity());
        newPayer.setInsurance(true);
        newPayer.setStatus(Status.ACTIVE);
        newPayer.setRegistrationDate(new Date());

        return payerRepository.save(newPayer);

    }

    private String generateUniqueContractName(String baseName) {

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        String dateSuffix = now.format(formatter);
        return baseName + " " + dateSuffix;
    }

    private ContractResponse.InsuredSummary filterDependants(ContractResponse.InsuredSummary summary, String searchKey) {

        if (searchKey == null || searchKey.trim().isEmpty()) {
            return summary;
        }

        String lowerCaseSearchKey = searchKey.toLowerCase().trim();
        List<ContractResponse.DependantSummary> filteredDependants = summary.getDependants().stream()
                .filter(dependant -> matchesDependantSearchCriteria(dependant, lowerCaseSearchKey))
                .collect(Collectors.toList());

        summary.setDependants(filteredDependants);
        return summary;

    }

    private boolean matchesSearchCriteria(ContractResponse.InsuredSummary summary, String searchKey) {
        if (searchKey == null || searchKey.trim().isEmpty()) {
            return true;
        }

        String lowerCaseSearchKey = searchKey.toLowerCase().trim();

        return summary.getFullName().toLowerCase().contains(lowerCaseSearchKey) ||
                summary.getMembershipNumber().toLowerCase().contains(lowerCaseSearchKey) ||
                summary.getPhone().toLowerCase().contains(lowerCaseSearchKey);
    }

    private boolean matchesDependantSearchCriteria(ContractResponse.DependantSummary dependant, String lowerCaseSearchKey) {
        return dependant.getFullName().toLowerCase().contains(lowerCaseSearchKey) ||
                dependant.getRelationshipType().toLowerCase().contains(lowerCaseSearchKey);
    }

    private ContractResponse.InsuredSummary mapInsuredSummary(Insured insured) {
        return ContractResponse.InsuredSummary.builder()
                .insuredUuid(insured.getInsuredUuid())
                .fullName(insured.getFirstName() + " " + insured.getFatherName())
                .membershipNumber(insured.getIdNumber())
                .phone(insured.getPhone())
                .dependants(insured.getDependants().stream()
                        .map(this::mapDependantSummary)
                        .collect(Collectors.toList()))
                .build();
    }

    private ContractResponse.DependantSummary mapDependantSummary(Dependant dependant) {
        return ContractResponse.DependantSummary.builder()
                .dependantUuid(dependant.getDependantUuid())
                .fullName(dependant.getFirstName() + " " + dependant.getFatherName())
                .relationshipType(dependant.getRelationship().toString())
                .phone(dependant.getPhone())
                .build();
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
    public ResponseEntity<?> addServiceToContract(String contractUuid, @Valid ContractDetailRequest detailRequest) {

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

        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
        if (contractDetail == null) {
            throw new ResourceNotFoundException("Contract Detail", "contractDetailUuid", contractDetailUuid);
        }

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!contractDetail.getContractHeader().getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        contractDetail.setNegotiatedPrice(detailRequest.getNegotiatedPrice());

        if (!contractDetail.getServiceUuid().equals(detailRequest.getServiceUuid())) {
            Servicelist service = servicelistRepository.findByServiceUuid(detailRequest.getServiceUuid())
                    .orElseThrow(() -> new ResourceNotFoundException("Service", "serviceUuid", detailRequest.getServiceUuid()));

            contractDetail.setServicelist(service);
            contractDetail.setServiceUuid(detailRequest.getServiceUuid());
        }

        if (detailRequest.getEmployeeGroupUuids() != null) {

            contractDetail.getEmployeeDependantGroups().clear();

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

        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
        if (contractDetail == null) {
            throw new ResourceNotFoundException("Contract Detail", "contractDetailUuid", contractDetailUuid);
        }

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!contractDetail.getContractHeader().getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        contractDetail.setDeleted(true);
        contractDetailRepository.save(contractDetail);

        return ResponseEntity.ok(new MessageResponse("Service removed from contract successfully"));
    }

    @Override
    public List<ContractDetailResponse> getContractDetails(String contractUuid, Pageable pageable) {

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        List<ContractDetail> details = contractDetailRepository.findByContractHeaderContractHeaderUuid(contractUuid);

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

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        List<ContractDetail> details = contractDetailRepository.findByContractHeaderContractHeaderUuid(contractUuid);
        if (details.isEmpty()) {
            throw new BadRequestException("Contract must have at least one service before submission");
        }

        contract.setStatus(Status.PENDING_APPROVAL);
        contractRepository.save(contract);

        // TODO: Send notification to approvers

        return ResponseEntity.ok(new MessageResponse("Contract submitted for approval successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> reviewContract(String contractUuid, String reviewerComments, boolean approved) {

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        if (contract.getStatus() != Status.PENDING_APPROVAL) {
            throw new BadRequestException("Contract is not pending approval");
        }

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String reviewerUuid = userDetails.getUserUuid();

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

        ContractHeader originalContract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (originalContract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!originalContract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        if (renewalRequest.getEndDate().isBefore(renewalRequest.getStartDate())) {
            throw new BadRequestException("End date must be after start date");
        }

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

        ContractHeader renewalContract = contractRepository.findByContractHeaderUuid(renewalUuid);
        if (renewalContract == null) {
            throw new ResourceNotFoundException("Contract", "renewalUuid", renewalUuid);
        }

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!renewalContract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        if (renewalContract.getStatus() != Status.DRAFT && renewalContract.getStatus() != Status.PENDING_APPROVAL) {
            throw new BadRequestException("Only draft or pending approval contracts can be cancelled");
        }

        renewalContract.setDeleted(true);
        contractRepository.save(renewalContract);

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

        ContractFilterRequest modifiedFilter = ContractFilterRequest.builder()
                .contractNumber(filter.getContractNumber())
                .contractName(filter.getContractName())
                .status(filter.getStatus())
                .payerUuid(filter.getPayerUuid())
                .providerUuid(filter.getProviderUuid())
                .startDateFrom(filter.getStartDateFrom())
                .startDateTo(filter.getStartDateTo())
                .endDateFrom(filter.getEndDateFrom())
                .endDateTo(filter.getEndDateTo())
                .preparedBy(filter.getPreparedBy())
                .isDeleted(filter.getIsDeleted())
                .build();

        Page<ContractHeader> contractPage = contractRepository.findFilteredContracts(modifiedFilter, pageable);

        Page<ContractResponse> responsePage = contractPage.map(contract -> {
            ContractResponse response = new ContractResponse();
            BeanUtils.copyProperties(contract, response);

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

            if (contract.getContractDetails() != null) {
                List<ContractResponse.ContractDetailSummary> contractDetails = contract.getContractDetails().stream()
                        .map(this::mapContractDetailSummary)
                        .collect(Collectors.toList());
                response.setContractDetails(contractDetails);
                response.setTotalServices(contractDetails.size());
            } else {
                response.setContractDetails(new ArrayList<>());
                response.setTotalServices(0);
            }

            if (contract.getInsured() != null) {
                response.setTotalInsured(contract.getInsured().size());
                response.setTotalDependants((int) contract.getInsured().stream()
                        .flatMap(i -> i.getDependants().stream())
                        .count());
            } else {
                response.setTotalInsured(0);
                response.setTotalDependants(0);
            }

            response.setContractNumber(Optional.ofNullable(response.getContractNumber()).orElse(""));
            response.setContractDescription(Optional.ofNullable(response.getContractDescription()).orElse(""));
            response.setRemark(Optional.ofNullable(response.getRemark()).orElse(""));
            response.setDescription(Optional.ofNullable(response.getDescription()).orElse(""));

            return response;
        });

        return ResponseEntity.ok(responsePage);
    }

    private ContractResponse.ContractDetailSummary mapContractDetailSummary(ContractDetail detail) {
        ContractResponse.ContractDetailSummary summary = new ContractResponse.ContractDetailSummary();
        summary.setContractDetailUuid(detail.getContractDetailUuid());

        summary.setNegotiatedPrice(detail.getNegotiatedPrice().doubleValue());

        if (detail.getServicelist() != null) {
            summary.setServiceUuid(detail.getServiceUuid());
            summary.setServiceName(detail.getServicelist().getServiceName());
            summary.setItemType("SERVICE");
            summary.setPrice(detail.getServicelist().getNegotiatedPrice());
            summary.setDescription(detail.getServicelist().getServiceDescription());
            summary.setServiceCode(detail.getServicelist().getServiceCode());
        } else if (detail.getDrug() != null) {
            summary.setDrugUuid(detail.getDrugUuid());
            summary.setDrugName(detail.getDrug().getDrugName());
            summary.setItemType("DRUG");
            summary.setPrice(detail.getDrug().getPrice());
            summary.setDescription(detail.getDrug().getDescription());
        }

        List<String> assignedGroups = detail.getEmployeeDependantGroups().stream()
                .map(EmployeeDependantGroup::getGroupName)
                .collect(Collectors.toList());
        summary.setAssignedGroups(assignedGroups);

        return summary;
    }

    @Override
    public DetailedContractResponse getDetailedContract(String contractHeaderUuid, String userType) {

        ContractHeader contractHeader = contractRepository.findByContractHeaderUuid(contractHeaderUuid);
        if (contractHeader == null) {
            throw new ResourceNotFoundException("Contract", "contractHeaderUuid", contractHeaderUuid);
        }

        DetailedContractResponse response = new DetailedContractResponse();

        response.setContractHeaderUuid(contractHeader.getContractHeaderUuid());
        response.setContractNumber(contractHeader.getContractNumber() != null ? contractHeader.getContractNumber() : "");
        response.setContractName(contractHeader.getContractName());
        response.setContractDescription(contractHeader.getContractDescription());
        response.setStartDate(contractHeader.getStartDate());
        response.setEndDate(contractHeader.getEndDate());
        response.setStatus(contractHeader.getStatus().toString());
        response.setPayerName(contractHeader.getPayer().getPayerName());
        response.setProviderName(contractHeader.getProvider().getProviderName());
        response.setCoPaymentPercentage(contractHeader.getCoPaymentPercentage());

        if ("payer".equalsIgnoreCase(userType)) {
            response.setPayerLogoBase64(getBase64FromPath(contractHeader.getPayer().getLogoPath(), "payer"));
            response.setProviderLogoBase64("");
        } else if ("provider".equalsIgnoreCase(userType)) {
            response.setProviderLogoBase64(getBase64FromPath(contractHeader.getProvider().getLogoPath(), "provider"));
            response.setPayerLogoBase64("");
        } else {
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

    @Override
    @Transactional
    public ResponseEntity<AssignServicesToGroupResponse> assignServicesToGroup(String groupUuid, List<String> contractDetailUuids) {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        EmployeeDependantGroup group = employeeDependantGroupRepository.findByGroupUuid(groupUuid);
        if (group == null) {
            logger.error("Employee Group not found with UUID: {}", groupUuid);
            throw new ResourceNotFoundException("Employee Group", "groupUuid", groupUuid);
        }

        if (!group.getPayerUuid().equals(payerUuid)) {
            logger.error("Group does not belong to payer with UUID: {}", payerUuid);
            throw new BadRequestException("Group does not belong to this payer");
        }

        int assignmentCount = 0;
        List<String> assignedItems = new ArrayList<>();
        List<String> skippedItems = new ArrayList<>();

        for (String contractDetailUuid : contractDetailUuids) {
            ContractDetail detail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
            if (detail == null) {
                logger.error("Contract Detail not found with UUID: {}", contractDetailUuid);
                throw new ResourceNotFoundException("Contract Detail", "contractDetailUuid", contractDetailUuid);
            }

            if (!detail.getContractHeader().getPayer().getPayerUuid().equals(payerUuid)) {
                logger.error("Contract detail does not belong to payer with UUID: {}", payerUuid);
                throw new BadRequestException("Contract detail does not belong to this payer");
            }

            String itemName;
            String itemType;
            if (detail.getServicelist() != null) {
                itemName = detail.getServicelist().getServiceName();
                itemType = "SERVICE";
            } else if (detail.getDrug() != null) {
                itemName = detail.getDrug().getDrugName();
                itemType = "DRUG";
            } else {
                logger.error("Contract detail {} has neither service nor drug", contractDetailUuid);
                throw new BadRequestException("Contract detail has neither service nor drug");
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
                assignedItems.add(itemType + ": " + itemName);
                logger.info("Assigned {} '{}' to group {}", itemType, itemName, groupUuid);
            } else {
                skippedItems.add(itemType + ": " + itemName);
                logger.info("Skipped {} '{}' for group {} (already assigned)", itemType, itemName, groupUuid);
            }
        }

        AssignServicesToGroupResponse response = AssignServicesToGroupResponse.builder()
                .message("Services and drugs assignment completed")
                .assignedCount(assignmentCount)
                .totalCount(contractDetailUuids.size())
                .assignedItems(assignedItems)
                .skippedItems(skippedItems)
                .build();

        logger.info("Assignment completed. Assigned: {}, Total: {}", assignmentCount, contractDetailUuids.size());
        return ResponseEntity.ok(response);

    }

    @Override
    public ResponseEntity<List<EligibleServiceResponse>> getEligibleServices(String contractHeaderUuid, String insuredUuid, String dependantUuid, String searchKey) {
        log.info("Fetching eligible services for contract: {}, insured: {}, dependant: {}, searchKey: {}", contractHeaderUuid, insuredUuid, dependantUuid, searchKey);

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractHeaderUuid);
        if (contract == null){
            throw new ResourceNotFoundException("Contract", "UUID", contractHeaderUuid);
        }
        log.info("Contract found: {}", contract.getContractHeaderUuid());

        String groupUuid;

        if (dependantUuid != null) {
            Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
            if (dependant == null){
                throw new ResourceNotFoundException("Dependant", "UUID", dependantUuid);
            }

            groupUuid = dependant.getEmployeeDependantGroup().getGroupUuid();
        } else {
            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null){
                throw new ResourceNotFoundException("Insured", "UUID", insuredUuid);
            }

            groupUuid = insured.getEmployeeDependantGroup().getGroupUuid();

        }

        log.info("Group UUID found: {}", groupUuid);

        List<ContractDetailEmployeeGroup> contractDetailGroups = contractDetailEmployeeGroupRepository
                .findByEmployeeGroupUuidAndContractDetail_ContractHeader(groupUuid, contract);

        log.info("Contract detail groups found: {}", contractDetailGroups.size());

        List<EligibleServiceResponse> eligibleServices = contractDetailGroups.stream()
                .map(cdeg -> mapToEligibleServiceResponse(cdeg, contract))
                .filter(response -> matchesSearchCriteria(response, searchKey))
                .collect(Collectors.toList());

        log.info("Eligible services found: {}", eligibleServices.size());

        return ResponseEntity.ok(eligibleServices);

    }

    private EligibleServiceResponse mapToEligibleServiceResponse(ContractDetailEmployeeGroup cdeg, ContractHeader contract) {

        ContractDetail detail = cdeg.getContractDetail();

        EligibleServiceResponse response = new EligibleServiceResponse();
        response.setContractHeaderUuid(contract.getContractHeaderUuid());
        response.setContractName(contract.getContractName());
        response.setContractDetailUuid(detail.getContractDetailUuid());
        response.setServiceUuid(detail.getServiceUuid());
        response.setNegotiatedPrice(detail.getNegotiatedPrice());
        response.setStatus(detail.getStatus().toString());

        if (detail.getServicelist() != null) {
            response.setServiceName(detail.getServicelist().getServiceName());
            response.setServiceCode(detail.getServicelist().getServiceCode());
        } else {
            response.setServiceName("N/A");
            response.setServiceCode("N/A");
        }

        if (detail.getDrug() != null) {
            response.setDrugUuid(detail.getDrug().getDrugUuid());
            response.setDrugName(detail.getDrug().getDrugName());
        } else {
            response.setDrugUuid(null);
            response.setDrugName(null);
        }

        return response;
    }

    private boolean matchesSearchCriteria(EligibleServiceResponse response, String searchKey) {
        if (searchKey == null || searchKey.trim().isEmpty()) {
            return true;
        }

        String lowerCaseSearchKey = searchKey.toLowerCase();

        return response.getServiceName().toLowerCase().contains(lowerCaseSearchKey) ||
                response.getServiceCode().toLowerCase().contains(lowerCaseSearchKey) ||
                response.getContractName().toLowerCase().contains(lowerCaseSearchKey) ||
                (response.getDrugName() != null && response.getDrugName().toLowerCase().contains(lowerCaseSearchKey));
    }

    @Override
    public ResponseEntity<?> updateContractStatus(String contractUuid, ContractStatusUpdateRequest updateRequest) {

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        String userUuid = userDetails.getUserUuid();
        boolean isPayer = userDetails.getPayerUuid() != null;
        boolean isProvider = userDetails.getProviderUuid() != null;

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);

        if (contract == null){
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        if (isPayer && !contract.getPayer().getPayerUuid().equals(userDetails.getPayerUuid())) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        if (isProvider && !contract.getProvider().getProviderUuid().equals(userDetails.getProviderUuid())) {
            throw new BadRequestException("Contract does not belong to this provider");
        }

        switch (updateRequest.getAction().toUpperCase()) {
            case "APPROVE":
                if ((isProvider && contract.getStatus() == Status.PENDING) || (isProvider && contract.getStatus() == Status.RESUBMITTED)) {
                    contract.setStatus(Status.ACTIVE);
                    contract.setProviderReviewedBy(userUuid);
                    contract.setProviderReviewDate(new Date());
                } else {
                    throw new BadRequestException("Invalid action for the current contract status or user role");
                }
                break;
            case "REJECT":
                if ((isProvider && contract.getStatus() == Status.PENDING) ||
                        (isPayer && contract.getStatus() == Status.APPROVED) || (isProvider && contract.getStatus() == Status.RESUBMITTED)) {
                    contract.setStatus(Status.REJECTED);
                    contract.setRejectionReason(updateRequest.getRejectionReason());
                    if (isProvider) {
                        contract.setProviderReviewedBy(userUuid);
                        contract.setProviderReviewDate(new Date());
                    } else {
                        contract.setPayerReviewedBy(userUuid);
                        contract.setPayerReviewDate(new Date());
                    }
                } else {
                    throw new BadRequestException("Invalid action for the current contract status or user role");
                }
                break;
            case "ACTIVATE":
                if (isPayer && contract.getStatus() == Status.APPROVED) {
                    contract.setStatus(Status.ACTIVE);
                    contract.setPayerReviewedBy(userUuid);
                    contract.setPayerReviewDate(new Date());
                } else {
                    throw new BadRequestException("Invalid action for the current contract status or user role");
                }
                break;
            case "RESUBMIT":
                if (isPayer && contract.getStatus() == Status.REJECTED) {
                    contract.setStatus(Status.PENDING);
                    contract.setResubmittedBy(userUuid);
                    contract.setResubmissionDate(new Date());
                    contract.setRejectionReason(null);
                } else {
                    throw new BadRequestException("Invalid action for the current contract status or user role");
                }
                break;
            default:
                throw new BadRequestException("Invalid action");
        }

        contract.setRemark(updateRequest.getRemark());
        contractRepository.save(contract);

        return ResponseEntity.ok(new MessageResponse("Contract status updated successfully"));
    }

    @Override
    @Transactional
    public ResponseEntity<?> softDeleteRejectedContract(String contractUuid) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        if (contract.getStatus() != Status.REJECTED) {
            throw new BadRequestException("Only rejected contracts can be soft deleted");
        }

        if (Boolean.TRUE.equals(contract.isDeleted())) {
            throw new BadRequestException("Contract is already deleted");
        }

        contract.setDeleted(true);
        contract.setDeletedAt(new Date());
        contract.setDeletedBy(userDetails.getUserUuid());

        contractRepository.save(contract);

        return ResponseEntity.ok(new MessageResponse("Contract soft deleted successfully"));
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
    public ResponseEntity<AddInsuredToContractResponse> addInsuredToContract(String contractUuid, AddInsuredToContractRequest request) {
        ContractHeader contract = contractRepository.findByContractHeaderUuid(contractUuid);
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", contractUuid);
        }

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        String payerUuid = userDetails.getPayerUuid();

        if (!contract.getPayer().getPayerUuid().equals(payerUuid)) {
            throw new BadRequestException("Contract does not belong to this payer");
        }

        List<String> invalidInsuredUuids = new ArrayList<>();
        List<AddInsuredToContractResponse.InsuredResponse> addedInsured = new ArrayList<>();

        for (String insuredUuid : request.getInsuredUuids()) {
            Insured insured = insuredRepository.findByInsuredUuid(insuredUuid);
            if (insured == null || !insured.getPayer().getPayerUuid().equals(payerUuid)) {
                invalidInsuredUuids.add(insuredUuid);
            } else {
                contract.addInsured(insured);
                addedInsured.add(mapInsuredToResponse(insured));
            }
        }

        if (!invalidInsuredUuids.isEmpty()) {
            throw new BadRequestException("Invalid insured UUIDs: " + String.join(", ", invalidInsuredUuids));
        }

        List<AddInsuredToContractResponse.DependantResponse> addedDependants = new ArrayList<>();

        if (request.getDependants() != null && !request.getDependants().isEmpty()) {
            List<String> invalidDependantRequests = new ArrayList<>();

            for (AddInsuredToContractRequest.DependantRequest dependantRequest : request.getDependants()) {

                if ("string".equals(dependantRequest.getInsuredUuid()) || "string".equals(dependantRequest.getDependantUuid())) {
                    continue;
                }

                Insured insured = insuredRepository.findByInsuredUuid(dependantRequest.getInsuredUuid());
                if (insured == null || !insured.getPayer().getPayerUuid().equals(payerUuid)) {
                    invalidDependantRequests.add("Invalid insured UUID: " + dependantRequest.getInsuredUuid());
                    continue;
                }

                Dependant dependant = dependantRepository.findByDependantUuid(dependantRequest.getDependantUuid());
                if (dependant == null || !dependant.getInsured().equals(insured)) {
                    invalidDependantRequests.add("Invalid dependant UUID: " + dependantRequest.getDependantUuid());
                    continue;
                }

                contract.addDependant(dependant);
                addedDependants.add(mapDependantToResponse(dependant));
            }

            if (!invalidDependantRequests.isEmpty()) {
                throw new BadRequestException("Invalid dependant requests: " + String.join(", ", invalidDependantRequests));
            }
        }

        contractRepository.save(contract);

        AddInsuredToContractResponse response = new AddInsuredToContractResponse();
        response.setMessage("Insured and dependants added to contract successfully");
        response.setContractUuid(contractUuid);
        response.setAddedInsured(addedInsured);
        response.setAddedDependants(addedDependants);

        return ResponseEntity.ok(response);
    }

    private AddInsuredToContractResponse.InsuredResponse mapInsuredToResponse(Insured insured) {
        AddInsuredToContractResponse.InsuredResponse response = new AddInsuredToContractResponse.InsuredResponse();
        response.setInsuredUuid(insured.getInsuredUuid());
        response.setFullName(insured.getFirstName() + " " + insured.getFatherName());
        response.setMembershipNumber(insured.getIdNumber());
        return response;
    }

    private AddInsuredToContractResponse.DependantResponse mapDependantToResponse(Dependant dependant) {
        AddInsuredToContractResponse.DependantResponse response = new AddInsuredToContractResponse.DependantResponse();
        response.setDependantUuid(dependant.getDependantUuid());
        response.setFullName(dependant.getFirstName() + " " + dependant.getFatherName());
        response.setRelationshipType(String.valueOf(dependant.getRelationship()));
        return response;
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