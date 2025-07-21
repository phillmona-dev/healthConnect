package com.medco.HealthConnectProvider.repository.claims;

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
////
////    @Query(value = """
////    SELECT DISTINCT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(
////        c.claimUuid,
////        b.batchCode,
////        b.claimDatingFrom,
////        b.claimDatingTo,
////        (SELECT m.dispensingUuid FROM MedicationDispensing m WHERE m.batchRecord = b AND m.dispensingUuid IS NOT NULL ORDER BY m.dispensingUuid ASC LIMIT 1),
////        p.payerUuid,
////        p.payerName,
////        pr.providerUuid,
////        pr.providerName,
////        ch.contractHeaderUuid,
////        ch.contractCode,
////        c.mrnNumber,
////        c.claimNumber,
////        c.visitDate,
////        c.totalAmount,
////        c.status,
////        c.submissionDate,
////        (SELECT COUNT(a) FROM Attachment a WHERE a.claim = c),
////        (SELECT COUNT(cm) FROM Comment cm WHERE cm.claim = c),
////        (SELECT COUNT(md) FROM MedicationDispensing md WHERE md.batchRecord = b)
////    )
////    FROM Claim c
////    LEFT JOIN c.batchRecord b
////    LEFT JOIN b.medicationDispensing m
////    LEFT JOIN m.items i
////    LEFT JOIN i.contractDetail cd
////    LEFT JOIN cd.contractHeader ch
////    LEFT JOIN ch.payer p
////    LEFT JOIN ch.provider pr
////    WHERE c.payerUuid = :payerUuid
////    """)
////    Page<ClaimCustomResponse> findAllPayerClaims(@Param("payerUuid") String payerUuid, Pageable pageable);
//
////    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
////            "c.claimUuid, b.batchCode, b.claimDatingFrom, b.claimDatingTo, " +
////            "m.dispensingUuid, p.payerUuid, p.payerName, pr.providerUuid, " +
////            "pr.providerName, ch.contractHeaderUuid, ch.contractCode, " +
////            "c.mrnNumber, c.claimNumber, c.visitDate, c.totalAmount, " +
////            "c.status, c.submissionDate, SIZE(c.attachments), " +
////            "SIZE(c.comments), SIZE(b.medicationDispensing)) " +
////            "FROM Claim c " +
////            "LEFT JOIN c.batchRecord b " +
////            "LEFT JOIN b.medicationDispensing m " +
////            "LEFT JOIN m.items i " +
////            "LEFT JOIN i.contractDetail cd " +
////            "LEFT JOIN cd.contractHeader ch " +
////            "LEFT JOIN ch.payer p " +
////            "LEFT JOIN ch.provider pr " +
////            "WHERE c.payerUuid = :payerUuid ")
//////            "WHERE (:status IS NULL OR c.status = :status)",
//////            countQuery = "SELECT COUNT(c) FROM Claim c " +
//////                    "WHERE (:status IS NULL OR c.status = :status)")
////    Page<ClaimCustomResponse> findAllPayerClaims(
////
////            @Param("payerUuid") String payerUuid,
////            Pageable pageable);


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

//
//    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
//            "c.claimUuid, " +
//            "b.batchCode, " +
//            "b.claimDatingFrom, " +
//            "b.claimDatingTo, " +
//            "m.dispensingUuid, " +
//            "p.payerUuid, " +
//            "p.payerName, " +
//            "pr.providerUuid, " +
//            "pr.providerName, " +
//            "ch.contractHeaderUuid, " +
//            "ch.contractCode, " +
//            "c.mrnNumber, " +
//            "c.claimNumber, " +
//            "c.visitDate, " +
//            "c.totalAmount, " +
//            "c.status, " +
//            "c.submissionDate, " +
//            "SIZE(c.attachments), " +
//            "SIZE(c.comments), " +
//            "SIZE(b.medicationDispensing)" +
//            ") " +
//            "FROM Claim c " +
//            "LEFT JOIN c.batchRecord b " +
//            "LEFT JOIN b.medicationDispensing m " +
//            "LEFT JOIN m.items i " +
//            "LEFT JOIN i.contractDetail cd " +
//            "LEFT JOIN cd.contractHeader ch " +
//            "LEFT JOIN ch.payer p " +
//            "LEFT JOIN ch.provider pr",
//            countQuery = "SELECT COUNT(c) FROM Claim c")
//    Page<ClaimCustomResponse> findAllClaims(@Param("payerUuid")String payerUuid, Pageable pageable);

//
//    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
//            "c.claimUuid, " +
//            "b.batchCode, " +
//            "b.claimDatingFrom, " +
//            "b.claimDatingTo, " +
//            "m.dispensingUuid, " +
//            "p.payerUuid, " +
//            "p.payerName, " +
//            "pr.providerUuid, " +
//            "pr.providerName, " +
//            "ch.contractHeaderUuid, " +
//            "ch.contractCode, " +
//            "c.mrnNumber, " +
//            "c.claimNumber, " +
//            "c.visitDate, " +
//            "c.totalAmount, " +
//            "c.status, " +
//            "c.submissionDate, " +
//            "SIZE(c.attachments), " +
//            "SIZE(c.comments), " +
//            "SIZE(b.medicationDispensing)" +
//            ") " +
//            "FROM Claim c " +
//            "LEFT JOIN c.batchRecord b " +
//            "LEFT JOIN b.medicationDispensing m " +
//            "LEFT JOIN m.items i " +
//            "LEFT JOIN i.contractDetail cd " +
//            "LEFT JOIN cd.contractHeader ch " +
//            "LEFT JOIN ch.payer p " +
//            "LEFT JOIN ch.provider pr",
//            countQuery = "SELECT COUNT(c) FROM Claim c")
//    Page<ClaimCustomResponse> findAllClaims(@Param("payerUuid")String payerUuid, Pageable pageable);

//    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimCustomResponse(" +
//            "c.claimUuid, " +
//            "b.batchCode, " +
//            "b.claimDatingFrom, " +
//            "b.claimDatingTo, " +
//            "c.payerUuid, " +
//            "c.providerUuid, " +
//            "c.mrnNumber, " +
//            "c.claimNumber, " +
//            "c.visitDate, " +
//            "c.totalAmount, " +
//            "c.status, " +
//            "c.submissionDate, " +
//            "SIZE(c.attachments), " +
//            "SIZE(c.comments), " +
//            "SIZE(b.medicationDispensing)" +
//            ") " +
//            "FROM Claim c " +
//            "LEFT JOIN c.batchRecord b " ,
//            countQuery = "SELECT COUNT(c) FROM Claim c")
//    Page<ClaimCustomResponse> findAllClaims(@Param("payerUuid")String payerUuid, Pageable pageable);
//
//

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

    int countByCreatedAtBetween(Instant startInstant, Instant endInstant);

}

