package com.medco.HealthConnectProvider.controller.providers;

import com.medco.HealthConnectProvider.services.providers.ProviderService;
import com.medco.HealthConnectProvider.ui.request.auth.password.providers.ProviderRequest;
import com.medco.HealthConnectProvider.ui.response.provider.PayersNameForProviderResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/provider")
@Tag(name = "Provider Management", description = "APIs for managing healthcare providers")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping("/createProvider")
    @Operation(summary = "Create provider", description = "Creates a new healthcare provider")
    public ResponseEntity<ProviderResponse> createProvider(@Valid @RequestBody ProviderRequest providerRequest) {
        return providerService.createProvider(providerRequest);
    }

    @PutMapping(path = "/{providerUuid}")
    @Operation(summary = "Update provider", description = "Updates an existing healthcare provider by UUID")
    public ResponseEntity<?> updateProvider(@PathVariable String providerUuid,
                                            @Valid @RequestBody ProviderRequest providerRequest) {
        return providerService.updateProvider(providerUuid, providerRequest);
    }

    @GetMapping(path = "/{providerUuid}")
    @Operation(summary = "Get provider", description = "Retrieves a specific healthcare provider by UUID")
    public ProviderResponse getProvider(@PathVariable String providerUuid) {
        return providerService.getProvider(providerUuid);
    }

    @GetMapping("/list")
    //@PreAuthorize("hasRole('Read-Providers')")
    @Operation(summary = "List providers", description = "Retrieves a list of healthcare providers with pagination and search")
    public List<ProviderResponse> getProviders(@RequestParam(value = "search", required = false) String searchKey,
                                               @RequestParam(value = "page", defaultValue = "1") int page,
                                               @RequestParam(value = "limit", defaultValue = "25") int limit) {
        return providerService.getProviders(searchKey, page, limit);
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
}