package com.medco.HealthConnectProvider.entity.payers;

import  com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "payers")
public class Payer extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String payerUuid;

    @Column(unique = true, nullable = false)
    private String payerName;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String telephone;

    @Size(max = 50)
    private String category;

    @Size(max = 50)
    private String address1;

    @Size(max = 50)
    private String address2;

    private String payerCode;

    @Size(max = 50)
    private String address3;

    @Size(max = 50)
    private String payerInsuranceNumber;

    private String city;
    private String state;
    private String zipCode;
    private String country;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date registrationDate;

    private String taxIdentification;

    @Pattern(regexp = "^\\d{10,13}$", message = "TIN number must be between 10 and 13 digits")
    private String tinNumber;

    private String bankingDetails;

    private Integer counter;

    @Size(max = 50)
    private String payerNumber;

    private double latitude;

    private double longitude;

    @Size(max = 15)
    private String referralType;
    @Size(max = 100)
    private String referredBy;

    @Size(max = 500)
    private String description;

    private boolean dependantCoverage;

    @Size(max = 255)
    private String logoPath;

    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "is_insurance", nullable = false)
    private boolean isInsurance = false;

    @OneToMany(mappedBy = "payer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @OneToMany(mappedBy = "payer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Insured> insureds = new ArrayList<>();

    @OneToMany(mappedBy = "payer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmployeeDependantGroup> employeeDependantGroups = new ArrayList<>();

    @OneToMany(mappedBy = "payer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContractHeader> contractHeaders = new ArrayList<>();


    @PrePersist
    public void prePersist() {
        if (payerUuid == null) {
            payerUuid = UUID.randomUUID().toString();
        }
    }

    public void addUser(User user) {
        users.add(user);
        user.setPayer(this);
        user.setPayerUuid(this.payerUuid);
    }

    public void removeUser(User user) {
        users.remove(user);
        user.setPayer(null);
    }

    public void addEmployeeInsured(Insured insured) {
        insureds.add(insured);
        insured.setPayer(this);
    }

    public void removeEmployeeInsured(Insured insured) {
        insureds.remove(insured);
        insured.setPayer(null);
    }

    public void addEmployeeDependantGroup(EmployeeDependantGroup group) {
        employeeDependantGroups.add(group);
        group.setPayer(this);
    }

    public void removeEmployeeDependantGroup(EmployeeDependantGroup group) {
        employeeDependantGroups.remove(group);
        group.setPayer(null);
    }

    public void addContractHeader(ContractHeader contractHeader) {
        contractHeaders.add(contractHeader);
        contractHeader.setPayer(this);
    }

    public void removeContractHeader(ContractHeader contractHeader) {
        contractHeaders.remove(contractHeader);
        contractHeader.setPayer(null);
    }

}
