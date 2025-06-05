//package com.medco.HealthConnectProvider.entity.claims;
//
//import com.medco.HealthConnectProvider.shared.Audit;
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//import java.io.Serial;
//import java.util.UUID;
//
//@Setter
//@Getter
//@NoArgsConstructor
//@AllArgsConstructor
//@Entity
//@Table(name = "claim_items")
//public class ClaimItem extends Audit {
//
//    @Serial
//    private static final long serialVersionUID = 1L;
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, unique = true)
//    private String itemUuid = UUID.randomUUID().toString();
//
//    @ManyToOne
//    @JoinColumn(name = "claim_id", nullable = false)
//    private Claim claim;
//
//    @Column(nullable = false)
//    private String serviceCode;
//
//    @Column(nullable = false)
//    private String serviceName;
//
//    private String itemDescription;
//
//    @Column(nullable = false)
//    private String itemType; // MEDICATION, SERVICE, PROCEDURE, SUPPLY, etc.
//
//    @Column(nullable = false)
//    private Double quantity;
//
//    @Column(nullable = false)
//    private Double unitPrice;
//
//    @Column(nullable = false)
//    private Double totalPrice;
//
//    @Column(nullable = false)
//    private Double insuranceCoverage;
//
//    @Column(nullable = false)
//    private Double patientResponsibility;
//
//    private String notes;
//
//    // Optional reference to a provided service if this item is linked to one
//    private String providedServiceUuid;
//
//    // Optional reference to a medication dispensing item if this is a pharmacy claim
//    private String dispensingItemUuid;
//}