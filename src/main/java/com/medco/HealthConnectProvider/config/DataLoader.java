package com.medco.HealthConnectProvider.config;

import com.medco.HealthConnectProvider.entity.user.Privilege;
import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.repository.user.PrivilegeRepository;
import com.medco.HealthConnectProvider.repository.user.RoleRepository;
import com.medco.HealthConnectProvider.repository.user.UserRepository;
import com.medco.HealthConnectProvider.utils.enums.PrivilegeType;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class DataLoader implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PrivilegeRepository privilegeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        loadPrivileges();
        loadRoles();
        loadUsers();
    }

    private void loadPrivileges() {
        List<String> privilegeNames = Arrays.asList(
                "CREATE_USER", "READ_USER", "UPDATE_USER", "DELETE_USER",
                "CREATE_ROLE", "READ_ROLE", "UPDATE_ROLE", "DELETE_ROLE",
                "CREATE_PRIVILEGE", "READ_PRIVILEGE", "UPDATE_PRIVILEGE", "DELETE_PRIVILEGE"

        );

        for (String name : privilegeNames) {
            if (!privilegeRepository.existsByPrivilegeName(name)) {
                Privilege privilege = new Privilege();
                privilege.setPrivilegeName(name);
                privilege.setPrivilegeUuid(UUID.randomUUID().toString());
                privilege.setPrivilegeDescription("Allows " + name.toLowerCase().replace("_", " "));
                privilege.setPrivilegeCategory("SYSTEM");
                privilege.setPrivilegeType(PrivilegeType.FOR_SYSTEM_ADMIN);
                privilegeRepository.save(privilege);
                System.out.println("Created privilege: " + name);
            } else {
                System.out.println("Privilege already exists: " + name);
            }
        }
    }

    @Transactional
    private void loadRoles() {
        Role superAdminRole = roleRepository.findByRoleName("ROLE_SUPER_ADMIN");
        if (superAdminRole == null) {
            superAdminRole = new Role();
            superAdminRole.setRoleName("ROLE_SUPER_ADMIN");
            superAdminRole.setRoleUuid(UUID.randomUUID().toString());
            superAdminRole.setRoleDescription("Super Administrator Role");

            List<String> createdPrivilegeNames = Arrays.asList(
                    "CREATE_USER", "READ_USER", "UPDATE_USER", "DELETE_USER",
                    "CREATE_ROLE", "READ_ROLE", "UPDATE_ROLE", "DELETE_ROLE",
                    "CREATE_PRIVILEGE", "READ_PRIVILEGE", "UPDATE_PRIVILEGE", "DELETE_PRIVILEGE"
            );
            List<Privilege> createdPrivileges = privilegeRepository.findByPrivilegeNameIn(createdPrivilegeNames);

            superAdminRole.setPrivileges(new ArrayList<>());

            for (Privilege privilege : createdPrivileges) {
                superAdminRole.getPrivileges().add(privilege);
                if (privilege.getRoles() == null) {
                    privilege.setRoles(new HashSet<>());
                }
                privilege.getRoles().add(superAdminRole);
            }

            roleRepository.save(superAdminRole);
            System.out.println("Created ROLE_SUPER_ADMIN with specified privileges");
        } else {
            List<String> createdPrivilegeNames = Arrays.asList(
                    "CREATE_USER", "READ_USER", "UPDATE_USER", "DELETE_USER",
                    "CREATE_ROLE", "READ_ROLE", "UPDATE_ROLE", "DELETE_ROLE",
                    "CREATE_PRIVILEGE", "READ_PRIVILEGE", "UPDATE_PRIVILEGE", "DELETE_PRIVILEGE"
            );
            List<Privilege> createdPrivileges = privilegeRepository.findByPrivilegeNameIn(createdPrivilegeNames);

            for (Privilege privilege : createdPrivileges) {
                if (!superAdminRole.getPrivileges().contains(privilege)) {
                    superAdminRole.getPrivileges().add(privilege);
                    if (privilege.getRoles() == null) {
                        privilege.setRoles(new HashSet<>());
                    }
                    privilege.getRoles().add(superAdminRole);
                }
            }
            roleRepository.save(superAdminRole);
            System.out.println("Updated ROLE_SUPER_ADMIN with specified privileges");
        }
    }

    private void loadUsers() {
        Optional<User> existingSuperAdmin = userRepository.findByEmail("superadmin@gmail.com");
        if (existingSuperAdmin.isEmpty()) {
            User superAdmin = new User();
            superAdmin.setUserUuid(UUID.randomUUID().toString());
            superAdmin.setEmail("superadmin@gmail.com");
            superAdmin.setPassword(passwordEncoder.encode("superadmin123"));
            superAdmin.setTitle("Mr.");
            superAdmin.setFirstName("Super");
            superAdmin.setFatherName("Admin");
            superAdmin.setGrandFatherName("System");
            superAdmin.setGender("male");
            superAdmin.setMobilePhone("1234567890");
            superAdmin.setStatus(Status.ACTIVE);
            superAdmin.setUserType("SUPER_ADMIN");
            superAdmin.setUserStatus(Status.ACTIVE);

            Role superAdminRole = roleRepository.findByRoleName("ROLE_SUPER_ADMIN");
            if (superAdminRole == null) {
                throw new RuntimeException("ROLE_SUPER_ADMIN not found. Ensure roles are loaded before users.");
            }
            superAdmin.setRole(superAdminRole);

            userRepository.save(superAdmin);
            System.out.println("Super Admin user created successfully.");
        } else {
            System.out.println("Super Admin user already exists.");
        }
    }
}
