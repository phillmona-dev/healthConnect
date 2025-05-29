package com.medco.HealthConnectProvider.services.impl.user;

import com.medco.HealthConnectProvider.entity.user.Privilege;
import com.medco.HealthConnectProvider.exception.BadRequestException;
import com.medco.HealthConnectProvider.repository.user.PrivilegeRepository;
import com.medco.HealthConnectProvider.services.user.PrivilegeService;
import com.medco.HealthConnectProvider.ui.request.auth.password.PrivilegeRequest;
import com.medco.HealthConnectProvider.ui.response.auth.PrivilegeResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PrivilegeServiceImpl implements PrivilegeService {

    private PrivilegeRepository privilegeRepository;

    public PrivilegeServiceImpl(PrivilegeRepository privilegeRepository) {
        this.privilegeRepository = privilegeRepository;
    }

    @Override
    public PrivilegeResponse createPrivilege(PrivilegeRequest privilegeRequest) {

        var response = new PrivilegeResponse();
        var privileges = new Privilege();
        BeanUtils.copyProperties(privilegeRequest,privileges);
        //privileges.setRole(null);


        Privilege privilege = privilegeRepository.save(privileges);
        BeanUtils.copyProperties(privilege,response);

        return response;
    }

    @Override
    public PrivilegeResponse getPrivilege(String privilegeUuid) {
        return privilegeRepository.findByPrivilegeUuid(privilegeUuid)
                .map(privilege -> new PrivilegeResponse(privilege.getPrivilegeUuid(),privilege.getPrivilegeName(),privilege.getPrivilegeDescription(),privilege.getPrivilegeCategory()))
                .orElseThrow(() -> new BadRequestException("Can't find Privilege With the provided Id"));
    }

    @Override
    @Cacheable(value = "privilegeCache", key = "#search ?: 'default'")
    public List<PrivilegeResponse> getAllPrivileges(String search, Pageable pageable) {
        return search != null ? findAllPrivilegesBySearch(search,pageable) : getAllWithOutSearch(pageable);
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
