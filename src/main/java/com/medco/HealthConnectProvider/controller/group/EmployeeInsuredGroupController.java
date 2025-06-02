package com.medco.HealthConnectProvider.controller.group;

import com.medco.HealthConnectProvider.services.group.EmployeeInsuredGroupService;
import com.medco.HealthConnectProvider.ui.request.group.EmployeeInsuredGroupRequest;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeInsuredGroupResponse;
import com.medco.HealthConnectProvider.utils.paginationUtils.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/employee-insured-groups")
@Tag(name = "Employee Insured Group Management", description = "APIs for managing associations between insured employees and groups")
public class EmployeeInsuredGroupController {

    @Autowired
    private EmployeeInsuredGroupService employeeInsuredGroupService;

    @PostMapping
    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Add insured to group", description = "Adds an insured employee to a group")
    public ResponseEntity<?> addInsuredToGroup(@Valid @RequestBody EmployeeInsuredGroupRequest request) {
        return employeeInsuredGroupService.addInsuredToGroup(request);
    }

    @GetMapping("/insured/{insuredUuid}")
    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "Get groups by insured", description = "Retrieves all groups that a specific insured employee belongs to")
    public List<EmployeeInsuredGroupResponse> getGroupsByInsured(@PathVariable String insuredUuid) {
        return employeeInsuredGroupService.getGroupsByInsured(insuredUuid);
    }

    @GetMapping("/group/{groupUuid}")
    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "Get insured by group", description = "Retrieves all insured employees in a specific group with pagination and search")
    public Page<EmployeeInsuredGroupResponse> getInsuredByGroup(
            @PathVariable String groupUuid,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {

        Pageable pageable = PaginationUtils.paginateResource(page, limit, "id", "desc");
        return employeeInsuredGroupService.getInsuredByGroup(groupUuid, search, pageable);
    }

    @DeleteMapping("/{insuredUuid}/{groupUuid}")
    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Remove insured from group", description = "Removes an insured employee from a group")
    public ResponseEntity<?> removeInsuredFromGroup(
            @PathVariable String insuredUuid,
            @PathVariable String groupUuid) {
        return employeeInsuredGroupService.removeInsuredFromGroup(insuredUuid, groupUuid);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Batch add insured to groups", description = "Adds multiple insured employees to groups in a single operation")
    public ResponseEntity<?> batchAddInsuredToGroup(@Valid @RequestBody List<EmployeeInsuredGroupRequest> requests) {
        return employeeInsuredGroupService.batchAddInsuredToGroup(requests);
    }

    @GetMapping("/count/{groupUuid}")
    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "Get insured count by group", description = "Retrieves the count of insured employees in a specific group")
    public Long getInsuredCountByGroup(@PathVariable String groupUuid) {
        return employeeInsuredGroupService.getInsuredCountByGroup(groupUuid);
    }
}