package com.medco.HealthConnectProvider.entity.claims;

import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.util.Date;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claim_payments")
public class ClaimPayment extends Audit {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String paymentUuid = UUID.randomUUID().toString();

    @ManyToOne
    @JoinColumn(name = "claim_id", nullable = false)
    private Claim claim;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String paymentType; // BANK_TRANSFER, CHECK, CASH

    private String checkNumber;

    private String fromBank;

    private String toBank;

    private String transactionNumber;

    @Column(nullable = false)
    private Date paymentDate;

    @Column(nullable = false)
    private String paidByUuid;

    private String paidByName;

}