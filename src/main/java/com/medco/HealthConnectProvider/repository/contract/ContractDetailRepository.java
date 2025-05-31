package com.medco.HealthConnectProvider.repository.contract;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractDetailRepository extends JpaRepository<ContractDetail, Long> {

    /**
     * Find a contract detail by its UUID
     * @param contractDetailUuid The UUID of the contract detail
     * @return The contract detail if found, null otherwise
     */
    ContractDetail findByContractDetailUuid(String contractDetailUuid);

    /**
     * Find all contract details for a specific contract header
     * @param contractHeader The contract header
     * @return List of contract details
     */
    List<ContractDetail> findByContractHeader(ContractHeader contractHeader);

    /**
     * Find all contract details for a specific contract header UUID
     * @param contractHeaderUuid The UUID of the contract header
     * @return List of contract details
     */
    List<ContractDetail> findByContractHeaderContractHeaderUuid(String contractHeaderUuid);

    /**
     * Check if a contract detail exists for a specific service in a contract
     * @param contractHeaderUuid The UUID of the contract header
     * @param serviceUuid The UUID of the service
     * @return True if exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(cd) > 0 THEN true ELSE false END FROM ContractDetail cd " +
            "WHERE cd.contractHeader.contractHeaderUuid = :contractHeaderUuid " +
            "AND cd.servicelist.serviceUuid = :serviceUuid")
    boolean existsByContractHeaderAndServicelist(
            @Param("contractHeaderUuid") String contractHeaderUuid,
            @Param("serviceUuid") String serviceUuid);

    // Alternative method using property path notation
    boolean existsByContractHeaderContractHeaderUuidAndServicelistServiceUuid(
            String contractHeaderUuid, String serviceUuid);
}