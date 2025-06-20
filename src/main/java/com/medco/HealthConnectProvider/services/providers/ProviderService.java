package com.medco.HealthConnectProvider.services.providers;


import com.medco.HealthConnectProvider.ui.request.auth.password.providers.ProviderRequest;
import com.medco.HealthConnectProvider.ui.response.provider.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PayersNameForProviderResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProviderService {

    ResponseEntity<?> updateProvider(String providerUuid, @Valid ProviderRequest providerRequest, MultipartFile logo);

    ProviderResponse getProvider(String providerUuid);

    List<ProviderResponse> getProviders(String searchKey, int page, int limit);

    ResponseEntity<?> deleteProvider(String providerUuid);

    List<ProviderResponse> getAvailableProvidersForPayerNotInContract(String payerUuid, String searchKey, int page, int limit);

    List<PayersNameForProviderResponse> getPayersNameForProvider(String providerUuid, String searchKey);

    ResponseEntity<ProviderResponse> createProvider(@Valid ProviderRequest providerRequest, MultipartFile logo);

    ResponseEntity<ByteArrayResource> getProviderLogo(String providerUuid);

    PagedResponse<ProviderResponse> getProvidersWithFilters(String searchKey, int page, int limit, Status status,
                                                            String category, String providerName,
                                                            String tinNumber, String level, String sortBy, String sortDir);

    /**
     * Updates the status of a provider
     * @param providerUuid The UUID of the provider to update
     * @param status The new status to set
     * @return ResponseEntity with success or error message
     */
    ResponseEntity<?> updateProviderStatus(String providerUuid, Status status);

    PagedResponse<ProviderResponse> getProvidersWithFiltersWithOutLogo(String searchKey, int page, int limit, Status status, String category, String providerName, String tinNumber, String level, String sortBy, String sortDir);
}
