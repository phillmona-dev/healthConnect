package com.medco.HealthConnectProvider.specifications;

import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.data.jpa.domain.Specification;

public class PayerSpecifications {

    public static Specification<Payer> isNotDeleted() {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("isDeleted"), false);
    }

    public static Specification<Payer> containsSearchKey(String searchKey) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.or(
                criteriaBuilder.like(criteriaBuilder.lower(root.get("payerName")), "%" + searchKey.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), "%" + searchKey.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("telephone")), "%" + searchKey.toLowerCase() + "%"),
                criteriaBuilder.like(criteriaBuilder.lower(root.get("payerInsuranceNumber")), "%" + searchKey.toLowerCase() + "%")
            );
    }

    public static Specification<Payer> hasStatus(Status status) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Payer> hasCategory(String category) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(criteriaBuilder.lower(root.get("category")), category.toLowerCase());
    }

    public static Specification<Payer> hasPayerName(String payerName) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.like(criteriaBuilder.lower(root.get("payerName")), "%" + payerName.toLowerCase() + "%");
    }

    public static Specification<Payer> hasTinNumber(Long tinNumber) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(root.get("tinNumber"), tinNumber);
    }

    public static Specification<Payer> hasLevel(String level) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.equal(criteriaBuilder.lower(root.get("level")), level.toLowerCase());
    }
}