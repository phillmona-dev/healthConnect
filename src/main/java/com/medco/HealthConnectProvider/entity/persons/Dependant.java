package com.medco.HealthConnectProvider.entity.persons;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.utils.enums.Relationship;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
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
public class Dependant implements Serializable {

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
    private String gender;
    @NotNull
    private Date birthDate;

    @Enumerated(EnumType.STRING)
    private Relationship relationship;

    @Size(min = 9, max = 13)
    private String phone;

    @Enumerated(EnumType.STRING)
    private Status status;


    @Column(nullable = false)
    private boolean isDeleted = false;

    @Column
    private LocalDateTime deletedAt;

    private String profilePicturePath;

    @ManyToOne
    @JoinColumn(name = "insured_uuid", nullable = false)
    private Insured insured;

//    @OneToMany(mappedBy = "dependant", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
//    @JsonManagedReference(value = "dependant-provided-services")
//    @Builder.Default
//    private List<ProvidedService> providedServices = new ArrayList<>();
//
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @JsonBackReference(value = "employee-dependant-groups")
    private EmployeeDependantGroup employeeDependantGroup;

//    // Helper methods
//    public void addProvidedService(ProvidedService providedService) {
//        providedServices.add(providedService);
//        providedService.setDependant(this);
//    }
//
//    public void removeProvidedService(ProvidedService providedService) {
//        providedServices.remove(providedService);
//        providedService.setDependant(null);
//    }



}
