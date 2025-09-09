package com.medco.HealthConnectProvider.config;

import com.medco.HealthConnectProvider.config.interceptor.ApiKeyInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiKeyInterceptor apiKeyInterceptor;

    public WebMvcConfig(ApiKeyInterceptor apiKeyInterceptor) {
        this.apiKeyInterceptor = apiKeyInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiKeyInterceptor);
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Add a custom JSON converter that can handle octet-stream content type
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();

        // Create a new mutable list with existing supported media types plus APPLICATION_OCTET_STREAM
        List<org.springframework.http.MediaType> supportedMediaTypes = new java.util.ArrayList<>(jsonConverter.getSupportedMediaTypes());
        supportedMediaTypes.add(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
        jsonConverter.setSupportedMediaTypes(supportedMediaTypes);

        converters.add(jsonConverter);
    }

}
