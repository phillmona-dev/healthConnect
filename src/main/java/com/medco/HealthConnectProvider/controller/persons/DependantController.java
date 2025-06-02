package com.medco.HealthConnectProvider.controller.persons;

import com.medco.HealthConnectProvider.services.persons.DependantService;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.DependantRequest;
import com.medco.HealthConnectProvider.ui.response.persons.DependantResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    //  @PreAuthorize("hasRole('Create-Dependant-Person')")
    @Operation(summary = "Create dependant", description = "Creates a new dependant person")
    public ResponseEntity<?> createDependant(@Valid @RequestBody DependantRequest dependantRequest) {
        return dependantService.createDependant(dependantRequest);
    }

    @PutMapping(path="/{dependantUuid}")
    @PreAuthorize("hasRole('Update-Dependant-Person')")
    @Operation(summary = "Update dependant", description = "Updates an existing dependant person")
    public ResponseEntity<?> updateDependant(@PathVariable String dependantUuid, @Valid @RequestBody DependantRequest dependantRequest) {
        return dependantService.updateDependant(dependantUuid, dependantRequest);
    }

    @GetMapping(path="/{dependantUuid}")
    @PreAuthorize("hasRole('Read-Dependant-Person')")
    @Operation(summary = "Get dependant", description = "Retrieves a specific dependant person by UUID")
    public DependantResponse getDependant(@PathVariable String dependantUuid) {
        return dependantService.getDependant(dependantUuid);
    }

    @GetMapping(path="/list/{insuredPersonUuid}")
    //@PreAuthorize("hasRole('Read-Dependant-Persons')")
    @Operation(summary = "List dependants", description = "Retrieves all dependants for a specific insured person filtered by status")
    public List<DependantResponse> getPersonDependants(@PathVariable String insuredPersonUuid, @RequestParam Status status) {
        return dependantService.getPersonDependants(insuredPersonUuid, status);
    }

    /*
    @GetMapping(path="/check-eligiblity/{dependantPersonUuid}")
    //@PreAuthorize("hasRole('Check-Dependant-Person-Eligibility')")
    public List<DependantListResponse> getDependantEligiblity(@PathVariable String dependantPersonUuid,@RequestParam(name = "search", required = false)  String searchKey, @RequestParam(value="page", defaultValue = "1") int page,
                                                                @RequestParam(value="limit", defaultValue = "25") int limit) {
        return dependantService.getDependantEligiblity(dependantPersonUuid,searchKey, page, limit);
    }*/

    @DeleteMapping(path="/{dependantUuid}")
    @PreAuthorize("hasRole('Delete-Dependant-Person')")
    @Operation(summary = "Delete dependant", description = "Deletes a dependant person by UUID")
    public ResponseEntity<?> deleteDependant(@PathVariable String dependantUuid) {
        return dependantService.deleteDependant(dependantUuid);
    }
}