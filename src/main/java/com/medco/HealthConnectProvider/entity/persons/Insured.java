package com.medco.HealthConnectProvider.entity.persons;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.services.ProvidedService;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
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
    @Size(min = 2, max = 25)
    private String firstName;

    @NotBlank(message = "fatherName cant be empty")
    @Size(min = 2, max = 25)
    private String fatherName;

    @NotBlank(message = "grandFatherName cant be empty")
    @Size(min = 2, max = 25)
    private String grandFatherName;

    @NotBlank(message = "Gender cant be empty")
    @Size( max = 8)
    private String gender;

    @NotNull(message = "birth date can't be empty ")
    private Date birthDate;

    @NotBlank(message = "phone can't be empty")
    @Size(min = 9, max = 13)
    private String phone;

    @Size( max = 50)
    private String branchOffice;

    @NotBlank(message = "position can't be empty")
    @Size( max = 50)
    private String position;

    @Size(max = 50)
    private String idNumber;

    @Size(min = 2, max = 50)
    private String insuranceId;

    @NotBlank(message = "address1 can't be empty")
    @Size(max = 50)
    private String address;

    @NotBlank(message = "state can't be empty")
    @Size(min = 2, max = 50)
    private String state;

    @NotBlank(message = "country can't be empty")
    @Size(min = 2, max = 50)
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
//    public void addEmployeeInsuredGroup(EmployeeDependantGroup group) {
//        employeeInsuredGroups.add(group);
//        group.setInsured(this);
//        group.setInsured(this.);
//    }



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private Payer payer;

    @OneToMany(mappedBy = "insured", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "employee-provided-services")
    @Builder.Default
    private List<ProvidedService> providedServices = new ArrayList<>();

    public void addProvidedService(ProvidedService providedService) {
        providedServices.add(providedService);
        providedService.setInsured(this);
    }

    public void removeProvidedService(ProvidedService providedService) {
        providedServices.remove(providedService);
        providedService.setInsured(null);
    }

    @OneToMany(mappedBy = "insured", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dependant> dependants;

    @JsonBackReference
    @ManyToMany(mappedBy = "insured")
    @Builder.Default
    private List<ContractHeader> contracts = new ArrayList<>();

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