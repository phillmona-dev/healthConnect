package com.medco.HealthConnectProvider.swagger;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "Health Connect", version = "1.0"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer"
)
public class OpenApi30Config {

    @Bean
    public OpenAPI healthConnectOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("HealthConnect Provider API")
                        .description("API documentation for HealthConnect")
                        .version("v1.0")
                        .license(new io.swagger.v3.oas.models.info.License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")
                        )
                )
                .externalDocs(new io.swagger.v3.oas.models.ExternalDocumentation()
                        .description("HealthConnect Documentation")
                        .url("https://healthconnect.org/docs")
                )
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}