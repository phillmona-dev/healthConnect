package com.medco.HealthConnectProvider.repository.claims;

import com.medco.HealthConnectProvider.entity.claims.ClaimPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClaimPaymentRepository extends JpaRepository<ClaimPayment, Long> {
    Optional<ClaimPayment> findByPaymentUuid(String paymentUuid);
    Optional<ClaimPayment> findByClaimClaimUuid(String claimUuid);
}