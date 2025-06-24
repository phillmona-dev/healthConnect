package com.medco.HealthConnectProvider.entity.providers;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.services.Servicelist;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.shared.Audit;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "providers", uniqueConstraints = { @UniqueConstraint(columnNames = "providerName"), @UniqueConstraint(columnNames = "providerUuid"),
        @UniqueConstraint(columnNames = "email"), @UniqueConstraint(columnNames = "telephone") })
public class Provider extends Audit implements Serializable {

    @Serial
    private static final long serialVersionUID = -2473104672068471594L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(min = 3)
    private String providerName;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(unique = true)
    private String threeDigitAcronym;

    @Size(min = 36, max = 40, message = "Provided Uuid Must be between 36 and 40")
    private String providerUuid = UUID.randomUUID().toString();

    private String providerCode;

    @NotBlank
    @Size(min = 5, max = 50, message = "Email Field must be between 5 and 50")
    @Email
    private String email;

    @Size(min = 3, max = 100)
    private String description;

    @NotBlank
    @Size(min = 9, max = 13)
    private String telephone;

    private String providerType;

    @NotBlank
    @Size(min = 3, max = 50)
    private String category;

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<ContractHeader> contractHeaders = new HashSet<>();

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Servicelist> servicelistLists = new ArrayList<>();

    @OneToMany(mappedBy = "provider", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<User> users = new ArrayList<>();

    @NotBlank
    @Size(min = 3, max = 50)
    private String level;

    @NotBlank
    @Size(min = 1, max = 50)
    private String address1;

    private String address2;

    private String address3;

    private String state;

    @Size(min = 1, max = 50)
    private String country;

    private double latitude;
    private double longitude;

    @NotBlank
    @Size(min = 3, max = 25)
    private String tinNumber;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;

    private String zipCode;

    private String branch;

    private String licenseInformation;

    private String taxIdentification;

    private String bankingDetails;
    private Date createdDate;
    private Date lastModifiedDate;
    private String createdBy;
    private String lastModifiedBy;

    @Size(max = 255)
    private String logoPath;

    private Long totalContract;

    // Add this method to help set the user's provider
    public void addUser(User user) {
        users.add(user);
        user.setProvider(this);
    }

}
