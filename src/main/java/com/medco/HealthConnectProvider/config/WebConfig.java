package com.medco.HealthConnectProvider.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
public class WebConfig {
    // This configuration enables stable JSON serialization for Spring Data Page objects
    // It resolves the warning about PageImpl serialization instability
}