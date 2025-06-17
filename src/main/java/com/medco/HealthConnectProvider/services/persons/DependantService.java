package com.medco.HealthConnectProvider.services.persons;

import com.medco.HealthConnectProvider.ui.request.auth.password.persons.DependantRequest;
import com.medco.HealthConnectProvider.ui.request.persons.DependantUpdateRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.persons.DependantResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DependantService {
   


    DependantResponse getDependant(String dependantUuid);

    List<DependantResponse> getPersonDependants(String insuredPersonUuid, Status status);

    ResponseEntity<?> deleteDependant(String dependantUuid);

    ResponseEntity<?> createDependant(@Valid DependantRequest dependantRequest, MultipartFile photo);

    DependantResponse getDependantByUuid(String dependantUuid);

    PagedResponse<DependantResponse> getDependantsByInsuredPersonUuid(String insuredPersonUuid, int page, int size);

    DependantResponse updateDependant(String dependantUuid, DependantUpdateRequest updateRequest, MultipartFile profilePicture);

    void softDeleteDependant(String dependantUuid);

    DependantResponse changeStatus(String dependantUuid, Status newStatus);
}
