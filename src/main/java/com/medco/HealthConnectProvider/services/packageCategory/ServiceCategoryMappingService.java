package com.medco.HealthConnectProvider.services.packageCategory;

import com.medco.HealthConnectProvider.ui.request.packageCategory.ServiceCategoryMappingRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ServiceCategoryMappingService {

    /**
     * Map services to categories for a contract detail
     */
    ResponseEntity<String> mapServiceToCategories(ServiceCategoryMappingRequest request);

    /**
     * Remove service from category mapping
     */
    ResponseEntity<String> removeServiceFromCategory(String contractDetailUuid, String categoryUuid);

    /**
     * Get categories mapped to a service
     */
    List<String> getCategoriesByService(String contractDetailUuid);

    /**
     * Get services mapped to a category
     */
    List<String> getServicesByCategory(String categoryUuid);

    /**
     * Update service category mapping
     */
    ResponseEntity<String> updateServiceCategoryMapping(String mappingUuid, ServiceCategoryMappingRequest request);
}
