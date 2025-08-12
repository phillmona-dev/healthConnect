package com.medco.HealthConnectProvider.services.impl.packageCategory;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategory;
import com.medco.HealthConnectProvider.entity.packageCategory.ServiceCategoryMapping;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractDetailRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.PackageCategoryRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.ServiceCategoryMappingRepository;
import com.medco.HealthConnectProvider.services.packageCategory.ServiceCategoryMappingService;
import com.medco.HealthConnectProvider.ui.request.packageCategory.ServiceCategoryMappingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ServiceCategoryMappingServiceImpl implements ServiceCategoryMappingService {

    private final ServiceCategoryMappingRepository mappingRepository;
    private final ContractDetailRepository contractDetailRepository;
    private final PackageCategoryRepository categoryRepository;

    @Override
    public ResponseEntity<String> mapServiceToCategories(ServiceCategoryMappingRequest request) {
        log.info("Mapping service {} to {} categories", request.getContractDetailUuid(), request.getCategoryUuids().size());

        ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(request.getContractDetailUuid());
        if (contractDetail == null){
            throw new ResourceNotFoundException("Contract detail not found with UUID: " + request.getContractDetailUuid());
        }

        List<ServiceCategoryMapping> mappings = new ArrayList<>();

        for (String categoryUuid : request.getCategoryUuids()) {
            PackageCategory category = categoryRepository.findByCategoryUuid(categoryUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("Package category not found with UUID: " + categoryUuid));

            // Check if mapping already exists
            if (mappingRepository.existsByContractDetailAndPackageCategory(contractDetail, category)) {
                log.warn("Mapping already exists for service {} and category {}", 
                        request.getContractDetailUuid(), categoryUuid);
                continue;
            }

            // Validate that category belongs to the same payer as the contract
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
        if (contractDetail == null){
            throw new ResourceNotFoundException("Contract detail not found with UUID: " + contractDetailUuid);
        }

        PackageCategory category = categoryRepository.findByCategoryUuid(categoryUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Package category not found with UUID: " + categoryUuid));

        ServiceCategoryMapping mapping = mappingRepository.findByContractDetailAndPackageCategory(contractDetail, category)
                .orElseThrow(() -> new ResourceNotFoundException("Service-category mapping not found"));

        // Soft delete the mapping
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
}
