//package com.medco.HealthConnectProvider.repository.group;
//
//import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
//import com.medco.HealthConnectProvider.entity.groups.EmployeeInsuredGroup;
//import com.medco.HealthConnectProvider.entity.persons.Insured;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface EmployeeInsuredGroupRepository extends JpaRepository<EmployeeInsuredGroup, Long> {
//
//    boolean existsByInsuredAndEmployeeDependantGroup(Insured insured, EmployeeDependantGroup employeeDependantGroup);
//
//    // Fix the method name to match the actual property names in the entity
//    Optional<EmployeeInsuredGroup> findByEmployeeInsuredUuidAndGroupUuid(String insuredUuid, String groupUuid);
//
//    List<EmployeeInsuredGroup> findByEmployeeInsuredUuid(String insuredUuid);
//
//    List<EmployeeInsuredGroup> findByGroupUuid(String groupUuid);
//
//    @Query("SELECT eig FROM EmployeeInsuredGroup eig " +
//            "JOIN eig.insured i " +
//            "JOIN eig.employeeDependantGroup edg " +
//            "WHERE edg.groupUuid = :groupUuid " +
//            "AND (i.firstName LIKE %:search% OR i.fatherName LIKE %:search% OR i.grandFatherName LIKE %:search% OR i.insuranceId LIKE %:search%) " +
//            "AND edg.isDeleted = false " +
//            "AND eig.isDeleted = false")
//    Page<EmployeeInsuredGroup> searchByGroupAndInsuredName(
//            @Param("groupUuid") String groupUuid,
//            @Param("search") String search,
//            Pageable pageable);
//
//    @Query("SELECT COUNT(eig) FROM EmployeeInsuredGroup eig " +
//            "WHERE eig.groupUuid = :groupUuid " +
//            "AND eig.isDeleted = false")
//    Long countByGroupUuid(@Param("groupUuid") String groupUuid);
//
//    void deleteByEmployeeInsuredUuidAndGroupUuid(String insuredUuid, String groupUuid);
//
//    List<EmployeeDependantGroup> findByInsuredAndIsDeleted(String employeeInsuredUuid, boolean isDeleted);
//}