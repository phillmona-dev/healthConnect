package com.medco.HealthConnectProvider.services.impl.user;

import com.medco.HealthConnectProvider.entity.user.Privilege;
import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.repository.user.PrivilegeRepository;
import com.medco.HealthConnectProvider.repository.user.RoleRepository;
import com.medco.HealthConnectProvider.services.user.RoleService;
import com.medco.HealthConnectProvider.ui.request.auth.password.RoleRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.auth.PrivilegeResponse;
import com.medco.HealthConnectProvider.ui.response.auth.RoleResponse;
import com.medco.HealthConnectProvider.utils.paginationUtils.Pagination;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    private final PrivilegeRepository privilegeRepository;

    public RoleServiceImpl(RoleRepository roleRepository, PrivilegeRepository privilegeRepository) {
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    public ResponseEntity<PagedResponse<RoleResponse>> createRole(RoleRequest roleRequest) {
        var role = new Role();
        BeanUtils.copyProperties(roleRequest, role);

        List<Privilege> privilegeList = roleRequest.getPrivilegeUuid()
                .stream()
                .map(privilegeUuid -> {
                    return privilegeRepository.findByPrivilegeUuid(privilegeUuid)
                            .orElseThrow(() -> new BadRequestException("Can't find Privilege with the provided ID: " + privilegeUuid));
                }).collect(Collectors.toList());

        role.setPrivileges(privilegeList);
        privilegeList.forEach(privilege -> privilege.getRoles().add(role));

        Role savedRole = roleRepository.save(role);

        RoleResponse roleResponse = new RoleResponse();
        BeanUtils.copyProperties(savedRole, roleResponse);

        List<PrivilegeResponse> privilegeResponses = savedRole.getPrivileges().stream()
                .map(privilege -> {
                    PrivilegeResponse privilegeResponse = new PrivilegeResponse();
                    privilegeResponse.setPrivilegeUuid(privilege.getPrivilegeUuid());
                    privilegeResponse.setPrivilegeName(privilege.getPrivilegeName());
                    privilegeResponse.setPrivilegeDescription(privilege.getPrivilegeDescription());
                    privilegeResponse.setPrivilegeCategory(privilege.getPrivilegeCategory());
                    return privilegeResponse;
                })
                .collect(Collectors.toList());

        roleResponse.setPrivilegeList(privilegeResponses);

        PagedResponse<RoleResponse> pagedResponse = new PagedResponse<>(
                List.of(roleResponse),
                1,
                1,
                1,
                1,
                true
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(pagedResponse);
    }

    @Override
    public RoleResponse getRoleByUuid(String roleUuid) {

        Role role = roleRepository.findByRoleUuid(roleUuid);

        if (role == null) {
            throw new RuntimeException("Role not found with UUID: " + roleUuid);
        }

        var response = new RoleResponse();

        BeanUtils.copyProperties(role, response);

        response.setPrivilegeList(
                role.getPrivileges().stream()
                        .map(privilege -> new PrivilegeResponse(
                                privilege.getPrivilegeUuid(),
                                privilege.getPrivilegeName(),
                                privilege.getPrivilegeDescription(),
                                privilege.getPrivilegeCategory())
                        )
                        .collect(Collectors.toList())
        );

        return response;
    }

    @Override
    public PagedResponse<RoleResponse> getAllRoles(String search, int page, int limit) {
        Pageable pageable = Pagination.paginateResource(page, limit, "id", "desc");
        Page<Role> rolePage = search != null ?
                (Page<Role>) roleRepository.findAllByRoleNameContaining(search, pageable) :
                roleRepository.findAll(pageable);

        List<RoleResponse> roleResponses = rolePage.getContent().stream()
                .map(this::mapRoleToResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                roleResponses,
                rolePage.getNumber(),
                rolePage.getSize(),
                rolePage.getTotalElements(),
                rolePage.getTotalPages(),
                rolePage.isLast()
        );
    }

    private RoleResponse mapRoleToResponse(Role role) {
        List<PrivilegeResponse> privilegeResponses = role.getPrivileges().stream()
                .map(privilege -> new PrivilegeResponse(
                        privilege.getPrivilegeUuid(),
                        privilege.getPrivilegeName(),
                        privilege.getPrivilegeDescription(),
                        privilege.getPrivilegeCategory()
                ))
                .collect(Collectors.toList());

        return new RoleResponse(
                role.getRoleUuid(),
                role.getRoleName(),
                role.getRoleDescription(),
                privilegeResponses
        );
    }

    @Override
    public ResponseEntity<?> deleteRole(String roleUuid) {

        Role role = roleRepository.findByRoleUuid(roleUuid);

        if (role == null) {
            throw new BadRequestException("Can't find Role with the provided ID");
        }

        roleRepository.delete(role);

        return ResponseEntity.ok("Role removed successfully");
    }

    private List<RoleResponse> getAllWithOutSearch(Pageable pageable) {
        return roleRepository.findAll(pageable)
                .stream()
                .map(role -> new RoleResponse(role.getRoleUuid(), role.getRoleName(),role.getRoleDescription(), role.getPrivileges()
                        .stream()
                        .map(privilege -> new PrivilegeResponse(privilege.getPrivilegeUuid(), privilege.getPrivilegeName(), privilege.getPrivilegeDescription(), privilege.getPrivilegeCategory()))
                        .collect(Collectors.toList())
                )).collect(Collectors.toList());
    }

    private List<RoleResponse> getAllRolesBySearch(String search, Pageable pageable) {
        return roleRepository.findAllByRoleNameContaining(search,pageable)
                .stream()
                .map(role -> new RoleResponse(role.getRoleUuid(),role.getRoleName(), role.getRoleDescription(), role.getPrivileges()
                        .stream()
                        .map(privilege -> new PrivilegeResponse(privilege.getPrivilegeUuid(), privilege.getPrivilegeName(), privilege.getPrivilegeDescription(), privilege.getPrivilegeCategory()))
                        .collect(Collectors.toList())
                )).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<?> updateRole(String roleUuid, RoleRequest roleUpdateRequest) {
        Role role = roleRepository.findByRoleUuid(roleUuid);
        if (role == null) {
            throw new BadRequestException("Can't find Role for Update");
        }

        role.setRoleName(roleUpdateRequest.getRoleName());
        role.setRoleDescription(roleUpdateRequest.getRoleDescription());

        // Remove the role from all existing privileges
        role.getPrivileges().forEach(privilege -> privilege.getRoles().remove(role));

        // Clear existing privileges from the role
        role.getPrivileges().clear();

        List<String> privilegeUuids = roleUpdateRequest.getPrivilegeUuid();
        if (privilegeUuids != null && !privilegeUuids.isEmpty()) {
            List<Privilege> privilegeList = privilegeUuids.stream()
                    .map(privilegeUuid -> privilegeRepository.findByPrivilegeUuid(privilegeUuid)
                            .orElseThrow(() -> new BadRequestException("Can't find Privilege with the Provided Id: " + privilegeUuid)))
                    .collect(Collectors.toList());

            role.setPrivileges(privilegeList);
            privilegeList.forEach(privilege -> {
                if (!privilege.getRoles().contains(role)) {
                    privilege.getRoles().add(role);
                }
            });
        }

        Role updatedRole = roleRepository.save(role);

        // Fetch the updated role to ensure we have the latest data
        Role fetchedRole = roleRepository.findByRoleUuid(updatedRole.getRoleUuid());

        RoleResponse roleResponse = new RoleResponse();

        BeanUtils.copyProperties(fetchedRole, roleResponse);
        roleResponse.setPrivilegeList(fetchedRole.getPrivileges().stream()
                .map(privilege -> {
                    PrivilegeResponse privilegeResponse = new PrivilegeResponse();
                    BeanUtils.copyProperties(privilege, privilegeResponse);
                    return privilegeResponse;
                })
                .collect(Collectors.toList()));

        return ResponseEntity.ok(roleResponse);

    }

}