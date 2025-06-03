package com.medco.HealthConnectProvider.controller.persons;

import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.services.persons.InsuredService;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.DependantRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredWithDependantsRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredAndDependantCashServiceResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredDependantResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredListResponse;
import com.medco.HealthConnectProvider.ui.response.persons.InsuredResponse;
import com.medco.HealthConnectProvider.utils.paginationUtils.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@RestController
@RequestMapping("/api/v1/healthConnect/insuredperson")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Insured Person Management", description = "APIs for managing insured persons and their dependants")
public class InsuredController {

    private final InsuredService insuredService;

    public InsuredController(InsuredService insuredService) {
        this.insuredService = insuredService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//@PreAuthorize("hasRole('Create-Insured-Person')")
    @Operation(summary = "Create insured person", description = "Creates a new insured person with profile photo")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Insured person created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input"),
            @ApiResponse(responseCode = "422", description = "Email or phone already in use"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createInsuredPerson(
            @RequestPart("insured") @Valid InsuredRequest insuredRequest,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return insuredService.createInsuredPerson(insuredRequest, photo);
    }

    @GetMapping("/photo/{insuredUuid}")
    @Operation(summary = "Get insured person photo", description = "Retrieves the profile photo of an insured person")
    public ResponseEntity<ByteArrayResource> getInsuredPhoto(@PathVariable String insuredUuid) {
        return insuredService.getInsuredPhoto(insuredUuid);
    }

    @PutMapping(path = "/{insuredUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update insured person", description = "Updates an existing insured person and their dependants")
    public ResponseEntity<?> updateInsuredPerson(
            @PathVariable String insuredUuid,
            @RequestPart("insured") InsuredWithDependantsRequest insuredRequest,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        // If dependants list is null, initialize it to avoid NPE
        if (insuredRequest.getDependants() == null) {
            insuredRequest.setDependants(new ArrayList<>());
        }

        // Validate the insured person fields manually
        validateInsuredRequest(insuredRequest);

        // Validate each dependant
        for (DependantRequest dependant : insuredRequest.getDependants()) {
            validateDependantRequest(dependant);
        }

        return insuredService.updateInsuredPersonWithDependants(insuredUuid, insuredRequest, photo);
    }

    /**
     * Validates the insured request fields
     * @param request The insured request to validate
     * @throws BadRequestException if validation fails
     */
    private void validateInsuredRequest(InsuredWithDependantsRequest request) {
        if (request.getFirstName() == null || request.getFirstName().trim().isEmpty()) {
            throw new BadRequestException("First name is required");
        }
        if (request.getFatherName() == null || request.getFatherName().trim().isEmpty()) {
            throw new BadRequestException("Father name is required");
        }
        if (request.getGrandFatherName() == null || request.getGrandFatherName().trim().isEmpty()) {
            throw new BadRequestException("Grandfather name is required");
        }
        if (request.getGender() == null || request.getGender().trim().isEmpty()) {
            throw new BadRequestException("Gender is required");
        }
        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            throw new BadRequestException("Phone is required");
        }
        if (request.getBirthDate() == null) {
            throw new BadRequestException("Birth date is required");
        }
    }

    /**
     * Validates a dependant request if it has a UUID (for updates)
     * @param dependant The dependant request to validate
     * @throws BadRequestException if validation fails
     */
    private void validateDependantRequest(DependantRequest dependant) {
        // Only validate if this is an update to an existing dependant (has UUID)
        if (dependant.getDependantUuid() != null && !dependant.getDependantUuid().trim().isEmpty()) {
            // For existing dependants, validate required fields
            if (dependant.getDependantFirstName() == null || dependant.getDependantFirstName().trim().isEmpty()) {
                throw new BadRequestException("Dependant first name is required");
            }
            if (dependant.getDependantFatherName() == null || dependant.getDependantFatherName().trim().isEmpty()) {
                throw new BadRequestException("Dependant father name is required");
            }
            if (dependant.getDependantGrandFatherName() == null || dependant.getDependantGrandFatherName().trim().isEmpty()) {
                throw new BadRequestException("Dependant grandfather name is required");
            }
            if (dependant.getDependantGender() == null || dependant.getDependantGender().trim().isEmpty()) {
                throw new BadRequestException("Dependant gender is required");
            }
            if (dependant.getDependantBirthDate() == null) {
                throw new BadRequestException("Dependant birth date is required");
            }
            if (dependant.getRelationship() == null) {
                throw new BadRequestException("Dependant relationship is required");
            }
        }
    }

    /**
     * Get an insured person by UUID with their dependants
     * @param insuredUuid The UUID of the insured person
     * @return ResponseEntity containing the insured person with their dependants
     */
    @GetMapping(path = "/{insuredUuid}")
    //@PreAuthorize("hasRole('Read-Insured-Person')")
    @Operation(summary = "Get insured person", description = "Retrieves a specific insured person by UUID with their dependants")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful operation"),
            @ApiResponse(responseCode = "400", description = "Insured person not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> getInsuredPerson(@PathVariable String insuredUuid) {
        return insuredService.getInsuredPerson(insuredUuid);
    }

    @GetMapping(path = "/list/{payerInstitutionContractId}")
//	@PreAuthorize("hasRole('Read-Insured-Persons')")
    @Operation(summary = "List insured persons", description = "Retrieves a list of insured persons for a specific payer institution contract with pagination and search")
    public List<InsuredResponse> getInsuredPersons(@PathVariable String payerInstitutionContractId,
                                                   @RequestParam(name = "search", required = false) String search,
                                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                                   @RequestParam(value = "limit", defaultValue = "25") int limit) {
        return insuredService.getInsuredPersons(payerInstitutionContractId, search, page, limit);
    }

    @GetMapping(path = "/list/withdependant/{payerInstitutionContractUuid}")
    @Operation(summary = "List insured persons with dependants", description = "Retrieves a list of insured persons with their dependants for a specific payer institution contract")
    public List<InsuredDependantResponse> getInsuredPersonsAndDependants(
            @PathVariable String payerInstitutionContractUuid,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {
        return insuredService.getInsuredPersonsAndDependants(payerInstitutionContractUuid, search, page, limit);
    }

    @GetMapping(path = "/check-eligiblity/{insuredPersonUuid}")
    // @PreAuthorize("hasRole('Check-Insured-Person-Eligibility')")
    @Operation(summary = "Check insured person eligibility", description = "Checks the eligibility status of an insured person")
    public List<InsuredListResponse> getInsuredPersonEligiblity(@PathVariable String insuredPersonUuid) {
        return insuredService.getInsuredPersonEligiblity(insuredPersonUuid);
    }

    @DeleteMapping(path = "/{insuredUuid}")
    @PreAuthorize("hasRole('Delete-Insured-Person')")
    @Operation(summary = "Delete insured person", description = "Deletes an insured person by UUID")
    public ResponseEntity<?> deleteInsuredPerson(@PathVariable String insuredUuid) {
        return insuredService.deleteInsuredPerson(insuredUuid);
    }

    @PostMapping(path = "/import-insured-and-dependant", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//	@PreAuthorize("hasRole('Upload-Insured-Persons')")
    @Operation(summary = "Import insured persons and dependants", description = "Imports insured persons and their dependants from a file")
    public ResponseEntity<?> importInsuredAndDependant(@RequestParam("file") MultipartFile file,
                                                       @RequestParam("institutionUuid") String institutionUuid) throws Exception {
        return insuredService.importInsuredPersonAndDependant(convert(file), institutionUuid);
    }

    /*
     * @PostMapping(path="/import",consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
     *
     * @PreAuthorize("hasRole('Upload-Insured-Persons')") public ResponseEntity<?>
     * importData(@RequestParam("file") MultipartFile
     * file, @RequestParam("institutionUuid") String institutionUuid,
     *
     * @RequestParam("payerInstitutionContractUuid") String
     * payerInstitutionContractUuid ) throws IOException { return
     * insuredService.importInsuredPersonData(convert(file),institutionUuid,
     * payerInstitutionContractUuid); }
     */

    private File convert(MultipartFile file) throws IOException {
        File convFile = new File(file.getOriginalFilename());
        convFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(convFile);
        fos.write(file.getBytes());
        fos.close();
        return convFile;
    }

    @PutMapping(path = "/profile-picture/{insuredUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    // @PreAuthorize("hasRole('Set-Insured-Profile-Picture')")
    @Operation(summary = "Set profile picture", description = "Sets or updates the profile picture for an insured person")
    public ResponseEntity<?> setProfilePicture(@RequestParam("file") MultipartFile file,
                                               @PathVariable("insuredUuid") String insuredUuid) throws IOException {
        return insuredService.setProfilePicture(file, insuredUuid);
    }

    @GetMapping(path = "/active/search/{institutionUuid}")
    @Operation(summary = "Search active insured persons", description = "Searches for active insured persons and their dependants for a specific institution")
    public List<InsuredAndDependantCashServiceResponse> getInsuredAndDependantCashServiceResponse(
            @PathVariable String institutionUuid,
            @RequestParam(required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {
        Pageable pageable = PaginationUtils.paginateResource(page,limit,"id","desc");
        return insuredService.getInsuredAndDependantCashServiceResponse(institutionUuid, search, pageable);
    }

    @GetMapping("/member/exist/{payerInstitutionContractUuid}")
    @Operation(summary = "Check if member exists", description = "Checks if any insured members exist for a specific payer institution contract")
    public boolean checkMemberExist(@PathVariable String payerInstitutionContractUuid) {
        return insuredService.checkMemberExist(payerInstitutionContractUuid);
    }

    /**
     * Get all insured persons with their dependants for a specific institution with search capability
     * @param institutionUuid The UUID of the institution
     * @param search Optional search key to filter results (name, phone, insurance ID)
     * @param page Page number (1-based)
     * @param limit Number of records per page
     * @return List of insured persons with their dependants
     */
    @GetMapping("/institution/with-dependants/{institutionUuid}")
    //@PreAuthorize("hasRole('Read-Insured-Persons')")
    @Operation(summary = "Get insured persons with dependants by institution",
            description = "Retrieves all insured persons with their dependants for a specific institution with search capability")
    public List<InsuredDependantResponse> getAllInsuredPersonsWithDependantsByInstitution(
            @PathVariable String institutionUuid,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {
        return insuredService.getAllInsuredPersonsWithDependantsByInstitution(institutionUuid, search, page, limit);
    }
}