package com.medco.HealthConnectProvider.entity.drug;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "drugs")
public class Drug {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drug_uuid", unique = true, nullable = false)
    private String drugUuid = UUID.randomUUID().toString();

    @Column(unique = true)
    private String drugCode;

    private String drugName;
    private String category;
    private String subCategory;
    private Double price;
    private String dosage;
    private String manufacturer;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private Provider provider;

    private boolean isDeleted = false;
    private Instant createdAt;
    private Instant updatedAt;

    private String genericName;
    private String brandName;
    private String formulation;
    private String route;
    private String indications;
    private String sideEffect;
    private String description;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

}
