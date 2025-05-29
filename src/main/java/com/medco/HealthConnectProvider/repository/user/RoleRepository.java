package com.medco.HealthConnectProvider.repository.user;

import com.medco.HealthConnectProvider.entity.user.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleUuid(String roleUuid);

    List<Role> findAllByRoleNameContaining(String search, Pageable pageable);
}
