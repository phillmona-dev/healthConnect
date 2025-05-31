package com.medco.HealthConnectProvider.controller.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import com.medco.HealthConnectProvider.services.service.ServicelistService;
import com.medco.HealthConnectProvider.ui.request.auth.password.service.ServicelistRequest;
import com.medco.HealthConnectProvider.ui.response.service.ServicelistResponse;
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

//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/provider/healthConnectProvider/service")
@SecurityRequirement(name = "bearerAuth")
public class ServiceListController {

    @Autowired
    ServicelistService serviceService;

    @PostMapping("/add/{providerUuid}")
    public ResponseEntity<?> createService(
            @PathVariable String providerUuid,
            @Valid @RequestBody ServicelistRequest serviceRequest){

        return serviceService.createService(providerUuid, serviceRequest);

    }

    @PutMapping(path="/{serviceUuid}")
    public ResponseEntity<?> updateService(
            @PathVariable String serviceUuid,
            @Valid @RequestBody ServicelistRequest serviceRequest) {
        return serviceService.updateService(serviceUuid, serviceRequest);
    }

    @GetMapping(path="/{serviceUuid}")
    public ServicelistResponse getService(@PathVariable String serviceUuid) {
        return serviceService.getService(serviceUuid);
    }

    @GetMapping("/search/{providerUuid}")
    public List<ServicelistResponse> searchServices(
            @PathVariable String providerUuid,
            @RequestParam(name="search", required=false) String searchKey,
            @RequestParam(value="page", defaultValue = "1") int page,
            @RequestParam(value="limit", defaultValue = "25") int limit){
        return serviceService.searchServices(providerUuid ,searchKey, page, limit);
    }

    @GetMapping(path="/export/{providerUuid}")
    public ResponseEntity<?> downloadServiceList(HttpServletResponse response, @PathVariable String providerUuid) throws IOException {

        String fileType = "attachment; filename=service_details_" + ".xls";
        response.setHeader("Content-Disposition", fileType);
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM.getType());
        serviceService.exportServiceList(response, providerUuid);
        return ResponseEntity.ok("Service list downloaded successfully");
    }

    @DeleteMapping(path="/{serviceUuid}")
    public ResponseEntity<?> deleteService(@PathVariable String serviceUuid) {
        return serviceService.deleteService(serviceUuid);
    }

    @PostMapping(path="/import",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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

}
