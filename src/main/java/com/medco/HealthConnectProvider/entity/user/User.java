package com.medco.HealthConnectProvider.entity.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medco.HealthConnectProvider.config.Auditing.UserDateAudit;
import com.medco.HealthConnectProvider.entity.payers.Payer;
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

    @NotBlank
    @Size(min = 2, max = 25)
    private String title;

    @NotBlank
    @Size(min = 2, max = 25)
    private String firstName;

    @NotBlank
    @Size(min = 2, max = 25)
    private String fatherName;

    @NotBlank
    @Size(min = 2, max = 25)
    private String grandFatherName;

    @NotBlank
    @Size(min = 1, max = 10)
    private String gender;

    @NotBlank
    @Size(min = 9, max = 13)
    private String mobilePhone;

    @Enumerated(EnumType.STRING)
    private Status userStatus;

    @Size( max = 40)
    private String providerUuid;

    private String payerUuid;

    private String passwordResetCode;
    private String emailVerificationToken;
    private String profilePicture;

    @Column(columnDefinition = "boolean default false")
    private boolean isDeleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "id")
    private Role role;

    private Date createdDate;
    private Date lastModifiedDate;
    private String createdBy;
    private String lastModifiedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private Payer payer;

}
