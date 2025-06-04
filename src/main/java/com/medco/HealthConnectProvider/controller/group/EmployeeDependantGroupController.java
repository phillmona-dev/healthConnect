package com.medco.HealthConnectProvider.controller.group;

import com.medco.HealthConnectProvider.services.group.EmployeeDependantGroupService;
import com.medco.HealthConnectProvider.ui.request.group.EmployeeDependantGroupRequest;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeDependantGroupResponse;
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
@RequestMapping("/api/v1/healthConnect/groups")
@Tag(name = "Group Management", description = "APIs for managing employee and dependant groups")
public class EmployeeDependantGroupController {

    @Autowired
    private EmployeeDependantGroupService groupService;

    @PostMapping
    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Create group", description = "Creates a new employee/dependant group")
    public ResponseEntity<EmployeeDependantGroupResponse> createGroup(@Valid @RequestBody EmployeeDependantGroupRequest request) {
        return groupService.createGroup(request);
    }

    @GetMapping("/{groupUuid}")
    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "Get group by UUID", description = "Retrieves a specific group by its UUID")
    public EmployeeDependantGroupResponse getGroupByUuid(@PathVariable String groupUuid) {
        return groupService.getGroupByUuid(groupUuid);
    }

    @GetMapping
    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "List groups", description = "Retrieves a list of groups with pagination and search")
    public Page<EmployeeDependantGroupResponse> listGroups(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {
        
        Pageable pageable = PaginationUtils.paginateResource(page, limit, "id", "desc");
        return groupService.listGroups(search, pageable);
    }

    @PutMapping("/{groupUuid}")
    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Update group", description = "Updates an existing employee/dependant group")
    public ResponseEntity<EmployeeDependantGroupResponse> updateGroup(
            @PathVariable String groupUuid,
            @Valid @RequestBody EmployeeDependantGroupRequest request) {
        return groupService.updateGroup(groupUuid, request);
    }

    @DeleteMapping("/{groupUuid}")
    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Delete group", description = "Soft deletes an employee/dependant group")
    public ResponseEntity<?> deleteGroup(@PathVariable String groupUuid) {
        return groupService.deleteGroup(groupUuid);
    }
}