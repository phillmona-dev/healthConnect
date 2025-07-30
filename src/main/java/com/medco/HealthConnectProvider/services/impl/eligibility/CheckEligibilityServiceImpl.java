package com.medco.HealthConnectProvider.services.impl.eligibility;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.services.eligibility.CheckEligibilityService;
import com.medco.HealthConnectProvider.ui.response.eligibility.CheckEligibilityResponse;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class CheckEligibilityServiceImpl implements CheckEligibilityService {

    private static final Logger logger = LoggerFactory.getLogger(EligibilityServiceImpl.class);

    private static final String BASE_URL = "http://192.168.100.85:8888";
    private static final String ELIGIBILITY_ENDPOINT = "/api/payer/claimconnect/insuredperson/eligiblity";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.key}")
    private String apiKey;

    @Override
    public List<CheckEligibilityResponse> checkEligibility(String institutionUuid, String search) {
        logger.info("Checking eligibility for institution: {}, search: {}", institutionUuid, search);

        UserPrincipal userPrincipal = SecurityUtils.getAuthenticatedUser();
        String providerUuid = userPrincipal.getProviderUuid();
        logger.debug("Provider UUID: {}", providerUuid);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-API-Key", apiKey);

        String url = BASE_URL + ELIGIBILITY_ENDPOINT +
                "?institutionUuid=" + institutionUuid +
                "&providerUuid=" + providerUuid +
                "&search=" + search;

        ResponseEntity<List<CheckEligibilityResponse>> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<List<CheckEligibilityResponse>>() {}
        );

        List<CheckEligibilityResponse> eligibilityResponses = response.getBody();
        if (eligibilityResponses == null || eligibilityResponses.isEmpty()) {
            logger.warn("No eligibility data received");
            return List.of();
        }

        logger.info("Received eligibility data for {} insured persons", eligibilityResponses.size());
        return eligibilityResponses;
    }
}
