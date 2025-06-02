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

import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import com.medco.HealthConnectProvider.repository.service.ServicelistRepository;
import com.medco.HealthConnectProvider.services.service.ServicelistService;
import com.medco.HealthConnectProvider.ui.request.auth.password.service.ServicelistRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.service.ServicelistResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.paginationUtils.Pagination;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import com.medco.HealthConnectProvider.entity.services.Servicelist;


@Service
public class ServicelistServiceImpl implements ServicelistService {

    @Value("${rabbitmq.services.exchange:service-exchange}")
    private String serviceExchange;

    @Value("${rabbitmq.services.routingkey:services_routingkey}")
    private String serviceKey;

    private final ServicelistRepository servicelistRepository;
    private final ProviderRepository providerRepository;

    // Make RabbitTemplate optional
    @Autowired(required = false)
    private RabbitTemplate template;

    @Autowired
    public ServicelistServiceImpl(
            ServicelistRepository servicelistRepository,
            ProviderRepository providerRepository) {
        this.servicelistRepository = servicelistRepository;
        this.providerRepository = providerRepository;
    }

    @Override
    public ResponseEntity<ServicelistResponse> createService(String providerUuid, ServicelistRequest serviceRequest) {
        Provider provider = providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new BadRequestException("can't find provider with the provided Id"));

        var service = new Servicelist();
        BeanUtils.copyProperties(serviceRequest, service);

        // Explicitly map fields from request to entity
        service.setServiceCode(serviceRequest.getServiceCode());
        service.setServiceName(serviceRequest.getServiceName());
        service.setServiceCategory(serviceRequest.getCategory());
        service.setServiceSubCategory(serviceRequest.getSubCategory());
        service.setPrice(BigDecimal.valueOf(serviceRequest.getPrice()));
        service.setServiceDescription(serviceRequest.getServiceDescription());

        // Set status enum from string
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

        // Explicitly set all fields in response
        response.setServiceUuid(serviceEntity.getServiceUuid());
        response.setServiceCode(serviceEntity.getServiceCode());
        response.setServiceName(serviceEntity.getServiceName());
        response.setServiceCategory(serviceEntity.getServiceCategory());
        response.setServiceSubCategory(serviceEntity.getServiceSubCategory());
        response.setProviderName(provider.getProviderName());

        // Convert Instant to LocalDateTime for audit fields
        if (serviceEntity.getCreatedAt() != null) {
            response.setCreatedAt(LocalDateTime.ofInstant(serviceEntity.getCreatedAt(), ZoneId.systemDefault()));
        }

        if (serviceEntity.getUpdatedAt() != null) {
            response.setUpdatedAt(LocalDateTime.ofInstant(serviceEntity.getUpdatedAt(), ZoneId.systemDefault()));
        }

        // Handle price - use either price or defaultPrice
        if (serviceEntity.getPrice() != null) {
            response.setPrice(serviceEntity.getPrice());
        } else if (serviceEntity.getDefaultPrice() != null) {
            response.setPrice(BigDecimal.valueOf(serviceEntity.getDefaultPrice().doubleValue()));
        } else {
            response.setPrice(BigDecimal.valueOf(0.0));
        }

        // Set status as string
        if (serviceEntity.getStatus() != null) {
            response.setStatus(serviceEntity.getStatus().toString());
        } else {
            response.setStatus("ACTIVE"); // Default status
        }

        // Only send message if RabbitTemplate is available
        if (template != null) {
            try {
                template.convertAndSend(serviceExchange, serviceKey, "Testing rabbit mq configuration");
            } catch (Exception e) {
                // Log the error but don't fail the operation
                System.err.println("Failed to send message to RabbitMQ: " + e.getMessage());
            }
        }

        return ResponseEntity.ok(response);
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
    public List<ServicelistResponse> searchServices(String providerUuid, String searchKey, int page, int limit) {

        Pageable pageable = Pagination.paginateResource(page,limit,"id","desc");

        Page<Servicelist> serviceLists = searchKey != null ? getServicesBySearch(searchKey,providerUuid,pageable) : getAllServices(providerUuid,pageable);

        int totalPages = serviceLists.getTotalPages();
        return  serviceLists.getContent().stream()
                .map(servicelist -> {
                    var servicelistResponse = new ServicelistResponse();
                    BeanUtils.copyProperties(servicelist,servicelistResponse);
                    servicelistResponse.setTotalPages(totalPages);

                    return servicelistResponse;
                }).collect(Collectors.toList());
    }

    private Page<Servicelist> getAllServices(String providerUuid, Pageable pageable) {
        return servicelistRepository.findAllByProviderProviderUuidAndIsDeleted(providerUuid, false, pageable);
    }

    private Page<Servicelist> getServicesBySearch(String searchKey, String providerUuid, Pageable pageable) {
        // Use the custom query method instead of the long method name
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
                // Handle both price fields
                if (service.getPrice() != null) {
                    serPriceCell.setCellValue((RichTextString) service.getPrice());
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

        Optional<Provider> provider = Optional.ofNullable(providerRepository.findByProviderUuid(providerUuid)
                .orElseThrow(() -> new BadRequestException("Can't find Provider With the Provided Id")));

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

                servicelist.setProvider(provider.get());
                servicelist.setServiceCode(row.getCell(0).getStringCellValue());
                servicelist.setServiceName(row.getCell(1).getStringCellValue());
                servicelist.setServiceCategory(row.getCell(2).getStringCellValue());
                servicelist.setServiceSubCategory(row.getCell(3).getStringCellValue());
                servicelist.setPrice(BigDecimal.valueOf(row.getCell(3).getNumericCellValue()));
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
