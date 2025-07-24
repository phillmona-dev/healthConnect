package com.medco.HealthConnectProvider.entity.claims;

import com.medco.HealthConnectProvider.entity.integration.MedicationDispensing;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "batch_records")
public class BatchRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String batchCode;

    @Column(name = "batch_number")
    private long batchNumber;

    private String payerName;

    private LocalDate requestedOn;

    private LocalDate claimDatingFrom;

    private LocalDate claimDatingTo;

    private Double totalAmount;

    private String status;

    private String claimUuid;

    @OneToOne(mappedBy = "batchRecord")
    private Claim claim;

    @Column(length = 1000)
    private String rejectionRemark;

    @Column(length = 1000)
    private String resubmissionRemark;

    private String rejectedBy;
    private String resubmittedBy;
    private LocalDateTime resubmittedAt;
    private LocalDateTime rejectedAt;

    private double numberOfClaims;

    @OneToMany(mappedBy = "batchRecord", cascade = CascadeType.ALL,fetch = FetchType.EAGER)
    private List<MedicationDispensing> medicationDispensing;

}
