package com.medco.HealthConnectProvider.services.persons;

import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredWithDependantsRequest;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredAndDependantCashServiceResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredDependantResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredListResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredResponse;
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
    ResponseEntity<?> deleteInsuredPerson(String insuredUuid);



    ResponseEntity<?> importInsuredPersonData(File convert, String institutionUuid, String payerInstitutionContractUuid) throws IOException;
    // List<InsuredResponse> getInsuredPersonsForProvider(int page, int limit);
    ResponseEntity<?> setProfilePicture(MultipartFile file, String insuredUuid) throws IOException;
//    List<InsuredListResponse> getInsuredPersonEligiblity(String insuredUuid);

    List<InsuredResponse> getInsuredPersons(String payerInstitutionContractId, String search, int page, int limit);
    ResponseEntity<?> importInsuredPersonAndDependant(File convert, String institutionUuid) throws Exception, IOException;
    List<InsuredDependantResponse> getInsuredPersonsAndDependants(String payerInstitutionContractId, String search,
                                                                  int page, int limit);


    List<InsuredAndDependantCashServiceResponse> getInsuredAndDependantCashServiceResponse(
            String institutionUuid, String search, Pageable pageable);
    boolean checkMemberExist(String payerInstitutionContractUuid);

    //Filmon
    /**
     * Get all insured persons with their dependants for a specific institution with search capability
     * @param institutionUuid The UUID of the institution
     * @param searchKey Optional search key to filter results (name, phone, insurance ID)
     * @param page Page number (1-based)
     * @param limit Number of records per page
     * @return List of insured persons with their dependants
     */
    List<InsuredDependantResponse> getAllInsuredPersonsWithDependantsByInstitution(
            String institutionUuid, String searchKey, int page, int limit);

    /**
     * Get an insured person by UUID with their dependants
     * @param insuredUuid The UUID of the insured person
     * @return ResponseEntity containing the insured person with their dependants
     */
    ResponseEntity<?> getInsuredPerson(String insuredUuid);

    /**
     * Update an insured person and their dependants
     * @param insuredUuid The UUID of the insured person
     * @param insuredRequest The request containing insured person and dependant data
     * @return ResponseEntity with success message
     */
    ResponseEntity<?> updateInsuredPersonWithDependants(String insuredUuid, InsuredWithDependantsRequest insuredRequest, MultipartFile photo);

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
}
