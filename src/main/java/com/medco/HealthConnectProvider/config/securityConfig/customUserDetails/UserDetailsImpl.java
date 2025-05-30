package com.medco.HealthConnectProvider.config.securityConfig.customUserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import java.util.stream.Collectors;

import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.utils.enums.Status;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDetailsImpl implements UserDetails {


    /**
     *
     */
    @Serial
    private static final long serialVersionUID = -8544780218833555579L;
    @Getter
    private String userUuid;
    @Getter
    private String email;
    @JsonIgnore
    private String password;
    private Integer roleId;
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String gender;
    private String mobilePhone;
    private Status userStatus;
    private String userType;
    private String providerUuid;
    private String institutionUuid;
    private String profilePicture;

    private Collection<? extends GrantedAuthority> authorities;

    public UserDetailsImpl(String userUuid, String email, String password, Integer roleId, String title, String firstName,
                           String fatherName, String grandFatherName, String gender, String mobilePhone, Status userStatus, String userType,
                           String institutionUuid, String providerUuid, String profilePicture,
                           Collection<? extends GrantedAuthority> authorities) {
        this.userUuid = userUuid;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.roleId=roleId; this.title=title; this.firstName=firstName; this.fatherName=fatherName;
        this.grandFatherName=grandFatherName; this.gender=gender; this.mobilePhone=mobilePhone; this.userStatus=userStatus;
        this.userType=userType; this.institutionUuid=institutionUuid; this.providerUuid=providerUuid; this.profilePicture=profilePicture;
    }

    public static UserDetailsImpl build(User user, List<String> privilegesForRole) {

        List<GrantedAuthority> authorities = privilegesForRole.stream().map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new UserDetailsImpl(user.getUserUuid(), user.getEmail(), user.getPassword(), Math.toIntExact(user.getRole().getId()),
                user.getTitle(), user.getFirstName(), user.getFatherName(), user.getGrandFatherName(),user.getGender(),
                user.getMobilePhone(), user.getUserStatus(), user.getUserType(), user.getPayerUuid(), user.getProviderUuid(), user.getProfilePicture(),
                authorities);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        UserDetailsImpl user = (UserDetailsImpl) o;
        return Objects.equals(userUuid, user.userUuid);
    }
    @Override
    public String getUsername() {
        return email;
    }

}
