package com.medco.HealthConnectProvider.services.persons;

import com.medco.HealthConnectProvider.ui.request.auth.password.persons.DependantRequest;
import com.medco.HealthConnectProvider.ui.response.persons.DependantResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface DependantService {
    ResponseEntity<?> createDependant(@Valid DependantRequest dependantRequest);

    ResponseEntity<?> updateDependant(String dependantUuid, @Valid DependantRequest dependantRequest);

    DependantResponse getDependant(String dependantUuid);

    List<DependantResponse> getPersonDependants(String insuredPersonUuid, Status status);

    ResponseEntity<?> deleteDependant(String dependantUuid);
}
