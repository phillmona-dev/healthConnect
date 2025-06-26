package com.medco.HealthConnectProvider.controller.persons;

import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.services.persons.InsuredService;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.DependantRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredRequest;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.InsuredWithDependantsRequest;
import com.medco.HealthConnectProvider.ui.request.persons.InsuredUpdateRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.persons.*;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.paginationUtils.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/healthConnect/insuredperson")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Insured Person Management", description = "APIs for managing insured persons and their dependants")
public class InsuredController {

    private final InsuredService insuredService;

    public InsuredController(InsuredService insuredService) {
        this.insuredService = insuredService;
    }

    @PostMapping(path = "/createInsuredPerson", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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


    /**
     * Validates the insured request fields
     *
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
     *
     * @param dependant The dependant request to validate
     * @throws BadRequestException if validation fails
     */
    private void validateDependantRequest(DependantRequest dependant) {
        // Only validate if this is an update to an existing dependant (has UUID)
        if (dependant.getInsuredPersonUuid() != null && !dependant.getInsuredPersonUuid().trim().isEmpty()) {
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

    @GetMapping(path = "/list/{payerInstitutionContractId}")
    @Operation(summary = "List insured persons", description = "Retrieves a list of insured persons for a specific payer institution contract with pagination and search")
    public List<InsuredResponse> getInsuredPersons(@PathVariable String payerInstitutionContractId,
                                                   @RequestParam(name = "search", required = false) String search,
                                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                                   @RequestParam(value = "limit", defaultValue = "25") int limit) {
        List<InsuredResponse> insuredList = insuredService.getInsuredPersons(payerInstitutionContractId, search, page, limit);

        for (InsuredResponse insured : insuredList) {
            insured.setPhotoBase64(insuredService.getInsuredPhotoBase64(insured.getInsuredUuid()));
        }

        return insuredList;
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

    @DeleteMapping(path = "/{insuredUuid}")
    // @PreAuthorize("hasRole('Delete-Insured-Person')")
    @Operation(summary = "Delete insured person", description = "Deletes an insured person by UUID")
    public ResponseEntity<?> deleteInsuredPerson(@PathVariable String insuredUuid) {
        return insuredService.deleteInsuredPerson(insuredUuid);
    }

    @PostMapping(path = "/import-insured-and-dependant", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//	@PreAuthorize("hasRole('Upload-Insured-Persons')")
    @Operation(summary = "Import insured persons and dependants", description = "Imports insured persons and their dependants from a file")
    public ResponseEntity<?> importInsuredAndDependant(@RequestParam("file") MultipartFile file,
                                                       @RequestParam("payerUuid") String payerUuid) throws Exception {
        return insuredService.importInsuredPersonAndDependant(convert(file), payerUuid);
    }

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
        Pageable pageable = PaginationUtils.paginateResource(page, limit, "id", "desc");
        return insuredService.getInsuredAndDependantCashServiceResponse(institutionUuid, search, pageable);
    }

    @GetMapping("/member/exist/{payerInstitutionContractUuid}")
    @Operation(summary = "Check if member exists", description = "Checks if any insured members exist for a specific payer institution contract")
    public boolean checkMemberExist(@PathVariable String payerInstitutionContractUuid) {
        return insuredService.checkMemberExist(payerInstitutionContractUuid);
    }

    @GetMapping("/insuredWithPhotoBase64/{insuredUuid}")
    @Operation(summary = "Get insured person with photo", description = "Retrieves an insured person with their photo as base64")
    public ResponseEntity<?> getInsuredPersonWithPhoto(@PathVariable String insuredUuid) {
        return insuredService.getInsuredPersonWithPhotoBase64(insuredUuid);
    }


//new

    @GetMapping("/{insuredUuid}")
    @Operation(summary = "Get insured person by UUID", description = "Retrieves an insured person's details by their UUID")
    public ResponseEntity<?> getInsuredPersonByUuid(@PathVariable String insuredUuid) {
        return insuredService.getInsuredPersonByUuid(insuredUuid);
    }

    @GetMapping("/all")
    @Operation(summary = "Get all insured persons with dependants", description = "Retrieves all insured persons with their dependants, with pagination and optional search")
    public ResponseEntity<PagedResponse<InsuredWithDependantsResponse>> getAllInsuredPersonsWithDependants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return insuredService.getAllInsuredPersonsWithDependants(page, size, search);
    }

    @GetMapping("/by-payer/{payerUuid}")
    @Operation(summary = "Get all insured persons with dependents by payer UUID",
            description = "Retrieves all insured persons with their dependents for a specific payer, with pagination and optional search")
    public ResponseEntity<PagedResponse<InsuredDependantResponse>> getAllInsuredPersonsWithDependentsByPayer(
            @PathVariable String payerUuid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        return insuredService.getAllInsuredPersonsWithDependentsByPayer(payerUuid, page, size, search);
    }

    @DeleteMapping("/softDelete/{insuredUuid}")
    @Operation(summary = "Soft delete insured person", description = "Soft deletes an insured person by UUID")
    public ResponseEntity<?> softDeleteInsuredPerson(@PathVariable String insuredUuid) {
        return insuredService.softDeleteInsuredPerson(insuredUuid);
    }

    @PutMapping(path = "/updateInsured/{insuredUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update insured person", description = "Updates an existing insured person's details and photo")
    public ResponseEntity<?> updateInsuredPerson(
            @PathVariable String insuredUuid,
            @RequestPart("insured") @Valid InsuredUpdateRequest insuredRequest,
            @RequestPart(value = "photo", required = false) MultipartFile photo) throws IOException {
        return insuredService.updateInsuredPerson(insuredUuid, insuredRequest, photo);
    }

    @PutMapping("/{insuredUuid}/status")
    @Operation(summary = "Update insured person's status",
            description = "Changes the status of an insured person to the specified status")
    public ResponseEntity<InsuredResponse> updateInsuredStatus(
            @PathVariable String insuredUuid,
            @RequestParam Status newStatus) {
        InsuredResponse updatedInsured = insuredService.updateInsuredStatus(insuredUuid, newStatus);
        return ResponseEntity.ok(updatedInsured);
    }

    @GetMapping("/search")
    @Operation(summary = "Search insured persons",
            description = "Searches for insured persons based on a single identifier (phone number, employeeId, insuranceId, or nationalId)")
    public ResponseEntity<List<InsuredSearchResponse>> searchInsuredPersons(
            @RequestParam String identifier) {
        List<InsuredSearchResponse> results = insuredService.searchInsuredPersons(identifier);
        return ResponseEntity.ok(results);
    }

}