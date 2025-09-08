package com.medco.HealthConnectProvider.entity.packageCategory;

import com.medco.HealthConnectProvider.entity.contracts.ContractDetail;
import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.context.annotation.DependsOn;

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
@SequenceGenerator(name = "service_category_mapping_seq", sequenceName = "service_category_mapping_seq", allocationSize = 1)
@EntityListeners(ServiceCategoryMappingEntityListener.class)
@DependsOn("contract_details")
public class ServiceCategoryMapping extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "service_category_mapping_seq")
    private Long id;

    @Column(unique = true, nullable = false)
    private String mappingUuid;

    @Column(name = "contract_detail_id", insertable = false, updatable = false)
    private Long contractDetailId;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_detail_id")
    private ContractDetail contractDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_category_id", nullable = false)
    private PackageCategory packageCategory;

    private String serviceUuid;
    private String categoryUuid;
    private String providerUuid;

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
        // Populate UUID fields from relationships
        populateUuidFields();
    }

    @PreUpdate
    public void preUpdate() {
        // Ensure UUID fields are synchronized with relationships
        populateUuidFields();
    }

    private void populateUuidFields() {
        if (contractDetail != null) {
            if (contractDetail.getServicelist() != null && serviceUuid == null) {
                serviceUuid = contractDetail.getServicelist().getServiceUuid();
            }
            if (contractDetail.getContractHeader() != null &&
                contractDetail.getContractHeader().getProvider() != null && providerUuid == null) {
                providerUuid = contractDetail.getContractHeader().getProvider().getProviderUuid();
            }
        }
        if (packageCategory != null && categoryUuid == null) {
            categoryUuid = packageCategory.getCategoryUuid();
        }
    }

    public void setContractDetail(ContractDetail contractDetail) {
        this.contractDetail = contractDetail;
        // Automatically populate UUID fields when contract detail is set
        if (contractDetail != null) {
            if (contractDetail.getServicelist() != null) {
                this.serviceUuid = contractDetail.getServicelist().getServiceUuid();
            }
            if (contractDetail.getContractHeader() != null &&
                contractDetail.getContractHeader().getProvider() != null) {
                this.providerUuid = contractDetail.getContractHeader().getProvider().getProviderUuid();
            }
        }
    }

    public void setPackageCategory(PackageCategory packageCategory) {
        this.packageCategory = packageCategory;
        // Automatically populate categoryUuid when package category is set
        if (packageCategory != null) {
            this.categoryUuid = packageCategory.getCategoryUuid();
        }
    }
}
