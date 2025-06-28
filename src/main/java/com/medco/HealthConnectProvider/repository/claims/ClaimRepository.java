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



//
//    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
//            "c.claimUuid, " +
//            "COALESCE(b.batchNumber, 0L), " +  // Handle null batchNumber
//            "b.claimDatingFrom, " +
//            "b.claimDatingTo, " +
//            "c.mrnNumber, " +
//            "c.claimNumber, " +
//            "c.visitDate, " +
//            "COALESCE(c.totalAmount, 0), " +  // Handle null totalAmount
//            "c.status, " +
//            "c.submissionDate, " +
//            "SIZE(c.attachments), " +
//            "SIZE(c.comments)" +
//            ") " +
//            "FROM Claim c " +
//            "LEFT JOIN c.batchRecord b",
//            countQuery = "SELECT COUNT(c) FROM Claim c")
//    Page<ClaimListResponse> findAllClaims(Pageable pageable);




//        @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
//                "c.claimUuid, " +
//    //            "p.payerUuid, " +
//    //            "p.payerName, " +
//    //            "pr.providerUuid, " +
//    //            "pr.providerName, " +
//    //            "ch.contractHeaderUuid, " +
//    //            "ch.contractCode, " +
//                "b.batchNumber, " +
//                "b.claimDatingFrom, " +
//                "b.claimDatingTo, " +
//                "c.mrnNumber, " +
//                "c.claimNumber, " +
//                "c.visitDate, " +
//                "c.totalAmount, " +
//                "c.status, " +
//                "c.submissionDate, " +
//                "SIZE(c.attachments), " +
//                "SIZE(c.comments)" +
//                ") " +
//                "FROM Claim c " +
//                "LEFT JOIN c.batchRecord b " ,
//    //            "LEFT JOIN b.medicationDispensing m " +
//    //            "LEFT JOIN m.items i " +  // Verify this matches your entity's property name
//    //            "JOIN i.contractDetail cd " +  // Changed to INNER JOIN (since nullable=false)
//    //            "JOIN cd.contractHeader ch " +  // Changed to INNER JOIN
//    //            "LEFT JOIN ch.payer p " +  // Keep LEFT JOIN for payer
//    //            "LEFT JOIN ch.provider pr",  // Keep LEFT JOIN for provider
//                countQuery = "SELECT COUNT(c) FROM Claim c")
//        Page<ClaimListResponse> findAllClaims(Pageable pageable);



    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
            "c.claimUuid, " +
            "b.batchCode, " +
            "b.claimDatingFrom, " +
            "b.claimDatingTo, " +
            "m.dispensingUuid, " +
            "p.payerUuid, " +
            "p.payerName, " +
            "pr.providerUuid, " +
            "pr.providerName, " +
            "ch.contractHeaderUuid, " +
            "ch.contractCode, " +
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
            "LEFT JOIN b.medicationDispensing m " +
            "LEFT JOIN m.items i " +
            "LEFT JOIN i.contractDetail cd " +
            "LEFT JOIN cd.contractHeader ch " +
            "LEFT JOIN ch.payer p " +
            "LEFT JOIN ch.provider pr",
            countQuery = "SELECT COUNT(c) FROM Claim c")
    Page<ClaimListResponse> findAllClaims(Pageable pageable);




//    @Query(value = "SELECT NEW com.medco.HealthConnectProvider.ui.response.claims.ClaimListResponse(" +
//            "c.claimUuid, " +
//            "p.payerUuid, " +
//            "p.payerName, " +
//            "pr.providerUuid, " +
//            "pr.providerName, " +
//            "ch.contractHeaderUuid, " +
//            "ch.contractCode, " +
////            "i.insuredUuid, " +
////            "i.firstName, " +
////            "i.dependant.dependantUuid, " +
////            "i.dependant.firstName, " +
//            "c.mrnNumber, " +
//            "c.claimNumber, " +
//            "c.visitDate, " +
//            "c.totalAmount, " +
//            "c.status, " +
//            "c.submissionDate, " +
//            "SIZE(c.attachments), " +
//            "SIZE(c.comments) " +
//            ") " +
//            "FROM Claim c " +
//            "LEFT JOIN c.batchRecord b " +
//            "LEFT JOIN b.medicationDispensing m " +
//            "LEFT JOIN m.items i " +
//            "LEFT JOIN i.contractDetail cd " +
//            "LEFT JOIN cd.contractHeader ch " +
//            "LEFT JOIN ch.payer p " +
//            "LEFT JOIN ch.provider pr " +
//            "WHERE i.patient.patientId = :patientId",
//            countQuery = "SELECT COUNT(c) FROM Claim c WHERE c.items.patient.patientId = :patientId")
//    Page<ClaimListResponse> findClaimServicesByPatientId(Pageable pageable);

}

