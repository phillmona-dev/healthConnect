package com.medco.HealthConnectProvider.repository.contract;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.ui.request.contract.ContractFilterRequest;
import com.medco.HealthConnectProvider.ui.response.payer.PayerProviderResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PolicyHolderListResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<ContractHeader, Long>, JpaSpecificationExecutor<ContractHeader> {

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

    default Page<ContractHeader> findFilteredContracts(ContractFilterRequest filter, Pageable pageable) {
        return findAll((root, query, cb) -> {

            if (query.getResultType() == ContractHeader.class) {
                root.fetch("payer", JoinType.LEFT);
                root.fetch("provider", JoinType.LEFT);
            }

            Predicate predicate = cb.conjunction();

            if (filter.getContractNumber() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("contractNumber")),
                                "%" + filter.getContractNumber().toLowerCase() + "%"));
            }

            if (filter.getContractName() != null) {
                predicate = cb.and(predicate,
                        cb.like(cb.lower(root.get("contractName")),
                                "%" + filter.getContractName().toLowerCase() + "%"));
            }

            if (filter.getStatus() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), filter.getStatus()));
            }

            if (filter.getPayerUuid() != null) {
                Join<ContractHeader, Payer> payerJoin = root.join("payer", JoinType.INNER);
                predicate = cb.and(predicate,
                        cb.equal(payerJoin.get("payerUuid"), filter.getPayerUuid()));
            }

            if (filter.getProviderUuid() != null) {
                Join<ContractHeader, Provider> providerJoin = root.join("provider", JoinType.INNER);
                predicate = cb.and(predicate,
                        cb.equal(providerJoin.get("providerUuid"), filter.getProviderUuid()));
            }

            if (filter.getStartDateFrom() != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(root.get("startDate"), filter.getStartDateFrom()));
            }

            if (filter.getStartDateTo() != null) {
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(root.get("startDate"), filter.getStartDateTo()));
            }

            if (filter.getEndDateFrom() != null) {
                predicate = cb.and(predicate,
                        cb.greaterThanOrEqualTo(root.get("endDate"), filter.getEndDateFrom()));
            }

            if (filter.getEndDateTo() != null) {
                predicate = cb.and(predicate,
                        cb.lessThanOrEqualTo(root.get("endDate"), filter.getEndDateTo()));
            }

            if (filter.getPreparedBy() != null) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("preparedBy"), filter.getPreparedBy()));
            }

            predicate = cb.and(predicate,
                    cb.equal(root.get("isDeleted"),
                            filter.getIsDeleted() != null ? filter.getIsDeleted() : false));

            return predicate;
        }, pageable);
    }

    @Query("SELECT ch FROM ContractHeader ch WHERE ch.payer.payerUuid = :payerUuid AND ch.status = 'ACTIVE' AND ch.endDate >= CURRENT_DATE ORDER BY ch.startDate DESC")
    Optional<ContractHeader> findActiveContractByPayerUuid(@Param("payerUuid") String payerUuid);

    Optional<ContractHeader> findActiveContractByProviderProviderUuidAndPayerPayerUuid(@Size(min = 36, max = 40, message = "Provided Uuid Must be between 36 and 40") String providerUuid, String payerUuid);

}
