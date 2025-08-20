package com.medco.HealthConnectProvider.services.impl.eligibility;

import com.medco.HealthConnectProvider.config.securityConfig.customUserDetails.UserPrincipal;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.eligibility.CheckEligibilityService;
import com.medco.HealthConnectProvider.ui.response.eligibility.CheckEligibilityResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import com.medco.HealthConnectProvider.utils.security.SecurityUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CheckEligibilityServiceImpl implements CheckEligibilityService {

    private static final Logger logger = LoggerFactory.getLogger(CheckEligibilityServiceImpl.class);

    //private static final String BASE_URL = "http://192.168.16.234:8888";
//    private static final String BASE_URL = "http://192.168.100.85:8888";
    private static final String ELIGIBILITY_ENDPOINT = "/api/payer/claimconnect/insuredperson/eligiblity";

    @Autowired
    private RestTemplate restTemplate;

    @Value("${api.key}")
    private String apiKey;
    @Value("${awash.api.base-url}")
    private String BASE_URL;

    @Autowired
    private InsuredRepository insuredRepository;

    @Autowired
    private PayerRepository payerRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<CheckEligibilityResponse> checkEligibility(String contractUuid, String institutionUuid, String search) {
        logger.info("[START] Checking eligibility - Contract: {}, Institution: {}, Search: {}",
                contractUuid, institutionUuid, search);

        try {
            UserPrincipal userPrincipal = SecurityUtils.getAuthenticatedUser();
            String providerUuid = userPrincipal.getProviderUuid();
            logger.debug("[AUTH] Authenticated Provider UUID: {}", providerUuid);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);
            logger.debug("[HEADERS] Prepared headers: {}", headers);

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL + ELIGIBILITY_ENDPOINT + "/" + providerUuid)
                    .queryParam("contractUuid", contractUuid)
                    .queryParam("institutionUuid", institutionUuid);

            if (StringUtils.hasText(search)) {
                builder.queryParam("search", URLEncoder.encode(search.trim(), StandardCharsets.UTF_8));
            }

            String url = builder.toUriString();
            logger.info("[URL] Final request URL: {}", url);

            logger.debug("[REQUEST] Sending GET request to downstream service");
            ResponseEntity<List<CheckEligibilityResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<List<CheckEligibilityResponse>>() {}
            );

            logger.debug("[RESPONSE] Received status: {}", response.getStatusCode());
            logger.debug("[RESPONSE] Headers: {}", response.getHeaders());

            List<CheckEligibilityResponse> responses = response.getBody();
            logger.info("[RESULT] Received {} eligibility records", responses != null ? responses.size() : 0);

            if (responses != null && !responses.isEmpty()) {
                logger.debug("[PROCESSING] Starting to register {} insured individuals", responses.size());
                registerInsuredIndividuals(responses);
            }

            return responses != null ? responses : List.of();

        } catch (HttpClientErrorException e) {
            logger.error("[HTTP CLIENT ERROR] Status: {} - Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (HttpServerErrorException e) {
            logger.error("[HTTP SERVER ERROR] Status: {} - Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            return List.of();
        } catch (Exception e) {
            logger.error("[UNEXPECTED ERROR] {}", ExceptionUtils.getStackTrace(e));
            return List.of();
        } finally {
            logger.info("[END] Eligibility check completed");
        }
    }

    private void registerInsuredIndividuals(List<CheckEligibilityResponse> responses) {
        for (CheckEligibilityResponse response : responses) {
            try {
                Insured existingInsured = insuredRepository.findByInsuredUuid(response.getInsuredUuid());
                if (existingInsured != null) {
                    logger.info("Insured already exists with UUID: {}", response.getInsuredUuid());
                    continue;
                }

                Insured insured = new Insured();
                modelMapper.map(response, insured);

                insured.setPhone(response.getInsuredPhone());
                insured.setInsuredUuid(response.getInsuredUuid());
                insured.setAddress(response.getAddress1());
                insured.setCity(response.getAddress2());
                insured.setState(response.getState());
                insured.setCountry(response.getCountry());


                insured.setBirthDate(response.getBirthDate());

                Payer payer = payerRepository.findByPayerName(response.getPayerName());
                if (payer == null) {
                    payer = createNewPayer(response);
                    payer = payerRepository.save(payer);
                }

                insured.setPayer(payer);
                insured.setPayerUuid(payer.getPayerUuid());

                insuredRepository.save(insured);
                logger.info("Successfully registered insured: {} {}",
                        response.getFirstName(), response.getInsuredUuid());

            } catch (Exception e) {
                logger.error("Failed to register insured with UUID {}: {}",
                        response.getInsuredUuid(), e.getMessage());
            }
        }
    }

    private Payer createNewPayer(CheckEligibilityResponse response) {
        Payer payer = new Payer();
        payer.setPayerName(response.getPayerName());
        payer.setPayerUuid(UUID.randomUUID().toString());
        payer.setEmail(response.getEmail());
        payer.setTelephone(response.getPayerPhone());
        payer.setStatus(Status.ACTIVE);
        return payer;
    }

}