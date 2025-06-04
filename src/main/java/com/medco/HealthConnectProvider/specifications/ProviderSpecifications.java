package com.medco.HealthConnectProvider.specifications;

import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.jpa.domain.Specification;

public class ProviderSpecifications {

    public static Specification<Provider> isNotDeleted() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("isDeleted"), false);
    }

    public static Specification<Provider> containsSearchKey(String searchKey) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("providerName")), "%" + searchKey.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + searchKey.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + searchKey.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("telephone")), "%" + searchKey.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("tinNumber")), "%" + searchKey.toLowerCase() + "%")
            );
    }

    public static Specification<Provider> hasStatus(Status status) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Provider> hasCategory(String category) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.toLowerCase());
    }

    public static Specification<Provider> hasProviderName(String providerName) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.like(criteriaBuilder.lower(root.get("providerName")), "%" + providerName.toLowerCase() + "%");
    }

    public static Specification<Provider> hasTinNumber(String tinNumber) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.like(criteriaBuilder.lower(root.get("tinNumber")), "%" + tinNumber.toLowerCase() + "%");
    }

    public static Specification<Provider> hasLevel(String level) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(criteriaBuilder.lower(root.get("level")), level.toLowerCase());
    }
}