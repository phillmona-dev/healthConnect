package com.medco.HealthConnectProvider.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medco.HealthConnectProvider.config.Auditing.UserDateAudit;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.providers.Provider;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "app_user")
@Builder
public class User extends UserDateAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String userUuid = UUID.randomUUID().toString();

    @NotBlank
    @Size(min = 5, max = 50)
    @Email
    private String email;

    @JsonIgnore
    @NotBlank
    private String password;

    private String title;

    @NotBlank
    private String firstName;

    @NotBlank
    private String fatherName;

    private String grandFatherName;

    @NotBlank
    private String gender;

    @NotBlank
    @Size(min = 9, max = 13)
    private String mobilePhone;

    @Enumerated(EnumType.STRING)
    private Status userStatus;

    private String userType;

    private Status status;

    @Size( max = 40)
    private String providerUuid;

    private String payerUuid;

    private String passwordResetCode;
    private String emailVerificationToken;
    private String profilePicture;

    @Column(name = "image_data")
    @Basic(fetch = FetchType.LAZY)
    @Lob
    private byte[] imageData;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "id")
    private Role role;

    private Date createdDate;

    private Date lastModifiedDate;

    private String createdBy;

    private String lastModifiedBy;

    @Column(columnDefinition = "boolean default false")
    private boolean firstTimeLogin;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "payer_id")
    private Payer payer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_id")
    private Provider provider;

    public void setPayer(Payer payer) {
        if (this.payer != null) {
            this.payer.getUsers().remove(this);
        }
        this.payer = payer;
        this.payerUuid = payer != null ? payer.getPayerUuid() : null;
        if (payer != null && !payer.getUsers().contains(this)) {
            payer.getUsers().add(this);
        }
    }

    public void setProvider(Provider provider) {
        if (this.provider != null) {
            this.provider.getUsers().remove(this);
        }
        this.provider = provider;
        this.providerUuid = provider != null ? provider.getProviderUuid() : null;
        if (provider != null && !provider.getUsers().contains(this)) {
            provider.getUsers().add(this);
        }
    }
}
