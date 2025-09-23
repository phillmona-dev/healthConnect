package com.medco.HealthConnectProvider.services.impl.packageCategory;

import com.medco.HealthConnectProvider.config.ExternalApiConfig;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategory;
import com.medco.HealthConnectProvider.entity.packageCategory.ServiceCategoryMapping;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.PackageCategoryRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.ServiceCategoryMappingRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.services.packageCategory.ServiceCategoryMappingService;
import com.medco.HealthConnectProvider.ui.request.packageCategory.BulkServiceCategoryAssignmentRequest;
import com.medco.HealthConnectProvider.ui.request.packageCategory.EligibleServiceSearchRequest;
import com.medco.HealthConnectProvider.ui.request.packageCategory.ServiceCategoryMappingRequest;
import com.medco.HealthConnectProvider.ui.response.packageCategory.BulkServiceCategoryAssignmentResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.EligibleServiceDto;
import com.medco.HealthConnectProvider.ui.response.packageCategory.EligibleServiceResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.ExternalPackageEligibleServicesResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ServiceCategoryMappingServiceImpl implements ServiceCategoryMappingService {

    private final ServiceCategoryMappingRepository mappingRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final PackageCategoryRepository categoryRepository;

    private final ExternalApiConfig externalApiConfig;
    private final RestTemplate restTemplate;

    private final ContractRepository contractHeaderRepository;
    private final ServicelistRepository servicelistRepository;

    @Override
    public ResponseEntity<String> mapServiceToCategories(ServiceCategoryMappingRequest request) {
        log.info("Mapping service {} to {} categories", request.getContractDetailUuid(), request.getCategoryUuids().size());

        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(request.getContractDetailUuid());
        if (contractDetail == null) {
            throw new ResourceNotFoundException("Contract detail not found with UUID: " + request.getContractDetailUuid());
        }

        List<ServiceCategoryMapping> mappings = new ArrayList<>();

        for (String categoryUuid : request.getCategoryUuids()) {
            PackageCategory category = categoryRepository.findByCategoryUuid(categoryUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Package category not found with UUID: " + categoryUuid));

            if (mappingRepository.existsByContractDetailAndPackageCategory(contractDetail, category)) {
                log.warn("Mapping already exists for service {} and category {}",
                        request.getContractDetailUuid(), categoryUuid);
                continue;
            }

            if (!category.getPayer().getId().equals(contractDetail.getContractHeader().getPayer().getId())) {
                throw new BadRequestException("Category does not belong to the same payer as the contract");
            }

            ServiceCategoryMapping mapping = ServiceCategoryMapping.builder()
                    .contractDetail(contractDetail)
                    .packageCategory(category)
                    .consumesFromLimit(request.isConsumesFromLimit())
                    .notes(request.getNotes())
                    .build();

            mappings.add(mappingRepository.save(mapping));
        }

        log.info("Successfully created {} service-category mappings", mappings.size());
        return ResponseEntity.ok("Service mapped to categories successfully");
    }

    @Override
    public ResponseEntity<String> removeServiceFromCategory(String contractDetailUuid, String categoryUuid) {
        log.info("Removing service {} from category {}", contractDetailUuid, categoryUuid);

        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
        if (contractDetail == null) {
            throw new ResourceNotFoundException("Contract detail not found with UUID: " + contractDetailUuid);
        }

        PackageCategory category = categoryRepository.findByCategoryUuid(categoryUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Package category not found with UUID: " + categoryUuid));

        ServiceCategoryMapping mapping = mappingRepository.findByContractDetailAndPackageCategory(contractDetail, category)
                .orElseThrow(() -> new ResourceNotFoundException("Service-category mapping not found"));

        mapping.setDeleted(true);
        mappingRepository.save(mapping);

        log.info("Successfully removed service from category");
        return ResponseEntity.ok("Service removed from category successfully");
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getCategoriesByService(String contractDetailUuid) {
        List<ServiceCategoryMapping> mappings = mappingRepository.findActiveByContractDetailUuid(contractDetailUuid);
        return mappings.stream()
                .map(mapping -> mapping.getPackageCategory().getCategoryUuid())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getServicesByCategory(String categoryUuid) {
        List<ServiceCategoryMapping> mappings = mappingRepository.findActiveByCategoryUuid(categoryUuid);
        return mappings.stream()
                .map(mapping -> mapping.getContractDetail().getContractDetailUuid())
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<String> updateServiceCategoryMapping(String mappingUuid, ServiceCategoryMappingRequest request) {
        log.info("Updating service category mapping: {}", mappingUuid);

        ServiceCategoryMapping mapping = mappingRepository.findByMappingUuid(mappingUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Service category mapping not found with UUID: " + mappingUuid));

        mapping.setConsumesFromLimit(request.isConsumesFromLimit());
        mapping.setNotes(request.getNotes());

        mappingRepository.save(mapping);

        log.info("Service category mapping updated successfully");
        return ResponseEntity.ok("Service category mapping updated successfully");
    }

    @Override
    public ResponseEntity<BulkServiceCategoryAssignmentResponse> assignServicesToCategory(BulkServiceCategoryAssignmentRequest request) {
        log.info("Assigning {} services to category {}", request.getContractDetailUuids().size(), request.getCategoryUuid());

        PackageCategory category = categoryRepository.findByCategoryUuid(request.getCategoryUuid())
                .orElseThrow(() -> new ResourceNotFoundException("PackageCategory", "categoryUuid", request.getCategoryUuid()));

        String contractUuid = null;
        String contractName = null;

        List<BulkServiceCategoryAssignmentResponse.ServiceAssignmentResult> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;
        int skippedCount = 0;

        for (String contractDetailUuid : request.getContractDetailUuids()) {
            try {
                BulkServiceCategoryAssignmentResponse.ServiceAssignmentResult result =
                        processServiceAssignment(contractDetailUuid, category, request);

                results.add(result);

                if (contractUuid == null && result.getContractDetailUuid() != null) {
                    ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
                    if (contractDetail != null) {
                        contractUuid = contractDetail.getContractHeader().getContractHeaderUuid();
                        contractName = contractDetail.getContractHeader().getContractName();
                    }
                }

                switch (result.getStatus()) {
                    case "SUCCESS":
                    case "REPLACED":
                        successCount++;
                        break;
                    case "SKIPPED":
                        skippedCount++;
                        break;
                    case "FAILED":
                        failedCount++;
                        break;
                }

            } catch (Exception e) {
                log.error("Error processing service assignment for contractDetail: {}", contractDetailUuid, e);
                results.add(BulkServiceCategoryAssignmentResponse.ServiceAssignmentResult.builder()
                        .contractDetailUuid(contractDetailUuid)
                        .status("FAILED")
                        .message("Error: " + e.getMessage())
                        .build());
                failedCount++;
            }
        }

        BulkServiceCategoryAssignmentResponse response = BulkServiceCategoryAssignmentResponse.builder()
                .categoryUuid(category.getCategoryUuid())
                .categoryName(category.getCategoryName())
                .categoryCode(category.getCategoryCode())
                .contractUuid(contractUuid)
                .contractName(contractName)
                .totalServicesProcessed(request.getContractDetailUuids().size())
                .successfulAssignments(successCount)
                .failedAssignments(failedCount)
                .skippedAssignments(skippedCount)
                .results(results)
                .errors(errors)
                .build();

        log.info("Bulk assignment completed: {} successful, {} failed, {} skipped",
                successCount, failedCount, skippedCount);

        return ResponseEntity.ok(response);
    }

    private BulkServiceCategoryAssignmentResponse.ServiceAssignmentResult processServiceAssignment(
            String contractDetailUuid, PackageCategory category, BulkServiceCategoryAssignmentRequest request) {

        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(contractDetailUuid);
        if (contractDetail == null) {
            return BulkServiceCategoryAssignmentResponse.ServiceAssignmentResult.builder()
                    .contractDetailUuid(contractDetailUuid)
                    .status("FAILED")
                    .message("Contract detail not found")
                    .build();
        }

        if (!category.getPayer().getId().equals(contractDetail.getContractHeader().getPayer().getId())) {
            return BulkServiceCategoryAssignmentResponse.ServiceAssignmentResult.builder()
                    .contractDetailUuid(contractDetailUuid)
                    .serviceName(contractDetail.getServicelist() != null ? contractDetail.getServicelist().getServiceName() : "Unknown")
                    .serviceCode(contractDetail.getServicelist() != null ? contractDetail.getServicelist().getServiceCode() : "Unknown")
                    .status("FAILED")
                    .message("Category does not belong to the same payer as the contract")
                    .build();
        }

        String serviceName = contractDetail.getServicelist() != null ? contractDetail.getServicelist().getServiceName() : "Unknown";
        String serviceCode = contractDetail.getServicelist() != null ? contractDetail.getServicelist().getServiceCode() : "Unknown";

        boolean mappingExists = mappingRepository.existsByContractDetailAndPackageCategory(contractDetail, category);

        if (mappingExists && !request.getReplaceExisting()) {
            return BulkServiceCategoryAssignmentResponse.ServiceAssignmentResult.builder()
                    .contractDetailUuid(contractDetailUuid)
                    .serviceName(serviceName)
                    .serviceCode(serviceCode)
                    .status("SKIPPED")
                    .message("Mapping already exists")
                    .build();
        }

        String status = "SUCCESS";
        String message = "Successfully assigned to category";

        if (mappingExists && request.getReplaceExisting()) {
            ServiceCategoryMapping existingMapping = mappingRepository
                    .findByContractDetailAndPackageCategory(contractDetail, category)
                    .orElse(null);
            if (existingMapping != null) {
                mappingRepository.delete(existingMapping);
                status = "REPLACED";
                message = "Replaced existing mapping";
            }
        }

        ServiceCategoryMapping mapping = ServiceCategoryMapping.builder()
                .contractDetail(contractDetail)
                .packageCategory(category)
                .consumesFromLimit(request.getConsumesFromLimit())
                .notes(request.getNotes())
                .build();

        mappingRepository.save(mapping);

        return BulkServiceCategoryAssignmentResponse.ServiceAssignmentResult.builder()
                .contractDetailUuid(contractDetailUuid)
                .serviceName(serviceName)
                .serviceCode(serviceCode)
                .status(status)
                .message(message)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<EligibleServiceResponse> getEligibleServicesForCategory(EligibleServiceSearchRequest request) {
        log.info("Fetching eligible services for category with search criteria - Page: {}, Size: {}",
                request.getPage(), request.getSize());

        Specification<ServiceCategoryMapping> spec = createSpecification(request);
        Pageable pageable = PageRequest.of(
                request.getPageForQuery(),
                request.getSize(),
                createSort(request.getSortBy(), request.getSortDirection())
        );

        Page<ServiceCategoryMapping> mappingPage = mappingRepository.findAll(spec, pageable);
        log.debug("Found {} eligible services out of {} total",
                mappingPage.getNumberOfElements(), mappingPage.getTotalElements());

        List<EligibleServiceResponse> responses = mappingPage.getContent().stream()
                .map(this::mapToEligibleServiceResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                responses,
                request.getPage(),
                mappingPage.getSize(),
                mappingPage.getTotalElements(),
                mappingPage.getTotalPages(),
                mappingPage.isLast()
        );
    }


    private Sort createSort(String sortBy, String sortDirection) {
        String actualSortField = Optional.ofNullable(sortBy)
                .orElse("createdAt");

        Map<String, String> fieldMappings = Map.of(
                "serviceName", "contractDetail.servicelist.serviceName",
                "serviceCode", "contractDetail.servicelist.serviceCode",
                "price", "contractDetail.negotiatedPrice",
                "mappedAt", "createdAt",
                "categoryName", "packageCategory.categoryName"
        );

        String sortField = fieldMappings.getOrDefault(actualSortField, actualSortField);

        Sort.Direction direction = Optional.ofNullable(sortDirection)
                .map(dir -> "DESC".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC)
                .orElse(Sort.Direction.ASC);

        return Sort.by(direction, sortField);
    }

    @Override
    public ExternalPackageEligibleServicesResponse getEligibleServices(
            String contractUuid,
            String packageUuid,
            String insuredUuid,
            String search,
            Integer page,
            Integer limit) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(externalApiConfig.getExternalApiBaseUrl())
                .path("/api/payer/claimconnect/package/packageEligibleServices/{contractUuid}")
                .queryParam("packageUuid", packageUuid)
                .queryParam("insuredUuid", insuredUuid);

        if (search != null && !search.isEmpty()) {
            builder.queryParam("search", search);
        }
        if (page != null) {
            builder.queryParam("page", page);
        }
        if (limit != null) {
            builder.queryParam("limit", limit);
        }

        String url = builder.buildAndExpand(contractUuid).toUriString();

        log.info("[External API] Fetching eligible services - Contract: {}, Package: {}, Insured: {}",
                contractUuid, packageUuid, insuredUuid);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", externalApiConfig.getApiKey());
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<ExternalPackageEligibleServicesResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    ExternalPackageEligibleServicesResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                ExternalPackageEligibleServicesResponse responseBody = response.getBody();

                if (responseBody.getPackageEligibleServices() != null && !responseBody.getPackageEligibleServices().isEmpty()) {
                    persistEligibleServices(contractUuid, responseBody.getPackageEligibleServices());
                }

                return responseBody;
            }
            throw new RuntimeException("External API returned status: " + response.getStatusCode());

        } catch (RestClientException e) {
            throw new RuntimeException("Error calling external API", e);
        }
    }


    @Transactional
    public void persistEligibleServices(String contractHeaderUuid, List<EligibleServiceDto> eligibleServices) {
        ContractHeader contractHeader = contractHeaderRepository.findByContractHeaderUuid(contractHeaderUuid);
        if (contractHeader == null) {
            throw new ResourceNotFoundException("ContractHeader", "contractHeaderUuid", contractHeaderUuid);
        }

        eligibleServices.forEach(eligibleService -> {
            if (contractDetailRepository.existsByContractDetailUuid(eligibleService.getEligibleServiceUuid())) {
                log.debug("ContractDetail already exists for eligibleServiceUuid: {}", eligibleService.getEligibleServiceUuid());
                return;
            }

            ContractDetail contractDetail = new ContractDetail();
            contractDetail.setContractDetailUuid(eligibleService.getEligibleServiceUuid());
            contractDetail.setContractHeaderUuid(contractHeaderUuid);
            contractDetail.setNegotiatedPrice(eligibleService.getPrice());
            contractDetail.setStatus(Status.ACTIVE);
            contractDetail.setContractHeader(contractHeader);

            servicelistRepository.findByGeneratedServiceId(eligibleService.getServiceId())
                    .ifPresentOrElse(
                            service -> {
                                contractDetail.setServicelist(service);
                                contractDetail.setServiceUuid(service.getServiceUuid());
                                contractDetail.setItemType("SERVICE");
                                contractDetailRepository.save(contractDetail);
                                log.info("Persisted ContractDetail for service: {}", service.getGeneratedServiceId());
                            },
                            () -> log.warn("Service not found with generatedServiceId: {}", eligibleService.getServiceId())
                    );
        });
    }

    private Specification<ServiceCategoryMapping> createSpecification(EligibleServiceSearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            Join<ServiceCategoryMapping, ContractDetail> contractDetailJoin =
                    root.join("contractDetail", JoinType.INNER);
            Join<ServiceCategoryMapping, PackageCategory> categoryJoin =
                    root.join("packageCategory", JoinType.INNER);
            Join<ContractDetail, com.medco.HealthConnectProvider.entity.services.Servicelist> serviceJoin =
                    contractDetailJoin.join("servicelist", JoinType.LEFT);
            Join<ContractDetail, com.medco.HealthConnectProvider.entity.contracts.ContractHeader> contractJoin =
                    contractDetailJoin.join("contractHeader", JoinType.INNER);

            predicates.add(criteriaBuilder.equal(
                    contractJoin.get("contractHeaderUuid"), request.getContractUuid()));

            predicates.add(criteriaBuilder.like(
                    criteriaBuilder.lower(categoryJoin.get("categoryName")),
                    "%" + request.getCategoryName().toLowerCase() + "%"));

            if (request.getSearchKey() != null && !request.getSearchKey().isEmpty()) {
                String searchPattern = "%" + request.getSearchKey().toLowerCase() + "%";
                Predicate searchPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(serviceJoin.get("serviceName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(serviceJoin.get("serviceCode")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(serviceJoin.get("serviceDescription")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(serviceJoin.get("serviceCategory")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(serviceJoin.get("serviceSubCategory")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(categoryJoin.get("categoryName")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(categoryJoin.get("categoryCode")), searchPattern),
                        criteriaBuilder.like(criteriaBuilder.lower(contractJoin.get("contractName")), searchPattern)
                );
                predicates.add(searchPredicate);
            }

            if (request.getServiceCode() != null && !request.getServiceCode().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        serviceJoin.get("serviceCode"), request.getServiceCode()));
            }

            if (request.getServiceCategory() != null && !request.getServiceCategory().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(serviceJoin.get("serviceCategory")),
                        "%" + request.getServiceCategory().toLowerCase() + "%"));
            }

            if (request.getServiceSubCategory() != null && !request.getServiceSubCategory().isEmpty()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(serviceJoin.get("serviceSubCategory")),
                        "%" + request.getServiceSubCategory().toLowerCase() + "%"));
            }

            if (request.getServiceCodes() != null && !request.getServiceCodes().isEmpty()) {
                predicates.add(serviceJoin.get("serviceCode").in(request.getServiceCodes()));
            }

            if (request.getExcludeServiceCodes() != null && !request.getExcludeServiceCodes().isEmpty()) {
                predicates.add(criteriaBuilder.not(serviceJoin.get("serviceCode").in(request.getExcludeServiceCodes())));
            }

            if (request.getMinPrice() != null) {
                if ("CONTRACT_PRICE".equals(request.getPriceType())) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            contractDetailJoin.get("negotiatedPrice"), request.getMinPrice()));
                } else {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            serviceJoin.get("defaultPrice"), request.getMinPrice()));
                }
            }

            if (request.getMaxPrice() != null) {
                if ("CONTRACT_PRICE".equals(request.getPriceType())) {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                            contractDetailJoin.get("negotiatedPrice"), request.getMaxPrice()));
                } else {
                    predicates.add(criteriaBuilder.lessThanOrEqualTo(
                            serviceJoin.get("defaultPrice"), request.getMaxPrice()));
                }
            }

            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        serviceJoin.get("status"), request.getStatus()));
            }

            if (request.getConsumesFromLimit() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("consumesFromLimit"), request.getConsumesFromLimit()));
            }

            if (request.getDateRange() != null && !request.getDateRange().isEmpty()) {
                Instant startDate = getStartDateForRange(request.getDateRange());
                if (startDate != null) {
                    predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                            root.get("createdAt"), startDate));
                }
            }

            predicates.add(criteriaBuilder.equal(root.get("isDeleted"), false));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Instant getStartDateForRange(String dateRange) {
        LocalDate now = LocalDate.now();
        return switch (dateRange.toUpperCase()) {
            case "TODAY" -> now.atStartOfDay().toInstant(ZoneOffset.UTC);
            case "WEEK" -> now.minusWeeks(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            case "MONTH" -> now.minusMonths(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            case "YEAR" -> now.minusYears(1).atStartOfDay().toInstant(ZoneOffset.UTC);
            default -> null;
        };
    }

    private EligibleServiceResponse mapToEligibleServiceResponse(ServiceCategoryMapping mapping) {
        ContractDetail contractDetail = mapping.getContractDetail();
        PackageCategory category = mapping.getPackageCategory();
        com.medco.HealthConnectProvider.entity.services.Servicelist service = contractDetail.getServicelist();

        List<String> additionalCategories = mappingRepository
                .findByContractDetailOrderByPackageCategory_CategoryNameAsc(contractDetail)
                .stream()
                .map(m -> m.getPackageCategory().getCategoryName())
                .filter(name -> !name.equals(category.getCategoryName()))
                .collect(Collectors.toList());

        return EligibleServiceResponse.builder()
                .contractDetailUuid(contractDetail.getContractDetailUuid())
                .serviceUuid(service != null ? service.getServiceUuid() : null)
                .serviceName(service != null ? service.getServiceName() : "Unknown Service")
                .serviceCode(service != null ? service.getServiceCode() : "N/A")
                .serviceId(service != null ? service.getGeneratedServiceId() : "N/A")
                .serviceDescription(service != null ? service.getServiceDescription() : null)
                .serviceCategory(service != null ? service.getServiceCategory() : null)
                .serviceSubCategory(service != null ? service.getServiceSubCategory() : null)
                .servicePrice(service != null ? service.getDefaultPrice() : null)
                .contractPrice(contractDetail.getNegotiatedPrice())
                .priceType("NEGOTIATED_PRICE")
                .consumesFromLimit(mapping.isConsumesFromLimit())
                .mappingNotes(mapping.getNotes())
                .mappedAt(mapping.getCreatedAt())
                .mappedBy(mapping.getCreatedBy())
                .contractUuid(contractDetail.getContractHeader().getContractHeaderUuid())
                .contractName(contractDetail.getContractHeader().getContractName())
                .categoryUuid(category.getCategoryUuid())
                .categoryName(category.getCategoryName())
                .categoryCode(category.getCategoryCode())
                .providerUuid(contractDetail.getContractHeader().getProvider().getProviderUuid())
                .providerName(contractDetail.getContractHeader().getProvider().getProviderName())
                .status(service != null ? service.getStatus().toString() : "UNKNOWN")
                .isActive(!mapping.isDeleted())
                .additionalCategories(additionalCategories)
                .build();
    }
}
