package com.medco.HealthConnectProvider.services.providers;


import com.medco.HealthConnectProvider.ui.request.auth.password.providers.ProviderRequest;
import com.medco.HealthConnectProvider.ui.response.provider.PayersNameForProviderResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProviderService {

    ResponseEntity<ProviderResponse> createProvider(@Valid ProviderRequest providerRequest);

    ResponseEntity<?> updateProvider(String providerUuid, @Valid ProviderRequest providerRequest);

    ProviderResponse getProvider(String providerUuid);

    List<ProviderResponse> getProviders(String searchKey, int page, int limit);

    ResponseEntity<?> deleteProvider(String providerUuid);

    List<ProviderResponse> getAvailableProvidersForPayer(String payerUuid, String searchKey, int page, int limit);

    List<PayersNameForProviderResponse> getPayersNameForProvider(String providerUuid, String searchKey);

}
