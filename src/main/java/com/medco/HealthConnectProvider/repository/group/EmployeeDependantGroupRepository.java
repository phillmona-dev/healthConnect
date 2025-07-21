package com.medco.HealthConnectProvider.repository.group;

import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.utils.enums.GroupType;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeDependantGroupRepository extends JpaRepository<EmployeeDependantGroup, Long> {
    
    /**
     * Find an employee dependant group by its UUID
     * @param groupUuid The UUID of the group
     * @return The group if found, null otherwise
     */
    EmployeeDependantGroup findByGroupUuid(String groupUuid);
    
    /**
     * Find all groups for a specific payer UUID
     * @param payerUuid The UUID of the payer
     * @return List of groups
     */
    List<EmployeeDependantGroup> findByPayerUuid(String payerUuid);

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

    //NEW

//    List<EmployeeDependantGroup> findByInsured_InsuredUuidAndIsDeleted(String employeeInsuredUuid, boolean isDeleted);

//    Long countByGroupUuid(String groupUuid);

//    List<EmployeeDependantGroup> findByInsuredAndType(Insured insured, GroupType groupType);

//    @Query("SELECT edg FROM EmployeeDependantGroup edg " +
//            "WHERE edg.groupUuid = :groupUuid " +
//            "AND edg.type = 'EMPLOYEE' " +
//            "AND edg.insured IS NOT NULL " +
//            "AND (edg.insured.firstName LIKE %:search% " +
//            "OR edg.insured.fatherName LIKE %:search% " +
//            "OR edg.insured.grandFatherName LIKE %:search% " +
//            "OR edg.insured.insuranceId LIKE %:search%) " +
//            "AND edg.isDeleted = false")
//    Page<EmployeeDependantGroup> searchByGroupUuidAndInsuredName(
//            @Param("groupUuid") String groupUuid,
//            @Param("search") String search,
//            Pageable pageable
//    );

//    Long countByGroupUuidAndInsuredIsNotNull(String groupUuid);

//    @Query("SELECT edg FROM EmployeeDependantGroup edg " +
//            "JOIN edg.dependant d " +
//            "WHERE edg.groupUuid = :groupUuid " +
//            "AND (d.firstName LIKE %:search% OR d.fatherName LIKE %:search% OR d.grandFatherName LIKE %:search%) " +
//            "AND edg.isDeleted = false")
//    Page<EmployeeDependantGroup> searchByGroupUuidAndDependantName(
//            @Param("groupUuid") String groupUuid,
//            @Param("search") String search,
//            Pageable pageable
//    );

//    Optional<EmployeeDependantGroup> findByDependant_DependantUuidAndGroupUuid(String dependantUuid, String groupUuid);


    List<EmployeeDependantGroup> findByInsuredsAndIsDeleted(Insured insured, boolean b);

    Page<EmployeeDependantGroup> findByPayerPayerUuid(String payerUUid, Pageable pageable);

    long countByPayerPayerUuid(String payerUuid);
}