package com.medco.HealthConnectProvider.entity.groups;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.GroupType;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a group that can contain employees, dependants, or both.
 * This entity is used to organize individuals into benefit groups.
 */
@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "employee_dependant_groups")
@Where(clause = "is_deleted = false")
public class EmployeeDependantGroup extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String groupUuid;

    private String groupName;
    private Integer estimatedMembers;

    @Column
    private String groupDescription;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private GroupType type;

    @Column(nullable = false)
    private String payerUuid;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Builder.Default
    private boolean isDeleted = false;

    // Many-to-One relationship with Payer
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    @JsonBackReference(value = "payer-groups")
    private Payer payer;

    // Many-to-Many relationship with ContractDetail
    @ManyToMany(mappedBy = "employeeDependantGroups")
    @Builder.Default
    private Set<ContractDetail> contractDetails = new HashSet<>();

    // One-to-Many relationship with ContractDetailEmployeeGroup
    @OneToMany(mappedBy = "employeeDependantGroup", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "employee-group-contracts")
    @Builder.Default
    private List<ContractDetailEmployeeGroup> contractDetailEmployeeGroups = new ArrayList<>();

    // One-to-Many relationship with DependantGroup
    @OneToMany(mappedBy = "employeeDependantGroup", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "group-dependants")
    @Builder.Default
    private List<DependantGroup> dependantGroups = new ArrayList<>();

    // One-to-Many relationship with EmployeeInsuredGroup
    @OneToMany(mappedBy = "employeeDependantGroup", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "group-employees")
    @Builder.Default
    private List<EmployeeInsuredGroup> employeeInsuredGroups = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        // Synchronize UUIDs if needed
        if (payerUuid == null && payer != null) {
            payerUuid = payer.getPayerUuid();
        }
    }

    // Helper methods for ContractDetailEmployeeGroup
    public void addContractDetailEmployeeGroup(ContractDetailEmployeeGroup group) {
        contractDetailEmployeeGroups.add(group);
        group.setEmployeeDependantGroup(this);
        group.setEmployeeGroupUuid(this.getGroupUuid());
    }

    public void removeContractDetailEmployeeGroup(ContractDetailEmployeeGroup group) {
        contractDetailEmployeeGroups.remove(group);
        group.setEmployeeDependantGroup(null);
    }

    // Helper methods for DependantGroup
    public void addDependant(Dependant dependant) {
        DependantGroup dependantGroup = DependantGroup.builder()
                .dependant(dependant)
                .employeeDependantGroup(this)
                .dependantUuid(dependant.getDependantUuid())
                .groupUuid(this.getGroupUuid())
                .build();

        this.dependantGroups.add(dependantGroup);
        dependant.getDependantGroups().add(dependantGroup);
    }

    public void removeDependant(Dependant dependant) {
        this.dependantGroups.stream()
                .filter(dg -> dg.getDependant().equals(dependant))
                .findFirst()
                .ifPresent(dg -> {
                    this.dependantGroups.remove(dg);
                    dependant.getDependantGroups().remove(dg);
                    dg.setDependant(null);
                    dg.setEmployeeDependantGroup(null);
                });
    }

    // Helper methods for EmployeeInsuredGroup
    public void addEmployee(Insured employee) {
        EmployeeInsuredGroup employeeGroup = EmployeeInsuredGroup.builder()
                .insured(employee)
                .employeeDependantGroup(this)
                .employeeInsuredUuid(employee.getInsuredUuid())
                .groupUuid(this.getGroupUuid())
                .build();

        this.employeeInsuredGroups.add(employeeGroup);
        employee.getEmployeeInsuredGroups().add(employeeGroup);
    }

    public void removeEmployee(Insured employee) {
        this.employeeInsuredGroups.stream()
                .filter(eg -> eg.getInsured().equals(employee))
                .findFirst()
                .ifPresent(eg -> {
                    this.employeeInsuredGroups.remove(eg);
                    employee.getEmployeeInsuredGroups().remove(eg);
                    eg.setInsured(null);
                    eg.setEmployeeDependantGroup(null);
                });
    }

}