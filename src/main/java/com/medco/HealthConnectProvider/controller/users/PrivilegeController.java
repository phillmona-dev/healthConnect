package com.medco.HealthConnectProvider.controller.users;

import com.medco.HealthConnectProvider.services.user.PrivilegeService;
import com.medco.HealthConnectProvider.ui.request.auth.password.PrivilegeRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.auth.PrivilegeResponse;
import com.medco.HealthConnectProvider.utils.paginationUtils.Pagination;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/healthConnect/users/privilege")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Privilege Management", description = "APIs for managing user privileges and permissions")
public class PrivilegeController {

    private PrivilegeService privilegeService;

    public PrivilegeController(PrivilegeService privilegeService) {
        this.privilegeService = privilegeService;
    }

    @PostMapping
//    @PreAuthorize("hasRole('Create-Privilege')")
    @Operation(summary = "Create privilege", description = "Creates a new privilege in the system")
    public PagedResponse<PrivilegeResponse> createPrivilege(@Valid @RequestBody PrivilegeRequest privilegeRequest) {
        return privilegeService.createPrivilege(privilegeRequest);

    }

    @GetMapping(path="/{privilegeUuid}")
    //@PreAuthorize("hasRole('Read-Privilege')")
    @Operation(summary = "Get privilege", description = "Retrieves a specific privilege by UUID")
    public PrivilegeResponse getRole(@PathVariable String privilegeUuid) {
        return privilegeService.getPrivilege(privilegeUuid);
    }

    @GetMapping("/all")
    @Operation(summary = "List privileges", description = "Retrieves a list of privileges with pagination and search capabilities")
    public ResponseEntity<PagedResponse<PrivilegeResponse>> getAllPrivileges(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit
    ) {
        Pageable pageable = Pagination.paginateResource(page, limit, "id", "desc");
        PagedResponse<PrivilegeResponse> response = privilegeService.getAllPrivileges(search, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{privilegeUuid}")
    @Operation(summary = "Update privilege", description = "Updates an existing privilege by UUID")
    public ResponseEntity<?> updatePrivilege(@PathVariable String privilegeUuid, @Valid @RequestBody PrivilegeRequest request){
        return privilegeService.updatePrivilege(privilegeUuid,request);
    }

    @DeleteMapping("/{privilegeUuid}")
    @Operation(summary = "Delete privilege", description = "Deletes a privilege by UUID")
    public ResponseEntity<?> deletePrivilege(@PathVariable String privilegeUuid){
        return privilegeService.deletePrivilege(privilegeUuid);
    }
}