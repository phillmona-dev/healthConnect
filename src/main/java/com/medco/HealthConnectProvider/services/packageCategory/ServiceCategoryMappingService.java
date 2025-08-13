package com.medco.HealthConnectProvider.services.packageCategory;

import com.medco.HealthConnectProvider.ui.request.packageCategory.BulkServiceCategoryAssignmentRequest;
import com.medco.HealthConnectProvider.ui.request.packageCategory.EligibleServiceSearchRequest;
import com.medco.HealthConnectProvider.ui.request.packageCategory.ServiceCategoryMappingRequest;
import com.medco.HealthConnectProvider.ui.response.packageCategory.BulkServiceCategoryAssignmentResponse;
import com.medco.HealthConnectProvider.ui.response.packageCategory.EligibleServiceResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
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

    /**
     * Assign multiple contract details (eligible services) to a category
     */
    ResponseEntity<BulkServiceCategoryAssignmentResponse> assignServicesToCategory(BulkServiceCategoryAssignmentRequest request);

    /**
     * Get eligible services for a category with advanced search and filtering
     */
    PagedResponse<EligibleServiceResponse> getEligibleServicesForCategory(EligibleServiceSearchRequest request);
}
