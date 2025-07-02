package com.medco.HealthConnectProvider.repository.claims;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse;
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


    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
            "c.claimUuid, b.batchCode, b.claimDatingFrom, b.claimDatingTo, " +
            "m.dispensingUuid, p.payerUuid, p.payerName, pr.providerUuid, " +
            "pr.providerName, ch.contractHeaderUuid, ch.contractCode, " +
            "c.mrnNumber, c.claimNumber, c.visitDate, c.totalAmount, " +
            "c.status, c.submissionDate, SIZE(c.attachments), " +
            "SIZE(c.comments), SIZE(b.medicationDispensing)) " +
            "FROM Claim c " +
            "LEFT JOIN c.batchRecord b " +
            "LEFT JOIN b.medicationDispensing m " +
            "LEFT JOIN m.items i " +
            "LEFT JOIN i.contractDetail cd " +
            "LEFT JOIN cd.contractHeader ch " +
            "LEFT JOIN ch.payer p " +
            "LEFT JOIN ch.provider pr " +
            "WHERE c.payerUuid = :payerUuid AND c.providerUuid = :providerUuid")
//            "WHERE (:status IS NULL OR c.status = :status)",
//            countQuery = "SELECT COUNT(c) FROM Claim c " +
//                    "WHERE (:status IS NULL OR c.status = :status)")
    Page<ClaimListResponse> findAllPayerProviderClaims(

            @Param("payerUuid") String payerUuid,
            @Param("providerUuid") String providerUuid,
            Pageable pageable);


    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
            "c.claimUuid, b.batchCode, b.claimDatingFrom, b.claimDatingTo, " +
            "m.dispensingUuid, p.payerUuid, p.payerName, pr.providerUuid, " +
            "pr.providerName, ch.contractHeaderUuid, ch.contractCode, " +
            "c.mrnNumber, c.claimNumber, c.visitDate, c.totalAmount, " +
            "c.status, c.submissionDate, SIZE(c.attachments), " +
            "SIZE(c.comments), SIZE(b.medicationDispensing)) " +
            "FROM Claim c " +
            "LEFT JOIN c.batchRecord b " +
            "LEFT JOIN b.medicationDispensing m " +
            "LEFT JOIN m.items i " +
            "LEFT JOIN i.contractDetail cd " +
            "LEFT JOIN cd.contractHeader ch " +
            "LEFT JOIN ch.payer p " +
            "LEFT JOIN ch.provider pr " +
            "WHERE c.status = :status AND c.payerUuid = :payerUuid AND c.providerUuid = :providerUuid")
//            "WHERE (:status IS NULL OR c.status = :status)",
//            countQuery = "SELECT COUNT(c) FROM Claim c " +
//                    "WHERE (:status IS NULL OR c.status = :status)")
    Page<ClaimListResponse> findAllPayerProviderClaimsByStatus(

            @Param("payerUuid") String payerUuid,
            @Param("providerUuid") String providerUuid,
            @Param("status") ClaimStatus status,
            Pageable pageable);


    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
            "c.claimUuid, b.batchCode, b.claimDatingFrom, b.claimDatingTo, " +
            "m.dispensingUuid, p.payerUuid, p.payerName, pr.providerUuid, " +
            "pr.providerName, ch.contractHeaderUuid, ch.contractCode, " +
            "c.mrnNumber, c.claimNumber, c.visitDate, c.totalAmount, " +
            "c.status, c.submissionDate, SIZE(c.attachments), " +
            "SIZE(c.comments), SIZE(b.medicationDispensing)) " +
            "FROM Claim c " +
            "LEFT JOIN c.batchRecord b " +
            "LEFT JOIN b.medicationDispensing m " +
            "LEFT JOIN m.items i " +
            "LEFT JOIN i.contractDetail cd " +
            "LEFT JOIN cd.contractHeader ch " +
            "LEFT JOIN ch.payer p " +
            "LEFT JOIN ch.provider pr " +
            "WHERE c.status = :status AND c.payerUuid = :payerUuid ")
