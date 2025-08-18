package com.medco.HealthConnectProvider.services.impl.drug;

import com.medco.HealthConnectProvider.entity.drug.Drug;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.repository.drug.DrugRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.services.drug.DrugService;
import com.medco.HealthConnectProvider.ui.request.drug.DrugRequest;
import com.medco.HealthConnectProvider.ui.response.ImportResponse;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.drug.DrugResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.paginationUtils.Pagination;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DrugServiceImpl implements DrugService {

    private final DrugRepository drugRepository;
    private final ProviderRepository providerRepository;

    @Autowired
    public DrugServiceImpl(DrugRepository drugRepository, ProviderRepository providerRepository) {
        this.drugRepository = drugRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    public ResponseEntity<DrugResponse> createDrug(String providerUuid, DrugRequest drugRequest) {

        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null){
            throw new BadRequestException("Can't find provider with the provided Id");
        }

        if (drugRepository.existsByDrugName(drugRequest.getDrugName())) {
            throw new BadRequestException("Drug with this name already exists");
        }

        Drug drug = new Drug();
        BeanUtils.copyProperties(drugRequest, drug);
        drug.setProvider(provider);
        drug.setStatus(Status.valueOf(drugRequest.getStatus().toUpperCase()));

        Drug savedDrug = drugRepository.save(drug);

        DrugResponse response = new DrugResponse();
        BeanUtils.copyProperties(savedDrug, response);
        response.setProviderName(provider.getProviderName());

        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateDrug(String drugUuid, DrugRequest drugRequest) {
        Drug drug = drugRepository.findByDrugUuid(drugUuid)
                .orElseThrow(() -> new BadRequestException("Drug not found"));

        if (!drug.getDrugName().equals(drugRequest.getDrugName()) &&
                drugRepository.existsByDrugName(drugRequest.getDrugName())) {
            throw new BadRequestException("Drug with this name already exists");
        }

        BeanUtils.copyProperties(drugRequest, drug);
        drug.setStatus(Status.valueOf(drugRequest.getStatus().toUpperCase()));

        Drug updatedDrug = drugRepository.save(drug);

        DrugResponse response = new DrugResponse();
        BeanUtils.copyProperties(updatedDrug, response);
        response.setProviderName(updatedDrug.getProvider().getProviderName());

        return ResponseEntity.ok(response);
    }

    @Override
    @Transactional
    public ResponseEntity<?> deleteDrug(String drugUuid) {
        Drug drug = drugRepository.findByDrugUuid(drugUuid)
                .orElseThrow(() -> new BadRequestException("Drug not found"));

        drug.setDeleted(true);
        drugRepository.save(drug);

        return ResponseEntity.ok(new MessageResponse("Drug soft deleted successfully."));
    }

    @Override
    public DrugResponse getDrug(String drugUuid) {
        Drug drug = drugRepository.findByDrugUuid(drugUuid)
                .orElseThrow(() -> new BadRequestException("Drug not found"));

        DrugResponse response = new DrugResponse();
        BeanUtils.copyProperties(drug, response);
        response.setProviderName(drug.getProvider().getProviderName());
        response.setCreatedAt(LocalDateTime.ofInstant(drug.getCreatedAt(), ZoneId.systemDefault()));
        response.setUpdatedAt(LocalDateTime.ofInstant(drug.getUpdatedAt(), ZoneId.systemDefault()));

        return response;
    }

    @Override
    public PagedResponse<DrugResponse> searchDrugs(String providerUuid, String searchKey, int page, int limit) {
        Pageable pageable = Pagination.paginateResource(page, limit, "id", "desc");
        Page<Drug> drugPage;
        try {
            drugPage = searchKey != null ?
                    drugRepository.findByProviderUuidAndSearchTermsAndIsDeletedFalse(providerUuid, searchKey, pageable) :
                    drugRepository.findAllByProviderProviderUuidAndIsDeletedFalse(providerUuid, pageable);
        } catch (Exception e) {
            log.error("Error fetching drugs from repository", e);
            throw new RuntimeException("Error fetching drugs", e);
        }

        List<DrugResponse> drugResponses = drugPage.getContent().stream()
                .map(drug -> {
                    DrugResponse response = new DrugResponse();
                    BeanUtils.copyProperties(drug, response);
                    if (drug.getProvider() != null) {
                        response.setProviderName(drug.getProvider().getProviderName());
                    }
                    if (drug.getCreatedAt() != null) {
                        response.setCreatedAt(LocalDateTime.ofInstant(drug.getCreatedAt(), ZoneId.systemDefault()));
                    }
                    if (drug.getUpdatedAt() != null) {
                        response.setUpdatedAt(LocalDateTime.ofInstant(drug.getUpdatedAt(), ZoneId.systemDefault()));
                    }
                    return response;
                })
                .collect(Collectors.toList());

        return new PagedResponse<>(
                drugResponses,
                drugPage.getNumber(),
                drugPage.getSize(),
                drugPage.getTotalElements(),
                drugPage.getTotalPages(),
                drugPage.isLast()
        );
    }

    @Override
    public ResponseEntity<?> exportDrugList(HttpServletResponse response, String providerUuid) {
        List<Drug> drugs = drugRepository.findAllByProviderProviderUuidAndStatusAndIsDeleted(
                providerUuid, Status.ACTIVE, false);

        if (drugs.isEmpty()) {
            return ResponseEntity.ok(new MessageResponse("No drugs found for the provider"));
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Drug List");

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Drug UUID", "Drug Code", "Drug Name", "Category", "Sub Category", "Price", "Dosage", "Manufacturer", "Status"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
            }

            // Create data rows
            int rowNum = 1;
            for (Drug drug : drugs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(drug.getDrugUuid());
                row.createCell(1).setCellValue(drug.getDrugCode());
                row.createCell(2).setCellValue(drug.getDrugName());
                row.createCell(3).setCellValue(drug.getCategory());
                row.createCell(4).setCellValue(drug.getSubCategory());
                row.createCell(5).setCellValue(drug.getPrice().doubleValue());
                row.createCell(6).setCellValue(drug.getDosage());
                row.createCell(7).setCellValue(drug.getManufacturer());
                row.createCell(8).setCellValue(drug.getStatus().toString());
            }

            // Auto-size columns
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            // Write to response
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=DrugList.xlsx");
            workbook.write(response.getOutputStream());

            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to export drug list: " + e.getMessage()));
        }
    }


    @Override
    @Transactional
    public ResponseEntity<?> importDrugListData(File file, String providerUuid) throws IOException {
        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null){
            throw new BadRequestException("Provider not found");
        }

        List<Drug> importedDrugs = new ArrayList<>();
        int totalRows = 0;
        int successfulImports = 0;
        List<String> errorMessages = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip header row
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                totalRows++;

                try {
                    Drug drug = createDrugFromRow(row, provider);
                    importedDrugs.add(drug);
                    successfulImports++;
                } catch (Exception e) {
                    String errorMessage = "Error in row " + totalRows + ": " + e.getMessage();
                    errorMessages.add(errorMessage);
                    log.error(errorMessage, e);
                }
            }

            drugRepository.saveAll(importedDrugs);

            String resultMessage = String.format("Processed %d rows. Successfully imported %d drugs.", totalRows, successfulImports);
            if (!errorMessages.isEmpty()) {
                resultMessage += " Errors occurred in " + errorMessages.size() + " rows.";
            }

            return ResponseEntity.ok(new ImportResponse(resultMessage, errorMessages));
        } catch (Exception e) {
            log.error("Failed to import drug list", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("Failed to import drug list: " + e.getMessage()));
        }
    }

    private Drug createDrugFromRow(Row row, Provider provider) {
        Drug drug = new Drug();
        drug.setProvider(provider);
        drug.setDrugCode(getCellValueAsString(row.getCell(0)));
        drug.setDrugName(getCellValueAsString(row.getCell(1)));
        drug.setCategory(getCellValueAsString(row.getCell(2)));
        drug.setSubCategory(getCellValueAsString(row.getCell(3)));
        drug.setPrice(parseDoubleFromString(getCellValueAsString(row.getCell(4))));
        drug.setDosage(getCellValueAsString(row.getCell(5)));
        drug.setManufacturer(getCellValueAsString(row.getCell(6)));
        drug.setStatus(Status.valueOf(getCellValueAsString(row.getCell(7)).toUpperCase()));
        drug.setGenericName(getCellValueAsString(row.getCell(8)));
        drug.setBrandName(getCellValueAsString(row.getCell(9)));
        drug.setFormulation(getCellValueAsString(row.getCell(10)));
        drug.setRoute(getCellValueAsString(row.getCell(11)));
        drug.setIndications(getCellValueAsString(row.getCell(12)));
        drug.setSideEffect(getCellValueAsString(row.getCell(13)));
        drug.setDescription(getCellValueAsString(row.getCell(14)));
        return drug;
    }

    private Double parseDoubleFromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toString();
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

}
