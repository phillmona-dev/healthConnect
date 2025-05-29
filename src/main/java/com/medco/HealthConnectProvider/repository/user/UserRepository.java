package com.medco.HealthConnectProvider.repository.user;

import com.medco.HealthConnectProvider.entity.user.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String username);
    boolean existsByEmail(String email);
    boolean existsByMobilePhone(String phone);

    Optional<User> findByUserUuid(String userUuid);

    List<User> findAllByIsDeletedAndFirstNameContainingOrMobilePhoneContaining(boolean b, String search, String search1, Pageable pageable);
}
