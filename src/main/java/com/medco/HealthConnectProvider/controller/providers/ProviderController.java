package com.medco.HealthConnectProvider.controller.providers;

import com.medco.HealthConnectProvider.services.providers.ProviderService;
import com.medco.HealthConnectProvider.ui.request.auth.password.providers.ProviderRequest;
import com.medco.HealthConnectProvider.ui.response.provider.PayersNameForProviderResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/provider")
public class ProviderController {

    private final ProviderService providerService;

    public ProviderController(ProviderService providerService) {
        this.providerService = providerService;
    }

    @PostMapping("/createProvider")
    public ResponseEntity<ProviderResponse> createProvider(@Valid @RequestBody ProviderRequest providerRequest) {
        return providerService.createProvider(providerRequest);
    }

    @PutMapping(path = "/{providerUuid}")
    public ResponseEntity<?> updateProvider(@PathVariable String providerUuid,
                                            @Valid @RequestBody ProviderRequest providerRequest) {
        return providerService.updateProvider(providerUuid, providerRequest);
    }

    @GetMapping(path = "/{providerUuid}")
    public ProviderResponse getProvider(@PathVariable String providerUuid) {
        return providerService.getProvider(providerUuid);
    }

    @GetMapping("/list")
    //@PreAuthorize("hasRole('Read-Providers')")
    public List<ProviderResponse> getProviders(@RequestParam(value = "search", required = false) String searchKey,
                                               @RequestParam(value = "page", defaultValue = "1") int page,
                                               @RequestParam(value = "limit", defaultValue = "25") int limit) {
        return providerService.getProviders(searchKey, page, limit);
    }

    @DeleteMapping(path = "/{providerUuid}")
    public ResponseEntity<?> deleteProvider(@PathVariable String providerUuid) {
        return providerService.deleteProvider(providerUuid);
    }

    @GetMapping(path = "/payers/dropdown/{providerUuid}")
    public List<PayersNameForProviderResponse> getPayersNameForProvider(@PathVariable String providerUuid, @RequestParam(name = "searchKey", required = false) String searchKey) {
        return providerService.getPayersNameForProvider(providerUuid, searchKey);
    }

    //Filmon
    @GetMapping("/payer/available-providers")
    public List<ProviderResponse> getAvailableProvidersForPayerNotInContract(
            @RequestParam(value = "payerUuid", required = true) String payerUuid,
            @RequestParam(value = "search", required = false) String searchKey,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {
        return providerService.getAvailableProvidersForPayerNotInContract(payerUuid, searchKey, page, limit);
    }

}
