package com.medco.HealthConnectProvider.entity.integration;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.utils.enums.ItemType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "medication_dispensing_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MedicationDispensingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String itemUuid;

    @ManyToOne
    @JoinColumn(name = "dispensing_id")
    private MedicationDispensing dispensing;

    @Column
    private String medicationCode;

    @Column
    private String medicationName;

    @Column
    private Double quantity;

    private String primaryDiagnosis;

    private String secondaryDiagnosis;

    @Column
    private String unitOfMeasure;

    @Column
    private Double unitPrice;

    @Column
    private Double totalPrice;

    @Column
    private String dosageInstructions;

    @Column
    private String strength;

    private String route;

    @Column
    private String formulation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType itemType;

    @Column(columnDefinition = "boolean default false")
    private boolean deleted = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private String remark;


    @ManyToOne
    @JoinColumn(name = "contract_detail_id", nullable = false)
    private ContractDetail contractDetail;
}
