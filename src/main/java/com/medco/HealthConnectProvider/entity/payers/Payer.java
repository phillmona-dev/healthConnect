package com.medco.HealthConnectProvider.entity.payers;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.persons.EmployeeInsured;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
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
public class Payer {

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

    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Temporal(TemporalType.TIMESTAMP)
    private Date registrationDate;

    private String taxIdentification;
    private String bankingDetails;

    @Builder.Default
    private boolean isDeleted = false;

    // One-to-Many relationship with User
    @OneToMany(mappedBy = "payer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<User> users = new ArrayList<>();

    // One-to-Many relationship with EmployeeInsured
    @OneToMany(mappedBy = "payer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmployeeInsured> employeeInsureds = new ArrayList<>();

    // One-to-Many relationship with EmployeeDependantGroup
    @OneToMany(mappedBy = "payer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmployeeDependantGroup> employeeDependantGroups = new ArrayList<>();

    // One-to-Many relationship with ContractHeader
    @OneToMany(mappedBy = "payer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContractHeader> contractHeaders = new ArrayList<>();

    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP)
    @Column(updatable = false)
    private Date createdDate;

    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastModifiedDate;

    @CreatedBy
    @Column(updatable = false)
    private String createdBy;

    @LastModifiedBy
    private String lastModifiedBy;

    @PrePersist
    public void prePersist() {
        if (payerUuid == null) {
            payerUuid = UUID.randomUUID().toString();
        }
    }

    // Helper methods to maintain bidirectional relationships
    public void addUser(User user) {
        users.add(user);
        user.setPayer(this);
    }

    public void removeUser(User user) {
        users.remove(user);
        user.setPayer(null);
    }

    public void addEmployeeInsured(EmployeeInsured employeeInsured) {
        employeeInsureds.add(employeeInsured);
        employeeInsured.setPayer(this);
    }

    public void removeEmployeeInsured(EmployeeInsured employeeInsured) {
        employeeInsureds.remove(employeeInsured);
        employeeInsured.setPayer(null);
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
