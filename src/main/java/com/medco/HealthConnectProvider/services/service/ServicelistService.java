package com.medco.HealthConnectProvider.services.service;


import com.medco.HealthConnectProvider.ui.request.auth.password.service.ServicelistRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.service.ServicelistResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface ServicelistService {

    ResponseEntity<?> updateService(String serviceUuid, @Valid ServicelistRequest serviceRequest);

    ServicelistResponse getService(String serviceUuid);

    PagedResponse<ServicelistResponse> searchServices(String providerUuid, String searchKey, int page, int limit);

    ResponseEntity<?> exportServiceList(HttpServletResponse response, String providerUuid);

    ResponseEntity<?> deleteService(String serviceUuid);

    ResponseEntity<?> importServiceListData(File convert, String providerUuid) throws IOException;

    ResponseEntity<ServicelistResponse> createService(String providerUuid, @Valid ServicelistRequest serviceRequest);
    
    ResponseEntity<?> exportDrugsToExcel(String providerUuid) throws IOException;

    ResponseEntity<?> exportServicesToExcel(String providerUuid, List<String> categories) throws IOException;

    List<String> getServiceCategories(String providerUuid);

}
