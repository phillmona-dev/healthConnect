package com.medco.HealthConnectProvider.repository.claims;

import com.medco.HealthConnectProvider.entity.claims.ClaimLogs;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClaimLogsRepository extends JpaRepository<ClaimLogs, Long> {
    List<ClaimLogs> findByClaimClaimUuidOrderByActionDateDesc(String claimUuid);
    Page<ClaimLogs> findByClaimClaimUuidOrderByActionDateDesc(String claimUuid, Pageable pageable);
}