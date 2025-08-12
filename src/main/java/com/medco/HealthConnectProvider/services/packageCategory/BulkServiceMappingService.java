package com.medco.HealthConnectProvider.services.packageCategory;

import com.medco.HealthConnectProvider.dto.packageCategory.BulkServiceMappingRequest;
import com.medco.HealthConnectProvider.ui.response.packageCategory.BulkServiceMappingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BulkServiceMappingService {

    /**
     * Process bulk service mappings from request
     */
    ResponseEntity<BulkServiceMappingResponse> processBulkServiceMappings(BulkServiceMappingRequest request);

    /**
     * Import service mappings from Excel file
     */
    ResponseEntity<BulkServiceMappingResponse> importServiceMappingsFromExcel(
            String contractUuid, 
            MultipartFile file) throws IOException;

    /**
     * Download template Excel file for service mappings
     */
    ResponseEntity<byte[]> downloadMappingTemplate();

    /**
     * Validate Excel file format before processing
     */
    ResponseEntity<String> validateExcelFile(MultipartFile file);
}
