package com.medco.HealthConnectProvider.entity.persons;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

@Entity
@Table(name = "benefit_balances")
public class BenefitBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Size(min = 36, max = 40)
    private String insuredPersonUuid;

    @Size(min = 24, max = 40)
    private String dependantUuid;

    @Size(min = 36, max = 40)
    private String packageUuid;

    double usedBenefit;
    double usedPoolBenefit;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;

}