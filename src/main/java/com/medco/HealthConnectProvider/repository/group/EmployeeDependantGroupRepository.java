package com.medco.HealthConnectProvider.repository.group;

import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeDependantGroupRepository extends JpaRepository<EmployeeDependantGroup, Long> {
    
    /**
     * Find an employee dependant group by its UUID
     * @param groupUuid The UUID of the group
     * @return The group if found, null otherwise
     */
    EmployeeDependantGroup findByGroupUuid(String groupUuid);
    
    /**
     * Find all groups for a specific payer
     * @param payer The payer
     * @return List of groups
     */
    List<EmployeeDependantGroup> findByPayer(Payer payer);
    
    /**
     * Find all groups for a specific payer UUID
     * @param payerUuid The UUID of the payer
     * @return List of groups
     */
    List<EmployeeDependantGroup> findByPayerUuid(String payerUuid);
    
    /**
     * Find all active groups for a specific payer
     * @param payerUuid The UUID of the payer
     * @param status The status of the groups
     * @return List of groups
     */
    List<EmployeeDependantGroup> findByPayerUuidAndStatus(String payerUuid, Status status);
    
    /**
     * Search for groups by name for a specific payer
     * @param payerUuid The UUID of the payer
     * @param search The search term
     * @param pageable Pagination information
     * @return Page of groups
     */
    @Query("SELECT g FROM EmployeeDependantGroup g WHERE g.payer.payerUuid = :payerUuid " +
           "AND g.isDeleted = false " +
           "AND (g.groupName LIKE %:search% OR g.groupDescription LIKE %:search%)")
    Page<EmployeeDependantGroup> searchByPayerAndName(
            @Param("payerUuid") String payerUuid,
            @Param("search") String search,
            Pageable pageable);


    // Add these methods to your existing repository

    /**
     * Check if a group with the given name exists for a specific payer
     * @param groupName The name of the group
     * @param payerUuid The UUID of the payer
     * @return True if exists, false otherwise
     */
    boolean existsByGroupNameAndPayerUuid(String groupName, String payerUuid);

    /**
     * Find groups by payer UUID and group name containing search term
     * @param payerUuid The UUID of the payer
     * @param search The search term
     * @param pageable Pagination information
     * @return Page of groups
     */
    Page<EmployeeDependantGroup> findByPayerUuidAndGroupNameContainingIgnoreCase(
            String payerUuid, String search, Pageable pageable);
}