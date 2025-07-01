package com.medco.HealthConnectProvider.services.group;

import com.medco.HealthConnectProvider.ui.request.group.EmployeeDependantGroupRequest;

import com.medco.HealthConnectProvider.ui.request.group.GroupMembersRequest;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeDependantGroupResponse;
import com.medco.HealthConnectProvider.ui.response.groups.GroupContractDetailResponse;
import com.medco.HealthConnectProvider.ui.response.groups.GroupMembersAndServicesResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PagedResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface EmployeeDependantGroupService {
    
    /**
     * Create a new employee/dependant group
     * @param request The group creation request
     * @return Response with the created group
     */
    ResponseEntity<EmployeeDependantGroupResponse> createGroup(String payerUuid,EmployeeDependantGroupRequest request);
    
    /**
     * Get a group by its UUID
     * @param groupUuid The UUID of the group
     * @return The group details
     */
    GroupMembersAndServicesResponse getGroupByUuid(String groupUuid);
    
    /**
     * List groups with pagination and search
     * @param search Search term
     * @param pageable Pagination information
     * @return Page of groups
     */
    Page<EmployeeDependantGroupResponse> listGroups(String search, Pageable pageable);
    
    /**
     * Update an existing group
     * @param groupUuid The UUID of the group to update
     * @param request The update request
     * @return Response with the updated group
     */
    ResponseEntity<EmployeeDependantGroupResponse> updateGroup(String groupUuid, EmployeeDependantGroupRequest request);
    
    /**
     * Delete a group
     * @param groupUuid The UUID of the group to delete
     * @return Response with success message
     */
    ResponseEntity<?> deleteGroup(String groupUuid);

    PagedResponse<EmployeeDependantGroupResponse> payerGroups(String payerUUid, String search, Pageable pageable);

    ResponseEntity<?> addMembersToGroup(String groupUuid, GroupMembersRequest request);

    ResponseEntity<?> addServicesToGroup(String groupUuid, List<String> services);

    ResponseEntity<List<GroupContractDetailResponse>> getContractDetailsByGroup(String groupUuid);

    ResponseEntity<List<GroupContractDetailResponse>> getContractDetailsByGroupAndContract(String groupUuid, String contractUuid);
}