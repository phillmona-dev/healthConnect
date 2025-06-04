package com.medco.HealthConnectProvider.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@Slf4j
public class FileStorageConfig {

    @Value("${file.upload-dir-claims:C:/Users/Administrator/OneDrive/Desktop/MedcoProjects/healthConnect_files}")
    private String claimsDirectory;
    
    @Value("${file.upload-dir-provider-logos:C:/Users/Administrator/OneDrive/Desktop/MedcoProjects/logos/providers}")
    private String providerLogosDirectory;
    
    @Value("${file.upload-dir-payer-logos:C:/Users/Administrator/OneDrive/Desktop/MedcoProjects/logos/payers}")
    private String payerLogosDirectory;

    @PostConstruct
    public void init() {
        createDirectoryIfNotExists(claimsDirectory);
        createDirectoryIfNotExists(providerLogosDirectory);
        createDirectoryIfNotExists(payerLogosDirectory);
        
        log.info("File storage directories initialized successfully");
    }
    
    private void createDirectoryIfNotExists(String directoryPath) {
        try {
            Path path = Paths.get(directoryPath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created directory: {}", directoryPath);
            }
        } catch (Exception e) {
            log.error("Failed to create directory {}: {}", directoryPath, e.getMessage(), e);
        }
    }
}