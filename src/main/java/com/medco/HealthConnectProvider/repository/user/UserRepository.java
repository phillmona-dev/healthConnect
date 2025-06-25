package com.medco.HealthConnectProvider.repository.user;

import com.medco.HealthConnectProvider.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String username);
    boolean existsByEmail(String email);
    boolean existsByMobilePhone(String phone);
    Optional<User> findByUserUuid(String userUuid);
    Page<User> findAllByIsDeleted(boolean b, Pageable pageable);

}
