package com.medco.HealthConnectProvider.repository.claims;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimResponse;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {
    Optional<Claim> findByClaimUuid(String claimUuid);
    Page<Claim> findByProviderUuid(String providerUuid, Pageable pageable);
    Page<Claim> findByPayerUuid(String payerUuid, Pageable pageable);
    Page<Claim> findByProviderUuidAndStatus(String providerUuid, ClaimStatus status, Pageable pageable);
    Page<Claim> findByPayerUuidAndStatus(String payerUuid, ClaimStatus status, Pageable pageable);



    @Query(value = """
        SELECT NEW com.your.package.ClaimServiceDto(
            c.id as claimId,
            c.claimNumber,
            s.uuid as providedServiceUuid,
            svc.uuid as serviceUuid,
            svc.name as serviceName,
            svc.code as serviceCode,
            s.quantity,
            s.unitPrice,
            s.totalPrice,
            svc.category as serviceCategory,
            svc.subCategory as serviceSubCategory,
            cd.negotiatedPrice
        )
        FROM Claim c
        LEFT JOIN c.medicationDispensingServices s
        LEFT JOIN s.service svc
        LEFT JOIN s.contractDetails cd
        """,
            countQuery = "SELECT COUNT(c) FROM Claim c")
        Page<ClaimResponse> findClaimServicesByPatientId(
                Pageable pageable);
    }

