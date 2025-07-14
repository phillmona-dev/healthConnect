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

import java.io.Serial;
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

    @Serial
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

    private String groupType;

    @Builder.Default
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    @JsonBackReference(value = "payer-groups")
    private Payer payer;

    @ManyToMany(mappedBy = "employeeDependantGroups")
    @Builder.Default
    private Set<ContractDetail> contractDetails = new HashSet<>();

    @OneToMany(mappedBy = "employeeDependantGroup", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "employee-group-contracts")
    @Builder.Default
    private List<ContractDetailEmployeeGroup> contractDetailEmployeeGroups = new ArrayList<>();

    @OneToMany(mappedBy = "employeeDependantGroup", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "employee-insured-groups")
    @Builder.Default
    private Set<Insured> insureds = new HashSet<>();

    @OneToMany(mappedBy = "employeeDependantGroup", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "employee-dependant-groups")
    @Builder.Default
    private Set<Dependant> dependants = new HashSet<>();


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

}