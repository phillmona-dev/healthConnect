package com.medco.HealthConnectProvider.services.persons;

import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredWithDependantsRequest;
import com.medco.HealthConnectProvider.ui.request.persons.InsuredUpdateRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.persons.*;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface InsuredService {
    ResponseEntity<?> createInsuredPerson(InsuredRequest insuredRequest, MultipartFile photo);

    @Transactional
    ResponseEntity<?> updateInsuredPerson(String insuredUuid, InsuredUpdateRequest insuredRequest, MultipartFile photo) throws IOException;

    ResponseEntity<?> deleteInsuredPerson(String insuredUuid);



    ResponseEntity<?> importInsuredPersonData(File convert, String institutionUuid, String payerInstitutionContractUuid) throws IOException;
    // List<InsuredResponse> getInsuredPersonsForProvider(int page, int limit);
    ResponseEntity<?> setProfilePicture(MultipartFile file, String insuredUuid) throws IOException;
//    List<InsuredListResponse> getInsuredPersonEligiblity(String insuredUuid);

    List<InsuredResponse> getInsuredPersons(String payerInstitutionContractId, String search, int page, int limit);
    ResponseEntity<?> importInsuredPersonAndDependant(File convert, String payerUuid) throws Exception, IOException;
    List<InsuredDependantResponse> getInsuredPersonsAndDependants(String payerInstitutionContractId, String search,
                                                                  int page, int limit);


    List<InsuredAndDependantCashServiceResponse> getInsuredAndDependantCashServiceResponse(
            String institutionUuid, String search, Pageable pageable);
    boolean checkMemberExist(String payerInstitutionContractUuid);


    ResponseEntity<ByteArrayResource> getInsuredPhoto(String insuredUuid);

    /**
     * Get an insured person's photo as base64
     * @param insuredUuid The UUID of the insured person
     * @return The photo as base64 string or null if not found
     */
    String getInsuredPhotoBase64(String insuredUuid);

    /**
     * Get an insured person by UUID with their dependants and photo in base64
     * @param insuredUuid The UUID of the insured person
     * @return ResponseEntity containing the insured person with their dependants and photo in base64
     */
    ResponseEntity<?> getInsuredPersonWithPhotoBase64(String insuredUuid);



    ResponseEntity<?> getInsuredPersonByUuid(String insuredUuid);

    ResponseEntity<PagedResponse<InsuredWithDependantsResponse>> getAllInsuredPersonsWithDependants(int page, int size, String search);


    ResponseEntity<PagedResponse<InsuredDependantResponse>> getAllInsuredPersonsWithDependentsByPayer(String payerUuid, int page, int size, String search);

    ResponseEntity<?> softDeleteInsuredPerson(String insuredUuid);


    InsuredResponse updateInsuredStatus(String insuredUuid, Status newStatus);

    List<InsuredSearchResponse> searchInsuredPersons(String identifier);

}
