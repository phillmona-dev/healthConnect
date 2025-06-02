package com.medco.HealthConnectProvider.entity.contracts;

import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "contract_headers")
public class ContractHeader extends Audit implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String contractHeaderUuid;

    @Column(nullable = false)
    private String contractNumber;

    private String contractName;
    private String contractDescription;

    private String approvedBy;
    private Date approvalDate;

    @Temporal(TemporalType.DATE)
    private LocalDate startDate;

    @Temporal(TemporalType.DATE)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String remark;

    private String preparedBy;

    private String description;

    private String contractCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private Payer payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @Temporal(TemporalType.DATE)
    private Date terminationDate;

    private String terminationReason;

    @Column(columnDefinition = "TEXT")
    private String terminationNotes;

    private String terminatedBy;

    @Temporal(TemporalType.TIMESTAMP)
    private Date terminationRequestDate;

    private Double coPaymentPercentage;

    @OneToMany(mappedBy = "contractHeader", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContractDetail> contractDetails = new ArrayList<>();

    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (contractHeaderUuid == null) {
            contractHeaderUuid = UUID.randomUUID().toString();
        }
    }

    @ManyToMany
    @JoinTable(
            name = "contract_insured",
            joinColumns = @JoinColumn(name = "contract_id"),
            inverseJoinColumns = @JoinColumn(name = "employee_insured_id")
    )
    @Builder.Default
    private List<Insured> insured = new ArrayList<>();

    // Helper methods for the relationship
    public void addInsured(Insured employee) {
        insured.add(employee);
        employee.getContracts().add(this);
    }

    public void removeInsured(Insured employee) {
        insured.remove(employee);
        employee.getContracts().remove(this);
    }

    // Helper methods to maintain bidirectional relationship with ContractDetail
    public void addContractDetail(ContractDetail contractDetail) {
        contractDetails.add(contractDetail);
        contractDetail.setContractHeader(this);
    }

    public void removeContractDetail(ContractDetail contractDetail) {
        contractDetails.remove(contractDetail);
        contractDetail.setContractHeader(null);
    }
}