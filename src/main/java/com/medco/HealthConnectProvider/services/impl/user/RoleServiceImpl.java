package com.medco.HealthConnectProvider.services.impl.user;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
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
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        var role = new Role();
        BeanUtils.copyProperties(roleRequest, role);

        List<Privilege> privilegeList = roleRequest.getPrivilegeUuid()
                .stream()
                .map(privilegeUuid -> {
                    return privilegeRepository.findByPrivilegeUuid(privilegeUuid)
                            .orElseThrow(() -> new BadRequestException("Can't find Privilege with the provided ID: " + privilegeUuid));
                }).collect(Collectors.toList());

        role.setPrivileges(privilegeList);
        if (userDetails.getPayerUuid()!=null)
         role.setPayerUuid(userDetails.getPayerUuid());
        else if (userDetails.getProviderUuid()!=null) {
            role.setProviderUuid(userDetails.getProviderUuid());
        }

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
                        .map(privilege -> {
                            PrivilegeResponse response1=new PrivilegeResponse();
                            BeanUtils.copyProperties(privilege,response1);
                            return response1;
                                }
                        )
                        .toList()
        );

        return response;
    }

    @Override
    public PagedResponse<RoleResponse> getAllRoles(String search, int page, int limit) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();

        Pageable pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "id"));
//
//        Specification<Role> spec = (root, query, cb) -> {
        Specification<Role> spec = Specification.where(null);

            if (userDetails.getProviderUuid() != null && !userDetails.getProviderUuid().isEmpty()) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("providerUuid"), userDetails.getProviderUuid()));
            }

            if (userDetails.getPayerUuid() != null && !userDetails.getPayerUuid().isEmpty()) {
                spec = spec.and((root, query, cb) ->
                        cb.equal(root.get("payerUuid"), userDetails.getPayerUuid()));
            }

            if (search != null && !search.trim().isEmpty()) {
                String likeSearch = "%" + search.trim().toLowerCase() + "%";
                spec = spec.and((root, query, cb) ->
                        cb.like(cb.lower(root.get("roleName")), likeSearch));
            }

        Page<Role> rolePage = roleRepository.findAll(spec, pageable);

        List<RoleResponse> roleResponses = rolePage.getContent().stream()
                .map(this::mapRoleToResponse)
                .collect(Collectors.toList());

        return new PagedResponse<>(
                roleResponses,
                rolePage.getNumber() + 1,
                rolePage.getSize(),
                rolePage.getTotalElements(),
                rolePage.getTotalPages(),
                rolePage.isLast()
        );
    }

    private RoleResponse mapRoleToResponse(Role role) {
        List<PrivilegeResponse> privilegeResponses = role.getPrivileges().stream()
                .map(privilege -> {
                            PrivilegeResponse response1=new PrivilegeResponse();
                            BeanUtils.copyProperties(privilege,response1);
                            return response1;
                        }
                )
                .toList();

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
                        .map(privilege -> {
                                    PrivilegeResponse response1=new PrivilegeResponse();
                                    BeanUtils.copyProperties(privilege,response1);
                                    return response1;
                                }
                        )
                        .toList()
                )).collect(Collectors.toList()
                );
    }

    private List<RoleResponse> getAllRolesBySearch(String search, Pageable pageable) {
        return roleRepository.findAllByRoleNameContaining(search,pageable)
                .stream()
                .map(role -> new RoleResponse(role.getRoleUuid(),role.getRoleName(), role.getRoleDescription(), role.getPrivileges()
                        .stream()
                        .map(privilege -> {
                                    PrivilegeResponse response1=new PrivilegeResponse();
                                    BeanUtils.copyProperties(privilege,response1);
                                    return response1;
                                }
                        )
                        .toList()
                )).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ResponseEntity<RoleResponse> updateRole(String roleUuid, RoleRequest roleUpdateRequest) {
        Role role = roleRepository.findByRoleUuid(roleUuid);
        if (role == null) {
            throw new BadRequestException("Can't find Role with UUID: " + roleUuid);
        }

        role.setRoleName(roleUpdateRequest.getRoleName());
        role.setRoleDescription(roleUpdateRequest.getRoleDescription());

        role.getPrivileges().forEach(privilege -> privilege.getRoles().remove(role));
        role.getPrivileges().clear();

        List<Privilege> privilegeList = roleUpdateRequest.getPrivilegeUuid().stream()
                .map(privilegeUuid -> privilegeRepository.findByPrivilegeUuid(privilegeUuid)
                        .orElseThrow(() -> new BadRequestException("Can't find Privilege with the provided ID: " + privilegeUuid)))
                .collect(Collectors.toList());

        role.setPrivileges(privilegeList);
        privilegeList.forEach(privilege -> privilege.getRoles().add(role));

        Role updatedRole = roleRepository.save(role);

        RoleResponse roleResponse = new RoleResponse();
        BeanUtils.copyProperties(updatedRole, roleResponse);

        List<PrivilegeResponse> privilegeResponses = updatedRole.getPrivileges().stream()
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

        return ResponseEntity.ok(roleResponse);
    }

}