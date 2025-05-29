package com.medco.HealthConnectProvider.entity.services;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.EmployeeInsured;
import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "provided_services")
@Where(clause = "is_deleted = false")
public class ProvidedService extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 4245199917479888677L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String providedServiceUuid;

    @Column(nullable = false)
    private String contractDetailUuid;

    @Column(nullable = false)
    private String employeeInsuredUuid;

    private String dependantUuid;

    @Temporal(TemporalType.TIMESTAMP)
    private Date serviceDate;

    private String claimUuid;

    // Financial details
    @Column(precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal qty;

    // Record tracking
    private String recordNumber;

    // File attachment
    private String file;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Builder.Default
    private boolean isDeleted = false;

    // Many-to-One relationship with ContractDetail
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_detail_id")
    @JsonBackReference(value = "contract-detail-provided-services")
    private ContractDetail contractDetail;

    // Many-to-One relationship with EmployeeInsured
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_insured_id")
    @JsonBackReference(value = "employee-provided-services")
    private EmployeeInsured employeeInsured;

    // Many-to-One relationship with Dependant (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependant_id")
    @JsonBackReference(value = "dependant-provided-services")
    private Dependant dependant;

    // Many-to-One relationship with Claim (optional)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_id")
    @JsonBackReference(value = "claim-provided-services")
    private Claim claim;

    @PrePersist
    public void prePersist() {
        if (providedServiceUuid == null) {
            providedServiceUuid = UUID.randomUUID().toString();
        }

        // Calculate amount if not set but unitPrice and qty are available
        if (amount == null && unitPrice != null && qty != null) {
            amount = unitPrice.multiply(qty);
        }
    }
}
