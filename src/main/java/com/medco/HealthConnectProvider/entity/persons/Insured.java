package com.medco.HealthConnectProvider.entity.persons;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Builder
@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(indexes = {
        @Index(name = "idx_insured_payer_uuid", columnList = "payerUuid"),
        @Index(name = "idx_insured_search_fields", columnList = "firstName, fatherName, grandFatherName, phone, insuranceId")
})
public class Insured implements Serializable {

    @Serial
    private static final long serialVersionUID = -8081981920526009965L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 36, max = 40)
    private String insuredUuid = UUID.randomUUID().toString();

    @Size(min = 36, max = 40)
    private String payerUuid;

    @Size(max = 50)
    private String employeeId;

    @Size(max = 50)
    private String nationalId;

    @Size(max = 50)
    private String policyNumber;

    private LocalDate policyStartDate;

    private LocalDate policyEndDate;

    @Size(max = 50)
    private String email;

    @Size(min = 2, max = 25)
    private String title;

    @NotBlank(message = "firstName cant be empty")
    private String firstName;

    @NotBlank(message = "fatherName cant be empty")
    private String fatherName;

    private String grandFatherName;

    private String gender;

    private Date birthDate;

    private String phone;

    private String branchOffice;

    private String position;

    private String idNumber;
    private String insuranceId;

    private String address;
    private String state;
    private String woreda;
    private String kebelle;
    private String subcity;
    private String city;
    private String country;

    @Size(max = 255)
    @Column(name = "profile_picture_path")
    private String profilePicturePath;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;

    @Column
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    @JsonBackReference(value = "employee-insured-groups")
    private EmployeeDependantGroup employeeDependantGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private Payer payer;

    @OneToMany(mappedBy = "insured", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dependant> dependants;

    @ManyToMany(mappedBy = "insured")
    @JsonIgnore
    private Set<ContractHeader> contracts = new HashSet<>();

    @ManyToMany
    @JoinTable(
            name = "insured_employee_dependant_group",
            joinColumns = @JoinColumn(name = "insured_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_dependant_group_id")
    )
    private List<EmployeeDependantGroup> employeeDependantGroups = new ArrayList<>();

    public void addContract(ContractHeader contract) {
        contracts.add(contract);
        if (!contract.getInsured().contains(this)) {
            contract.getInsured().add(this);
        }
    }

    public void removeContract(ContractHeader contract) {
        contracts.remove(contract);
        contract.getInsured().remove(this);
    }
}