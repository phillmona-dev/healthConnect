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
@Table(name = "dispensing_rejection", indexes = {
        @Index(name = "idx_dispensing_rejection_uuid", columnList = "dispensing_uuid"),
        @Index(name = "idx_dispensing_rejection_batch", columnList = "batch_code"),
        @Index(name = "idx_dispensing_rejection_status", columnList = "rejection_status"),
        @Index(name = "idx_dispensing_rejection_category", columnList = "rejection_category"),
        @Index(name = "idx_dispensing_rejection_date", columnList = "rejected_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DispensingRejection extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String rejectionUuid;

    @Column(nullable = false)
    private String dispensingUuid;

    @Column(nullable = false)
    private String claimUuid;

    @Column(nullable = false)
    private String batchCode;

    @Column(nullable = false)
    private String contractUuid;

    @Column(nullable = false)
    private String providerUuid;

    @Column(nullable = false)
    private String rejectionCode;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(nullable = false)
    private String rejectionCategory; // ELIGIBILITY, COVERAGE, DOCUMENTATION, DUPLICATE, FRAUD

    @Column(nullable = false)
    private Double rejectedAmount;

    @Column(nullable = false)
    private LocalDateTime rejectedAt;

    @Column(columnDefinition = "TEXT")
    private String reviewerComments;

    @Column(nullable = false)
    @Builder.Default
    private Boolean canResubmit = false;

    @Column(columnDefinition = "TEXT")
    private String requiredDocuments; // JSON array of required documents

    @Column(nullable = false)
    @Builder.Default
    private String rejectionStatus = "ACTIVE"; // ACTIVE, RESOLVED, RESUBMITTED, APPEALED

    @Column(columnDefinition = "TEXT")
    private String externalReferenceId; // External system's reference for this rejection

    @Column(columnDefinition = "TEXT")
    private String originalRequestPayload; // Original request that was rejected

    @Column(columnDefinition = "TEXT")
    private String externalResponsePayload; // Full response from external system

    private LocalDateTime resolvedAt;

    private String resolvedBy;

    @Column(columnDefinition = "TEXT")
    private String resolutionComments;

    private LocalDateTime resubmittedAt;

    private String resubmittedBy;

    @Column(columnDefinition = "TEXT")
    private String resubmissionComments;

    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (rejectionUuid == null) {
            rejectionUuid = UUID.randomUUID().toString();
        }
        if (rejectedAt == null) {
            rejectedAt = LocalDateTime.now();
        }
    }
}
