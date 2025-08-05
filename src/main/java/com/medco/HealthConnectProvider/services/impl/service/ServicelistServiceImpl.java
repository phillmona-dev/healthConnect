package com.medco.HealthConnectProvider.services.impl.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.medco.HealthConnectProvider.encryption.EncryptionUtil;
import com.medco.HealthConnectProvider.entity.drug.Drug;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.drug.DrugRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.services.service.ServicelistService;
import com.medco.HealthConnectProvider.ui.request.auth.password.service.ServicelistRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.service.ServicelistResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.paginationUtils.Pagination;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.medco.HealthConnectProvider.entity.services.Servicelist;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;


@Service
@Slf4j
public class ServicelistServiceImpl implements ServicelistService {

    private static final int BATCH_SIZE = 1000;

    @Value("${rabbitmq.services.exchange:service-exchange}")
    private String serviceExchange;

    @Value("${rabbitmq.services.routingkey:services_routingkey}")
    private String serviceKey;

    private final ServicelistRepository servicelistRepository;
    private final ProviderRepository providerRepository;
    private final DrugRepository drugRepository;
    private final EncryptionUtil encryptionUtil;

    @Autowired(required = false)
    private RabbitTemplate template;

    @Autowired
    public ServicelistServiceImpl(
            ServicelistRepository servicelistRepository,
            ProviderRepository providerRepository, DrugRepository drugRepository, EncryptionUtil encryptionUtil) {
        this.servicelistRepository = servicelistRepository;
        this.providerRepository = providerRepository;
        this.drugRepository = drugRepository;
        this.encryptionUtil = encryptionUtil;
    }

    @Override
    public ResponseEntity<ServicelistResponse> createService(String providerUuid, ServicelistRequest serviceRequest) {
        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null){
            throw new BadRequestException("can't find provider with the provided Id");
        }

        String serviceNamePrefix = serviceRequest.getServiceName() != null &&
                serviceRequest.getServiceName().length() >= 3 ?
                serviceRequest.getServiceName().substring(0, 3).toUpperCase() :
                "SRV";

        Long lastSequenceNumber = servicelistRepository.findMaxGeneratedIdSequenceNumber(provider.getId());
        long currentSequence = lastSequenceNumber != null ? lastSequenceNumber + 1 : 1;
        String generatedId = String.format("%s%010d", serviceNamePrefix, currentSequence);

        var service = new Servicelist();
        BeanUtils.copyProperties(serviceRequest, service);

        service.setGeneratedServiceId(generatedId);

        service.setServiceCode(serviceRequest.getServiceCode());
        service.setServiceName(serviceRequest.getServiceName());
        service.setServiceCategory(serviceRequest.getServiceCategory());
        service.setServiceSubCategory(serviceRequest.getSubCategory());
        service.setServiceDescription(serviceRequest.getServiceDescription());

