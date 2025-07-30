package com.medco.HealthConnectProvider.controller.drug;

import com.medco.HealthConnectProvider.services.drug.DrugService;
import com.medco.HealthConnectProvider.services.service.ServicelistService;
import com.medco.HealthConnectProvider.ui.request.drug.DrugRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.drug.DrugResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/api/v1/healthConnect/drugs")
public class DrugController {

    private final DrugService drugService;
    private final ServicelistService servicelistService;

    @Autowired
    public DrugController(DrugService drugService, ServicelistService servicelistService) {
        this.drugService = drugService;
        this.servicelistService = servicelistService;
    }

    @PostMapping("/{providerUuid}")
    @Operation(summary = "Create a new drug", description = "Creates a new drug for the specified provider")
    public ResponseEntity<DrugResponse> createDrug(@PathVariable String providerUuid, @RequestBody DrugRequest drugRequest) {
        return drugService.createDrug(providerUuid, drugRequest);
    }

    @PutMapping("/{drugUuid}")
    @Operation(summary = "Update a drug", description = "Updates an existing drug with the specified UUID")
    public ResponseEntity<?> updateDrug(@PathVariable String drugUuid, @RequestBody DrugRequest drugRequest) {
        return drugService.updateDrug(drugUuid, drugRequest);
    }

    @DeleteMapping("/{drugUuid}")
    @Operation(summary = "Delete a drug", description = "Deletes the drug with the specified UUID")
    public ResponseEntity<?> deleteDrug(@PathVariable String drugUuid) {
        return drugService.deleteDrug(drugUuid);
    }

    @GetMapping("/{drugUuid}")
    @Operation(summary = "Get a drug", description = "Retrieves the drug with the specified UUID")
    public DrugResponse getDrug(@PathVariable String drugUuid) {
        return drugService.getDrug(drugUuid);
    }

    @GetMapping("/search/{providerUuid}")
    @Operation(summary = "Search drugs", description = "Searches for drugs for the specified provider, with optional search key and pagination")
    public PagedResponse<DrugResponse> searchDrugs(
            @PathVariable String providerUuid,
            @RequestParam(required = false) String searchKey,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit) {
        return drugService.searchDrugs(providerUuid, searchKey, page, limit);
    }

    @GetMapping("/export/{providerUuid}")
    @Operation(summary = "Export drug list", description = "Exports the list of drugs for the specified provider")
    public ResponseEntity<?> exportDrugList(HttpServletResponse response, @PathVariable String providerUuid) {
        return drugService.exportDrugList(response, providerUuid);
    }

    @PostMapping("/import/{providerUuid}")
    @Operation(summary = "Import drug list", description = "Imports a list of drugs for the specified provider from a file")
    public ResponseEntity<?> importDrugList(@RequestParam("file") MultipartFile file, @PathVariable String providerUuid) throws IOException {
        File convertedFile = File.createTempFile("temp", null);
        file.transferTo(convertedFile);
        return drugService.importDrugListData(convertedFile, providerUuid);
    }

    @GetMapping("/{providerUuid}/drugs/export")
    public ResponseEntity<?> exportDrugsToExcel(@PathVariable String providerUuid) throws IOException {
        return servicelistService.exportDrugsToExcel(providerUuid);
    }

}
