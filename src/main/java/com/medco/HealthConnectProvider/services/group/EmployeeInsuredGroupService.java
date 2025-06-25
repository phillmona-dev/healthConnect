package com.medco.HealthConnectProvider.services.group;

import com.medco.HealthConnectProvider.ui.request.group.EmployeeInsuredGroupRequest;
import com.medco.HealthConnectProvider.ui.response.groups.EmployeeInsuredGroupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface EmployeeInsuredGroupService {
    
    /**
     * Add an insured person to a group
     * @param request The request containing insured and group UUIDs
     * @return Response with success message
     */
    ResponseEntity<?> addInsuredToGroup(String groupUuid,EmployeeInsuredGroupRequest request);
    
    /**
     * Get all groups for an insured person
     * @param insuredUuid The UUID of the insured person
     * @return List of groups the insured person belongs to
     */
//    List<EmployeeInsuredGroupResponse> getGroupsByInsured(String insuredUuid);
    
    /**
     * Get all insured persons in a group
     * @param groupUuid The UUID of the group
     * @param search Optional search term for filtering
     * @param pageable Pagination information
     * @return Page of insured persons in the group
     */
    Page<EmployeeInsuredGroupResponse> getInsuredByGroup(String groupUuid, String search, Pageable pageable);
    
    /**
     * Remove an insured person from a group
     * @param insuredUuid The UUID of the insured person
     * @param groupUuid The UUID of the group
     * @return Response with success message
     */
    ResponseEntity<?> removeInsuredFromGroup(String insuredUuid, String groupUuid);
    
    /**
     * Batch add insured persons to a group
     * @param requests List of requests containing insured and group UUIDs
     * @return Response with success message
     */
//    ResponseEntity<?> batchAddInsuredToGroup(List<EmployeeInsuredGroupRequest> requests);
    
    /**
     * Get count of insured persons in a group
     * @param groupUuid The UUID of the group
     * @return Count of insured persons
     */
    Long getInsuredCountByGroup(String groupUuid);
}