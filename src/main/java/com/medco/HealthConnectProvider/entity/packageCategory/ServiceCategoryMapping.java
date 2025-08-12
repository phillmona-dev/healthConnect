package com.medco.HealthConnectProvider.entity.packageCategory;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "service_category_mappings", indexes = {
        @Index(name = "idx_service_category_contract", columnList = "contract_detail_id"),
        @Index(name = "idx_service_category_package", columnList = "package_category_id"),
        @Index(name = "idx_service_category_unique", columnList = "contract_detail_id, package_category_id", unique = true)
})
@Where(clause = "is_deleted = false")
public class ServiceCategoryMapping extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String mappingUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_detail_id", nullable = false)
    private ContractDetail contractDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_category_id", nullable = false)
    private PackageCategory packageCategory;

    @Builder.Default
    private boolean consumesFromLimit = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Builder.Default
    private boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (mappingUuid == null) {
            mappingUuid = UUID.randomUUID().toString();
        }
    }
}
