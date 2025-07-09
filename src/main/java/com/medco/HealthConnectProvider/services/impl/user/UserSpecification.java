package com.medco.HealthConnectProvider.services.impl.user;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserSpecification {

    public static Specification<User> searchUsers(String search) {



        return (root, query, criteriaBuilder) -> {
            UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
            List<Predicate> predicates = new ArrayList<>();

            // Base condition: not deleted
            predicates.add(criteriaBuilder.equal(root.get("isDeleted"), false));

            // Add provider/payer restrictions (security filtering)
            if (userDetails.getProviderUuid() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("providerUuid"),
                        userDetails.getProviderUuid()));
            }

            if (userDetails.getPayerUuid() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("payerUuid"),
                        userDetails.getPayerUuid()));
            }

            // Add search term conditions (only if search term provided)
            if (StringUtils.hasText(search)) {
                String lowercaseSearch = "%" + search.toLowerCase() + "%";

                List<Predicate> searchPredicates = new ArrayList<>();

                // Text search fields
                searchPredicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("firstName")),
                        lowercaseSearch));
                searchPredicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("fatherName")),
                        lowercaseSearch));
                searchPredicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("mobilePhone")),
                        lowercaseSearch));
                searchPredicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        lowercaseSearch));

                // UUID exact match (if search term looks like UUID)
                try {
                    UUID.fromString(search);
                    searchPredicates.add(criteriaBuilder.equal(
                            root.get("role").get("roleUuid"),
                            search));
                } catch (IllegalArgumentException e) {
                    // Not a valid UUID, skip this condition
                }

                predicates.add(criteriaBuilder.or(
                        searchPredicates.toArray(new Predicate[0])));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };


//        return (root, query, criteriaBuilder) -> {
//            UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
//            List<Predicate> predicates = new ArrayList<>();
//
//            predicates.add(criteriaBuilder.equal(root.get("isDeleted"), false));
//
//            if (search != null && !search.trim().isEmpty()) {
//
//                String lowercaseSearch = "%" + search.toLowerCase() + "%";
//                predicates.add(criteriaBuilder.or(
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), lowercaseSearch),
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fatherName")), lowercaseSearch),
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get("mobilePhone")), lowercaseSearch),
//                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), lowercaseSearch),
//                        criteriaBuilder.equal(root.get("role").get("roleUuid"), search),
//                        criteriaBuilder.equal(root.get("providerUuid"), userDetails.getProviderUuid()),
//                        criteriaBuilder.equal(root.get("payerUuid"), userDetails.getPayerUuid())
//                ));
//            }
//
//            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
//        };
    }
}
