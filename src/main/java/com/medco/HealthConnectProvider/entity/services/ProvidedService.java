package com.medco.HealthConnectProvider.entity.services;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "provided_services")
public class ProvidedService extends Audit {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String providedServiceUuid = UUID.randomUUID().toString();

    @Column(nullable = false)
    private String claimUuid;

    @ManyToOne
    @JoinColumn(name = "contract_detail_id", nullable = false)
    private ContractDetail contractDetail;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private Double unitPrice;

    @Column(nullable = false)
    private Double totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insured_id")
    @JsonBackReference(value = "employee-provided-services")
    private Insured insured;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dependant_id")
    @JsonBackReference(value = "dependant-provided-services")
    private Dependant dependant;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "claim_id")
//    @JsonBackReference
//    private Claim claim;


    @OneToOne(mappedBy = "providedService")
    private Claim claim;

}