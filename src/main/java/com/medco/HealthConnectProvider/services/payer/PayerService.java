package com.medco.HealthConnectProvider.services.payer;

import com.medco.HealthConnectProvider.ui.request.auth.password.payer.PayerRequest;
import com.medco.HealthConnectProvider.ui.request.claims.ClaimReviewRequest;
import com.medco.HealthConnectProvider.ui.request.search.PayerSearchRequest;
import com.medco.HealthConnectProvider.ui.response.claims.ClaimResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerProviderResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PolicyHolderListResponse;
import com.medco.HealthConnectProvider.ui.response.provider.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.providers.ProviderResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PayerService {
    PayerResponse createPayer(@Valid PayerRequest payerRequest, MultipartFile logo);

    /**
     * Update an existing payer with optional logo update
     * @param payerUuid The UUID of the payer to update
     * @param payerRequest The update request data
     * @param logo Optional logo file to update
     * @return The updated payer response
     */
    PayerResponse updatePayer(String payerUuid, @Valid PayerRequest payerRequest, MultipartFile logo);


    ResponseEntity<?> updatePayerStatus(String payerUuid, Status payerStatus);

    PayerResponse getPayer(String payerUuid);

    List<PayerResponse> getPayers(String search, int page, int limit, Status status);

    List<PolicyHolderListResponse> getPolicyHolders(String search, int page, int limit, Status status);

    List<PayerProviderResponse> getProviderPolicyHolders(String providerUuid, String payerUuid);


    ResponseEntity<?> deletePayer(String payerUuid);

    ResponseEntity<ByteArrayResource> getPayerLogo(String payerUuid);

    PagedResponse<PayerResponse> getPayersWithFilters(String searchKey, int page, int limit, Status status, String category, String payerName, Long tinNumber, String level, String sortBy, String sortDir);


    Page<ClaimResponse> getClaimsForReview(int page, int size);

    ResponseEntity<?> reviewClaim(String claimUuid, ClaimReviewRequest reviewRequest);

    PagedResponse<PayerResponse> getPayersWithFiltersWithOutLogo(String searchKey, int page, int limit, Status status, String category, String payerName, Long tinNumber, String level, String sortBy, String sortDir);
}
