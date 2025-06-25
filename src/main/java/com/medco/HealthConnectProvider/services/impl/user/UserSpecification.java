package com.medco.HealthConnectProvider.services.impl.user;

import com.medco.HealthConnectProvider.entity.user.User;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class UserSpecification {

    public static Specification<User> searchUsers(String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.equal(root.get("isDeleted"), false));

            if (search != null && !search.trim().isEmpty()) {
                String lowercaseSearch = "%" + search.toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), lowercaseSearch),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fatherName")), lowercaseSearch),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("mobilePhone")), lowercaseSearch),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), lowercaseSearch),
                        criteriaBuilder.equal(root.get("role").get("roleUuid"), search),
                        criteriaBuilder.equal(root.get("providerUuid"), search),
                        criteriaBuilder.equal(root.get("payerUuid"), search)
                ));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
