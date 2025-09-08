package com.medco.HealthConnectProvider.entity.integration;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.medco.HealthConnectProvider.entity.claims.BatchRecord;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.utils.enums.MedicationStatus;
import com.medco.HealthConnectProvider.utils.enums.SourceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medication_dispensing")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationDispensing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String invoiceNumber;

    private String batchCode;

    private String dispensingUuid;

    private String providerUuid;

    private String payerUuid;

    private String insuredUuid;

    private String prescriptionNumber;

    private String pharmacyTransactionId;

    private LocalDate dispensingDate;

    private String prescribingPhysicianName;

    private String prescribingPhysicianId;

    private LocalDate recordedAt;

    private String branchName;

    private String claimStatus;

    private String remark;

    @Enumerated(EnumType.STRING)
    private MedicationStatus status;

    private String claimUuid;

    private Double totalAmount;

    private Double patientResponsibility;

    private Double insuranceCoverage;

    private String pharmacistNotes;

    @Enumerated(EnumType.STRING)
    private SourceType source;

    @OneToMany(mappedBy = "dispensing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MedicationDispensingItem> items = new ArrayList<>();

    @ManyToOne
    @JoinColumn(name = "batch_id")
    private BatchRecord batchRecord;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String primaryDiagnosis;

    private String secondaryDiagnosis;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insured_id")
    @JsonBackReference(value = "employee-provided-services")
    private Insured insured;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependant_id")
    @JsonBackReference(value = "dependant-provided-services")
    private Dependant dependant;

    private String attachmentFileName;
    private String attachmentContentType;

    @Column(columnDefinition = "BYTEA")
    @Basic(fetch = FetchType.LAZY)
    private byte[] attachmentData;

}