package com.medco.HealthConnectProvider.repository.contract;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.ui.response.payer.PayerProviderResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PolicyHolderListResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<ContractHeader, Long> {

    /**
     * Find policy holders list
     *
     * @param search   Search term for filtering
     * @param status   Status of the contracts
     * @param pageable Pagination information
     * @return List of PolicyHolderListResponse objects
     */
    @Query("SELECT new com.medco.HealthConnectProvider.ui.response.payer.PolicyHolderListResponse(" + "p.payerUuid, " + "p.payerName, " + "p.description, " +
            "p.telephone, " + "p.tinNumber, " + "p.category, " + "p.payerInsuranceNumber, " + "p.address1, " + "p.address2, " + "p.address3, " +
            "p.state, " + "p.country, " + "ch.status, " + "p.referralType, " + "p.referredBy) " + "FROM ContractHeader ch " + "JOIN ch.payer p " +
            "WHERE ch.isDeleted = false " + "AND ch.status = :status " + "AND (:search IS NULL OR p.payerName LIKE %:search% OR p.payerInsuranceNumber LIKE %:search% OR p.telephone LIKE %:search%) " + "GROUP BY p.payerUuid " +
            "ORDER BY p.payerName ASC")
    List<PolicyHolderListResponse> findPolicyHoldersList(
            @Param("search") String search,
            @Param("status") Status status,
            Pageable pageable);

    /**
     * Find payers associated with a specific provider
     *
     * @param providerUuid UUID of the provider
     * @param payerUuid    UUID of the payer (optional filter)
     * @return List of PayerProviderResponse objects
     */
    @Query("SELECT new com.medco.HealthConnectProvider.ui.response.payer.PayerProviderResponse(" + "p.payerUuid, " + "p.payerName, " + "p.payerInsuranceNumber) " +
            "FROM ContractHeader ch " + "JOIN ch.payer p " + "JOIN ch.provider pr " + "WHERE ch.isDeleted = false " + "AND pr.providerUuid = :providerUuid " +
            "AND (:payerUuid IS NULL OR p.payerUuid = :payerUuid) " + "GROUP BY p.payerUuid " + "ORDER BY p.payerName ASC")
    List<PayerProviderResponse> findProviderPolicyHolders(
            @Param("providerUuid") String providerUuid,
            @Param("payerUuid") String payerUuid);

    ContractHeader findByContractHeaderUuid(String contractHeaderUuid);

    @Query("SELECT CASE WHEN COUNT(ch) > 0 THEN true ELSE false END FROM ContractHeader ch " +
            "WHERE ch.provider.providerUuid = :providerUuid")
    boolean existsByProviderProviderUuid(@Param("providerUuid") String providerUuid);

    @Query("SELECT ch FROM ContractHeader ch WHERE ch.provider.providerUuid = :providerUuid AND ch.isDeleted = :isDeleted")
    Page<ContractHeader> findAllByProviderProviderUuidAndIsDeleted(
            @Param("providerUuid") String providerUuid,
            @Param("isDeleted") boolean isDeleted,
            Pageable pageable);

    @Query("SELECT ch FROM ContractHeader ch WHERE ch.provider.providerUuid = :providerUuid AND ch.isDeleted = :isDeleted AND ch.payer.payerName LIKE %:searchKey%")
    Page<ContractHeader> findAllByProviderProviderUuidAndIsDeletedAndPayerPayerNameContaining(
            @Param("providerUuid") String providerUuid,
            @Param("isDeleted") boolean isDeleted,
            @Param("searchKey") String searchKey,
            Pageable pageable);

    @Query("SELECT ch FROM ContractHeader ch WHERE ch.provider.providerUuid = :providerUuid AND ch.isDeleted = :isDeleted AND ch.payer.payerName LIKE %:searchKey%")
    List<ContractHeader> findAllByProviderProviderUuidAndIsDeletedAndPayerPayerNameContaining(
            @Param("providerUuid") String providerUuid,
            @Param("isDeleted") boolean isDeleted,
            @Param("searchKey") String searchKey);

    @Query("SELECT c FROM ContractHeader c WHERE c.id IN (SELECT MAX(c2.id) FROM ContractHeader c2 WHERE c2.provider.providerUuid = :providerUuid AND c2.isDeleted = :isDeleted GROUP BY c2.payer.payerUuid)")
    List<ContractHeader> findAllByProviderProviderUuidAndIsDeletedGroupByPayerPayerUuid(
            @Param("providerUuid") String providerUuid,
            @Param("isDeleted") boolean isDeleted);

    @Query("SELECT c FROM ContractHeader c WHERE c.provider.providerUuid = :providerUuid AND c.isDeleted = :isDeleted AND c.payer.payerName LIKE %:searchKey% GROUP BY c.payer.payerUuid")
    List<ContractHeader> findAllByProviderProviderUuidAndIsDeletedGroupByPayerPayerUuidAndContainingName(
            @Param("providerUuid") String providerUuid,
            @Param("isDeleted") boolean isDeleted,
            @Param("searchKey") String searchKey);

//    List<ContractHeader> findAllByProviderProviderUuidAndIsDeleted(String providerUuid, boolean b);

    @Query("SELECT CASE WHEN COUNT(ch) > 0 THEN true ELSE false END FROM ContractHeader ch " +
            "WHERE ch.provider.providerUuid = :providerUuid " +
            "AND ch.payer.payerUuid = :payerUuid " +
            "AND ch.isDeleted = :isDeleted")
    boolean existsByProviderAndPayerUuids(
            @Param("providerUuid") String providerUuid,
            @Param("payerUuid") String payerUuid,
            @Param("isDeleted") boolean isDeleted);

    @Query("SELECT ch FROM ContractHeader ch WHERE ch.provider.providerUuid = :providerUuid AND ch.status = :status AND ch.isDeleted = :isDeleted")
    Page<ContractHeader> findByProviderProviderUuidAndStatusAndIsDeleted(
            @Param("providerUuid") String providerUuid,
            @Param("status") String status,
            @Param("isDeleted") boolean isDeleted,
            Pageable pageable);

    /**
     * Find active contract between a provider and payer
     *
     * @param providerUuid UUID of the provider
     * @param payerUuid    UUID of the payer
     * @param status       Status of the contract (should be ACTIVE)
     * @return The active ContractHeader or null if none exists
     */
    @Query("SELECT ch FROM ContractHeader ch " +
            "WHERE ch.provider.providerUuid = :providerUuid " +
            "AND ch.payer.payerUuid = :payerUuid " +
            "AND ch.status = :status " +
            "AND ch.isDeleted = false " +
            "AND CURRENT_DATE BETWEEN ch.startDate AND ch.endDate")
    ContractHeader findActiveContractBetweenProviderAndPayer(
            @Param("providerUuid") String providerUuid,
            @Param("payerUuid") String payerUuid,
            @Param("status") Status status);

    Long countByPayerPayerUuidAndIsDeleted(String payerUuid, boolean isDeleted);

    Long countByProviderProviderUuidAndIsDeleted(String providerUuid, boolean isDeleted);

    /**
     * Find contracts by status and deletion status
     *
     * @param status    Status of the contracts
     * @param isDeleted Deletion status
     * @param pageable  Pagination information
     * @return Page of ContractHeader objects
     */
    Page<ContractHeader> findByStatusAndIsDeleted(
            Status status,
            boolean isDeleted,
            Pageable pageable);

    /**
     * Find contracts by status, deletion status, and contract name containing search term
     *
     * @param status       Status of the contracts
     * @param isDeleted    Deletion status
     * @param contractName Search term for contract name
     * @param pageable     Pagination information
     * @return Page of ContractHeader objects
     */
    Page<ContractHeader> findByStatusAndIsDeletedAndContractNameContaining(
            Status status,
            boolean isDeleted,
            String contractName,
            Pageable pageable);

}
