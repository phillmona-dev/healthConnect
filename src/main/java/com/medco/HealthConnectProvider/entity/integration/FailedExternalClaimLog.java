package com.medco.HealthConnectProvider.entity.integration;

import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "failed_external_claim_log", indexes = {
        @Index(name = "idx_failed_claim_status", columnList = "status"),
        @Index(name = "idx_failed_claim_retry_count", columnList = "retry_count"),
        @Index(name = "idx_failed_claim_next_retry", columnList = "next_retry_at"),
        @Index(name = "idx_failed_claim_batch_code", columnList = "batch_code"),
        @Index(name = "idx_failed_claim_uuid", columnList = "claim_uuid")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedExternalClaimLog extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String logUuid;

    @Column(nullable = false)
    private String claimUuid;

    @Column(nullable = false)
    private String batchCode;

    @Column(nullable = false)
    private String contractUuid;

    @Column(nullable = false)
    private String providerUuid;

    @Column(nullable = false)
    private String claimFromDate;

    @Column(nullable = false)
    private String claimToDate;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(columnDefinition = "TEXT")
    private String serviceProvidedUuids; // JSON array of UUIDs

    @Column(columnDefinition = "TEXT")
    private String externalApiUrl;

    @Column(columnDefinition = "TEXT")
    private String requestPayload; // JSON payload sent to external system

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(columnDefinition = "TEXT")
    private String lastResponse;

    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxRetries = 5;

    @Column(nullable = false)
    private LocalDateTime nextRetryAt;

    @Column(nullable = false)
    private LocalDateTime firstFailedAt;

    private LocalDateTime lastAttemptAt;

    private LocalDateTime succeededAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE; // ACTIVE = pending retry, INACTIVE = max retries reached, COMPLETED = successfully sent

    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (logUuid == null) {
            logUuid = UUID.randomUUID().toString();
        }
        if (firstFailedAt == null) {
            firstFailedAt = LocalDateTime.now();
        }
        if (nextRetryAt == null) {
            nextRetryAt = LocalDateTime.now().plusHours(1);
        }
    }
}
