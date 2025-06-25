//package com.medco.HealthConnectProvider.services.group;
//
//import com.medco.HealthConnectProvider.ui.request.group.DependantGroupRequest;
//import com.medco.HealthConnectProvider.ui.response.groups.DependantGroupResponse;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.http.ResponseEntity;
//
//import java.util.List;
//
//public interface DependantGroupService {
//
//    /**
//     * Add a dependant to a group
//     * @param request The request containing dependant and group UUIDs
//     * @return Response with success message
//     */
//    ResponseEntity<?> addDependantToGroup(DependantGroupRequest request);
//
//    /**
//     * Get all groups for a dependant
//     * @param dependantUuid The UUID of the dependant
//     * @return List of groups the dependant belongs to
//     */
//    List<DependantGroupResponse> getGroupsByDependant(String dependantUuid);
//
//    /**
//     * Get all dependants in a group
//     * @param groupUuid The UUID of the group
//     * @param search Optional search term for filtering
//     * @param pageable Pagination information
//     * @return Page of dependants in the group
//     */
//    Page<DependantGroupResponse> getDependantsByGroup(String groupUuid, String search, Pageable pageable);
//
//    /**
//     * Remove a dependant from a group
//     * @param dependantUuid The UUID of the dependant
//     * @param groupUuid The UUID of the group
//     * @return Response with success message
//     */
//    ResponseEntity<?> removeDependantFromGroup(String dependantUuid, String groupUuid);
//
//    /**
//     * Batch add dependants to a group
//     * @param requests List of requests containing dependant and group UUIDs
//     * @return Response with success message
//     */
//    ResponseEntity<?> batchAddDependantToGroup(List<DependantGroupRequest> requests);
//
//    /**
//     * Get count of dependants in a group
//     * @param groupUuid The UUID of the group
//     * @return Count of dependants
//     */
//    Long getDependantCountByGroup(String groupUuid);
//}