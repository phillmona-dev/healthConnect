package com.medco.HealthConnectProvider.controller.group;

import com.medco.HealthConnectProvider.services.group.EmployeeDependantGroupService;
import com.medco.HealthConnectProvider.ui.request.group.EmployeeDependantGroupRequest;
import com.medco.HealthConnectProvider.ui.request.group.GroupMembersRequest;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeDependantGroupResponse;
import com.medco.HealthConnectProvider.ui.response.groups.GroupMembersAndServicesResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PagedResponse;
import com.medco.HealthConnectProvider.utils.paginationUtils.PaginationUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1/healthConnect/groups")
@Tag(name = "Group Management", description = "APIs for managing employee and dependant groups")
public class EmployeeDependantGroupController {


    private final EmployeeDependantGroupService groupService;

    public EmployeeDependantGroupController(EmployeeDependantGroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping("/createGroup/{payerUuid}")
//    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Create group", description = "Creates a new employee/dependant group")
    public ResponseEntity<EmployeeDependantGroupResponse> createGroup(@PathVariable(value = "payerUuid")String payerUuid,
                                                                      @Valid @RequestBody EmployeeDependantGroupRequest request) {

        System.out.println("in the create group controller");
        return groupService.createGroup(payerUuid,request);
    }

    @GetMapping("membersAndServices/{groupUuid}")
//    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "Get group by UUID", description = "Retrieves a specific group by its UUID")
    public GroupMembersAndServicesResponse getGroupByUuid(@PathVariable String groupUuid) {
        return groupService.getGroupByUuid(groupUuid);
    }

    @GetMapping("/groups")
//    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "List groups", description = "Retrieves a list of groups with pagination and search")
    public Page<EmployeeDependantGroupResponse> listGroups(
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {
        
        Pageable pageable = PaginationUtils.paginateResource(page, limit, "id", "desc");
        return groupService.listGroups(search, pageable);
    }

    @GetMapping("/payerGroups/{payerUuid}")
//    @PreAuthorize("hasRole('View-Groups')")
    @Operation(summary = "List groups", description = "Retrieves a list of payer groups with pagination and search")
    public PagedResponse<EmployeeDependantGroupResponse> payerGroups(
            @PathVariable(value = "payerUuid") String payerUuid,
            @RequestParam(required = false, defaultValue = "") String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit) {

        Pageable pageable = PaginationUtils.paginateResource(page, limit, "id", "desc");
        return groupService.payerGroups(payerUuid,search, pageable);
    }


    @PutMapping("/{groupUuid}")
//    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Update group", description = "Updates an existing employee/dependant group")
    public ResponseEntity<EmployeeDependantGroupResponse> updateGroup(
            @PathVariable String groupUuid,
            @Valid @RequestBody EmployeeDependantGroupRequest request) {
        return groupService.updateGroup(groupUuid, request);
    }

    @PutMapping("addMembersToGroup/{groupUuid}")
//    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Update group", description = "Updates an existing employee/dependant group")
    public ResponseEntity<?> addMembersToGroup(
            @PathVariable(value = "groupUuid") String groupUuid,
            @Valid @RequestBody()GroupMembersRequest request) {
        return groupService.addMembersToGroup(groupUuid, request);
    }

    @PutMapping("addServicesToGroup/{groupUuid}")
//    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Update group", description = "Updates an existing employee/dependant group")
    public ResponseEntity<?> addServicesToGroup(
            @PathVariable(value = "groupUuid") String groupUuid,
            @RequestParam(value = "members") List<String>  eligibleServices) {
        return groupService.addServicesToGroup(groupUuid,eligibleServices);
    }

    @DeleteMapping("/{groupUuid}")
//    @PreAuthorize("hasRole('Manage-Groups')")
    @Operation(summary = "Delete group", description = "Soft deletes an employee/dependant group")
    public ResponseEntity<?> deleteGroup(@PathVariable String groupUuid) {
        return groupService.deleteGroup(groupUuid);
    }
}