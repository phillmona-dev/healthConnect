package com.medco.HealthConnectProvider.services.impl.packageCategory;

import com.medco.HealthConnectProvider.dto.packageCategory.BulkServiceMappingRequest;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategory;
import com.medco.HealthConnectProvider.entity.packageCategory.ServiceCategoryMapping;
import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.PackageCategoryRepository;
import com.medco.HealthConnectProvider.repository.packageCategory.ServiceCategoryMappingRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.services.packageCategory.BulkServiceMappingService;
import com.medco.HealthConnectProvider.ui.response.packageCategory.BulkServiceMappingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BulkServiceMappingServiceImpl implements BulkServiceMappingService {

    private final ServiceCategoryMappingRepository mappingRepository;
    private final PackageCategoryRepository categoryRepository;
    private final ContractRepository contractRepository;
    private final ServicelistRepository serviceRepository;

    @Override
    public ResponseEntity<BulkServiceMappingResponse> processBulkServiceMappings(BulkServiceMappingRequest request) {
        log.info("Processing bulk service mappings for contract: {}", request.getContractUuid());

        // Validate contract exists
        ContractHeader contract = contractRepository.findByContractHeaderUuid(request.getContractUuid());
        if (contract == null) {
            throw new ResourceNotFoundException("Contract", "contractUuid", request.getContractUuid());
        }

        BulkServiceMappingResponse.BulkServiceMappingResponseBuilder responseBuilder = 
                BulkServiceMappingResponse.builder()
                        .contractUuid(contract.getContractHeaderUuid())
                        .contractName(contract.getContractName())
                        .totalServicesProcessed(request.getServiceMappings().size());

        List<BulkServiceMappingResponse.MappingResult> results = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int successCount = 0;
        int failedCount = 0;

        for (BulkServiceMappingRequest.ServiceMappingItem item : request.getServiceMappings()) {
            try {
                BulkServiceMappingResponse.MappingResult result = processServiceMapping(contract, item);
                results.add(result);
                
                if ("SUCCESS".equals(result.getStatus())) {
                    successCount++;
                } else {
                    failedCount++;
                }
            } catch (Exception e) {
                log.error("Error processing service mapping for service: {}", item.getServiceCode(), e);
                results.add(BulkServiceMappingResponse.MappingResult.builder()
                        .serviceCode(item.getServiceCode())
                        .serviceName(item.getServiceName())
                        .status("FAILED")
                        .message("Error: " + e.getMessage())
                        .build());
                failedCount++;
            }
        }

        return ResponseEntity.ok(responseBuilder
                .successfulMappings(successCount)
                .failedMappings(failedCount)
                .results(results)
                .errors(errors)
                .build());
    }

    @Override
    public ResponseEntity<BulkServiceMappingResponse> importServiceMappingsFromExcel(
            String contractUuid, MultipartFile file) throws IOException {
        
        log.info("Importing service mappings from Excel for contract: {}", contractUuid);

        if (file.isEmpty()) {
            throw new BadRequestException("Excel file is empty");
        }

        // Validate file format
        if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
            throw new BadRequestException("File must be an Excel file (.xlsx or .xls)");
        }

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            List<BulkServiceMappingRequest.ServiceMappingItem> mappingItems = parseExcelSheet(sheet);
            
            BulkServiceMappingRequest request = BulkServiceMappingRequest.builder()
                    .contractUuid(contractUuid)
                    .serviceMappings(mappingItems)
                    .build();

            return processBulkServiceMappings(request);
        }
    }

    @Override
    public ResponseEntity<byte[]> downloadMappingTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Service Category Mappings");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "Service Code", "Service Name", "Category Codes (comma-separated)", 
                "Consumes From Limit (true/false)", "Notes"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                
                // Style header
                CellStyle headerStyle = workbook.createCellStyle();
                Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }
            
            // Add sample data
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("SURG001");
            sampleRow.createCell(1).setCellValue("Heart Surgery");
            sampleRow.createCell(2).setCellValue("INP,SURG");
            sampleRow.createCell(3).setCellValue("true");
            sampleRow.createCell(4).setCellValue("Major surgical procedure");
            
            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            HttpHeaders headers2 = new HttpHeaders();
            headers2.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers2.setContentDispositionFormData("attachment", "service_mapping_template.xlsx");
            
            return ResponseEntity.ok()
                    .headers(headers2)
                    .body(outputStream.toByteArray());
                    
        } catch (IOException e) {
            log.error("Error creating template file", e);
            throw new RuntimeException("Failed to create template file", e);
        }
    }

    @Override
    public ResponseEntity<String> validateExcelFile(MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is empty");
        }
        
        if (!file.getOriginalFilename().endsWith(".xlsx") && !file.getOriginalFilename().endsWith(".xls")) {
            return ResponseEntity.badRequest().body("File must be an Excel file (.xlsx or .xls)");
        }
        
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            if (sheet.getPhysicalNumberOfRows() < 2) {
                return ResponseEntity.badRequest().body("File must contain at least one data row");
            }
            
            // Validate headers
            Row headerRow = sheet.getRow(0);
            if (headerRow == null || headerRow.getPhysicalNumberOfCells() < 4) {
                return ResponseEntity.badRequest().body("Invalid file format. Please use the provided template.");
            }
            
            return ResponseEntity.ok("File format is valid");
            
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Error reading file: " + e.getMessage());
        }
    }

    private List<BulkServiceMappingRequest.ServiceMappingItem> parseExcelSheet(Sheet sheet) {
        List<BulkServiceMappingRequest.ServiceMappingItem> items = new ArrayList<>();
        
        // Skip header row (row 0)
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            
            try {
                String serviceCode = getCellValueAsString(row.getCell(0));
                String serviceName = getCellValueAsString(row.getCell(1));
                String categoryCodesStr = getCellValueAsString(row.getCell(2));
                String consumesFromLimitStr = getCellValueAsString(row.getCell(3));
                String notes = getCellValueAsString(row.getCell(4));
                
                if (serviceCode.isEmpty() || serviceName.isEmpty() || categoryCodesStr.isEmpty()) {
                    continue; // Skip empty rows
                }
                
                List<String> categoryCodes = Arrays.stream(categoryCodesStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                
                boolean consumesFromLimit = Boolean.parseBoolean(consumesFromLimitStr);
                
                items.add(BulkServiceMappingRequest.ServiceMappingItem.builder()
                        .serviceCode(serviceCode)
                        .serviceName(serviceName)
                        .categoryCodes(categoryCodes)
                        .consumesFromLimit(consumesFromLimit)
                        .notes(notes)
                        .build());
                        
            } catch (Exception e) {
                log.warn("Error parsing row {}: {}", i, e.getMessage());
            }
        }
        
        return items;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private BulkServiceMappingResponse.MappingResult processServiceMapping(
            ContractHeader contract, BulkServiceMappingRequest.ServiceMappingItem item) {

        // Find service by code - need to search through all services since there's no direct findByServiceCode method
        List<Servicelist> allServices = serviceRepository.findAll();
        Servicelist service = allServices.stream()
                .filter(s -> item.getServiceCode().equals(s.getServiceCode()))
                .findFirst()
                .orElse(null);

        if (service == null) {
            return BulkServiceMappingResponse.MappingResult.builder()
                    .serviceCode(item.getServiceCode())
                    .serviceName(item.getServiceName())
                    .status("FAILED")
                    .message("Service not found with code: " + item.getServiceCode())
                    .build();
        }

        // Find contract detail for this service
        ContractDetail contractDetail = contract.getContractDetails().stream()
                .filter(cd -> service.getServiceUuid().equals(cd.getServiceUuid()))
                .findFirst()
                .orElse(null);

        if (contractDetail == null) {
            return BulkServiceMappingResponse.MappingResult.builder()
                    .serviceCode(item.getServiceCode())
                    .serviceName(item.getServiceName())
                    .status("FAILED")
                    .message("Service not found in contract")
                    .build();
        }

        List<String> mappedCategories = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Process each category mapping
        for (String categoryCode : item.getCategoryCodes()) {
            try {
                // Find category by code - need to search through all categories since there's no direct findByCategoryCode method
                List<PackageCategory> allCategories = categoryRepository.findAll();
                PackageCategory category = allCategories.stream()
                        .filter(c -> categoryCode.equals(c.getCategoryCode()))
                        .findFirst()
                        .orElse(null);

                if (category == null) {
                    errors.add("Category not found: " + categoryCode);
                    continue;
                }

                // Check if mapping already exists
                if (mappingRepository.existsByContractDetailAndPackageCategory(contractDetail, category)) {
                    mappedCategories.add(categoryCode + " (already exists)");
                    continue;
                }

                // Create new mapping
                ServiceCategoryMapping mapping = ServiceCategoryMapping.builder()
                        .contractDetail(contractDetail)
                        .packageCategory(category)
                        .consumesFromLimit(item.getConsumesFromLimit())
                        .notes(item.getNotes())
                        .build();

                mappingRepository.save(mapping);
                mappedCategories.add(categoryCode);

            } catch (Exception e) {
                errors.add("Error mapping to " + categoryCode + ": " + e.getMessage());
            }
        }

        String status = errors.isEmpty() ? "SUCCESS" : (mappedCategories.isEmpty() ? "FAILED" : "PARTIAL");
        String message = errors.isEmpty() ?
                "Successfully mapped to " + mappedCategories.size() + " categories" :
                "Errors: " + String.join("; ", errors);

        return BulkServiceMappingResponse.MappingResult.builder()
                .serviceCode(item.getServiceCode())
                .serviceName(item.getServiceName())
                .status(status)
                .message(message)
                .mappedCategories(mappedCategories)
                .build();
    }
}
