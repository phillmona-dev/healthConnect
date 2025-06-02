package com.medco.HealthConnectProvider.controller.group;

import com.medco.HealthConnectProvider.services.group.DependantGroupService;
import com.medco.HealthConnectProvider.ui.request.group.DependantGroupRequest;
import com.medco.HealthConnectProvider.ui.response.groups.DependantGroupResponse;
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
@RequestMapping("/api/v1/healthConnect/dependant-groups")
@Tag(name = "Dependant Group Management", description = "APIs for managing associations between dependants and groups")
public class DependantGroupController {

    @Autowired
    private DependantGroupService dependantGroupService;

    @PostMapping
    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Add dependant to group", description = "Adds a dependant to an employee group")
    public ResponseEntity<?> addDependantToGroup(@Valid @RequestBody DependantGroupRequest request) {
        return dependantGroupService.addDependantToGroup(request);
    }

    @GetMapping("/dependant/{dependantUuid}")
    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "Get groups by dependant", description = "Retrieves all groups that a specific dependant belongs to")
    public List<DependantGroupResponse> getGroupsByDependant(@PathVariable String dependantUuid) {
        return dependantGroupService.getGroupsByDependant(dependantUuid);
    }

    @GetMapping("/group/{groupUuid}")
    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "Get dependants by group", description = "Retrieves all dependants in a specific group with pagination and search")
    public Page<DependantGroupResponse> getDependantsByGroup(
            @PathVariable String groupUuid,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {

        Pageable pageable = PaginationUtils.paginateResource(page, limit, "id", "desc");
        return dependantGroupService.getDependantsByGroup(groupUuid, search, pageable);
    }

    @DeleteMapping("/{dependantUuid}/{groupUuid}")
    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Remove dependant from group", description = "Removes a dependant from an employee group")
    public ResponseEntity<?> removeDependantFromGroup(
            @PathVariable String dependantUuid,
            @PathVariable String groupUuid) {
        return dependantGroupService.removeDependantFromGroup(dependantUuid, groupUuid);
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Batch add dependants to groups", description = "Adds multiple dependants to groups in a single operation")
    public ResponseEntity<?> batchAddDependantToGroup(@Valid @RequestBody List<DependantGroupRequest> requests) {
        return dependantGroupService.batchAddDependantToGroup(requests);
    }

    @GetMapping("/count/{groupUuid}")
    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "Get dependant count by group", description = "Retrieves the count of dependants in a specific group")
    public Long getDependantCountByGroup(@PathVariable String groupUuid) {
        return dependantGroupService.getDependantCountByGroup(groupUuid);
    }
}