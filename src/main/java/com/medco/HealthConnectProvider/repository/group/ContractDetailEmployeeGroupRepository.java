package com.medco.HealthConnectProvider.repository.group;


import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.groups.ContractDetailEmployeeGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractDetailEmployeeGroupRepository extends JpaRepository<ContractDetailEmployeeGroup, Long> {
    boolean existsByContractDetailAndEmployeeDependantGroup(ContractDetail contractDetail,
                                                            EmployeeDependantGroup employeeDependantGroup);

    Optional<ContractDetailEmployeeGroup> findByContractDetailUuidAndEmployeeGroupUuid(String contractDetailUuid, String employeeGroupUuid);

    List<ContractDetailEmployeeGroup> findByContractDetailUuid(String contractDetailUuid);

    List<ContractDetailEmployeeGroup> findByEmployeeGroupUuid(String employeeGroupUuid);

    @Query("SELECT cdeg FROM ContractDetailEmployeeGroup cdeg " +
            "JOIN cdeg.contractDetail cd " +
            "JOIN cdeg.employeeDependantGroup edg " +
            "WHERE cd.contractHeader.contractHeaderUuid = :contractUuid " +
            "AND edg.isDeleted = false")
    List<ContractDetailEmployeeGroup> findByContractUuid(@Param("contractUuid") String contractUuid);

    @Query("SELECT cdeg FROM ContractDetailEmployeeGroup cdeg " +
            "JOIN cdeg.contractDetail cd " +
            "JOIN cdeg.employeeDependantGroup edg " +
            "WHERE cd.contractHeader.contractHeaderUuid = :contractUuid " +
            "AND (edg.groupName LIKE %:search% OR edg.groupDescription LIKE %:search%) " +
            "AND edg.isDeleted = false")
    Page<ContractDetailEmployeeGroup> searchByContractAndGroupName(
            @Param("contractUuid") String contractUuid,
            @Param("search") String search,
            Pageable pageable);

    void deleteByContractDetailUuidAndEmployeeGroupUuid(String contractDetailUuid, String employeeGroupUuid);

}
