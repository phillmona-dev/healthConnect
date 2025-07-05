package com.medco.HealthConnectProvider.entity.contracts;

import com.medco.HealthConnectProvider.entity.drug.Drug;
import com.medco.HealthConnectProvider.entity.groups.ContractDetailEmployeeGroup;
import com.medco.HealthConnectProvider.entity.groups.EmployeeDependantGroup;
import com.medco.HealthConnectProvider.entity.integration.MedicationDispensingItem;

import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "contract_details")
@Where(clause = "is_deleted = false")
public class ContractDetail extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String contractDetailUuid;

    @Column(nullable = false)
    private String contractHeaderUuid;

    @Column(nullable = true)
    private String serviceUuid;

    @Column(precision = 19, scale = 2)
    private BigDecimal negotiatedPrice;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Builder.Default
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_header_id")
    @JsonBackReference(value = "contract-header-details")
    private ContractHeader contractHeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    @JsonBackReference(value = "service-details")
    private Servicelist servicelist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "drug_uuid", referencedColumnName = "drug_uuid")
    private Drug drug;

    @Column(name = "drug_uuid", insertable = false, updatable = false)
    private String drugUuid;

    private String itemType;

    @ManyToMany
    @JoinTable(
            name = "contract_detail_employee_dependant_groups",
            joinColumns = @JoinColumn(name = "contract_detail_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_dependant_group_id")
    )
    @Builder.Default
    private Set<EmployeeDependantGroup> employeeDependantGroups = new HashSet<>();
//
    @OneToMany(mappedBy = "contractDetail", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @JsonManagedReference(value = "contract-detail-medication-dispensing-item ")
    @Builder.Default
    private List<MedicationDispensingItem> medicationDispensingItems = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (contractDetailUuid == null) {
            contractDetailUuid = UUID.randomUUID().toString();
        }
    }

    // Helper methods for EmployeeDependantGroup
    public void addEmployeeDependantGroup(EmployeeDependantGroup group) {
        employeeDependantGroups.add(group);
        group.getContractDetails().add(this);
    }

    public void removeEmployeeDependantGroup(EmployeeDependantGroup group) {
        employeeDependantGroups.remove(group);
        group.getContractDetails().remove(this);
    }
//
//    // Helper methods for ProvidedService
//    public void addProvidedService(ProvidedService providedService) {
//        providedServices.add(providedService);
//        providedService.setContractDetail(this);
//    }
//
//    public void removeProvidedService(ProvidedService providedService) {
//        providedServices.remove(providedService);
//        providedService.setContractDetail(null);
//    }

    @OneToMany(mappedBy = "contractDetail", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JsonManagedReference(value = "contract-detail-groups")
    @Builder.Default
    private List<ContractDetailEmployeeGroup> contractDetailEmployeeGroups = new ArrayList<>();

    // Helper methods
    public void addContractDetailEmployeeGroup(ContractDetailEmployeeGroup group) {
        contractDetailEmployeeGroups.add(group);
        group.setContractDetail(this);
        group.setContractDetailUuid(this.getContractDetailUuid());
    }

    public void removeContractDetailEmployeeGroup(ContractDetailEmployeeGroup group) {
        contractDetailEmployeeGroups.remove(group);
        group.setContractDetail(null);
    }
}