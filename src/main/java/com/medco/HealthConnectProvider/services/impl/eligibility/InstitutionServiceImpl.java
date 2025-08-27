package com.medco.HealthConnectProvider.services.impl.eligibility;

import com.medco.HealthConnectProvider.services.eligibility.InstitutionService;
import com.medco.HealthConnectProvider.ui.response.eligibility.InstitutionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InstitutionServiceImpl implements InstitutionService {

    private static final Logger logger = LoggerFactory.getLogger(InstitutionServiceImpl.class);

    private static final String INSTITUTIONS_ENDPOINT = "/api/payer/claimconnect/institution/NamesList";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.key}")
    private String apiKey;

    @Value("${awash.api.base-url}")
    private String BASE_URL;

    @Override
    public List<InstitutionResponse> getInstitutions(String contractUuid) {
        logger.info("Fetching list of institutions for contract: {}", contractUuid);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);

        String url = String.format("%s%s/%s",
                BASE_URL,
                INSTITUTIONS_ENDPOINT,
                URLEncoder.encode(contractUuid, StandardCharsets.UTF_8));

        logger.debug("Making GET request to: {}", url);

        try {
            ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<Map<String, String>>() {}
            );

            Map<String, String> institutionsMap = response.getBody();
            if (institutionsMap == null || institutionsMap.isEmpty()) {
                logger.warn("No institutions found for contract: {}", contractUuid);
                return List.of();
            }

            List<InstitutionResponse> institutions = institutionsMap.entrySet().stream()
                    .map(entry -> new InstitutionResponse(entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());

            logger.info("Retrieved {} institutions for contract {}", institutions.size(), contractUuid);
            return institutions;

        } catch (HttpClientErrorException e) {
            logger.error("Client error when fetching institutions for contract {}: {}",
                    contractUuid, e.getStatusCode());
            return List.of();
        } catch (HttpServerErrorException e) {
            logger.error("Server error when fetching institutions for contract {}: {}",
                    contractUuid, e.getStatusCode());
            return List.of();
        } catch (Exception e) {
            logger.error("Unexpected error when fetching institutions for contract {}", contractUuid, e);
            return List.of();
        }
    }
}
