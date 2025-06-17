package com.medco.HealthConnectProvider.controller.persons;

import com.medco.HealthConnectProvider.services.persons.DependantService;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.DependantRequest;
import com.medco.HealthConnectProvider.ui.request.persons.DependantUpdateRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.persons.DependantResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/api/v1/healthConnect/dependant")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Dependant Management", description = "APIs for managing dependant persons")
public class DependantController {

    private final DependantService dependantService;

    public DependantController(DependantService dependantService) {
        this.dependantService = dependantService;
    }

    @PostMapping(path = "/createDependant", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    //@PreAuthorize("hasRole('Create-Dependant-Person')")
    @Operation(summary = "Create dependant", description = "Creates a new dependant person with photo")
    public ResponseEntity<?> createDependant(
            @RequestPart("dependant") @Valid DependantRequest dependantRequest,
            @RequestPart(value = "photo", required = false) MultipartFile photo) {
        return dependantService.createDependant(dependantRequest, photo);
    }


    @GetMapping("/{dependantUuid}")
    @Operation(summary = "Get dependant by UUID", description = "Retrieves a dependant's details by their UUID")
    public ResponseEntity<DependantResponse> getDependantByUuid(@PathVariable String dependantUuid) {
        DependantResponse dependant = dependantService.getDependantByUuid(dependantUuid);
        return ResponseEntity.ok(dependant);
    }

    @DeleteMapping(path="/{dependantUuid}")
    //@PreAuthorize("hasRole('Delete-Dependant-Person')")
    @Operation(summary = "Delete dependant", description = "Deletes a dependant person by UUID")
    public ResponseEntity<?> deleteDependant(@PathVariable String dependantUuid) {
        return dependantService.deleteDependant(dependantUuid);
    }

    @DeleteMapping("/softDelete/{dependantUuid}")
    @Operation(summary = "Soft delete a dependant", description = "Marks a dependant as deleted without removing the record from the database")
    public ResponseEntity<ApiResponse> softDeleteDependant(@PathVariable String dependantUuid) {
        dependantService.softDeleteDependant(dependantUuid);
        return ResponseEntity.ok(new ApiResponse());
    }

    @PutMapping(value = "/{dependantUuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update dependant", description = "Updates a dependant's details by their UUID")
    public ResponseEntity<DependantResponse> updateDependant(
            @PathVariable String dependantUuid,
            @RequestPart("updateRequest") @Valid DependantUpdateRequest updateRequest,
            @RequestPart(value = "profilePicture", required = false) MultipartFile profilePicture) {
        DependantResponse updatedDependant = dependantService.updateDependant(dependantUuid, updateRequest, profilePicture);
        return ResponseEntity.ok(updatedDependant);
    }

    @GetMapping("/{insuredPersonUuid}/dependants")
    @Operation(summary = "Get all dependants of an insured person", description = "Retrieves all dependants associated with the given insured person UUID")
    public ResponseEntity<List<DependantResponse>> getDependantsByInsuredPersonUuid(
            @PathVariable String insuredPersonUuid,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PagedResponse<DependantResponse> pagedDependants = dependantService.getDependantsByInsuredPersonUuid(insuredPersonUuid, page, size);
        return ResponseEntity.ok()
                .header("X-Total-Count", String.valueOf(pagedDependants.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(pagedDependants.getTotalPages()))
                .body(pagedDependants.getContent());


    }

    @PutMapping("/{dependantUuid}/status")
    @Operation(summary = "Update a dependant status", description = "Changes the status of a dependant")
    public ResponseEntity<DependantResponse> changeStatus(@PathVariable String dependantUuid, Status newStatus) {
        DependantResponse changeStatus = dependantService.changeStatus(dependantUuid, newStatus);
        return ResponseEntity.ok(changeStatus);
    }

}