package com.medco.HealthConnectProvider.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class ExternalApiConfig {

    @Value("${awash.api.base-url}")
    private String externalApiBaseUrl;

    @Value("${api.key}")
    private String apiKey;

}