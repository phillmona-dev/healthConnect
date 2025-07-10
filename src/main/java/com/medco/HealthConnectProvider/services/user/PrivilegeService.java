package com.medco.HealthConnectProvider.services.user;

import com.medco.HealthConnectProvider.ui.request.auth.password.PrivilegeRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.auth.PrivilegeResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface PrivilegeService {
    PagedResponse<PrivilegeResponse> createPrivilege(PrivilegeRequest privilegeRequest);

    PrivilegeResponse getPrivilege(String privilegeUuid);

    PagedResponse<PrivilegeResponse> getAllPrivileges(String search, Pageable pageable);


    ResponseEntity<?> updatePrivilege(String privilegeUuid, PrivilegeRequest request);

    ResponseEntity<?> deletePrivilege(String privilegeUuid);
}

