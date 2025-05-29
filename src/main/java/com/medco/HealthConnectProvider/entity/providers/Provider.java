package com.medco.HealthConnectProvider.entity.providers;

import com.medco.HealthConnectProvider.entity.contracts.ContractHeader;
import com.medco.HealthConnectProvider.entity.services.Service;
import com.medco.HealthConnectProvider.shared.Audit;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.*;

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

    @Size(min = 36, max = 40, message = "Provided Uuid Must be between 36 and 40")
    private String providerUuid = UUID.randomUUID().toString();

    @NotBlank(message = "Provider Code Field Must not be Blank")
    private String providerCode;

    @NotBlank
    @Size(min = 5, max = 50, message = "Email Field must be between 5 and 50")
    @Email
    private String email;

    @NotBlank
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
    private List<Service> serviceLists = new ArrayList<>();

    @NotBlank
    @Size(min = 3, max = 50)
    private String level;

    @NotBlank
    @Size(min = 1, max = 50)
    private String address1;

    @NotBlank
    @Size(min = 1, max = 50)
    private String address2;

    @NotBlank
    @Size(min = 1, max = 50)
    private String address3;

    @NotBlank
    @Size(min = 1, max = 50)
    private String state;

    @NotBlank
    @Size(min = 1, max = 50)
    private String country;

    private double latitude;
    private double longitude;

    @NotBlank
    @Size(min = 3, max = 25)
    private String tinNumber;

    @NotBlank
    @Size(min = 3, max = 25)
    private String status;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;

    private String zipCode;

    private String licenseInformation;

    private String taxIdentification;

    private String bankingDetails;
    private Date createdDate;
    private Date lastModifiedDate;
    private String createdBy;
    private String lastModifiedBy;

}