        if (serviceRequest.getStatus() != null && !serviceRequest.getStatus().isEmpty()) {
            try {
                service.setStatus(Status.valueOf(serviceRequest.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                service.setStatus(Status.ACTIVE);
            }
        } else {
            service.setStatus(Status.ACTIVE);
        }

        service.setProvider(provider);

        var serviceEntity = servicelistRepository.save(service);

        var response = new ServicelistResponse();

        response.setServiceUuid(serviceEntity.getServiceUuid());
        response.setGeneratedServiceId(serviceEntity.getGeneratedServiceId());
        response.setServiceCode(serviceEntity.getServiceCode());
        response.setServiceName(serviceEntity.getServiceName());
        response.setServiceCategory(serviceEntity.getServiceCategory());
        response.setServiceSubCategory(serviceEntity.getServiceSubCategory());
        response.setProviderName(provider.getProviderName());

        if (serviceEntity.getCreatedAt() != null) {
            response.setCreatedAt(LocalDateTime.ofInstant(serviceEntity.getCreatedAt(), ZoneId.systemDefault()));
        }

        if (serviceEntity.getUpdatedAt() != null) {
            response.setUpdatedAt(LocalDateTime.ofInstant(serviceEntity.getUpdatedAt(), ZoneId.systemDefault()));
        }

        if (serviceEntity.getPrice() != null) {
            response.setPrice(BigDecimal.valueOf(serviceEntity.getPrice()));
        } else if (serviceEntity.getDefaultPrice() != null) {
            response.setPrice(BigDecimal.valueOf(serviceEntity.getDefaultPrice().doubleValue()));
        } else {
            response.setPrice(BigDecimal.valueOf(0.0));
        }

        if (serviceEntity.getStatus() != null) {
            response.setStatus(serviceEntity.getStatus().toString());
        } else {
            response.setStatus("ACTIVE");
        }

        if (template != null) {
            try {
                template.convertAndSend(serviceExchange, serviceKey, "Testing rabbit mq configuration");
            } catch (Exception e) {
                System.err.println("Failed to send message to RabbitMQ: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
    }

    @Transactional
    @Override
    public ResponseEntity<InputStreamResource> exportServicesToExcel(String providerUuid, List<String> categories) throws IOException {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        logger.info("Starting export of services to Excel for provider UUID: {}", providerUuid);

        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null) {
            logger.error("Provider not found for UUID: {}", providerUuid);
            throw new BadRequestException("Can't find provider with the provided UUID");
        }
        logger.info("Provider found: {}", provider.getProviderName());

        List<Servicelist> services;
        if (categories == null || categories.isEmpty()) {
            logger.info("Fetching all services for provider");
            services = servicelistRepository.findAllByProviderWithEagerFetch(provider);
        } else {
            logger.info("Fetching services for provider with categories: {}", categories);
            services = servicelistRepository.findByProviderAndServiceCategoryInWithEagerFetch(provider, categories);
        }
        logger.info("Retrieved {} services", services.size());

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Services");

        createHeaderRow(workbook, sheet);
        logger.info("Created header row in Excel sheet");

        CellStyle dataStyle = createDataStyle(workbook);
        CellStyle categoryStyle = createCategoryStyle(workbook);

        int rowNum = 1;
        String currentCategory = null;

        for (Servicelist service : services) {
            if (!Objects.equals(service.getServiceCategory(), currentCategory)) {
                currentCategory = service.getServiceCategory();
                Row categoryRow = sheet.createRow(rowNum++);
                Cell categoryCell = categoryRow.createCell(0);
                categoryCell.setCellValue(currentCategory != null ? currentCategory : "");
                categoryCell.setCellStyle(categoryStyle);
                sheet.addMergedRegion(new CellRangeAddress(rowNum - 1, rowNum - 1, 0, 7));
                logger.debug("Added category row: {}", currentCategory);
            }
            Row row = sheet.createRow(rowNum++);
            populateServiceRow(service, row, dataStyle);
            logger.debug("Added service row: {}", service.getServiceName());
        }

        autoSizeColumns(sheet);
        logger.info("Finished populating Excel sheet with {} rows", rowNum - 1);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        logger.info("Wrote workbook to ByteArrayOutputStream");

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        String fileName = "services_" + provider.getProviderName() +
                (categories != null && !categories.isEmpty() ? "_" + String.join("_", categories) : "") + ".xlsx";
        logger.info("Created file name: {}", fileName);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(inputStream));
    }

    @Override
    public List<String> getServiceCategories(String providerUuid) {

        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null) {
            throw new ResourceNotFoundException("Provider", "uuid", providerUuid);
        }

        return servicelistRepository.findDistinctCategoriesByProvider(provider);

    }

    private CellStyle createCategoryStyle(Workbook workbook) {
        CellStyle categoryStyle = workbook.createCellStyle();
        categoryStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        categoryStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        categoryStyle.setAlignment(HorizontalAlignment.CENTER);
        Font categoryFont = workbook.createFont();
        categoryFont.setBold(true);
        categoryStyle.setFont(categoryFont);
        return categoryStyle;
    }

    @Override
    public ResponseEntity<InputStreamResource> exportDrugsToExcel(String providerUuid) throws IOException {
        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null) {
            throw new BadRequestException("Can't find provider with the provided UUID");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Drugs");

        createDrugHeaderRow(workbook, sheet);

        CellStyle dataStyle = createDataStyle(workbook);

        List<Drug> allDrugs = drugRepository.findByProvider(provider);

        final int BATCH_SIZE = 1000;

        int totalDrugs = allDrugs.size();

        int rowNum = 1;

        for (int batchStart = 0; batchStart < totalDrugs; batchStart += BATCH_SIZE) {
            int batchEnd = Math.min(batchStart + BATCH_SIZE, totalDrugs);
            List<Drug> batch = allDrugs.subList(batchStart, batchEnd);

            for (Drug drug : batch) {
                Row row = sheet.createRow(rowNum++);
                populateDrugRow(drug, row, dataStyle);
            }

            if (batchEnd % (BATCH_SIZE * 10) == 0 || batchEnd == totalDrugs) {
                log.info("Processed {} of {} drugs", batchEnd, totalDrugs);
            }
        }

        autoSizeDrugColumns(sheet);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        String fileName = "drugs_" + provider.getProviderName() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(inputStream));
    }

