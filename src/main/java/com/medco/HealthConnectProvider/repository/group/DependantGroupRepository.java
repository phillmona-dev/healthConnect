//package com.medco.HealthConnectProvider.repository.group;
//
//import com.medco.HealthConnectProvider.entity.groups.DependantGroup;
//import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
//import com.medco.HealthConnectProvider.entity.persons.Dependant;
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
//public interface DependantGroupRepository extends JpaRepository<DependantGroup, Long> {
//
//    boolean existsByDependantUuidAndGroupUuid(String dependantUuid, String groupUuid);
//
//    Optional<DependantGroup> findByDependantUuidAndGroupUuid(String dependantUuid, String groupUuid);
//
//    List<DependantGroup> findByDependantUuid(String dependantUuid);
//
//    List<DependantGroup> findByGroupUuid(String groupUuid);
//
//    @Query("SELECT dg FROM DependantGroup dg " +
//           "JOIN dg.dependant d " +
//           "JOIN dg.employeeDependantGroup edg " +
//           "WHERE edg.groupUuid = :groupUuid " +
//           "AND (d.firstName LIKE %:search% OR d.fatherName LIKE %:search% OR d.grandFatherName LIKE %:search%) " +
//           "AND edg.isDeleted = false " +
//           "AND dg.isDeleted = false")
//    Page<DependantGroup> searchByGroupAndDependantName(
//            @Param("groupUuid") String groupUuid,
//            @Param("search") String search,
//            Pageable pageable);
//
//    @Query("SELECT COUNT(dg) FROM DependantGroup dg " +
//           "WHERE dg.groupUuid = :groupUuid " +
//           "AND dg.isDeleted = false")
//    Long countByGroupUuid(@Param("groupUuid") String groupUuid);
//
//    void deleteByDependantUuidAndGroupUuid(String dependantUuid, String groupUuid);
//
//    List<DependantGroup> findByDependantUuidAndIsDeleted(String dependantUuid, boolean isDeleted);
//}