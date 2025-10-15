package com.medco.HealthConnectProvider.entity.services;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "services",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_services_code_provider", columnNames = {"service_code", "provider_id"}),
        @UniqueConstraint(name = "uk_services_generated_id_provider", columnNames = {"generated_service_id", "provider_id"})
    },
    indexes = {
        @Index(name = "idx_services_code_provider", columnList = "service_code, provider_id"),
        @Index(name = "idx_services_generated_id_provider", columnList = "generated_service_id, provider_id")
    }
)
public class Servicelist extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serviceUuid;

    @Column
    private String generatedServiceId;

    @Column(nullable = false)
    private String serviceName;

    @Column
    private String serviceCode;

    @Column(columnDefinition = "TEXT")
    private String serviceDescription;

    private String serviceCategory;

    private String serviceSubCategory;

    @Column(precision = 19)
    private Double defaultPrice;

    private Double negotiatedPrice;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Double price;

    private String unitOfMeasure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    @OneToMany(mappedBy = "servicelist", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ContractDetail> contractDetails = new ArrayList<>();

    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (serviceUuid == null) {
            serviceUuid = UUID.randomUUID().toString();
        }

    }

    public void addContractDetail(ContractDetail contractDetail) {
        contractDetails.add(contractDetail);
        contractDetail.setServicelist(this);
    }

    public void removeContractDetail(ContractDetail contractDetail) {
        contractDetails.remove(contractDetail);
        contractDetail.setServicelist(null);
    }

}

