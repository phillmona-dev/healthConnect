package com.medco.HealthConnectProvider.services.impl.payer;

import com.medco.HealthConnectProvider.dto.PayerAdminDto;
import com.medco.HealthConnectProvider.entity.payers.Payer;
import com.medco.HealthConnectProvider.entity.user.Role;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.contract.ContractRepository;
import com.medco.HealthConnectProvider.repository.payer.PayerRepository;
import com.medco.HealthConnectProvider.repository.user.RoleRepository;
import com.medco.HealthConnectProvider.services.payer.PayerService;
import com.medco.HealthConnectProvider.services.user.UserService;
import com.medco.HealthConnectProvider.ui.request.auth.password.payer.PayerRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerProviderResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PayerResponse;
import com.medco.HealthConnectProvider.ui.response.payer.PolicyHolderListResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.transaction.Transactional;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class PayerServiceImpl implements PayerService {

    private final PayerRepository payerRepository;
    private final RoleRepository roleRepository;
    private final UserService userService;
    private final ContractRepository contractRepository;

    public PayerServiceImpl(PayerRepository payerRepository, RoleRepository roleRepository, UserService userService, ContractRepository contractRepository) {
        this.payerRepository = payerRepository;
        this.roleRepository = roleRepository;
        this.userService = userService;
        this.contractRepository = contractRepository;
    }


    @Transactional
    @Override
    public PayerResponse createPayer(PayerRequest payerRequest) {

        if (payerRepository.existsByEmail(payerRequest.getEmail())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Error: Email is already in use!");
        }
        if (payerRepository.existsByTelephone(payerRequest.getTelephone())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Error: Phone is already in use!");
        }

        if (payerRepository.existsByPayerName(payerRequest.getPayerName())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Error: Payer name (group policy holder name) is already in use!");

        }

        Payer payer = new Payer();
        BeanUtils.copyProperties(payerRequest, payer);
        if (payerRequest.getStatus() != null)
            payer.setStatus(payerRequest.getStatus());
        else
            payer.setStatus(Status.PENDING);
        Payer payer1=payerRepository.save(payer);

        Role role=new Role();
        role.setRoleName(payerRequest.getPayerName()+"_Admin");
        role.setPayerUuid(payer1.getPayerUuid());
        role.setRoleDescription("manages the system for " + payerRequest.getPayerName());

        Role savedRole = roleRepository.save(role);

        PayerAdminDto payerAdminDto = getPayerAdminDto(payerRequest, savedRole, payer1);

        userService.createUser(payerAdminDto);

        return getPayerResponse(payer);

    }

    private static PayerAdminDto getPayerAdminDto(PayerRequest payerRequest, Role savedRole, Payer payer1) {
        PayerAdminDto payerAdminDto = new PayerAdminDto();
        payerAdminDto.setEmail(payerRequest.getEmail());
        payerAdminDto.setGender("Male");
        payerAdminDto.setTitle("Mr");
        payerAdminDto.setFirstName("Institution" );
        payerAdminDto.setFatherName("Admin");
        payerAdminDto.setGrandFatherName("Default");
        payerAdminDto.setMobilePhone(payerRequest.getTelephone());

        // Generate a random password
        String randomPassword = generateRandomPassword();
        payerAdminDto.setPassword(randomPassword);

        payerAdminDto.setRoleUuid(savedRole.getRoleUuid());
        payerAdminDto.setPayerUuid(payer1.getPayerUuid());
        return payerAdminDto;
    }

    private static String generateRandomPassword() {
        // Generate a random password with 10 characters
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    @Override
    public PayerResponse updatePayer(String payerUuid, PayerRequest payerRequest) {
        Payer payer = payerRepository.findByPayerUuid(payerUuid);
        List<Payer> institutionName = payerRepository
                .findAllByPayerName(payerRequest.getPayerName());

        if (payer == null)
            throw new ResourceNotFoundException("Institution", "institutionUuid", payerUuid);

        if (institutionName.size() >= 2)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Error: Duplicated Institution Name is not allowed.");

        BeanUtils.copyProperties(payerRequest, payer);
        if (payerRequest.getStatus() != null)
            payer.setStatus(payerRequest.getStatus());
        else
            payer.setStatus(Status.PENDING);
        payerRepository.save(payer);
        return getPayerResponse( payer);
    }

    @Override
    public ResponseEntity<?> setPayerInsuranceNumber(String payerUuid, String payerInsuranceNumber) {

            Payer payer = payerRepository.findByPayerUuid(payerUuid);

            if (payer == null)
                throw new ResourceNotFoundException("Institution", "payerUuid", payerUuid);

        payer.setPayerInsuranceNumber(payerInsuranceNumber);
            payerRepository.save(payer);
            return ResponseEntity.ok(new MessageResponse("Policy Issued successfully!"));
    }

    @Override
    public ResponseEntity<?> updatePayerStatus(String payerUuid, Status payerStatus) {
        Payer payer=payerRepository.findByPayerUuid(payerUuid);
        payer.setStatus(payerStatus);
        payerRepository.save(payer);
        return ResponseEntity.ok("your institution is " + payerStatus);
    }

    @Override
    public PayerResponse getPayer(String payerUuid) {
        Payer payer = payerRepository.findByPayerUuid(payerUuid);

        if (payer == null)
            throw new ResourceNotFoundException("Institution", "payerUuid", payerUuid);
        return getPayerResponse(payer);
    }

    @Override
    public List<PayerResponse> getPayers(String search, int page, int limit, Status status) {
        if (page > 0)
            page = page - 1;
        Pageable pageRequest = PageRequest.of(page, limit, Sort.by("id").descending());
        Page<Payer>
                payer = search!=null?payerRepository.findAllByIsDeletedAndPayerNameContainingAndStatus(false, search,
                status, pageRequest): payerRepository.findAllByIsDeletedAndStatus(false, status, pageRequest);
        long totalPages = payer.getTotalPages();
        List<Payer> institutionList = payer.getContent();

        List<PayerResponse> institutionResponse = new ArrayList<>();
        for (Payer p : institutionList) {
            PayerResponse pr = new PayerResponse();
            if (institutionResponse.isEmpty())
                pr.setTotalPages(totalPages);
            BeanUtils.copyProperties(p, pr);
            institutionResponse.add(pr);
        }
        return institutionResponse;
    }

    @Override
    public List<PolicyHolderListResponse> getPolicyHolders(String search, int page, int limit, Status status) {
        Pageable pageable = PageRequest.of(page > 0 ? page - 1 : 0, limit, Sort.by("id").ascending());
        return contractRepository.findPolicyHoldersList(search, status, pageable);
    }

    @Override
    public List<PayerProviderResponse> getProviderPolicyHolders(String providerUuid, String payerUuid) {
        return contractRepository.findProviderPolicyHolders(providerUuid, payerUuid);
    }

    @Override
    public ResponseEntity<?> deletePayer(String payerUuid) {
        Payer institution = payerRepository.findByPayerUuid(payerUuid);
        if (institution == null)
            throw new ResourceNotFoundException("Institution", "institutionUuid", payerUuid);

        institution.setDeleted(true);
        payerRepository.save(institution);
        return ResponseEntity.ok(new MessageResponse("Institution deleted successfully!"));
    }

    private PayerResponse getPayerResponse(Payer payer) {
        PayerResponse payerResponse = new PayerResponse();
        BeanUtils.copyProperties(payer, payerResponse);
        return payerResponse;
    }

}
