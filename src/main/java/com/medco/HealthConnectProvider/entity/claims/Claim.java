package com.medco.HealthConnectProvider.entity.claims;

import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.ClaimStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serial;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "claims", indexes = {
        @Index(name = "idx_claim_payer_status", columnList = "payer_uuid, status"),
        @Index(name = "idx_claim_provider_status", columnList = "provider_uuid, status"),
        @Index(name = "idx_claim_payer_provider_status", columnList = "payer_uuid, provider_uuid, status")
})
public class Claim extends Audit {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private String claimUuid = UUID.randomUUID().toString();

    @Column(unique = true)
    private Long claimNumber;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ClaimStatus status;

    @Column(name = "provider_uuid")
    private String providerUuid;

    @Column(name = "payer_uuid")
    private String payerUuid;

    @Column(nullable = false)
    private String mrnNumber;

    @Column(nullable = false)
    private LocalDateTime visitDate;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    private String claimType;

    @Column(nullable = false)
    private LocalDate serviceDate;

    @Column(length = 1000)
    private String providerComment;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime submissionDate;

    @Column(nullable = false)
    private String submittedByUuid;

    @Column(nullable = false)
    private String submittedByName;

    private String preparedByProviderUuid;

    private String preparedByProviderStatus;

    private LocalDateTime preparedByProviderDate;

    private String approvedByProviderUuid;
    private String approvedByProviderStatus;
    private LocalDateTime approvedByProviderDate;

    private String approvedByPayerUuid;
    private String approvedByPayerStatus;
    private LocalDateTime approvedByPayerDate;

    private String paymentRequestedByUuid;
    private LocalDateTime paymentRequestedDate;
    private String paidByPayerUuid;
    private String paidByPayerName;
    private LocalDateTime paidDate;
    private String paidStatus;
    private String paymentCode;
    private String paymentType;
    private String checkNumber;
    private String fromBank;
    private String toBank;
    private String transactionNumber;

    private String cancelledByUuid;
    private LocalDateTime cancelledDate;

    @Column( precision = 10, scale = 2)
    private BigDecimal copayAmount;

    @Column( precision = 10, scale = 2)
    private BigDecimal deductibleAmount;

    private String diagnosisCodes;

    private String procedureCodes;

    @Column(length = 1000)
    private String reviewComment;

    @Column
    private String reviewedByUuid;

    @Column
    private Instant reviewedAt;

    @OneToOne
    @JoinColumn(name = "batch_record_id")
    private BatchRecord batchRecord;

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimAttachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimComment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimLogs> logs = new ArrayList<>();

    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimPayment> payments = new ArrayList<>();


    @UpdateTimestamp
    private LocalDateTime lastUpdated;

    public void addAttachment(ClaimAttachment attachment) {
        attachments.add(attachment);
        attachment.setClaim(this);
    }

    public void removeAttachment(ClaimAttachment attachment) {
        attachments.remove(attachment);
        attachment.setClaim(null);
    }

    public void addComment(ClaimComment comment) {
        comments.add(comment);
        comment.setClaim(this);
    }

    public void removeComment(ClaimComment comment) {
        comments.remove(comment);
        comment.setClaim(null);
    }

    public void addLog(ClaimLogs log) {
        logs.add(log);
        log.setClaim(this);
        log.setClaimUuid(this.claimUuid);
    }

    public void removeLog(ClaimLogs log) {
        logs.remove(log);
        log.setClaim(null);
    }

    public void addPayment(ClaimPayment payment) {
        payments.add(payment);
        payment.setClaim(this);
    }

    public void removePayment(ClaimPayment payment) {
        payments.remove(payment);
        payment.setClaim(null);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Claim)) return false;
        return id != null && id.equals(((Claim) o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}