package com.medco.HealthConnectProvider.controller.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.medco.HealthConnectProvider.services.service.ServicelistService;
import com.medco.HealthConnectProvider.ui.request.auth.password.service.ServicelistRequest;
import com.medco.HealthConnectProvider.ui.request.service.ExportRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.service.ServicelistResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/healthConnect/healthConnectProvider/service")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Service Management", description = "APIs for managing healthcare services offered by providers")
public class ServiceListController {

    private final ServicelistService serviceService;

    @Autowired
    public ServiceListController(ServicelistService serviceService) {
        this.serviceService = serviceService;
    }

    @PostMapping("/add/{providerUuid}")
    @Operation(summary = "Create service", description = "Creates a new healthcare service for a specific provider")
    public ResponseEntity<ServicelistResponse> createService(
            @PathVariable String providerUuid,
            @Valid @RequestBody ServicelistRequest serviceRequest) {
        return serviceService.createService(providerUuid, serviceRequest);
    }

    @PutMapping(path="/{serviceUuid}")
    @Operation(summary = "Update service", description = "Updates an existing healthcare service by UUID")
    public ResponseEntity<?> updateService(
            @PathVariable String serviceUuid,
            @Valid @RequestBody ServicelistRequest serviceRequest) {
        return serviceService.updateService(serviceUuid, serviceRequest);
    }

    @GetMapping(path="/{serviceUuid}")
    @Operation(summary = "Get service", description = "Retrieves a specific healthcare service by UUID")
    public ServicelistResponse getService(@PathVariable String serviceUuid) {
        return serviceService.getService(serviceUuid);
    }

    @GetMapping("/search/{providerUuid}")
    @Operation(summary = "Search services", description = "Searches for healthcare services for a specific provider with pagination")
    public PagedResponse<ServicelistResponse> searchServices(
            @PathVariable String providerUuid,
            @RequestParam(name="search", required=false) String searchKey,
            @RequestParam(value="page", defaultValue = "1") int page,
            @RequestParam(value="limit", defaultValue = "25") int limit){
        return serviceService.searchServices(providerUuid, searchKey, page, limit);
    }


    @GetMapping(path="/export/{providerUuid}")
    @Operation(summary = "Export service list", description = "Exports the list of services for a specific provider as an Excel file")
    public ResponseEntity<?> downloadServiceList(HttpServletResponse response, @PathVariable String providerUuid) throws IOException {
        String fileType = "attachment; filename=service_details_" + ".xls";
        response.setHeader("Content-Disposition", fileType);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM.getType());
        serviceService.exportServiceList(response, providerUuid);
        return ResponseEntity.ok("Service list downloaded successfully");
    }

    @DeleteMapping(path="/{serviceUuid}")
    @Operation(summary = "Delete service", description = "Deletes a healthcare service by UUID")
    public ResponseEntity<?> deleteService(@PathVariable String serviceUuid) {
        return serviceService.deleteService(serviceUuid);
    }

    @PostMapping(path="/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import services", description = "Imports healthcare services from an Excel file for a specific provider")
    public ResponseEntity<?> importData(
            @RequestParam("file") MultipartFile file,
            @RequestParam("providerUuid") String providerUuid) throws IOException {
        return serviceService.importServiceListData(convert(file), providerUuid);
    }


    private File convert(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    @PostMapping("/{providerUuid}/services/export")
    public ResponseEntity<?> exportServicesToExcel(
            @PathVariable String providerUuid,
            @RequestBody(required = false) ExportRequest exportRequest) throws IOException {
        List<String> categories = (exportRequest != null) ? exportRequest.getCategories() : null;
        return serviceService.exportServicesToExcel(providerUuid, categories);
    }

    @GetMapping("/{providerUuid}/service-categories")
    public ResponseEntity<List<String>> getServiceCategories(@PathVariable String providerUuid) {
        List<String> categories = serviceService.getServiceCategories(providerUuid);
        return ResponseEntity.ok(categories);
    }

}