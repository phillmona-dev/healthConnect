package com.medco.HealthConnectProvider.config.securityConfig.customUserDetails;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.medco.HealthConnectProvider.entity.user.User;
import com.medco.HealthConnectProvider.utils.enums.Status;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Builder
public class UserPrincipal implements UserDetails {

    private String userUuid;
    private String email;
    @JsonIgnore
    private String password;
    private String title;
    private String firstName;
    private String fatherName;
    private String grandFatherName;
    private String gender;
    private String mobilePhone;
    private Status userStatus;
    private String providerUuid;
    private String payerUuid;
    private String profilePicture;
    private Collection<? extends GrantedAuthority> authorities;


    public UserPrincipal(String userUuid, String email, String password, String title, String firstName, String fatherName, String grandFatherName, String gender, String mobilePhone, Status userStatus, String payerUuid,String providerUuid, String profilePicture, Collection<? extends GrantedAuthority> authorities) {
        this.userUuid = userUuid;
        this.email = email;
        this.password = password;
        this.title = title;
        this.firstName = firstName;
        this.fatherName = fatherName;
        this.grandFatherName = grandFatherName;
        this.gender = gender;
        this.mobilePhone = mobilePhone;
        this.userStatus = userStatus;
        this.payerUuid = payerUuid;
        this.providerUuid = providerUuid;
        this.profilePicture = profilePicture;
        this.authorities = authorities;
    }

    public static UserPrincipal build(User user, List<GrantedAuthority> privilegesForRole) {

        return new UserPrincipal(user.getUserUuid(), user.getEmail(), user.getPassword(),
                user.getTitle(), user.getFirstName(), user.getFatherName(), user.getGrandFatherName(),user.getGender(),
                user.getMobilePhone(), user.getUserStatus(), user.getPayerUuid(), user.getProviderUuid(), user.getProfilePicture(),
                privilegesForRole);
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
    public String getUsername() {
        return email;
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
}
