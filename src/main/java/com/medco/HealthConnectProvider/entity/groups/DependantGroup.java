package com.medco.HealthConnectProvider.entity.groups;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.io.Serializable;
import java.util.UUID;

/**
 * Represents the association between dependants and their assigned groups.
 * This entity allows a dependant to belong to multiple benefit groups.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "dependant_groups")
@Where(clause = "is_deleted = false")
public class DependantGroup extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String dependantUuid;

    @Column(nullable = false)
    private String groupUuid;

    @Builder.Default
    private boolean isDeleted = false;

    // Many-to-One relationship with Dependant
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependant_id")
    @JsonBackReference(value = "dependant-groups")
    private Dependant dependant;

    // Many-to-One relationship with EmployeeDependantGroup
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_dependant_group_id")
    @JsonBackReference(value = "group-dependants")
    private EmployeeDependantGroup employeeDependantGroup;

    @PrePersist
    public void prePersist() {
        // Synchronize UUIDs if needed
        if (dependantUuid == null && dependant != null) {
            dependantUuid = dependant.getDependantUuid();
        }

        if (groupUuid == null && employeeDependantGroup != null) {
            groupUuid = employeeDependantGroup.getGroupUuid();
        }
    }
}