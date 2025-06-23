package com.medco.HealthConnectProvider.entity.integration;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    private String claimStatus; // PENDING, CLAIMED

    private String claimUuid;

    private Double totalAmount;

    private Double patientResponsibility;

    private Double insuranceCoverage;

    private String pharmacistNotes;

    @OneToMany(mappedBy = "dispensing", cascade = CascadeType.ALL)
    private List<MedicationDispensingItem> items;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean deleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}