package com.medco.HealthConnectProvider.controller.group;

import com.medco.HealthConnectProvider.services.group.ContractDetailEmployeeGroupService;
import com.medco.HealthConnectProvider.ui.request.group.ContractDetailEmployeeGroupRequest;
import com.medco.HealthConnectProvider.ui.response.groups.ContractDetailEmployeeGroupResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/v1/healthConnect/contract-detail-employee-groups")
@Tag(name = "Contract Detail Employee Group Management", description = "APIs for managing associations between contract details and employee groups")
public class ContractDetailEmployeeGroupController {

    @Autowired
    private ContractDetailEmployeeGroupService contractDetailEmployeeGroupService;

//    @PostMapping
//    @PreAuthorize("hasRole('Create-Provider-Contract')")
//    @Operation(summary = "Create association", description = "Creates an association between a contract detail and an employee group")
//    public ResponseEntity<?> createContractDetailEmployeeGroup(
//            @Valid @RequestBody ContractDetailEmployeeGroupRequest request) {
//        return contractDetailEmployeeGroupService.createContractDetailEmployeeGroup(request);
//    }

    @PostMapping("/batch/{employeeGroupUuid}")
    @PreAuthorize("hasRole('Create-Provider-Contract')")
    @Operation(summary = "Batch create associations", description = "Creates multiple associations between contract details and employee groups")
    public ResponseEntity<?> batchCreateContractDetailEmployeeGroups(@PathVariable(value = "employeeGroupUuid")String employeeGroupUuid,
            @Valid @RequestBody ContractDetailEmployeeGroupRequest request) {
        return contractDetailEmployeeGroupService.batchCreateContractDetailEmployeeGroups(employeeGroupUuid,request);
    }

    @GetMapping("/contract/{contractUuid}")
    @PreAuthorize("hasRole('View-Provider-Contract')")
    @Operation(summary = "Get by contract", description = "Retrieves all associations for a specific contract")
    public List<ContractDetailEmployeeGroupResponse> getContractDetailEmployeeGroupsByContract(
            @PathVariable String contractUuid) {
        return contractDetailEmployeeGroupService.getContractDetailEmployeeGroupsByContract(contractUuid);
    }

    @GetMapping("/contract-detail/{contractDetailUuid}")
    @PreAuthorize("hasRole('View-Provider-Contract')")
    @Operation(summary = "Get by contract detail", description = "Retrieves all associations for a specific contract detail")
    public List<ContractDetailEmployeeGroupResponse> getContractDetailEmployeeGroupsByContractDetail(
            @PathVariable String contractDetailUuid) {
        return contractDetailEmployeeGroupService.getContractDetailEmployeeGroupsByContractDetail(contractDetailUuid);
    }

    @GetMapping("/employee-group/{employeeGroupUuid}")
    @PreAuthorize("hasRole('View-Provider-Contract')")
    @Operation(summary = "Get by employee group", description = "Retrieves all associations for a specific employee group")
    public List<ContractDetailEmployeeGroupResponse> getContractDetailEmployeeGroupsByEmployeeGroup(
            @PathVariable String employeeGroupUuid) {
        return contractDetailEmployeeGroupService.getContractDetailEmployeeGroupsByEmployeeGroup(employeeGroupUuid);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('View-Provider-Contract')")
    @Operation(summary = "Search associations", description = "Searches for associations by contract and group name with pagination")
    public Page<ContractDetailEmployeeGroupResponse> searchContractDetailEmployeeGroups(
            @RequestParam String contractUuid,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return contractDetailEmployeeGroupService.searchContractDetailEmployeeGroups(contractUuid, search, pageable);
    }

    @DeleteMapping("/{contractDetailUuid}/{employeeGroupUuid}")
    @PreAuthorize("hasRole('Delete-Provider-Contract')")
    @Operation(summary = "Delete association", description = "Deletes an association between a contract detail and an employee group")
    public ResponseEntity<?> deleteContractDetailEmployeeGroup(
            @PathVariable String contractDetailUuid,
            @PathVariable String employeeGroupUuid) {
        return contractDetailEmployeeGroupService.deleteContractDetailEmployeeGroup(contractDetailUuid, employeeGroupUuid);
    }

}