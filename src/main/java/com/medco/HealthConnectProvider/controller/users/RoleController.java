package com.medco.HealthConnectProvider.controller.users;

import com.medco.HealthConnectProvider.services.user.RoleService;
import com.medco.HealthConnectProvider.ui.request.auth.password.RoleRequest;
import com.medco.HealthConnectProvider.ui.response.auth.RoleResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;

//@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/provider/healthConnectProvider/users/role")
//@SecurityRequirement(name = "bearerAuth")
public class RoleController {

    private RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @PostMapping
    //@PreAuthorize("hasRole('Create_Role')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody RoleRequest roleRequest) {
        return roleService.createRole(roleRequest);
    }

    @GetMapping("/{roleUuid}")
    public RoleResponse getRoleByUuid(@PathVariable String roleUuid){
        return roleService.getRoleByUuid(roleUuid);
    }

    @GetMapping("/all")
    public List<RoleResponse> getAllRoles(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "limit", defaultValue = "25") int limit
    ){
        return roleService.getAllRoles(search,page,limit);
    }

    @PutMapping(path="/{roleUuid}")
    //@PreAuthorize("hasRole('Update_Role')")
    public ResponseEntity<?> updateRole(@PathVariable String roleUuid, @Valid @RequestBody RoleRequest roleUpdateRequest) {
        return roleService.updateRole(roleUuid, roleUpdateRequest);
    }

    @DeleteMapping("/{roleUuid}")
    public ResponseEntity<?> deleteRole(@PathVariable String roleUuid){
        return roleService.deleteRole(roleUuid);
    }
}