    private void createDrugHeaderRow(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Drug UUID","Drug Code", "Drug Name", "Generic Name", "Brand Name",
                "Category", "Sub Category", "Manufacturer", "Formulation",
                "Dosage", "Route", "Price", "Status",
                "Indications", "Side Effects", "Description"
        };

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void populateDrugRow(Drug drug, Row row, CellStyle dataStyle) {

        int colNum = 0;

        row.createCell(colNum++).setCellValue(drug.getDrugUuid() != null ? drug.getDrugUuid() : "");
        row.createCell(colNum++).setCellValue(drug.getDrugCode() != null ? drug.getDrugCode() : "");
        row.createCell(colNum++).setCellValue(drug.getDrugName() != null ? drug.getDrugName() : "");
        row.createCell(colNum++).setCellValue(drug.getGenericName() != null ? drug.getGenericName() : "");
        row.createCell(colNum++).setCellValue(drug.getBrandName() != null ? drug.getBrandName() : "");
        row.createCell(colNum++).setCellValue(drug.getCategory() != null ? drug.getCategory() : "");
        row.createCell(colNum++).setCellValue(drug.getSubCategory() != null ? drug.getSubCategory() : "");
        row.createCell(colNum++).setCellValue(drug.getManufacturer() != null ? drug.getManufacturer() : "");
        row.createCell(colNum++).setCellValue(drug.getFormulation() != null ? drug.getFormulation() : "");
        row.createCell(colNum++).setCellValue(drug.getDosage() != null ? drug.getDosage() : "");
        row.createCell(colNum++).setCellValue(drug.getRoute() != null ? drug.getRoute() : "");

        if (drug.getPrice() != null) {

            row.createCell(colNum++).setCellValue(drug.getPrice().doubleValue());

        } else {
            row.createCell(colNum++).setCellValue(0.0);
        }

        row.createCell(colNum++).setCellValue(drug.getStatus() != null ? drug.getStatus().toString() : "ACTIVE");
        row.createCell(colNum++).setCellValue(drug.getIndications() != null ? drug.getIndications() : "");
        row.createCell(colNum++).setCellValue(drug.getSideEffect() != null ? drug.getSideEffect() : "");
        row.createCell(colNum++).setCellValue(drug.getDescription() != null ? drug.getDescription() : "");

        for (int i = 0; i < colNum; i++) {
            row.getCell(i).setCellStyle(dataStyle);
        }
    }

    private void autoSizeDrugColumns(Sheet sheet) {
        for (int i = 0; i < 16; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createHeaderRow(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        String[] headers = {
                "Service Code", "Service Name", "Category", "Sub Category",
                "Description", "Negotiated Price", "Status", "Unit of Measure",
                "Service ID"
        };

        CellStyle headerStyle = createHeaderStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 256 * 20);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {

        CellStyle headerStyle = workbook.createCellStyle();

        headerStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.DARK_GREEN.getIndex());
        headerStyle.setFont(headerFont);

        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        return headerStyle;

    }

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setWrapText(true);
        return dataStyle;
    }

