package com.medco.HealthConnectProvider.services.user;

import com.medco.HealthConnectProvider.ui.request.auth.password.RoleRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.auth.RoleResponse;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface RoleService {
    ResponseEntity<?> updateRole(String roleUuid, RoleRequest roleUpdateRequest);

    ResponseEntity<PagedResponse<RoleResponse>> createRole(RoleRequest roleRequest);

    RoleResponse getRoleByUuid(String roleUuid);

    PagedResponse<RoleResponse> getAllRoles(String search, int page, int limit);

    ResponseEntity<?> deleteRole(String roleUuid);
}
