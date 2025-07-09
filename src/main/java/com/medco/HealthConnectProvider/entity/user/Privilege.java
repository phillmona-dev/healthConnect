package com.medco.HealthConnectProvider.entity.user;

import com.medco.HealthConnectProvider.utils.enums.PrivilegeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Privilege {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Size(min = 36, max = 40)
    private String privilegeUuid = UUID.randomUUID().toString();

    @Column(length = 50)
    private String privilegeName;

    @NotBlank
    @Size(max = 100)
    private String privilegeDescription;

    @NotBlank
//    @Column(length = 50)
    private String privilegeCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PrivilegeType privilegeType;

    @ManyToMany(cascade = CascadeType.ALL)
    @JoinTable(
            name = "role_privilege",
            joinColumns = @JoinColumn(name = "privilege_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();


}
