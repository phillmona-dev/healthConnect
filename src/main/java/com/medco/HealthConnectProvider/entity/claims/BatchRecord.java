package com.medco.HealthConnectProvider.entity.claims;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "batch_records")
public class BatchRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String batchCode;

    private String payerName;
    private LocalDateTime requestedOn;
    private LocalDate claimDatingFrom;
    private LocalDate claimDatingTo;
    private BigDecimal totalAmount;
    private String status;
    private String claimUuid;

    @OneToOne
    @JoinColumn(name = "claim_id", referencedColumnName = "id")
    private Claim claim;

}
