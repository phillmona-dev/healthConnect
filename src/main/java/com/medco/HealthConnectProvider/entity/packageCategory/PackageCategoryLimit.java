package com.medco.HealthConnectProvider.entity.packageCategory;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.LimitType;
import com.medco.HealthConnectProvider.utils.enums.PeriodType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "package_category_limits", indexes = {
        @Index(name = "idx_category_limit_contract", columnList = "contract_header_id"),
        @Index(name = "idx_category_limit_category", columnList = "package_category_id"),
        @Index(name = "idx_category_limit_period", columnList = "period_type, reset_date")
})
@Where(clause = "is_deleted = false")
@SequenceGenerator(name = "package_category_limit_seq", sequenceName = "package_category_limit_seq", allocationSize = 1)
public class PackageCategoryLimit extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "package_category_limit_seq")
    private Long id;

    @Column(unique = true, nullable = false)
    private String limitUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_category_id", nullable = false)
    private PackageCategory packageCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_header_id", nullable = false)
    private ContractHeader contractHeader;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LimitType limitType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal limitValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PeriodType periodType;

    @Column(nullable = false)
    private LocalDate resetDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    private boolean isActive = true;

    @Builder.Default
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "categoryLimit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PackageCategoryUsage> usageRecords = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (limitUuid == null) {
            limitUuid = UUID.randomUUID().toString();
        }
    }

    public void addUsageRecord(PackageCategoryUsage usage) {
        usageRecords.add(usage);
        usage.setCategoryLimit(this);
    }

    public void removeUsageRecord(PackageCategoryUsage usage) {
        usageRecords.remove(usage);
        usage.setCategoryLimit(null);
    }

    public BigDecimal getRemainingLimit() {
        BigDecimal totalUsed = usageRecords.stream()
                .filter(usage -> !usage.isDeleted())
                .map(PackageCategoryUsage::getUsedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return limitValue.subtract(totalUsed);
    }

    public boolean hasAvailableLimit(BigDecimal requestedAmount) {
        return getRemainingLimit().compareTo(requestedAmount) >= 0;
    }

    public boolean isExpired() {
        return LocalDate.now().isAfter(resetDate);
    }
}
