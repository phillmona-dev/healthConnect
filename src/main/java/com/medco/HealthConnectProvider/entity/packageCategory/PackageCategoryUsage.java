package com.medco.HealthConnectProvider.entity.packageCategory;

import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "package_category_usage", indexes = {
        @Index(name = "idx_usage_insured_category", columnList = "insured_person_id, category_limit_id"),
        @Index(name = "idx_usage_period", columnList = "period_start_date, period_end_date"),
        @Index(name = "idx_usage_claim", columnList = "claim_uuid"),
        @Index(name = "idx_usage_service", columnList = "service_uuid")
})
@Where(clause = "is_deleted = false")
public class PackageCategoryUsage extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String usageUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insured_person_id", nullable = false)
    private Insured insuredPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_limit_id", nullable = false)
    private PackageCategoryLimit categoryLimit;

    @Column(nullable = false)
    private LocalDate periodStartDate;

    @Column(nullable = false)
    private LocalDate periodEndDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal usedAmount;

    @Column(nullable = false)
    private Double usedQuantity;

    @Column(nullable = false)
    private Integer usedVisits;

    @Column(nullable = false)
    private LocalDateTime serviceDate;

    @Column(nullable = false)
    private String serviceUuid;

    @Column(nullable = false)
    private String serviceName;

    @Column(nullable = true)
    private String claimUuid;

    @Column(nullable = true)
    private String providedServiceUuid;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (usageUuid == null) {
            usageUuid = UUID.randomUUID().toString();
        }
    }

    public BigDecimal getRemainingAmount() {
        return categoryLimit.getLimitValue().subtract(usedAmount);
    }

    public boolean isWithinPeriod(LocalDate checkDate) {
        return !checkDate.isBefore(periodStartDate) && !checkDate.isAfter(periodEndDate);
    }

    public boolean canAccommodateAmount(BigDecimal requestedAmount) {
        return getRemainingAmount().compareTo(requestedAmount) >= 0;
    }
}
