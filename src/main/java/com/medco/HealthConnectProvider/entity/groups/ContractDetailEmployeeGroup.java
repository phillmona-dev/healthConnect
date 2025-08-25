package com.medco.HealthConnectProvider.entity.groups;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "contract_detail_employee_groups")
@Where(clause = "is_deleted = false")
public class ContractDetailEmployeeGroup extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String contractDetailUuid;

    @Column(nullable = false)
    private String employeeGroupUuid;

    @Builder.Default
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_detail_id")
    @JsonBackReference(value = "contract-detail-groups")
    private ContractDetail contractDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_dependant_group_id")
    @JsonBackReference(value = "employee-group-contracts")
    private EmployeeDependantGroup employeeDependantGroup;

    @PrePersist
    public void prePersist() {
        // Generate UUIDs if needed
        if (contractDetailUuid == null && contractDetail != null) {
            contractDetailUuid = contractDetail.getContractDetailUuid();
        }

        if (employeeGroupUuid == null && employeeDependantGroup != null) {
            employeeGroupUuid = employeeDependantGroup.getGroupUuid();
        }
    }
}
