package com.medco.HealthConnectProvider.ui.response.auth;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenResponse {
    private String newToken;
    private String refreshToken;
}
