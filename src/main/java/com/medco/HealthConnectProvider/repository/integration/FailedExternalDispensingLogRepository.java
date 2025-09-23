package com.medco.HealthConnectProvider.repository.integration;

import com.medco.HealthConnectProvider.entity.integration.FailedExternalDispensingLog;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FailedExternalDispensingLogRepository extends JpaRepository<FailedExternalDispensingLog, Long> {

    Optional<FailedExternalDispensingLog> findByLogUuid(String logUuid);

    List<FailedExternalDispensingLog> findByDispensingItemUuid(String dispensingItemUuid);

    List<FailedExternalDispensingLog> findByDispensingUuid(String dispensingUuid);

    @Query("SELECT f FROM FailedExternalDispensingLog f " +
           "WHERE f.status = :status " +
           "AND f.nextRetryAt <= :currentTime " +
           "AND f.retryCount < f.maxRetries " +
           "AND f.isDeleted = false " +
           "ORDER BY f.nextRetryAt ASC")
    List<FailedExternalDispensingLog> findPendingRetries(
            @Param("status") Status status,
            @Param("currentTime") LocalDateTime currentTime);

    @Query("SELECT f FROM FailedExternalDispensingLog f " +
           "WHERE f.status = :status " +
           "AND f.isDeleted = false " +
           "ORDER BY f.firstFailedAt DESC")
    List<FailedExternalDispensingLog> findByStatus(@Param("status") Status status);

    @Query("SELECT COUNT(f) FROM FailedExternalDispensingLog f " +
           "WHERE f.status = :status " +
           "AND f.isDeleted = false")
    Long countByStatus(@Param("status") Status status);

    @Query("SELECT f FROM FailedExternalDispensingLog f " +
           "WHERE f.retryCount >= f.maxRetries " +
           "AND f.status = 'ACTIVE' " +
           "AND f.isDeleted = false")
    List<FailedExternalDispensingLog> findMaxRetriesReached();

    @Query("SELECT f FROM FailedExternalDispensingLog f " +
           "WHERE f.firstFailedAt BETWEEN :startDate AND :endDate " +
           "AND f.isDeleted = false " +
           "ORDER BY f.firstFailedAt DESC")
    List<FailedExternalDispensingLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Check if a dispensingUuid has been successfully sent to external system (COMPLETED status)
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FailedExternalDispensingLog f " +
           "WHERE f.dispensingUuid = :dispensingUuid " +
           "AND f.status = 'COMPLETED' " +
           "AND f.isDeleted = false")
    boolean existsByDispensingUuidAndStatusCompleted(@Param("dispensingUuid") String dispensingUuid);
}
