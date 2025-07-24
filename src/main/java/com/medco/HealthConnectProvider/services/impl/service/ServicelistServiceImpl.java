package com.medco.HealthConnectProvider.services.impl.service;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.medco.HealthConnectProvider.entity.drug.Drug;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.exception.BadRequestException;
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
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

    @Autowired(required = false)
    private RabbitTemplate template;

    @Autowired
    public ServicelistServiceImpl(
            ServicelistRepository servicelistRepository,
            ProviderRepository providerRepository, DrugRepository drugRepository) {
        this.servicelistRepository = servicelistRepository;
        this.providerRepository = providerRepository;
        this.drugRepository = drugRepository;
    }

    @Override
    public ResponseEntity<ServicelistResponse> createService(String providerUuid, ServicelistRequest serviceRequest) {
        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null){
            throw new BadRequestException("can't find provider with the provided Id");
        }

        var service = new Servicelist();
        BeanUtils.copyProperties(serviceRequest, service);

        service.setServiceCode(serviceRequest.getServiceCode());
        service.setServiceName(serviceRequest.getServiceName());
        service.setServiceCategory(serviceRequest.getServiceCategory());
        service.setServiceSubCategory(serviceRequest.getSubCategory());
        service.setPrice(serviceRequest.getPrice());
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

    @Override
    public ResponseEntity<InputStreamResource> exportServicesToExcel(String providerUuid) throws IOException {

        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null) {
            throw new BadRequestException("Can't find provider with the provided UUID");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Services");

        createHeaderRow(workbook, sheet);

        CellStyle dataStyle = createDataStyle(workbook);

        int pageNumber = 0;
        Page<Servicelist> servicePage;
        int rowNum = 1;

        do {
            Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
            servicePage = servicelistRepository.findByProvider(Optional.of(provider), pageable);

            for (Servicelist service : servicePage.getContent()) {
                Row row = sheet.createRow(rowNum++);
                populateServiceRow(service, row, dataStyle);
            }

            pageNumber++;
        } while (servicePage.hasNext());

        autoSizeColumns(sheet);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        String fileName = "services_" + provider.getProviderName() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(inputStream));
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
                "Service UUID","Service Code", "Service Name", "Category", "Sub Category",
                "Description", "Price", "Status", "Unit of Measure"
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

    private CellStyle createDataStyle(Workbook workbook) {
        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setWrapText(true);
        return dataStyle;
    }

    private void populateServiceRow(Servicelist service, Row row, CellStyle dataStyle) {
        int colNum = 0;

        row.createCell(colNum++).setCellValue(service.getServiceUuid() != null ? service.getServiceUuid() : "");
        row.createCell(colNum++).setCellValue(service.getServiceCode() != null ? service.getServiceCode() : "");
        row.createCell(colNum++).setCellValue(service.getServiceName() != null ? service.getServiceName() : "");
        row.createCell(colNum++).setCellValue(service.getServiceCategory() != null ? service.getServiceCategory() : "");
        row.createCell(colNum++).setCellValue(service.getServiceSubCategory() != null ? service.getServiceSubCategory() : "");
        row.createCell(colNum++).setCellValue(service.getServiceDescription() != null ? service.getServiceDescription() : "");

        if (service.getPrice() != null) {
            row.createCell(colNum++).setCellValue(service.getPrice());
        } else if (service.getDefaultPrice() != null) {
            row.createCell(colNum++).setCellValue(service.getDefaultPrice().doubleValue());
        } else {
            row.createCell(colNum++).setCellValue(0.0);
        }

        row.createCell(colNum++).setCellValue(service.getStatus() != null ? service.getStatus().toString() : "ACTIVE");
        row.createCell(colNum++).setCellValue(service.getUnitOfMeasure() != null ? service.getUnitOfMeasure() : "");

        for (int i = 0; i < colNum; i++) {
            row.getCell(i).setCellStyle(dataStyle);
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
                    servicelistResponse.setPrice(BigDecimal.valueOf(servicelist.getPrice()));
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
                providerUuid, "Active", false);

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

        Workbook workbook = WorkbookFactory.create(file);
        Sheet sheet = workbook.getSheetAt(0);

        Provider provider = providerRepository.findByProviderUuid(providerUuid);
        if (provider == null){
            throw new BadRequestException("Can't find Provider With the Provided Id");
        }

        int i = 0;
        List<Servicelist> servicelistList= new ArrayList<>();
        for (Row row : sheet) {

            if (row.getCell(0) == null || row.getCell(0).getCellType() == CellType.BLANK) {
                continue;
            }

            Servicelist servicelist = new Servicelist();
            if (i == 0) {

                if (!row.getCell(0).getStringCellValue().equalsIgnoreCase("Item Code")
                        || !row.getCell(1).getStringCellValue().equalsIgnoreCase("Item")
                        || !row.getCell(2).getStringCellValue().equalsIgnoreCase("Category")
                        || !row.getCell(3).getStringCellValue().equalsIgnoreCase("Sub Category")
                        ||  !row.getCell(4).getStringCellValue().equalsIgnoreCase("Price")) {
                    return ResponseEntity.ok(new MessageResponse(
                            "The Excel sheet for uploading Service list should be used the format given."));
                }
            } else {

                servicelist.setProvider(provider);
                servicelist.setServiceCode(row.getCell(0).getStringCellValue());
                servicelist.setServiceName(row.getCell(1).getStringCellValue());
                servicelist.setServiceCategory(row.getCell(2).getStringCellValue());
                servicelist.setServiceSubCategory(row.getCell(3).getStringCellValue());
                servicelist.setPrice(row.getCell(3).getNumericCellValue());
                servicelist.setStatus(Status.valueOf("Active"));
                servicelistList.add(servicelist);

            }
            i++;
        }

        servicelistRepository.saveAll(servicelistList);
        workbook.close();
        file.delete();

        return ResponseEntity.ok(new MessageResponse("Service list imported successfully!"));
    }
}
