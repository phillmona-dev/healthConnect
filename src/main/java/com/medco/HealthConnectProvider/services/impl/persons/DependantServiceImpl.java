package com.medco.HealthConnectProvider.services.impl.persons;

import com.medco.HealthConnectProvider.entity.persons.Dependant;
import com.medco.HealthConnectProvider.entity.persons.Insured;
import com.medco.HealthConnectProvider.exception.ResourceNotFoundException;
import com.medco.HealthConnectProvider.repository.persons.DependantRepository;
import com.medco.HealthConnectProvider.repository.persons.InsuredRepository;
import com.medco.HealthConnectProvider.services.persons.DependantService;
import com.medco.HealthConnectProvider.ui.request.auth.password.persons.DependantRequest;
import com.medco.HealthConnectProvider.ui.response.MessageResponse;
import com.medco.HealthConnectProvider.ui.response.persons.DependantResponse;
import com.medco.HealthConnectProvider.utils.enums.Status;
import jakarta.validation.Valid;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DependantServiceImpl implements DependantService {

    private final DependantRepository dependantRepository;
    private final InsuredRepository insuredRepository;

    public DependantServiceImpl(DependantRepository dependantRepository, InsuredRepository insuredRepository) {
        this.dependantRepository = dependantRepository;
        this.insuredRepository = insuredRepository;
    }

    @Override
    public ResponseEntity<?> createDependant(@Valid DependantRequest dependantRequest) {

        Dependant dependant = new Dependant();

        BeanUtils.copyProperties(dependantRequest, dependant);
        if (dependantRequest.getDependantStatus() != null)
            dependant.setStatus(dependantRequest.getDependantStatus());
        else
            dependant.setStatus(Status.PENDING);
        Insured insured=insuredRepository.findByInsuredUuid(dependantRequest.getDependantUuid());
        dependant.setInsured(insured);
        dependantRepository.save(dependant);
        return ResponseEntity.ok(new MessageResponse("Dependant person added successfully!"));

    }

    @Override
    public ResponseEntity<?> updateDependant(String dependantUuid, @Valid DependantRequest dependantRequest) {
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);

        if (dependant == null)
            throw new ResourceNotFoundException("Dependant", "dependantUuid", dependantUuid);
        BeanUtils.copyProperties(dependantRequest, dependant);
        if (dependantRequest.getDependantStatus() != null)
            dependant.setStatus(dependantRequest.getDependantStatus());
        else
            dependant.setStatus(Status.PENDING);
        Insured insured=insuredRepository.findByInsuredUuid(dependantRequest.getDependantUuid());
        dependant.setInsured(insured);

        dependantRepository.save(dependant);

        return ResponseEntity.ok(new MessageResponse("Dependant person updated Successfully!"));
    }

    @Override
    public DependantResponse getDependant(String dependantUuid) {
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);
        DependantResponse dependantResponse = new DependantResponse();
        BeanUtils.copyProperties(dependant, dependantResponse);
        return dependantResponse;
    }

    @Override
    public List<DependantResponse> getPersonDependants(String insuredPersonUuid, Status status) {

        List<Dependant> dependantList = dependantRepository.findAllByIsDeletedAndInsuredInsuredUuidAndStatus(false,
                insuredPersonUuid, status);

        List<DependantResponse> dependantResponse = new ArrayList<>();
        for (Dependant p : dependantList) {
            DependantResponse pr = new DependantResponse();
            BeanUtils.copyProperties(p, pr);
            dependantResponse.add(pr);
        }
        return dependantResponse;
    }

    @Override
    public ResponseEntity<?> deleteDependant(String dependantUuid) {
        Dependant dependant = dependantRepository.findByDependantUuid(dependantUuid);

        if (dependant == null)
            throw new ResourceNotFoundException("Dependant", "dependantUuid", dependantUuid);
        dependant.setDeleted(true);
        dependantRepository.save(dependant);
        return ResponseEntity.ok(new MessageResponse("Dependant person deleted successfully!"));
    }

}
