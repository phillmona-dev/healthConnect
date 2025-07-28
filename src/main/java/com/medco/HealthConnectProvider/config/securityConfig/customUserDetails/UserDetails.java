package com.medco.HealthConnectProvider.config.securityConfig.customUserDetails;

import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.repository.user.RoleRepository;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserDetails implements UserDetailsService {

    private final UserRepository repository;
    private final RoleRepository roleRepository;

    public UserDetails(UserRepository repository, RoleRepository roleRepository) {
        this.repository = repository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {

        Optional<User> user = Optional.ofNullable(repository.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User with Email " + username + " Not found.")));

        Optional<Role> role = roleRepository.findById(user.get().getRole().getId());

        List<String> privilegesForRole = new ArrayList<>();
        List<String> privilegeRoles = role.get().getPrivileges().stream()
                .map(p -> {
                    privilegesForRole.add("ROLE_" + p.getPrivilegeName());
                    return "ROLE_" + p.getPrivilegeName();
                }).toList();

        List<GrantedAuthority> authorities = privilegesForRole.stream().map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return UserPrincipal.build(user.get(),authorities);

    }

}
