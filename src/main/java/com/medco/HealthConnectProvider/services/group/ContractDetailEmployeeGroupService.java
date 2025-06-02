package com.medco.HealthConnectProvider.services.group;

import com.medco.HealthConnectProvider.ui.request.group.ContractDetailEmployeeGroupRequest;
import com.medco.HealthConnectProvider.ui.response.groups.ContractDetailEmployeeGroupResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ContractDetailEmployeeGroupService {
    
    /**
     * Create a new contract detail employee group association
     * @param request The request containing contract detail and employee group UUIDs
     * @return Response with success message
     */
    ResponseEntity<?> createContractDetailEmployeeGroup(ContractDetailEmployeeGroupRequest request);
    
    /**
     * Get all contract detail employee group associations for a contract
     * @param contractUuid The UUID of the contract
     * @return List of contract detail employee group associations
     */
    List<ContractDetailEmployeeGroupResponse> getContractDetailEmployeeGroupsByContract(String contractUuid);
    
    /**
     * Get all contract detail employee group associations for a contract detail
     * @param contractDetailUuid The UUID of the contract detail
     * @return List of contract detail employee group associations
     */
    List<ContractDetailEmployeeGroupResponse> getContractDetailEmployeeGroupsByContractDetail(String contractDetailUuid);
    
    /**
     * Get all contract detail employee group associations for an employee group
     * @param employeeGroupUuid The UUID of the employee group
     * @return List of contract detail employee group associations
     */
    List<ContractDetailEmployeeGroupResponse> getContractDetailEmployeeGroupsByEmployeeGroup(String employeeGroupUuid);
    
    /**
     * Search contract detail employee group associations by contract and group name
     * @param contractUuid The UUID of the contract
     * @param search The search term
     * @param pageable Pagination information
     * @return Page of contract detail employee group associations
     */
    Page<ContractDetailEmployeeGroupResponse> searchContractDetailEmployeeGroups(
            String contractUuid, String search, Pageable pageable);
    
    /**
     * Delete a contract detail employee group association
     * @param contractDetailUuid The UUID of the contract detail
     * @param employeeGroupUuid The UUID of the employee group
     * @return Response with success message
     */
    ResponseEntity<?> deleteContractDetailEmployeeGroup(String contractDetailUuid, String employeeGroupUuid);
    
    /**
     * Batch create contract detail employee group associations
     * @param requests List of requests containing contract detail and employee group UUIDs
     * @return Response with success message
     */
    ResponseEntity<?> batchCreateContractDetailEmployeeGroups(List<ContractDetailEmployeeGroupRequest> requests);
}