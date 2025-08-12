package com.medco.HealthConnectProvider.entity.packageCategory;

import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "package_categories", indexes = {
        @Index(name = "idx_package_category_payer", columnList = "payer_id"),
        @Index(name = "idx_package_category_code", columnList = "category_code"),
        @Index(name = "idx_package_category_status", columnList = "status")
})
@Where(clause = "is_deleted = false")
public class PackageCategory extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String categoryUuid;

    @Column(nullable = false, length = 100)
    private String categoryName;

    @Column(unique = true, nullable = false, length = 10)
    private String categoryCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @Builder.Default
    private boolean isDeleted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id", nullable = false)
    private Payer payer;

    @OneToMany(mappedBy = "packageCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PackageCategoryLimit> categoryLimits = new ArrayList<>();

    @OneToMany(mappedBy = "packageCategory", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ServiceCategoryMapping> serviceCategoryMappings = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (categoryUuid == null) {
            categoryUuid = UUID.randomUUID().toString();
        }
    }

    // Helper methods for bidirectional relationships
    public void addCategoryLimit(PackageCategoryLimit limit) {
        categoryLimits.add(limit);
        limit.setPackageCategory(this);
    }

    public void removeCategoryLimit(PackageCategoryLimit limit) {
        categoryLimits.remove(limit);
        limit.setPackageCategory(null);
    }

    public void addServiceCategoryMapping(ServiceCategoryMapping mapping) {
        serviceCategoryMappings.add(mapping);
        mapping.setPackageCategory(this);
    }

    public void removeServiceCategoryMapping(ServiceCategoryMapping mapping) {
        serviceCategoryMappings.remove(mapping);
        mapping.setPackageCategory(null);
    }
}
