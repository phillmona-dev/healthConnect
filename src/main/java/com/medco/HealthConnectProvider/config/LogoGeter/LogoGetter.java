package com.medco.HealthConnectProvider.config.LogoGeter;

import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.provider.ProviderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
public class LogoGetter {
    private final PayerRepository payerRepository;
    private final ProviderRepository providerRepository;

    @Value("${file.upload-dir-payer-logos:C:/Users/Administrator/OneDrive/Desktop/MedcoProjects/logos/payers}")
    private String payerLogosDirectory;

    public  LogoGetter(PayerRepository payerRepository, ProviderRepository providerRepository) {
        this.payerRepository = payerRepository;
        this.providerRepository = providerRepository;
    }

    public  ResponseEntity<ByteArrayResource>PayerLogo(String payerUuid){
        try {
            Payer payer = payerRepository.findByPayerUuid(payerUuid);
            if (payer == null || payer.getLogoPath() == null || payer.getLogoPath().isEmpty()) {
                log.warn("Payer logo not found for UUID: {}", payerUuid);
                return serveDefaultLogo();
            }

            String logoPath = payerLogosDirectory + "/" + payer.getLogoPath();
            log.debug("Attempting to load logo from path: {}", logoPath);

            File logoFile = new File(logoPath);
            if (!logoFile.exists() || !logoFile.isFile()) {
                log.warn("Logo file does not exist at path: {}", logoPath);
                return serveDefaultLogo();
            }

            Path path = logoFile.toPath();
            ByteArrayResource resource = new ByteArrayResource(Files.readAllBytes(path));

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(determineContentType(logoPath)))
                    .contentLength(logoFile.length())
                    .body(resource);
        } catch (IOException e) {
            log.error("Error retrieving payer logo: {}", e.getMessage(), e);
            return serveDefaultLogo();
        }
    }
    private ResponseEntity<ByteArrayResource> serveDefaultLogo() {
        try {

            Resource resource = new ClassPathResource("static/images/default-payer-logo.png");
            if (resource.exists()) {
                ByteArrayResource byteResource = new ByteArrayResource(
                        FileCopyUtils.copyToByteArray(resource.getInputStream()));

                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .contentLength(resource.contentLength())
                        .body(byteResource);
            }

            return ResponseEntity.notFound().build();
        } catch (IOException e) {
            log.error("Error serving default logo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    private String determineContentType(String filePath) {
        String extension = filePath.substring(filePath.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }
}