    private void populateServiceRow(Servicelist service, Row row, CellStyle dataStyle) {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        logger.debug("Populating row for service: {}", service.getServiceName());

        int colNum = 0;

        setCellValue(row, colNum++, service.getServiceCode(), "Service Code", dataStyle);
        setCellValue(row, colNum++, service.getServiceName(), "Service Name", dataStyle);
        setCellValue(row, colNum++, service.getServiceCategory(), "Category", dataStyle);
        setCellValue(row, colNum++, service.getServiceSubCategory(), "Sub Category", dataStyle);
        setCellValue(row, colNum++, service.getServiceDescription(), "Description", dataStyle);
        setCellValue(row, colNum++, service.getNegotiatedPrice(), "Negotiated Price", dataStyle);
        setCellValue(row, colNum++, service.getStatus() != null ? service.getStatus().toString() : "ACTIVE", "Status", dataStyle);
        setCellValue(row, colNum++, service.getUnitOfMeasure(), "Unit of Measure", dataStyle);

        setCellValue(row, colNum, service.getGeneratedServiceId(), "Service ID", dataStyle);

        logger.debug("Finished populating row for service: {}", service.getServiceName());
    }

    private void setCellValue(Row row, int colNum, Object value, String fieldName, CellStyle style) {
        Logger logger = LoggerFactory.getLogger(this.getClass());
        try {
            Cell cell = row.createCell(colNum);
            if (value != null) {
                if (value instanceof String) {
                    cell.setCellValue((String) value);
                } else if (value instanceof Integer) {
                    cell.setCellValue((Integer) value);
                } else if (value instanceof Double) {
                    cell.setCellValue((Double) value);
                } else {
                    cell.setCellValue(value.toString());
                }
                logger.trace("Set {} to: {}", fieldName, value);
            } else {
                cell.setCellValue("");
                logger.warn("{} is null for this service", fieldName);
            }
            cell.setCellStyle(style);
        } catch (Exception e) {
            logger.error("Error setting value for {} at column {}: {}",
                    fieldName, colNum, e.getMessage());
            throw e;
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    @Override
    public ResponseEntity<?> updateService(String serviceUuid, ServicelistRequest serviceRequest) {
       Servicelist  service = servicelistRepository.findByServiceUuid(serviceUuid)
                .orElseThrow(() -> new BadRequestException("Service not found"));

        if(servicelistRepository.existsByServiceName(serviceRequest.getServiceName())){
            throw new BadRequestException("Duplicate Service entry is not followed");
        }

        BeanUtils.copyProperties(serviceRequest, service);
        servicelistRepository.save(service);
        return ResponseEntity.ok(new MessageResponse("Service Updated Successfully!"));
    }

    @Override
    public ResponseEntity<?> deleteService(String serviceUuid) {
        Servicelist service = servicelistRepository.findByServiceUuid(serviceUuid)
                .orElseThrow(() -> new BadRequestException("Service Not found"));

        service.setDeleted(true);
        servicelistRepository.save(service);
        return ResponseEntity.ok(new MessageResponse("Service soft deleted successfully."));

    }

    @Override
    public ServicelistResponse getService(String serviceUuid) {
        Servicelist service = servicelistRepository.findByServiceUuidAndIsDeleted(serviceUuid, false);
        ServicelistResponse serviceResponse = new ServicelistResponse();
        BeanUtils.copyProperties(service, serviceResponse);
        return serviceResponse;
    }

    @Override
    public PagedResponse<ServicelistResponse> searchServices(String providerUuid, String searchKey, int page, int limit) {
        Pageable pageable = Pagination.paginateResource(page, limit, "id", "desc");
        Page<Servicelist> serviceLists = searchKey != null ? getServicesBySearch(searchKey, providerUuid, pageable) : getAllServices(providerUuid, pageable);

        List<ServicelistResponse> servicelistResponses = serviceLists.getContent().stream()
                .map(servicelist -> {
                    var servicelistResponse = new ServicelistResponse();
                    BeanUtils.copyProperties(servicelist, servicelistResponse);
//                    servicelistResponse.setPrice(BigDecimal.valueOf(servicelist.getPrice()));
                    servicelistResponse.setStatus(String.valueOf(servicelist.getStatus()));
                    servicelistResponse.setProviderName(servicelist.getProvider().getProviderName());

                    if (servicelistResponse.getCreatedAt() != null) {
                        servicelist.setCreatedAt(servicelistResponse.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
                    }
                    if (servicelistResponse.getUpdatedAt() != null) {
                        servicelist.setUpdatedAt(servicelistResponse.getUpdatedAt().atZone(ZoneId.systemDefault()).toInstant());
                    }

                    return servicelistResponse;
                }).collect(Collectors.toList());

        return new PagedResponse<>(
                servicelistResponses,
                serviceLists.getNumber(),
                serviceLists.getSize(),
                serviceLists.getTotalElements(),
                serviceLists.getTotalPages(),
                serviceLists.isLast()
        );
    }

    private Page<Servicelist> getAllServices(String providerUuid, Pageable pageable) {
        return servicelistRepository.findAllByProviderProviderUuidAndIsDeleted(providerUuid, false, pageable);
    }

    private Page<Servicelist> getServicesBySearch(String searchKey, String providerUuid, Pageable pageable) {
        return servicelistRepository.findByProviderUuidAndSearchTerms(providerUuid, searchKey, pageable);
    }

    @Override
    public ResponseEntity<?> exportServiceList(HttpServletResponse response, String providerUuid) {
        List<Servicelist> services = servicelistRepository.findAllByProviderProviderUuidAndStatusAndIsDeleted(
                providerUuid, Status.ACTIVE, false);

        if (services.isEmpty())
            return ResponseEntity.ok(new MessageResponse("No Service Lists found for the provider"));

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Service List Excel file");

            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setBorderTop(BorderStyle.MEDIUM);
            cellStyle.setBorderRight(BorderStyle.MEDIUM);
            cellStyle.setBorderBottom(BorderStyle.MEDIUM);
            cellStyle.setBorderLeft(BorderStyle.MEDIUM);
            cellStyle.setAlignment(HorizontalAlignment.LEFT);

            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("ServiceUuid");
            cell.setCellStyle(cellStyle);
            sheet.autoSizeColumn(0);

            Cell cell1 = row.createCell(1);
            cell1.setCellValue("Item Code");
            cell1.setCellStyle(cellStyle);

            Cell cell2 = row.createCell(2);
            cell2.setCellValue("Item");
            cell2.setCellStyle(cellStyle);

            Cell cell3 = row.createCell(3);
            cell3.setCellValue("Category");
            cell3.setCellStyle(cellStyle);

            Cell cell4 = row.createCell(4);
            cell4.setCellValue("Sub_Category");
            cell4.setCellStyle(cellStyle);

            Cell cell5 = row.createCell(5);
            cell5.setCellValue("Price");
            cell5.setCellStyle(cellStyle);

            int rowNum = 1;
            for (Servicelist service : services) {
                Row serDataRow = sheet.createRow(rowNum++);

                Cell serproviderUuidCell = serDataRow.createCell(0);
                serproviderUuidCell.setCellStyle(cellStyle);
                serproviderUuidCell.setCellValue(service.getServiceUuid());

                Cell serItemCodeCell = serDataRow.createCell(1);
                serItemCodeCell.setCellStyle(cellStyle);
                serItemCodeCell.setCellValue(service.getServiceCode());

                Cell serItemCel1 = serDataRow.createCell(2);
                serItemCel1.setCellStyle(cellStyle);
                serItemCel1.setCellValue(service.getServiceName());

                Cell serCategoryCell = serDataRow.createCell(3);
                serCategoryCell.setCellStyle(cellStyle);
                serCategoryCell.setCellValue(service.getServiceCategory());

                Cell serSubCategoryCell = serDataRow.createCell(4);
                serSubCategoryCell.setCellStyle(cellStyle);
                serSubCategoryCell.setCellValue(service.getServiceSubCategory());

                Cell serPriceCell = serDataRow.createCell(5);
                serPriceCell.setCellStyle(cellStyle);
                if (service.getPrice() != null) {
                    serPriceCell.setCellValue((service.getPrice()));
                } else if (service.getDefaultPrice() != null) {
                    serPriceCell.setCellValue(service.getDefaultPrice().doubleValue());
                } else {
                    serPriceCell.setCellValue(0.0);
                }
            }

            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ResponseEntity.ok(new MessageResponse("Successfully Exported!"));

    }

    @Override
    @Transactional
    public ResponseEntity<?> importServiceListData(File file, String providerUuid) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);

            Provider provider = providerRepository.findByProviderUuid(providerUuid);
            if (provider == null) {
                throw new ResourceNotFoundException("Provider", "UUID", providerUuid);
            }

            // Get the highest existing generated ID number for this provider
            Long lastSequenceNumber = servicelistRepository.findMaxGeneratedIdSequenceNumber(provider.getId());
            long currentSequence = lastSequenceNumber != null ? lastSequenceNumber + 1 : 1;

            List<Servicelist> servicelistList = new ArrayList<>();
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                throw new BadRequestException("The Excel sheet is empty.");
            }

            Row headerRow = rowIterator.next();
            Map<String, Integer> headerMap = validateAndMapHeaders(headerRow);

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (isRowEmpty(row)) {
                    continue;
                }

                Servicelist servicelist = createServicelistFromRow(row, provider, headerMap);

                String serviceNamePrefix = servicelist.getServiceName() != null && servicelist.getServiceName().length() >= 3
                        ? servicelist.getServiceName().substring(0, 3).toUpperCase()
                        : "SRV";

                String generatedId = String.format("%s%010d", serviceNamePrefix, currentSequence++);
                servicelist.setGeneratedServiceId(generatedId);

                servicelistList.add(servicelist);
            }

