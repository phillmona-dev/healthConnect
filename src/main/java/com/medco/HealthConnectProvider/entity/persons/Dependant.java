package com.medco.HealthConnectProvider.entity.persons;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
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
import java.util.*;

@Builder
@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_dependant_insured_uuid", columnList = "insured_uuid"),
        @Index(name = "idx_dependant_insured_deleted", columnList = "insured_uuid, isDeleted"),
        @Index(name = "idx_dependant_uuid", columnList = "dependantUuid"),
        @Index(name = "idx_dependant_phone", columnList = "phone")
})
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
    private String firstName;

    @NotBlank
    private String fatherName;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @JsonBackReference(value = "employee-dependant-groups")
    private EmployeeDependantGroup employeeDependantGroup;

    @ManyToMany(mappedBy = "dependants")
    private Set<ContractHeader> contracts = new HashSet<>();

}
