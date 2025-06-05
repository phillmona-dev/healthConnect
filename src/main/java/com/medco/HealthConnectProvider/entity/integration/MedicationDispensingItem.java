//package com.medco.HealthConnectProvider.entity.integration;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import org.hibernate.annotations.CreationTimestamp;
//import org.hibernate.annotations.UpdateTimestamp;
//
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "medication_dispensing_item")
//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//public class MedicationDispensingItem {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(unique = true, nullable = false)
//    private String itemUuid;
//
//    @ManyToOne
//    @JoinColumn(name = "dispensing_id", nullable = false)
//    private MedicationDispensing dispensing;
//
//    @Column(nullable = false)
//    private String medicationCode;
//
//    @Column(nullable = false)
//    private String medicationName;
//
//    @Column(nullable = false)
//    private Double quantity;
//
//    @Column(nullable = false)
//    private String unitOfMeasure;
//
//    @Column(nullable = false)
//    private Double unitPrice;
//
//    @Column(nullable = false)
//    private Double totalPrice;
//
//    private String dosageInstructions;
//
//    private String strength;
//
//    private String formulation;
//
//    @Column(nullable = false, columnDefinition = "boolean default false")
//    private boolean deleted = false;
//
//    @CreationTimestamp
//    private LocalDateTime createdAt;
//
//    @UpdateTimestamp
//    private LocalDateTime updatedAt;
//}