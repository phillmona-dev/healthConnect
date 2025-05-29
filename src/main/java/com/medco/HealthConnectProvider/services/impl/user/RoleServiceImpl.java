package com.medco.HealthConnectProvider.services.impl.user;

import com.medco.HealthConnectProvider.entity.user.Privilege;
import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.repository.user.PrivilegeRepository;
import com.medco.HealthConnectProvider.repository.user.RoleRepository;
import com.medco.HealthConnectProvider.services.user.RoleService;
import com.medco.HealthConnectProvider.ui.request.auth.password.RoleRequest;
import com.medco.HealthConnectProvider.ui.response.auth.PrivilegeResponse;
import com.medco.HealthConnectProvider.ui.response.auth.RoleResponse;
import com.medco.HealthConnectProvider.utils.paginationUtils.Pagination;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.beans.beancontext.BeanContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class RoleServiceImpl implements RoleService {

    private RoleRepository roleRepository;

    private PrivilegeRepository privilegeRepository;

    public RoleServiceImpl(RoleRepository roleRepository, PrivilegeRepository privilegeRepository) {
        this.roleRepository = roleRepository;
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    public ResponseEntity<?> createRole(RoleRequest roleRequest) {

        var role = new Role();
        BeanUtils.copyProperties(roleRequest, role);

        List<Privilege> privilegeList = roleRequest.getPrivilegeUuid()
                .stream()
                .map(privilegeUuid -> {
                    Privilege privilege = privilegeRepository.findByPrivilegeUuid(privilegeUuid)
                            .orElseThrow(() -> new BadRequestException("can't find Privilege With the provided Id "+ privilegeUuid));

                    return privilege;
                }).collect(Collectors.toList());
        role.setPrivileges(privilegeList);
        privilegeList.forEach(privilege -> privilege.getRoles().add(role));

        roleRepository.save(role);
        return ResponseEntity.ok("Role Added Successfully");
    }

    @Override
    public RoleResponse getRoleByUuid(String roleUuid) {
        var response = new RoleResponse();

        Optional<Role> role = Optional.ofNullable(roleRepository.findByRoleUuid(roleUuid)
                .orElse(null));

        BeanUtils.copyProperties(role.get(), response);
        response.setPrivilegeList(
                role.get().getPrivileges().stream()
                        .map(privilege -> new PrivilegeResponse(privilege.getPrivilegeUuid(), privilege.getPrivilegeName(), privilege.getPrivilegeDescription(), privilege.getPrivilegeCategory())
                        ).collect(Collectors.toList())
        );

        return response;
    }

    @Override
    public List<RoleResponse> getAllRoles(String search, int page, int limit) {
        Pageable pageable = Pagination.paginateResource(page,limit,"id","desc");
        return search != null ? getAllRolesBySearch(search,pageable) : getAllWithOutSearch(pageable);
    }

    @Override
    public ResponseEntity<?> deleteRole(String roleUuid) {
        roleRepository.delete(
                roleRepository.findByRoleUuid(roleUuid)
                        .orElseThrow(() -> new BadRequestException("Can't find Role With The Provided Id"))
        );

        return ResponseEntity.ok("Role Removed Successfully");
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
    public ResponseEntity<?> updateRole(String roleUuid, RoleRequest roleUpdateRequest) {
        Role role = roleRepository.findByRoleUuid(roleUuid).orElseThrow(() -> new BadRequestException("Can't find Role for Update"));
        BeanUtils.copyProperties(roleUpdateRequest, role);

        role.getPrivileges().clear();
        if(roleUpdateRequest.getPrivilegeUuid().size() != 0) {
            List<Privilege> privilegeList = roleUpdateRequest.getPrivilegeUuid()
                    .stream()
                    .map(privilegeUuid -> {
                        Privilege privilege = privilegeRepository.findByPrivilegeUuid(privilegeUuid)
                                .orElseThrow(() -> new BadRequestException("Can't find Privilege With the Provided Id."));

                        return privilege;
                    }).collect(Collectors.toList());
            role.setPrivileges(privilegeList);
            privilegeList.forEach(privilege -> {
                privilege.getRoles().clear();
                privilege.getRoles().add(role);
            });
        }

        roleRepository.save(role);
        return ResponseEntity.ok("Role Updated Successfully");
    }

}