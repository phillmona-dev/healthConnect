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
@Table(name = "failed_external_dispensing_log", indexes = {
        @Index(name = "idx_failed_dispensing_status", columnList = "status"),
        @Index(name = "idx_failed_dispensing_retry_count", columnList = "retry_count"),
        @Index(name = "idx_failed_dispensing_next_retry", columnList = "next_retry_at"),
        @Index(name = "idx_failed_dispensing_item", columnList = "dispensing_item_uuid")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FailedExternalDispensingLog extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String logUuid;

    @Column(nullable = false)
    private String dispensingItemUuid;

    @Column(nullable = false)
    private String dispensingUuid;

    @Column(nullable = false)
    private String contractHeaderUuid;

    @Column(nullable = false)
    private String serviceId;

    @Column(nullable = false)
    private String insuredUuid;

    @Column(nullable = false)
    private String packageUuid;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false)
    private String providedDate;

    @Column(nullable = false)
    private String providerUuid;

    @Column(columnDefinition = "TEXT")
    private String externalApiUrl;

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
            nextRetryAt = LocalDateTime.now().plusHours(1); // Default retry in 1 hour
        }
    }
}
