package com.medco.HealthConnectProvider.services.group;

import com.medco.HealthConnectProvider.ui.request.group.EmployeeDependantGroupRequest;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeDependantGroupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

public interface EmployeeDependantGroupService {
    
    /**
     * Create a new employee/dependant group
     * @param request The group creation request
     * @return Response with the created group
     */
    ResponseEntity<EmployeeDependantGroupResponse> createGroup(EmployeeDependantGroupRequest request);
    
    /**
     * Get a group by its UUID
     * @param groupUuid The UUID of the group
     * @return The group details
     */
    EmployeeDependantGroupResponse getGroupByUuid(String groupUuid);
    
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
}