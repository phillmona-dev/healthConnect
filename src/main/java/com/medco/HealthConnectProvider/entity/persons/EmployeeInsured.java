package com.medco.HealthConnectProvider.entity.persons;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.groups.EmployeeInsuredGroup;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.services.ProvidedService;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.lang.Contract;

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
public class EmployeeInsured {

    @Serial
    private static final long serialVersionUID = -8081981920526009965L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 36, max = 40)
    private String employeeInsuredUuid = UUID.randomUUID().toString();

    @Size(min = 36, max = 40)
    private String institutionUuid;


    @Size(max = 50)
    private String email;

    @NotBlank(message = "title cant be empty")
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
    private String Gender;

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

    //
//	@NotBlank(message ="insurance Id can't be empty")
    @Size(min = 2, max = 50)
    private String insuranceId;

    @NotBlank(message = "address1 can't be empty")
    @Size(max = 50)
    private String address1;
    //
//	@NotBlank(message = "address2 can't be empty")
    @Size(max = 50)
    private String address2;
    //
//	@NotBlank(message = "address3 can't be empty")
    @Size(max = 50)
    private String address3;

    @NotBlank(message = "state can't be empty")
    @Size(min = 2, max = 50)
    private String state;

    @NotBlank(message = "country can't be empty")
    @Size(min = 2, max = 50)
    private String country;

    private Date beginDate;

    private Date endDate;


    private String profilePicture;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;

    @OneToMany(mappedBy = "employeeInsured", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "employee-insured-groups")
    @Builder.Default
    private List<EmployeeInsuredGroup> employeeInsuredGroups = new ArrayList<>();

    public void addEmployeeInsuredGroup(EmployeeInsuredGroup group) {
        employeeInsuredGroups.add(group);
        group.setEmployeeInsured(this);
        group.setEmployeeInsuredUuid(this.getEmployeeInsuredUuid());
    }

    public void removeEmployeeInsuredGroup(EmployeeInsuredGroup group) {
        employeeInsuredGroups.remove(group);
        group.setEmployeeInsured(null);
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private Payer payer;

    @OneToMany(mappedBy = "employeeInsured", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "employee-provided-services")
    @Builder.Default
    private List<ProvidedService> providedServices = new ArrayList<>();

    public void addProvidedService(ProvidedService providedService) {
        providedServices.add(providedService);
        providedService.setEmployeeInsured(this);
    }

    public void removeProvidedService(ProvidedService providedService) {
        providedServices.remove(providedService);
        providedService.setEmployeeInsured(null);
    }

    @OneToMany(mappedBy = "insured", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dependant> dependents;


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
