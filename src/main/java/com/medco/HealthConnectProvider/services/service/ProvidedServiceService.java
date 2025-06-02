package com.medco.HealthConnectProvider.services.service;

import com.medco.HealthConnectProvider.ui.request.service.ProvidedServiceRequest;
import com.medco.HealthConnectProvider.ui.response.service.ProvidedServiceResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ProvidedServiceService {

    ResponseEntity<?> addProvidedService(String claimUuid, @Valid ProvidedServiceRequest providedServiceRequest);

    ResponseEntity<?> updateProvidedService(String providedServiceUuid, @Valid ProvidedServiceRequest providedServiceRequest);

    ProvidedServiceResponse getProvidedService(String providedServiceUuid);

    List<ProvidedServiceResponse> getProvidedServicesByClaim(String claimUuid);

    ResponseEntity<?> deleteProvidedService(String providedServiceUuid);

    Double calculateTotalPriceForClaim(String claimUuid);

    Long countProvidedServicesForClaim(String claimUuid);

    ResponseEntity<?> addMultipleProvidedServices(String claimUuid, List<ProvidedServiceRequest> providedServiceRequests);
}