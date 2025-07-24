package com.medco.HealthConnectProvider.repository.user;

import com.medco.HealthConnectProvider.entity.user.Privilege;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrivilegeRepository extends JpaRepository<Privilege,Long> {

    Optional<Privilege> findByPrivilegeUuid(String privilegeUuid);

    Page<Privilege> findAllByPrivilegeNameContainingIgnoreCaseOrPrivilegeCategoryContainingIgnoreCase(String search, String search1,Pageable pageable);

    boolean existsByPrivilegeName(String privilegeName);

    List<Privilege> findByPrivilegeNameIn(List<String> names);

    Optional<Privilege> findByPrivilegeName(String privilegeName);
}
