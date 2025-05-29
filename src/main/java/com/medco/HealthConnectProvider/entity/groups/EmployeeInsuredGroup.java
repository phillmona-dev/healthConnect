package com.medco.HealthConnectProvider.entity.groups;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.medco.HealthConnectProvider.entity.persons.EmployeeInsured;
import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "employee_insured_groups")
@Where(clause = "is_deleted = false")
public class EmployeeInsuredGroup extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeInsuredUuid;

    @Column(nullable = false)
    private String groupUuid;

    @Builder.Default
    private boolean isDeleted = false;

    // Many-to-One relationship with EmployeeInsured
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_insured_id")
    @JsonBackReference(value = "employee-insured-groups")
    private EmployeeInsured employeeInsured;

    // Many-to-One relationship with EmployeeDependantGroup
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_dependant_group_id")
    @JsonBackReference(value = "group-employees")
    private EmployeeDependantGroup employeeDependantGroup;

    @PrePersist
    public void prePersist() {
        // Synchronize UUIDs if needed
        if (employeeInsuredUuid == null && employeeInsured != null) {
            employeeInsuredUuid = employeeInsured.getEmployeeInsuredUuid();
        }

        if (groupUuid == null && employeeDependantGroup != null) {
            groupUuid = employeeDependantGroup.getGroupUuid();
        }
    }
}