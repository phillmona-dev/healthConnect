package com.medco.HealthConnectProvider.controller.users;

import com.medco.HealthConnectProvider.services.user.RoleService;
import com.medco.HealthConnectProvider.ui.request.auth.password.RoleRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.auth.RoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/v1/healthConnect/users/role")
//@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Role Management", description = "APIs for managing user roles and their associated privileges")
public class RoleController {

    private RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    //@PreAuthorize("hasRole('Create_Role')")
    @Operation(summary = "Create role", description = "Creates a new role with associated privileges")
    public ResponseEntity<PagedResponse<RoleResponse>> createRole(@Valid @RequestBody RoleRequest roleRequest) {
        return roleService.createRole(roleRequest);
    }

    @GetMapping("/{roleUuid}")
    @Operation(summary = "Get role", description = "Retrieves a specific role by UUID")
    public RoleResponse getRoleByUuid(@PathVariable String roleUuid){
        return roleService.getRoleByUuid(roleUuid);
    }

    @GetMapping("/all")
    @Operation(
            summary = "List roles",
            description = "Retrieves a list of roles with pagination and search capabilities. " +
                    "The search parameter can match against role name, provider UUID, or payer UUID."
    )
    public ResponseEntity<PagedResponse<RoleResponse>> getAllRoles(
            @Parameter(description = "Search term for role name, provider UUID, or payer UUID")
            @RequestParam(value = "search", required = false) String search,

            @Parameter(description = "Page number (1-based)", example = "1")
            @RequestParam(value = "page", defaultValue = "1") int page,

            @Parameter(description = "Number of items per page", example = "250")
            @RequestParam(value = "limit", defaultValue = "2500") int limit
    ) {
        PagedResponse<RoleResponse> pagedResponse = roleService.getAllRoles(search, page - 1, limit);
        return ResponseEntity.ok(pagedResponse);
    }

    @PutMapping(path="/{roleUuid}")
    //@PreAuthorize("hasRole('Update_Role')")
    @Operation(summary = "Update role", description = "Updates an existing role and its associated privileges")
    public ResponseEntity<?> updateRole(@PathVariable String roleUuid, @Valid @RequestBody RoleRequest roleUpdateRequest) {
        return roleService.updateRole(roleUuid, roleUpdateRequest);
    }

    @DeleteMapping("/{roleUuid}")
    @Operation(summary = "Delete role", description = "Deletes a role by UUID")
    public ResponseEntity<?> deleteRole(@PathVariable String roleUuid){
        return roleService.deleteRole(roleUuid);
    }
}