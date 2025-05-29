package com.medco.HealthConnectProvider.entity.persons;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.medco.HealthConnectProvider.entity.groups.DependantGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.services.ProvidedService;
import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Builder
@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Dependant {

    @Serial
    private static final long serialVersionUID = -8081981920526009965L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 24, max = 40)
    private String dependantUuid = UUID.randomUUID().toString();


    @Size(max = 15)
    private String title;

    @NotBlank
    @Size(min = 2, max = 25)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 25)
    private String fatherName;

    @NotBlank
    @Size(min = 2, max = 25)
    private String grandFatherName;

    @NotBlank
    @Size(min = 1, max = 10)
    private String Gender;
    @NotNull
    private Date birthDate;

    @Enumerated(EnumType.STRING)
    private Relationship relationship;

    @Size(min = 9, max = 13)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;

    @ManyToOne
    @JoinColumn(name = "insured_uuid", nullable = false)
    private EmployeeInsured insured;

    @OneToMany(mappedBy = "dependant", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "dependant-provided-services")
    @Builder.Default
    private List<ProvidedService> providedServices = new ArrayList<>();

    // Helper methods
    public void addProvidedService(ProvidedService providedService) {
        providedServices.add(providedService);
        providedService.setDependant(this);
    }

    public void removeProvidedService(ProvidedService providedService) {
        providedServices.remove(providedService);
        providedService.setDependant(null);
    }

    @OneToMany(mappedBy = "dependant", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "dependant-groups")
    @Builder.Default
    private List<DependantGroup> dependantGroups = new ArrayList<>();

    // Helper methods
    public void addToGroup(EmployeeDependantGroup group) {
        DependantGroup dependantGroup = DependantGroup.builder()
                .dependant(this)
                .employeeDependantGroup(group)
                .dependantUuid(this.getDependantUuid())
                .groupUuid(group.getGroupUuid())
                .build();

        this.dependantGroups.add(dependantGroup);
        group.getDependantGroups().add(dependantGroup);
    }

    public void removeFromGroup(EmployeeDependantGroup group) {
        this.dependantGroups.stream()
                .filter(dg -> dg.getEmployeeDependantGroup().equals(group))
                .findFirst()
                .ifPresent(dg -> {
                    this.dependantGroups.remove(dg);
                    group.getDependantGroups().remove(dg);
                    dg.setDependant(null);
                    dg.setEmployeeDependantGroup(null);
                });
    }

}
