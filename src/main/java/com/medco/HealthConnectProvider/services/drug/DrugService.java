package com.medco.HealthConnectProvider.services.drug;

import com.medco.HealthConnectProvider.ui.request.drug.DrugRequest;
import com.medco.HealthConnectProvider.ui.response.PagedResponse;
import com.medco.HealthConnectProvider.ui.response.drug.DrugResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.util.List;

public interface DrugService {

    ResponseEntity<DrugResponse> createDrug(String providerUuid, DrugRequest drugRequest);

    ResponseEntity<?> updateDrug(String drugUuid, DrugRequest drugRequest);

    ResponseEntity<?> deleteDrug(String drugUuid);

    DrugResponse getDrug(String drugUuid);

    PagedResponse<DrugResponse> searchDrugs(String providerUuid, String searchKey, int page, int limit);

    ResponseEntity<?> exportDrugList(HttpServletResponse response, String providerUuid);

    ResponseEntity<?> importDrugListData(File file, String providerUuid) throws IOException;
}