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
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InstitutionServiceImpl implements InstitutionService {

    private static final Logger logger = LoggerFactory.getLogger(InstitutionServiceImpl.class);

    private static final String BASE_URL = "http://192.168.100.85:8888";
    private static final String INSTITUTIONS_ENDPOINT = "/api/payer/claimconnect/institution/NamesList";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.key}")
    private String apiKey;

    @Override
    public List<InstitutionResponse> getInstitutions() {
        logger.info("Fetching list of institutions");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);

        ResponseEntity<Map<String, String>> response = restTemplate.exchange(
                BASE_URL + INSTITUTIONS_ENDPOINT,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<Map<String, String>>() {}
        );

        Map<String, String> institutionsMap = response.getBody();
        if (institutionsMap == null || institutionsMap.isEmpty()) {
            logger.warn("No institutions found");
            return List.of();
        }

        List<InstitutionResponse> institutions = institutionsMap.entrySet().stream()
                .map(entry -> new InstitutionResponse(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        logger.info("Retrieved {} institutions", institutions.size());
        return institutions;
    }
}
