package com.medco.HealthConnectProvider.repository.contract;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.drug.Drug;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * Find contract details by contract header UUID and service UUID
     * @param contractHeaderUuid The UUID of the contract header
     * @param serviceUuid The UUID of the service
     * @return List of contract details
     */
    @Query("SELECT cd FROM ContractDetail cd " +
            "WHERE cd.contractHeaderUuid = :contractHeaderUuid " +
            "AND cd.serviceUuid = :serviceUuid " +
            "AND cd.isDeleted = false")
    List<ContractDetail> findByContractHeaderAndService(
            @Param("contractHeaderUuid") String contractHeaderUuid,
            @Param("serviceUuid") String serviceUuid);

    List<ContractDetail> findByContractDetailUuidIn(List<String> eligibleServicesUuids);

    ContractDetail findByContractHeaderUuidAndServiceUuidAndEmployeeDependantGroups(String contractHeaderUuid, String serviceUuid, EmployeeDependantGroup employeeDependantGroup);

    ContractDetail findByContractHeaderUuidAndServiceUuid(String contractHeaderUuid, String serviceUuid);

    Optional<ContractDetail> findByContractHeaderAndServiceUuid(ContractHeader activeContract, String serviceUuid);

    @Query("SELECT cd FROM ContractDetail cd " +
            "WHERE cd.contractHeader = :contract " +
            "AND cd.status = 'ACTIVE' " +
            "AND EXISTS (SELECT 1 FROM cd.employeeDependantGroups edg WHERE edg IN :groups)")
    List<ContractDetail> findEligibleServicesByContractAndGroups(
            @Param("contract") ContractHeader contract,
            @Param("groups") List<EmployeeDependantGroup> groups);

    void deleteByContractHeader(ContractHeader contract);

    ContractDetail findByContractHeaderAndServicelistServiceUuid(ContractHeader activeContract, String contractDetailUuid);

    @Query("SELECT cd FROM ContractDetail cd " +
            "JOIN cd.contractHeader ch " +
            "WHERE ch.provider.providerUuid = :providerUuid " +
            "AND ch.payer.payerUuid = :payerUuid " +
            "AND (cd.drug.drugName = :medicationName OR cd.servicelist.serviceName = :medicationName) " +
            "AND ch.status = 'ACTIVE' " +
            "AND CURRENT_DATE BETWEEN ch.startDate AND ch.endDate")
    ContractDetail findByContractHeaderAndMedicationName(
            @Param("providerUuid") String providerUuid,
            @Param("payerUuid") String payerUuid,
            @Param("medicationName") String medicationName
    );

    Optional<ContractDetail> findByContractHeaderAndDrug(ContractHeader contractHeader, Drug drug);

}