//            "WHERE (:status IS NULL OR c.status = :status)",
//            countQuery = "SELECT COUNT(c) FROM Claim c " +
//                    "WHERE (:status IS NULL OR c.status = :status)")
    Page<ClaimListResponse> findAllPayerClaimsByStatus(

            @Param("payerUuid") String payerUuid,
            @Param("status") ClaimStatus status,
            Pageable pageable);


    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
            "c.claimUuid, b.batchCode, b.claimDatingFrom, b.claimDatingTo, " +
            "m.dispensingUuid, p.payerUuid, p.payerName, pr.providerUuid, " +
            "pr.providerName, ch.contractHeaderUuid, ch.contractCode, " +
            "c.mrnNumber, c.claimNumber, c.visitDate, c.totalAmount, " +
            "c.status, c.submissionDate, SIZE(c.attachments), " +
            "SIZE(c.comments), SIZE(b.medicationDispensing)) " +
            "FROM Claim c " +
            "LEFT JOIN c.batchRecord b " +
            "LEFT JOIN b.medicationDispensing m " +
            "LEFT JOIN m.items i " +
            "LEFT JOIN i.contractDetail cd " +
            "LEFT JOIN cd.contractHeader ch " +
            "LEFT JOIN ch.payer p " +
            "LEFT JOIN ch.provider pr " +
            "WHERE c.status = :status AND c.providerUuid = :providerUuid ")
//            "WHERE (:status IS NULL OR c.status = :status)",
//            countQuery = "SELECT COUNT(c) FROM Claim c " +
//                    "WHERE (:status IS NULL OR c.status = :status)")
    Page<ClaimListResponse> findAllProviderClaimsByStatus(

            @Param("providerUuid") String providerUuid,
            @Param("status") ClaimStatus status,
            Pageable pageable);


    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
            "c.claimUuid, b.batchCode, b.claimDatingFrom, b.claimDatingTo, " +
            "m.dispensingUuid, p.payerUuid, p.payerName, pr.providerUuid, " +
            "pr.providerName, ch.contractHeaderUuid, ch.contractCode, " +
            "c.mrnNumber, c.claimNumber, c.visitDate, c.totalAmount, " +
            "c.status, c.submissionDate, SIZE(c.attachments), " +
            "SIZE(c.comments), SIZE(b.medicationDispensing)) " +
            "FROM Claim c " +
            "LEFT JOIN c.batchRecord b " +
            "LEFT JOIN b.medicationDispensing m " +
            "LEFT JOIN m.items i " +
            "LEFT JOIN i.contractDetail cd " +
            "LEFT JOIN cd.contractHeader ch " +
            "LEFT JOIN ch.payer p " +
            "LEFT JOIN ch.provider pr " +
            "WHERE c.payerUuid = :payerUuid ")
//            "WHERE (:status IS NULL OR c.status = :status)",
//            countQuery = "SELECT COUNT(c) FROM Claim c " +
//                    "WHERE (:status IS NULL OR c.status = :status)")
    Page<ClaimListResponse> findAllPayerClaims(

            @Param("payerUuid") String payerUuid,
            Pageable pageable);


    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
            "c.claimUuid, b.batchCode, b.claimDatingFrom, b.claimDatingTo, " +
            "m.dispensingUuid, p.payerUuid, p.payerName, pr.providerUuid, " +
            "pr.providerName, ch.contractHeaderUuid, ch.contractCode, " +
            "c.mrnNumber, c.claimNumber, c.visitDate, c.totalAmount, " +
            "c.status, c.submissionDate, SIZE(c.attachments), " +
            "SIZE(c.comments), SIZE(b.medicationDispensing)) " +
            "FROM Claim c " +
            "LEFT JOIN c.batchRecord b " +
            "LEFT JOIN b.medicationDispensing m " +
            "LEFT JOIN m.items i " +
            "LEFT JOIN i.contractDetail cd " +
            "LEFT JOIN cd.contractHeader ch " +
            "LEFT JOIN ch.payer p " +
            "LEFT JOIN ch.provider pr " +
            "WHERE c.providerUuid = :providerUuid ")
//            "WHERE (:status IS NULL OR c.status = :status)",
//            countQuery = "SELECT COUNT(c) FROM Claim c " +
//                    "WHERE (:status IS NULL OR c.status = :status)")
    Page<ClaimListResponse> findAllProviderClaims(

            @Param("providerUuid") String providerUuid,
            Pageable pageable);

//
//    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
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
//    Page<ClaimListResponse> findAllClaims(@Param("payerUuid")String payerUuid, Pageable pageable);

//
//    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
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
//    Page<ClaimListResponse> findAllClaims(@Param("payerUuid")String payerUuid, Pageable pageable);

//    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
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
//    Page<ClaimListResponse> findAllClaims(@Param("payerUuid")String payerUuid, Pageable pageable);




}

