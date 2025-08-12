package com.medco.HealthConnectProvider.controller.packageCategory;

import com.medco.HealthConnectProvider.dto.packageCategory.BulkServiceMappingRequest;
import com.medco.HealthConnectProvider.services.packageCategory.BulkServiceMappingService;
import com.medco.HealthConnectProvider.ui.response.packageCategory.BulkServiceMappingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/healthConnect/bulk-service-mappings")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bulk Service Mapping", description = "APIs for bulk mapping services to package categories")
public class BulkServiceMappingController {

    private final BulkServiceMappingService bulkServiceMappingService;

    @PostMapping("/process")
    @Operation(summary = "Process bulk service mappings", 
               description = "Process multiple service-to-category mappings from request body")
    public ResponseEntity<BulkServiceMappingResponse> processBulkServiceMappings(
            @Valid @RequestBody BulkServiceMappingRequest request) {
        log.info("Processing bulk service mappings for contract: {}", request.getContractUuid());
        return bulkServiceMappingService.processBulkServiceMappings(request);
    }

    @PostMapping(value = "/import/{contractUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import service mappings from Excel", 
               description = "Import service-to-category mappings from Excel file")
    public ResponseEntity<BulkServiceMappingResponse> importServiceMappingsFromExcel(
            @Parameter(description = "Contract UUID") @PathVariable String contractUuid,
            @Parameter(description = "Excel file with service mappings") @RequestParam("file") MultipartFile file) 
            throws IOException {
        log.info("Importing service mappings from Excel for contract: {}", contractUuid);
        return bulkServiceMappingService.importServiceMappingsFromExcel(contractUuid, file);
    }

    @GetMapping("/template")
    @Operation(summary = "Download mapping template", 
               description = "Download Excel template for service mappings")
    public ResponseEntity<byte[]> downloadMappingTemplate() {
        log.info("Downloading service mapping template");
        return bulkServiceMappingService.downloadMappingTemplate();
    }

    @PostMapping(value = "/validate-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Validate Excel file", 
               description = "Validate Excel file format before processing")
    public ResponseEntity<String> validateExcelFile(
            @Parameter(description = "Excel file to validate") @RequestParam("file") MultipartFile file) {
        log.info("Validating Excel file: {}", file.getOriginalFilename());
        return bulkServiceMappingService.validateExcelFile(file);
    }

}
