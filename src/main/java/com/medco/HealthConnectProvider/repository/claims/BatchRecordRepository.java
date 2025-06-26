package com.medco.HealthConnectProvider.repository.claims;

import com.medco.HealthConnectProvider.entity.claims.BatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BatchRecordRepository extends JpaRepository<BatchRecord, Long>, JpaSpecificationExecutor<BatchRecord> {

    Optional<BatchRecord> findByBatchCode(String batchCode);

    Optional<BatchRecord> findByClaimUuid(String claimUuid);

    Optional<BatchRecord> findByClaim_ClaimUuid(String claimUuid);

    @Query("SELECT MAX(b.batchNumber) FROM BatchRecord b")
    Long findMaxBatchNumber();

}