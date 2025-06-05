//package com.medco.HealthConnectProvider.repository.claims;
//
//import com.medco.HealthConnectProvider.entity.claims.Claim;
//import com.medco.HealthConnectProvider.entity.claims.ClaimItem;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface ClaimItemRepository extends JpaRepository<ClaimItem, Long> {
//
//    /**
//     * Find a claim item by its UUID
//     * @param itemUuid The UUID of the claim item
//     * @return The claim item if found
//     */
//    Optional<ClaimItem> findByItemUuid(String itemUuid);
//
//    /**
//     * Find all claim items for a specific claim
//     * @param claim The claim
//     * @return List of claim items
//     */
//    List<ClaimItem> findByClaim(Claim claim);
//
//    /**
//     * Find all claim items for a specific claim UUID
//     * @param claimUuid The UUID of the claim
//     * @return List of claim items
//     */
//    List<ClaimItem> findByClaimClaimUuid(String claimUuid);
//
//    /**
//     * Count the number of items in a claim
//     * @param claimUuid The UUID of the claim
//     * @return The count of items
//     */
//    int countByClaimClaimUuid(String claimUuid);
//
//    /**
//     * Calculate the total amount for all items in a claim
//     * @param claimUuid The UUID of the claim
//     * @return The total amount
//     */
//    @Query("SELECT SUM(ci.totalPrice) FROM ClaimItem ci WHERE ci.claim.claimUuid = :claimUuid")
//    Double sumTotalPriceByClaimUuid(@Param("claimUuid") String claimUuid);
//
//    /**
//     * Calculate the total insurance coverage for all items in a claim
//     * @param claimUuid The UUID of the claim
//     * @return The total insurance coverage
//     */
//    @Query("SELECT SUM(ci.insuranceCoverage) FROM ClaimItem ci WHERE ci.claim.claimUuid = :claimUuid")
//    Double sumInsuranceCoverageByClaimUuid(@Param("claimUuid") String claimUuid);
//
//    /**
//     * Calculate the total patient responsibility for all items in a claim
//     * @param claimUuid The UUID of the claim
//     * @return The total patient responsibility
//     */
//    @Query("SELECT SUM(ci.patientResponsibility) FROM ClaimItem ci WHERE ci.claim.claimUuid = :claimUuid")
//    Double sumPatientResponsibilityByClaimUuid(@Param("claimUuid") String claimUuid);
//
//    /**
//     * Delete all items for a specific claim
//     * @param claimUuid The UUID of the claim
//     */
//    void deleteByClaimClaimUuid(String claimUuid);
//}