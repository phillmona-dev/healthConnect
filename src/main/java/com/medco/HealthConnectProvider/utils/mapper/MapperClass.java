package com.medco.HealthConnectProvider.utils.mapper;

import com.medco.HealthConnectProvider.entity.claims.Claim;
import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.entity.user.Privilege;
import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.ui.response.auth.PrivilegeResponse;
import com.medco.HealthConnectProvider.ui.response.auth.RoleResponse;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimResponse;
import com.medco.HealthConnectProvider.ui.response.contracts.ContractResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.ui.response.user.UserResponse;
import org.springframework.beans.BeanUtils;

public class MapperClass {

    public static UserResponse mapToUserResponse(User user){
        var response = new UserResponse();
        BeanUtils.copyProperties(user,response);

        return response;
    }

    public ClaimResponse mapToClaimResponse(Claim claim){
        var response = new ClaimResponse();
        BeanUtils.copyProperties(claim, response);

        return response;
    }

    public ContractResponse mapToContractResponse(ContractHeader contractHeader){
        var response = new ContractResponse();
        BeanUtils.copyProperties(contractHeader, response);

        return response;
    }

    public PrivilegeResponse mapToPrivilegeResponse(Privilege privilege){
        var response = new PrivilegeResponse();
        BeanUtils.copyProperties(privilege, response);

        return response;
    }

    public RoleResponse mapToRoleResponse(Role role){
        var response = new RoleResponse();
        BeanUtils.copyProperties(role, response);

        return response;
    }

    public ProviderResponse mapToProviderResponse(Provider provider){
        var response = new ProviderResponse();
        BeanUtils.copyProperties(provider, response);

        return response;
    }
}
