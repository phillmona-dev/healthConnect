package com.medco.HealthConnectProvider.controller.providers;

import com.medco.HealthConnectProvider.annotation.RequiresApiKey;
import com.medco.HealthConnectProvider.services.providers.ProviderService;
import com.medco.HealthConnectProvider.ui.request.auth.password.providers.ProviderRequest;
import com.medco.HealthConnectProvider.ui.response.payer.PayerResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PayersNameForProviderResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/provider")
@Tag(name = "Provider Management", description = "APIs for managing healthcare providers")
public class ProviderController {

    private static final Logger logger = LoggerFactory.getLogger(ProviderController.class);
    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

//    @PostMapping(value = "/createProvider", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    @Operation(summary = "Create provider", description = "Creates a new healthcare provider with logo")
//    public ResponseEntity<ProviderResponse> createProvider(
//            @RequestPart("provider") @Valid ProviderRequest providerRequest,
//            @RequestPart(value = "logo", required = false) MultipartFile logo) {
//        return providerService.createProvider(providerRequest, logo);
//    }

    @PostMapping(value = "/createProvider", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProviderResponse> createProvider(
            @RequestPart("provider") @Valid ProviderRequest providerRequest,
            @RequestPart(value = "logo", required = false) MultipartFile logo) {

        logger.info("Received provider: {}", providerRequest.toString());
        if (logo != null) {
            logger.info("Received logo: {} ({} bytes)", logo.getOriginalFilename(), logo.getSize());
        } else {
            logger.info("No logo received");
        }

        return providerService.createProvider(providerRequest, logo);
    }

    @GetMapping("/logo/{providerUuid}")
    @Operation(summary = "Get provider logo", description = "Retrieves the logo image for a provider")
    public ResponseEntity<ByteArrayResource> getProviderLogo(@PathVariable String providerUuid) {
        return providerService.getProviderLogo(providerUuid);
    }

    @PutMapping(path = "/{providerUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update provider", description = "Updates an existing healthcare provider by UUID")
    public ResponseEntity<?> updateProvider(@PathVariable String providerUuid,
                                            @RequestPart("provider") @Valid ProviderRequest providerRequest,
                                            @RequestPart(value = "logo", required = false) MultipartFile logo) {
        return providerService.updateProvider(providerUuid, providerRequest, logo);
    }

    @GetMapping(path = "/{providerUuid}")
    @Operation(summary = "Get provider", description = "Retrieves a specific healthcare provider by UUID")
    public ProviderResponse getProvider(@PathVariable String providerUuid) {
        return providerService.getProvider(providerUuid);
    }

    @PutMapping("/{providerUuid}/status")
    //@PreAuthorize("hasRole('Update-Provider')")
    @Operation(summary = "Update provider status",
            description = "Updates the status of a healthcare provider by UUID")
    public ResponseEntity<?> updateProviderStatus(
            @PathVariable String providerUuid,
            @RequestParam Status status) {
        return providerService.updateProviderStatus(providerUuid, status);
    }

    @GetMapping("/list")
    //@PreAuthorize("hasRole('Read-Providers')")
    @Operation(
            summary = "List providers",
            description = "Retrieves a list of healthcare providers with pagination, search, and advanced filtering options"
    )
    public PagedResponse<ProviderResponse> getProviders(
            @RequestParam(value = "search", required = false) String searchKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit,
            @RequestParam(value = "status", required = false) Status status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "providerName", required = false) String providerName,
            @RequestParam(value = "tinNumber", required = false) String tinNumber,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc", required = false) String sortDir) {

        return providerService.getProvidersWithFilters(searchKey, page, limit, status, category,
                providerName, tinNumber, level, sortBy, sortDir);
    }

    //list of providers for hcPayer
    @RequiresApiKey
    @GetMapping("/list/forHcPayer")
    @Operation(
            summary = "List providers for hcPayer",
            description = "Retrieves a list of healthcare providers with pagination, search, and advanced filtering options"
    )
    public ResponseEntity<List<ProviderResponse>> getProvidersForHcPayer(
            @RequestParam(value = "search", required = false) String searchKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit,
            @RequestParam(value = "status", required = false) Status status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "providerName", required = false) String providerName,
            @RequestParam(value = "tinNumber", required = false) String tinNumber,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc", required = false) String sortDir) {

        PagedResponse<ProviderResponse> pagedResponse = providerService.getProvidersWithFilters(
                searchKey, page, limit, status, category,
                providerName, tinNumber, level, sortBy, sortDir);

        List<ProviderResponse> providerList = pagedResponse.getContent();

        return ResponseEntity.ok(providerList);
    }


    @GetMapping("/list/withOutLogo")
    @Operation(
            summary = "List providers",
            description = "Retrieves a list of healthcare providers with out their logo with pagination, search, and advanced filtering options"
    )
    public ResponseEntity<PagedResponse<ProviderResponse>> getProvidersWithOutLogo(

            @RequestParam(value = "search", required = false) String searchKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit,
            @RequestParam(value = "status", required = false) Status status,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "providerName", required = false) String providerName,
            @RequestParam(value = "tinNumber", required = false) String tinNumber,
            @RequestParam(value = "level", required = false) String level,
            @RequestParam(value = "sortBy", defaultValue = "id", required = false) String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "desc", required = false) String sortDir) {

        PagedResponse<ProviderResponse> response = providerService.getProvidersWithFiltersWithOutLogo(
                searchKey, page, limit, status, category, providerName, tinNumber, level, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }


    @DeleteMapping(path = "/{providerUuid}")
    @Operation(summary = "Delete provider", description = "Deletes a healthcare provider by UUID")
    public ResponseEntity<?> deleteProvider(@PathVariable String providerUuid) {
        return providerService.deleteProvider(providerUuid);
    }

    @GetMapping(path = "/payers/dropdown/{providerUuid}")
    @Operation(summary = "Get payers for provider", description = "Retrieves a list of payers associated with a specific provider for dropdown selection")
    public List<PayersNameForProviderResponse> getPayersNameForProvider(
            @PathVariable String providerUuid,
            @RequestParam(name = "searchKey", required = false) String searchKey) {
        return providerService.getPayersNameForProvider(providerUuid, searchKey);
    }

    @GetMapping("/payer/available-providers")
    @Operation(summary = "Get available providers for payer", description = "Retrieves a list of providers that are not currently in contract with a specific payer")
    public List<ProviderResponse> getAvailableProvidersForPayerNotInContract(

            @RequestParam(value = "payerUuid", required = true) String payerUuid,
            @RequestParam(value = "search", required = false) String searchKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {

        return providerService.getAvailableProvidersForPayerNotInContract(payerUuid, searchKey, page, limit);

    }

    @GetMapping("/providers/{providerUuid}/payers-with-contract")
    @Operation(summary = "Get payers with contract for a provider")
    public ResponseEntity<PagedResponse<PayerResponse>> getPayersWithContract(
            @PathVariable String providerUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "payerName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(required = false) String search) {
        return providerService.getPayersWithContract(providerUuid, page, size, sortBy, sortDir, search);
    }

}