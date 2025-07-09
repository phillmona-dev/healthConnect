package com.medco.HealthConnectProvider.services.impl.user;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.user.Privilege;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.repository.user.PrivilegeRepository;
import com.medco.HealthConnectProvider.services.user.PrivilegeService;
import com.medco.HealthConnectProvider.ui.request.auth.password.PrivilegeRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.auth.PrivilegeResponse;
import com.medco.HealthConnectProvider.utils.enums.PrivilegeType;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrivilegeServiceImpl implements PrivilegeService {

    private final PrivilegeRepository privilegeRepository;

    public PrivilegeServiceImpl(PrivilegeRepository privilegeRepository) {
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    public PagedResponse<PrivilegeResponse> createPrivilege(PrivilegeRequest privilegeRequest) {
        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        var privileges = new Privilege();
        BeanUtils.copyProperties(privilegeRequest, privileges);
        if (userDetails.getPayerUuid()!=null){
            privileges.setPrivilegeType(PrivilegeType.FOR_PAYER);

        } else if (userDetails.getProviderUuid()!=null) {
            privileges.setPrivilegeType(PrivilegeType.FOR_PROVIDER);

        }else privileges.setPrivilegeType(PrivilegeType.FOR_ALL);

        Privilege savedPrivilege = privilegeRepository.save(privileges);

        PrivilegeResponse response = new PrivilegeResponse();
        BeanUtils.copyProperties(savedPrivilege, response);

        List<PrivilegeResponse> content = List.of(response);

        return new PagedResponse<>(
                content,
                0,
                1,
                1,
                1,
                true
        );
    }

    @Override
    public PrivilegeResponse getPrivilege(String privilegeUuid) {
        return privilegeRepository.findByPrivilegeUuid(privilegeUuid)
                .map(privilege -> new PrivilegeResponse(privilege.getPrivilegeUuid(),privilege.getPrivilegeName(),privilege.getPrivilegeDescription(),privilege.getPrivilegeCategory(),privilege.getPrivilegeType()))
                .orElseThrow(() -> new BadRequestException("Can't find Privilege With the provided Id"));
    }

    @Override
    @Cacheable(value = "privilegeCache", key = "#search ?: 'default' ")
    public PagedResponse<PrivilegeResponse> getAllPrivileges(String search, Pageable pageable) {

        UserPrincipal userDetails = SecurityUtils.getAuthenticatedUser();
        PrivilegeType privilegeType;
        if (userDetails.getPayerUuid()!=null)
            privilegeType=PrivilegeType.FOR_PAYER;
        else if (userDetails.getProviderUuid()!=null) {
            privilegeType=PrivilegeType.FOR_PROVIDER;

        } else {
            privilegeType = null;
        }
        System.out.println("privilege type "+privilegeType);
        Page<Privilege> privilegePage = search != null ?
                 privilegeRepository.findAllByPrivilegeNameContaining(search, pageable) :
                privilegeRepository.findAll(pageable);


        List<PrivilegeResponse> privilegeResponses;
        if (privilegeType==null) {
            privilegeResponses = privilegePage.getContent().stream()

                    .map(this::mapPrivilegeToResponse)
                    .collect(Collectors.toList());
        }else {
           privilegeResponses = privilegePage.getContent().stream()
                   .filter(privilege -> privilege.getPrivilegeType().equals(privilegeType)||privilege.getPrivilegeType().equals(PrivilegeType.FOR_ALL))
                    .map(this::mapPrivilegeToResponse)
                    .collect(Collectors.toList());
        }
        return new PagedResponse<>(
                privilegeResponses,
                privilegePage.getNumber(),
                privilegePage.getSize(),
                privilegePage.getTotalElements(),
                privilegePage.getTotalPages(),
                privilegePage.isLast()
        );
    }

    private PrivilegeResponse mapPrivilegeToResponse(Privilege privilege) {
        var response = new PrivilegeResponse();
        BeanUtils.copyProperties(privilege, response);
        return response;
    }

    @Override
    public ResponseEntity<?> updatePrivilege(String privilegeUuid, PrivilegeRequest request) {
        Privilege privilege = privilegeRepository.findByPrivilegeUuid(privilegeUuid)
                .orElseThrow(() -> new BadRequestException("Can't Find Privilege With the Provided Id"));

        BeanUtils.copyProperties(request,privilege);

        return ResponseEntity.ok("Privilege Updated Successfully");
    }

    @Override
    public ResponseEntity<?> deletePrivilege(String privilegeUuid) {
        privilegeRepository.delete(
                privilegeRepository.findByPrivilegeUuid(privilegeUuid)
                        .orElseThrow(() -> new BadRequestException("Can't Find Privilege With the Provided Id"))
        );
        return ResponseEntity.ok("Privilege Deleted Successfully");
    }

    private List<PrivilegeResponse> getAllWithOutSearch(Pageable pageable) {

        return privilegeRepository.findAll(pageable)
                .stream()
                .map(privilege -> {
                    var response = new PrivilegeResponse();
                    BeanUtils.copyProperties(privilege,response);

                    return response;
                })
                .collect(Collectors.toList());

    }

    private List<PrivilegeResponse> findAllPrivilegesBySearch(String search, Pageable pageable) {

        return privilegeRepository.findAllByPrivilegeNameContaining(search, pageable)
                .stream()
                .map(privilege -> {
                    var response = new PrivilegeResponse();
                    BeanUtils.copyProperties(privilege,response);

                    return response;
                }).collect(Collectors.toList());

    }
}
