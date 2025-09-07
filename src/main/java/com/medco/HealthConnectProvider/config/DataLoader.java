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

    private static final boolean ENABLE_DATA_LOADER = true; // Set to false to disable

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
        if (!ENABLE_DATA_LOADER) {
            System.out.println("DataLoader is disabled. Skipping data initialization.");
            return;
        }

        System.out.println("DataLoader is enabled. Starting data initialization...");
        loadPrivileges();
        loadRoles();
        loadUsers();
        System.out.println("DataLoader completed successfully.");
    }

    private void loadPrivileges() {
        try {
            List<String> privilegeNames = Arrays.asList(
                "CREATE_USER", "READ_USER", "UPDATE_USER", "DELETE_USER",
                "CREATE_ROLE", "READ_ROLE", "UPDATE_ROLE", "DELETE_ROLE",
                "CREATE_PRIVILEGE", "READ_PRIVILEGE", "UPDATE_PRIVILEGE", "DELETE_PRIVILEGE","VIEW_USER",
                "Delete Groups","Update Groups","Create Groups","Delete Employees","Update Employees","Create Employees","Delete Drugs",
                "Update Drugs","Create Drugs","Delete Services","Update Services","Create Services","CREATE_SERVICE", "Mange_kenema"

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
        } catch (Exception e) {
            System.err.println("Error loading privileges: " + e.getMessage());
            e.printStackTrace();
            // Don't rethrow - allow application to continue
        }
    }

    @Transactional
    private void loadRoles() {
        try {
            Role superAdminRole = roleRepository.findByRoleName("ROLE_SUPER_ADMIN");
            if (superAdminRole == null) {
            superAdminRole = new Role();
            superAdminRole.setRoleName("ROLE_SUPER_ADMIN");
            superAdminRole.setRoleUuid(UUID.randomUUID().toString());
            superAdminRole.setRoleDescription("Super Administrator Role");

            List<String> superAdminPrivilegeNames = Arrays.asList(
                    "CREATE_USER", "READ_USER", "UPDATE_USER", "DELETE_USER",
                    "CREATE_ROLE", "READ_ROLE", "UPDATE_ROLE", "DELETE_ROLE",
                    "CREATE_PRIVILEGE", "READ_PRIVILEGE", "UPDATE_PRIVILEGE", "DELETE_PRIVILEGE", "VIEW_USER"
            );
            List<Privilege> superAdminPrivileges = privilegeRepository.findByPrivilegeNameIn(superAdminPrivilegeNames);

            superAdminRole.setPrivileges(new ArrayList<>());

            for (Privilege privilege : superAdminPrivileges) {
                if (!superAdminRole.getPrivileges().contains(privilege)) {
                    superAdminRole.getPrivileges().add(privilege);
                }
                if (privilege.getRoles() == null) {
                    privilege.setRoles(new ArrayList<>());
                }
                if (!privilege.getRoles().contains(superAdminRole)) {
                    privilege.getRoles().add(superAdminRole);
                }
            }

            roleRepository.save(superAdminRole);
            System.out.println("Created ROLE_SUPER_ADMIN with specified privileges");
        } else {
            // Role exists, check if it already has the required privileges
            List<String> superAdminPrivilegeNames = Arrays.asList(
                    "CREATE_USER", "READ_USER", "UPDATE_USER", "DELETE_USER",
                    "CREATE_ROLE", "READ_ROLE", "UPDATE_ROLE", "DELETE_ROLE",
                    "CREATE_PRIVILEGE", "READ_PRIVILEGE", "UPDATE_PRIVILEGE", "DELETE_PRIVILEGE", "VIEW_USER"
            );

            // Check if role already has all required privileges
            List<String> existingPrivilegeNames = superAdminRole.getPrivileges().stream()
                    .map(Privilege::getPrivilegeName)
                    .toList();

            boolean hasAllPrivileges = existingPrivilegeNames.containsAll(superAdminPrivilegeNames);

            if (!hasAllPrivileges) {
                System.out.println("ROLE_SUPER_ADMIN exists but missing some privileges, updating...");
                List<Privilege> superAdminPrivileges = privilegeRepository.findByPrivilegeNameIn(superAdminPrivilegeNames);

                // Only add missing privileges
                for (Privilege privilege : superAdminPrivileges) {
                    if (!superAdminRole.getPrivileges().contains(privilege)) {
                        superAdminRole.getPrivileges().add(privilege);
                        if (privilege.getRoles() == null) {
                            privilege.setRoles(new ArrayList<>());
                        }
                        if (!privilege.getRoles().contains(superAdminRole)) {
                            privilege.getRoles().add(superAdminRole);
                        }
                    }
                }
                roleRepository.save(superAdminRole);
                System.out.println("Updated ROLE_SUPER_ADMIN with missing privileges");
            } else {
                System.out.println("ROLE_SUPER_ADMIN already has all required privileges");
            }
        }
        } catch (Exception e) {
            System.err.println("Error loading roles: " + e.getMessage());
            e.printStackTrace();
            // Don't rethrow - allow application to continue
        }
    }

    private void loadUsers() {
        try {
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
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
            e.printStackTrace();
            // Don't rethrow - allow application to continue
        }
    }
}
