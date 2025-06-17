package com.medco.HealthConnectProvider.entity.integration;

import com.medco.HealthConnectProvider.utils.enums.Status;
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

    @Column(unique = true, nullable = false)
    private String dispensingUuid;

    @Column(nullable = false)
    private String providerUuid;

    @Column(nullable = false)
    private String payerUuid;

    @Column(nullable = false)
    private String insuredUuid;

    @Column(nullable = false)
    private String prescriptionNumber;

    @Column(nullable = false, unique = true)
    private String pharmacyTransactionId;

    @Column(nullable = false)
    private LocalDate dispensingDate;

    private String prescribingPhysicianName;

    private String prescribingPhysicianId;

    @Column(nullable = false)
    private LocalDate recordedAt;

    private String branchName;

    private String claimStatus; // PENDING, CLAIMED

    private String claimUuid;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
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