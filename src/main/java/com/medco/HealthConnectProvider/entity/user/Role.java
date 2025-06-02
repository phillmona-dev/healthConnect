package com.medco.HealthConnectProvider.entity.user;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 50)
    private String roleName;

    @NotBlank
    @Size(max = 100)
    private String roleDescription;

    @Size(min = 36, max = 40)
    private String roleUuid = UUID.randomUUID().toString();

    @ManyToMany(mappedBy = "roles",cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JsonManagedReference
    private List<Privilege> privileges;

    private String payerUuid;
    private String providerUuid;

}
