package com.medco.HealthConnectProvider.repository.group;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.groups.ContractDetailEmployeeGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractDetailEmployeeGroupRepository extends JpaRepository<ContractDetailEmployeeGroup, Long> {
    boolean existsByContractDetailAndEmployeeDependantGroup(ContractDetail contractDetail,
            EmployeeDependantGroup employeeDependantGroup);
}
