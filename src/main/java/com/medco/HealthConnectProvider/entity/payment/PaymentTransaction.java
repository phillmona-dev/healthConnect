package com.medco.HealthConnectProvider.entity.payment;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String transactionUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "claim_uuid", referencedColumnName = "claimUuid")
    private Claim claim;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String paymentMethod;

    @Column(nullable = false)
    private LocalDateTime initiatedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    private String chapaTransactionId;

    @Column(nullable = false)
    private String checkoutUrl;

    private String payerName;

    private String payerEmail;

    private String payerPhone;

    @Column(columnDefinition = "TEXT")
    private String transactionDetails;

    @PrePersist
    protected void onCreate() {
        this.initiatedAt = LocalDateTime.now();
    }
}
