package com.medco.HealthConnectProvider.repository.integration;

import com.medco.HealthConnectProvider.entity.integration.FailedExternalClaimLog;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FailedExternalClaimLogRepository extends JpaRepository<FailedExternalClaimLog, Long> {

    Optional<FailedExternalClaimLog> findByLogUuid(String logUuid);

    List<FailedExternalClaimLog> findByClaimUuid(String claimUuid);

    List<FailedExternalClaimLog> findByBatchCode(String batchCode);

    @Query("SELECT f FROM FailedExternalClaimLog f " +
           "WHERE f.status = :status " +
           "AND f.nextRetryAt <= :currentTime " +
           "AND f.retryCount < f.maxRetries " +
           "AND f.isDeleted = false " +
           "ORDER BY f.nextRetryAt ASC")
    List<FailedExternalClaimLog> findPendingRetries(
            @Param("status") Status status,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT f FROM FailedExternalClaimLog f " +
           "WHERE f.status = :status " +
           "AND f.isDeleted = false " +
           "ORDER BY f.firstFailedAt DESC")
    List<FailedExternalClaimLog> findByStatus(@Param("status") Status status);

    @Query("SELECT COUNT(f) FROM FailedExternalClaimLog f " +
           "WHERE f.status = :status " +
           "AND f.isDeleted = false")
    Long countByStatus(@Param("status") Status status);

    @Query("SELECT f FROM FailedExternalClaimLog f " +
           "WHERE f.retryCount >= f.maxRetries " +
           "AND f.status = 'ACTIVE' " +
           "AND f.isDeleted = false")
    List<FailedExternalClaimLog> findMaxRetriesReached();

    @Query("SELECT f FROM FailedExternalClaimLog f " +
           "WHERE f.firstFailedAt BETWEEN :startDate AND :endDate " +
           "AND f.isDeleted = false " +
           "ORDER BY f.firstFailedAt DESC")
    List<FailedExternalClaimLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FailedExternalClaimLog f " +
           "WHERE f.claimUuid = :claimUuid " +
           "AND f.status = 'COMPLETED' " +
           "AND f.isDeleted = false")
    boolean existsByClaimUuidAndStatusCompleted(@Param("claimUuid") String claimUuid);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FailedExternalClaimLog f " +
           "WHERE f.batchCode = :batchCode " +
           "AND f.status = 'COMPLETED' " +
           "AND f.isDeleted = false")
    boolean existsByBatchCodeAndStatusCompleted(@Param("batchCode") String batchCode);
}