            servicelistRepository.saveAll(servicelistList);
        } finally {
            if (file.exists()) {
                file.delete();
            }
        }

        return ResponseEntity.ok(new MessageResponse("Service list imported successfully!"));
    }

    private Map<String, Integer> validateAndMapHeaders(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        String[] expectedHeaders = {
                "Service Code", "Service Name", "Service Description", "Service Category",
                "Service Sub Category", "Default Price", "Negotiated Price", "Status",
                "Price", "Unit of Measure"
        };

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = cell.getStringCellValue().trim();
                if (Arrays.asList(expectedHeaders).contains(headerValue)) {
                    headerMap.put(headerValue, i);
                }
            }
        }

        if (headerMap.isEmpty()) {
            throw new BadRequestException("No valid headers found in the Excel sheet.");
        }

        return headerMap;
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int cellNum = row.getFirstCellNum(); cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    private Servicelist createServicelistFromRow(Row row, Provider provider, Map<String, Integer> headerMap) {

        Servicelist servicelist = new Servicelist();
        servicelist.setProvider(provider);

        setFieldIfPresent(headerMap, row, "Service Code", servicelist::setServiceCode);
        setFieldIfPresent(headerMap, row, "Service Name", servicelist::setServiceName);
        setFieldIfPresent(headerMap, row, "Service Description", servicelist::setServiceDescription);
        setFieldIfPresent(headerMap, row, "Service Category", servicelist::setServiceCategory);
        setFieldIfPresent(headerMap, row, "Service Sub Category", servicelist::setServiceSubCategory);
        setFieldIfPresent(headerMap, row, "Default Price", value -> servicelist.setDefaultPrice(new BigDecimal(value)));
        setFieldIfPresent(headerMap, row, "Negotiated Price", value -> servicelist.setNegotiatedPrice(Integer.parseInt(value)));
        setFieldIfPresent(headerMap, row, "Status", value -> servicelist.setStatus(Status.valueOf(value.toUpperCase())));
        setFieldIfPresent(headerMap, row, "Price", value -> servicelist.setPrice(Double.parseDouble(value)));
        setFieldIfPresent(headerMap, row, "Unit of Measure", servicelist::setUnitOfMeasure);

        if (servicelist.getStatus() == null) {
            servicelist.setStatus(Status.ACTIVE);
        }
        if (servicelist.getServiceUuid() == null) {
            servicelist.setServiceUuid(UUID.randomUUID().toString());
        }

        return servicelist;

    }

    private void setFieldIfPresent(Map<String, Integer> headerMap, Row row, String headerName, Consumer<String> setter) {
        Integer columnIndex = headerMap.get(headerName);
        if (columnIndex != null) {
            String value = getCellValueAsString(row.getCell(columnIndex));
            if (!value.isEmpty()) {
                setter.accept(value);
            }
        }
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield DateTimeFormatter.ofPattern("yyyy-MM-dd").format(cell.getLocalDateTimeCellValue());
                }
                yield String.valueOf(cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield String.valueOf(cell.getNumericCellValue());
                } catch (IllegalStateException e) {
                    yield cell.getRichStringCellValue().getString();
                }
            }
            default -> "";
        };
    }
}
