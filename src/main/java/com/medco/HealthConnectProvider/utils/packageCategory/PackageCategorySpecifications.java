package com.medco.HealthConnectProvider.utils.packageCategory;

import com.medco.HealthConnectProvider.entity.packageCategory.PackageCategory;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.jpa.domain.Specification;

public class PackageCategorySpecifications {

    public static Specification<PackageCategory> withPayer(Payer payer) {
        return (root, query, cb) -> payer == null ? cb.conjunction() : cb.equal(root.get("payer"), payer);
    }

    public static Specification<PackageCategory> withStatus(Status status) {
        return (root, query, cb) -> status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<PackageCategory> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("isDeleted"));
    }

    public static Specification<PackageCategory> withSearchKey(String searchKey) {
        return (root, query, cb) -> {
            if (searchKey == null || searchKey.isEmpty()) {
                return cb.conjunction();
            }
            String likePattern = "%" + searchKey.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("categoryName")), likePattern),
                    cb.like(cb.lower(root.get("categoryCode")), likePattern)
            );
        };
    }
}
