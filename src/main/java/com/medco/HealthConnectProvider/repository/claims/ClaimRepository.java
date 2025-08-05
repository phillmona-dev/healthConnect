package com.medco.HealthConnectProvider.repository.claims;

import com.medco.HealthConnectProvider.entity.claims.BatchRecord;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClaimRepository extends JpaRepository<Claim, Long> {

    Optional<Claim> findByClaimUuid(String claimUuid);
    Page<Claim> findByProviderUuid(String providerUuid, Pageable pageable);
    Page<Claim> findByPayerUuid(String payerUuid, Pageable pageable);
    Page<Claim> findByProviderUuidAndStatus(String providerUuid, ClaimStatus status, Pageable pageable);
    Page<Claim> findByPayerUuidAndStatus(String payerUuid, ClaimStatus status, Pageable pageable);


    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
            "c.claimUuid, " +
            "b.batchCode, " +
            "b.claimDatingFrom, " +
            "b.claimDatingTo, " +
            "c.payerUuid, " +
            "c.providerUuid, " +
            "c.mrnNumber, " +
            "c.claimNumber, " +
            "c.visitDate, " +
            "c.totalAmount, " +
            "c.status, " +
            "c.submissionDate, " +
            "SIZE(c.attachments), " +
            "SIZE(c.comments), " +
            "SIZE(b.medicationDispensing)" +
            ") " +
            "FROM Claim c " +
            "LEFT JOIN c.batchRecord b " +
            "WHERE c.payerUuid = :payerUuid AND c.providerUuid = providerUuid ",
            countQuery = "SELECT COUNT(c) FROM Claim c WHERE c.payerUuid = :payerUuid")
    Page<ClaimCustomResponse> findAllPayerProviderClaims(

            @Param("payerUuid") String payerUuid,
            @Param("providerUuid") String providerUuid,
            Pageable pageable);


    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
            "c.claimUuid, " +
            "b.batchCode, " +
            "b.claimDatingFrom, " +
            "b.claimDatingTo, " +
            "c.payerUuid, " +
            "c.providerUuid, " +
            "c.mrnNumber, " +
            "c.claimNumber, " +
            "c.visitDate, " +
            "c.totalAmount, " +
            "c.status, " +
            "c.submissionDate, " +
            "SIZE(c.attachments), " +
            "SIZE(c.comments), " +
            "SIZE(b.medicationDispensing)" +
            ") " +
            "FROM Claim c " +
            "LEFT JOIN c.batchRecord b " +
            "WHERE c.payerUuid = :payerUuid AND c.providerUuid = :providerUuid AND c.status = :status",
            countQuery = "SELECT COUNT(c) FROM Claim c WHERE c.payerUuid = :payerUuid")
    Page<ClaimCustomResponse> findAllPayerProviderClaimsByStatus(

            @Param("payerUuid") String payerUuid,
            @Param("providerUuid") String providerUuid,
            @Param("status") ClaimStatus status,
            Pageable pageable);





@Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
        "c.claimUuid, " +
        "b.batchCode, " +
        "b.claimDatingFrom, " +
        "b.claimDatingTo, " +
        "c.payerUuid, " +
        "c.providerUuid, " +
        "c.mrnNumber, " +
        "c.claimNumber, " +
        "c.visitDate, " +
        "c.totalAmount, " +
        "c.status, " +
        "c.submissionDate, " +
        "SIZE(c.attachments), " +
        "SIZE(c.comments), " +
        "SIZE(b.medicationDispensing)" +
        ") " +
        "FROM Claim c " +
        "LEFT JOIN c.batchRecord b " +
        "WHERE c.payerUuid = :payerUuid AND c.status = :status",
        countQuery = "SELECT COUNT(c) FROM Claim c WHERE c.payerUuid = :payerUuid")
    Page<ClaimCustomResponse> findAllPayerClaimsByStatus(

            @Param("payerUuid") String payerUuid,
            @Param("status") ClaimStatus status,
            Pageable pageable);


    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
            "c.claimUuid, " +
            "b.batchCode, " +
            "b.claimDatingFrom, " +
            "b.claimDatingTo, " +
            "c.payerUuid, " +
            "c.providerUuid, " +
            "c.mrnNumber, " +
            "c.claimNumber, " +
            "c.visitDate, " +
            "c.totalAmount, " +
            "c.status, " +
            "c.submissionDate, " +
            "SIZE(c.attachments), " +
            "SIZE(c.comments), " +
            "SIZE(b.medicationDispensing)" +
            ") " +
            "FROM Claim c " +
            "LEFT JOIN c.batchRecord b " +
            "WHERE c.providerUuid = :providerUuid AND c.status = :status",
            countQuery = "SELECT COUNT(c) FROM Claim c WHERE c.payerUuid = :payerUuid")
    Page<ClaimCustomResponse> findAllProviderClaimsByStatus(

            @Param("providerUuid") String providerUuid,
            @Param("status") ClaimStatus status,
            Pageable pageable);


@Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
        "c.claimUuid, " +
        "b.batchCode, " +
        "b.claimDatingFrom, " +
        "b.claimDatingTo, " +
        "c.payerUuid, " +
        "c.providerUuid, " +
        "c.mrnNumber, " +
        "c.claimNumber, " +
        "c.visitDate, " +
        "c.totalAmount, " +
        "c.status, " +
        "c.submissionDate, " +
        "SIZE(c.attachments), " +
        "SIZE(c.comments), " +
        "SIZE(b.medicationDispensing)" +
        ") " +
        "FROM Claim c " +
        "LEFT JOIN c.batchRecord b " +
        "WHERE c.providerUuid = :providerUuid ",
        countQuery = "SELECT COUNT(c) FROM Claim c WHERE c.payerUuid = :payerUuid")
    Page<ClaimCustomResponse> findAllProviderClaims(

            @Param("providerUuid") String providerUuid,
            Pageable pageable);

    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
            "c.claimUuid, " +
            "b.batchCode, " +
            "b.claimDatingFrom, " +
            "b.claimDatingTo, " +
            "c.payerUuid, " +
            "c.providerUuid, " +
            "c.mrnNumber, " +
            "c.claimNumber, " +
            "c.visitDate, " +
            "c.totalAmount, " +
            "c.status, " +
            "c.submissionDate, " +
            "SIZE(c.attachments), " +
            "SIZE(c.comments), " +
            "SIZE(b.medicationDispensing)" +
            ") " +
            "FROM Claim c " +
            "LEFT JOIN c.batchRecord b " +
            "WHERE c.payerUuid = :payerUuid",
            countQuery = "SELECT COUNT(c) FROM Claim c WHERE c.payerUuid = :payerUuid")
    Page<ClaimCustomResponse> findAllPayerClaims(@Param("payerUuid") String payerUuid, Pageable pageable);

    long countByPayerUuid(String payerUuid);

    long countByProviderUuid(@Size(min = 36, max = 40, message = "Provided Uuid Must be between 36 and 40") String providerUuid);

    long countByStatus(ClaimStatus claimStatus);

    Claim findByBatchRecord(BatchRecord batch);

    int countByProviderUuidAndSubmissionDateBetween(String providerUuid, LocalDateTime startDate, LocalDateTime endDate);

    int countByPayerUuidAndSubmissionDateBetween(String payerUuid, LocalDateTime startDate, LocalDateTime endDate);


    int countBySubmissionDateBetween(LocalDateTime startDate, LocalDateTime endDate);

    int countByProviderUuidAndStatus(String providerUuid, ClaimStatus status);

    int countByPayerUuidAndStatus(String payerUuid, ClaimStatus status);

    @Query("SELECT new com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
            "c.claimUuid, b.batchCode, b.claimDatingFrom, b.claimDatingTo, " +
            "c.payerUuid, c.providerUuid, c.mrnNumber, c.claimNumber, c.visitDate, " +
            "c.totalAmount, c.status, c.submissionDate, " +
            "(SELECT COUNT(ca) FROM ClaimAttachment ca WHERE ca.claim = c), " +
            "(SELECT COUNT(cc) FROM ClaimComment cc WHERE cc.claim = c), " +
            "(SELECT COUNT(cl) FROM ClaimLogs cl WHERE cl.claim = c)) " +
            "FROM Claim c LEFT JOIN c.batchRecord b " +
            "WHERE c.payerUuid = :payerUuid AND c.status IN :statuses " +
            "ORDER BY c.createdAt DESC")
    Page<ClaimCustomResponse> findAllPayerClaimsByStatusIn(
            @Param("payerUuid") String payerUuid,
            @Param("statuses") List<ClaimStatus> statuses,
            Pageable pageable);

    @Query("SELECT new com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
            "c.claimUuid, b.batchCode, b.claimDatingFrom, b.claimDatingTo, " +
            "c.payerUuid, c.providerUuid, c.mrnNumber, c.claimNumber, c.visitDate, " +
            "c.totalAmount, c.status, c.submissionDate, " +
            "(SELECT COUNT(ca) FROM ClaimAttachment ca WHERE ca.claim = c), " +
            "(SELECT COUNT(cc) FROM ClaimComment cc WHERE cc.claim = c), " +
            "(SELECT COUNT(cl) FROM ClaimLogs cl WHERE cl.claim = c)) " +
            "FROM Claim c LEFT JOIN c.batchRecord b " +
            "WHERE c.providerUuid = :providerUuid AND c.status IN :statuses " +
            "ORDER BY c.createdAt DESC")
    Page<ClaimCustomResponse> findAllProviderClaimsByStatusIn(
            @Param("providerUuid") String providerUuid,
            @Param("statuses") List<ClaimStatus> statuses,
            Pageable pageable);

    @Query("SELECT new com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
            "c.claimUuid, b.batchCode, b.claimDatingFrom, b.claimDatingTo, " +
            "c.payerUuid, c.providerUuid, c.mrnNumber, c.claimNumber, c.visitDate, " +
            "c.totalAmount, c.status, c.submissionDate, " +
            "(SELECT COUNT(ca) FROM ClaimAttachment ca WHERE ca.claim = c), " +
            "(SELECT COUNT(cc) FROM ClaimComment cc WHERE cc.claim = c), " +
            "(SELECT COUNT(cl) FROM ClaimLogs cl WHERE cl.claim = c)) " +
            "FROM Claim c LEFT JOIN c.batchRecord b " +
            "WHERE c.payerUuid = :payerUuid AND c.providerUuid = :providerUuid AND c.status IN :statuses " +
            "ORDER BY c.createdAt DESC")
    Page<ClaimCustomResponse> findAllPayerProviderClaimsByStatusIn(
            @Param("payerUuid") String payerUuid,
            @Param("providerUuid") String providerUuid,
            @Param("statuses") List<ClaimStatus> statuses,
            Pageable pageable);

}

