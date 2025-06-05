//package com.medco.HealthConnectProvider.services.integration;
//
//import com.medco.HealthConnectProvider.ui.request.integration.MedicationDispensingRequest;
//import com.medco.HealthConnectProvider.ui.response.integration.DispensingResponse;
//import org.springframework.http.ResponseEntity;
//
//public interface PharmacyIntegrationService {
//
//    /**
//     * Records medications dispensed to a patient from an external pharmacy system
//     *
//     * @param request The dispensing details including medications, patient info, etc.
//     * @return Response with dispensing record ID and status
//     */
//    ResponseEntity<DispensingResponse> recordMedicationDispensing(MedicationDispensingRequest request);
//
//    /**
//     * Retrieves dispensing records that haven't been included in a claim yet
//     *
//     * @param providerUuid The UUID of the pharmacy provider
//     * @param patientId Optional patient identifier to filter records
//     * @param page Page number for pagination
//     * @param size Page size for pagination
//     * @return List of pending dispensing records
//     */
//    ResponseEntity<?> getPendingDispensingRecords(String providerUuid, String patientId, int page, int size);
//
//    /**
//     * Creates a new claim from selected dispensing records
//     *
//     * @param providerUuid The UUID of the pharmacy provider
//     * @param dispensingUuids Array of dispensing record UUIDs to include in the claim
//     * @return Response with the created claim details
//     */
//    ResponseEntity<?> createClaimFromDispensingRecords(String providerUuid, String[] dispensingUuids);
//}