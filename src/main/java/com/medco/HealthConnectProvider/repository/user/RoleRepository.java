package com.medco.HealthConnectProvider.repository.user;

import com.medco.HealthConnectProvider.entity.user.Role;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {
    Role findByRoleUuid(String roleUuid);

    List<Role> findAllByRoleNameContaining(String search, Pageable pageable);

    Role findByRoleName(String roleName);
}
