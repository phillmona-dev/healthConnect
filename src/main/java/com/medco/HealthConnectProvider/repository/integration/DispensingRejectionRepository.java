package com.medco.HealthConnectProvider.repository.integration;

import com.medco.HealthConnectProvider.entity.integration.DispensingRejection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DispensingRejectionRepository extends JpaRepository<DispensingRejection, Long> {

    Optional<DispensingRejection> findByRejectionUuid(String rejectionUuid);

    List<DispensingRejection> findByDispensingUuid(String dispensingUuid);

    List<DispensingRejection> findByClaimUuid(String claimUuid);

    List<DispensingRejection> findByBatchCode(String batchCode);

    List<DispensingRejection> findByProviderUuid(String providerUuid);

    @Query("SELECT dr FROM DispensingRejection dr " +
           "WHERE dr.rejectionStatus = :status " +
           "AND dr.isDeleted = false " +
           "ORDER BY dr.rejectedAt DESC")
    List<DispensingRejection> findByRejectionStatus(@Param("status") String status);

    @Query("SELECT dr FROM DispensingRejection dr " +
           "WHERE dr.rejectionCategory = :category " +
           "AND dr.isDeleted = false " +
           "ORDER BY dr.rejectedAt DESC")
    List<DispensingRejection> findByRejectionCategory(@Param("category") String category);

    @Query("SELECT dr FROM DispensingRejection dr " +
           "WHERE dr.canResubmit = true " +
           "AND dr.rejectionStatus = 'ACTIVE' " +
           "AND dr.isDeleted = false " +
           "ORDER BY dr.rejectedAt DESC")
    List<DispensingRejection> findResubmittableRejections();

    @Query("SELECT dr FROM DispensingRejection dr " +
           "WHERE dr.rejectedAt BETWEEN :startDate AND :endDate " +
           "AND dr.isDeleted = false " +
           "ORDER BY dr.rejectedAt DESC")
    List<DispensingRejection> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT dr FROM DispensingRejection dr " +
           "WHERE dr.providerUuid = :providerUuid " +
           "AND dr.rejectionStatus = :status " +
           "AND dr.isDeleted = false " +
           "ORDER BY dr.rejectedAt DESC")
    List<DispensingRejection> findByProviderAndStatus(
            @Param("providerUuid") String providerUuid,
            @Param("status") String status);

    @Query("SELECT COUNT(dr) FROM DispensingRejection dr " +
           "WHERE dr.rejectionStatus = :status " +
           "AND dr.isDeleted = false")
    Long countByRejectionStatus(@Param("status") String status);

    @Query("SELECT COUNT(dr) FROM DispensingRejection dr " +
           "WHERE dr.rejectionCategory = :category " +
           "AND dr.isDeleted = false")
    Long countByRejectionCategory(@Param("category") String category);

    @Query("SELECT COUNT(dr) FROM DispensingRejection dr " +
           "WHERE dr.providerUuid = :providerUuid " +
           "AND dr.rejectedAt BETWEEN :startDate AND :endDate " +
           "AND dr.isDeleted = false")
    Long countByProviderAndDateRange(
            @Param("providerUuid") String providerUuid,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Check if a dispensing has been rejected
     */
    @Query("SELECT CASE WHEN COUNT(dr) > 0 THEN true ELSE false END FROM DispensingRejection dr " +
           "WHERE dr.dispensingUuid = :dispensingUuid " +
           "AND dr.rejectionStatus = 'ACTIVE' " +
           "AND dr.isDeleted = false")
    boolean isDispensingRejected(@Param("dispensingUuid") String dispensingUuid);

    /**
     * Get rejection statistics by provider
     */
    @Query("SELECT dr.rejectionCategory, COUNT(dr) FROM DispensingRejection dr " +
           "WHERE dr.providerUuid = :providerUuid " +
           "AND dr.rejectedAt BETWEEN :startDate AND :endDate " +
           "AND dr.isDeleted = false " +
           "GROUP BY dr.rejectionCategory")
    List<Object[]> getRejectionStatsByProvider(
            @Param("providerUuid") String providerUuid,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
