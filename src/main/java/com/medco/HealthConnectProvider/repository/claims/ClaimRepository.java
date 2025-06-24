package com.medco.HealthConnectProvider.repository.claims;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimUuid(String claimUuid);
    Page<Claim> findByProviderUuid(String providerUuid, Pageable pageable);
    Page<Claim> findByPayerUuid(String payerUuid, Pageable pageable);
    Page<Claim> findByStatus(String status, Pageable pageable);
    Page<Claim> findByProviderUuidAndStatus(String providerUuid, ClaimStatus status, Pageable pageable);
    Page<Claim> findByPayerUuidAndStatus(String payerUuid, ClaimStatus status, Pageable pageable);
    Long countByProviderUuid(String providerUuid);
    Long countByPayerUuid(String payerUuid);
    Long countByStatus(String status);
}