# Kenema Pharmacy Integration Guide

This document provides technical details for integrating the Kenema Pharmacy Management System (KPMS) with HealthConnect.

## Overview

The integration allows Kenema Pharmacies to:
1. Verify patient insurance eligibility in real-time
2. Record medication dispensing events in HealthConnect
3. Create insurance claims from dispensed medications

## Authentication

All API calls require a valid JWT token obtained through the HealthConnect authentication API:

```
POST /api/v1/healthConnect/auth/login
Content-Type: application/json

{
  "email": "your-api-user@kenema.com",
  "password": "your-password"
}
```

The response will include a JWT token that should be included in all subsequent requests:

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## API Endpoints

### 1. Eligibility Verification

**Endpoint:** `GET /api/v1/healthConnect/eligibility/check/{providerUuid}`

**Request Body:**
```json
{
  "payerUuid": "payer-uuid-from-healthconnect",
  "insuranceId": "patient-insurance-id",
  "employeeId": "optional-employee-id",
  "nationalId": "optional-national-id",
  "phoneNumber": "optional-phone-number",
  "serviceUuid": "optional-service-uuid-for-specific-service-check"
}
```

**Response:**
```json
{
  "eligible": true,
  "insuredPerson": {
    "insuredUuid": "uuid",
    "firstName": "John",
    "lastName": "Doe",
    "insuranceId": "INS12345",
    "policyStartDate": "2023-01-01",
    "policyEndDate": "2023-12-31"
  },
  "groups": [
    {
      "groupUuid": "group-uuid",
      "groupName": "Standard Coverage"
    }
  ],
  "requestedService": {
    "serviceUuid": "service-uuid",
    "serviceName": "Prescription Medication",
    "covered": true,
    "price": 100.00,
    "coPaymentAmount": 20.00,
    "coPaymentPercentage": 20.0,
    "insuranceCoverage": 80.00
  }
}
```

### 2. Record Medication Dispensing

**Endpoint:** `POST /api/v1/healthConnect/integration/pharmacy/dispensing`

**Request Body:**
```json
{
  "providerUuid": "your-pharmacy-provider-uuid",
  "payerUuid": "patient-insurance-payer-uuid",
  "insuranceId": "patient-insurance-id",
  "prescriptionNumber": "RX12345",
  "pharmacyTransactionId": "KPMS-TX-67890",
  "dispensingDate": "2023-06-15T14:30:00",
  "prescribingPhysicianName": "Dr. Jane Smith",
  "prescribingPhysicianId": "MD12345",
  "medicationItems": [
    {
      "medicationCode": "MED001",
      "medicationName": "Amoxicillin",
      "quantity": 30.0,
      "unitOfMeasure": "tablets",
      "unitPrice": 2.50,
      "totalPrice": 75.00,
      "dosageInstructions": "Take 1 tablet 3 times daily",
      "strength": "500mg",
      "formulation": "tablet"
    },
    {
      "medicationCode": "MED002",
      "medicationName": "Ibuprofen",
      "quantity": 20.0,
      "unitOfMeasure": "tablets",
      "unitPrice": 1.25,
      "totalPrice": 25.00,
      "dosageInstructions": "Take 1 tablet as needed for pain",
      "strength": "200mg",
      "formulation": "tablet"
    }
  ],
  "pharmacistNotes": "Patient advised about potential drowsiness"
}
```

**Response:**
```json
{
  "dispensingUuid": "generated-uuid",
  "status": "SUCCESS",
  "message": "Medication dispensing recorded successfully",
  "recordedAt": "2023-06-15T14:35:22",
  "totalAmount": 100.00,
  "patientResponsibility": 20.00,
  "insuranceCoverage": 80.00
}
```

### 3. Get Pending Dispensing Records

**Endpoint:** `GET /api/v1/healthConnect/integration/pharmacy/dispensing/{providerUuid}?patientId=INS12345&page=0&size=20`

**Response:**
```json
{
  "content": [
    {
      "dispensingUuid": "uuid-1",
      "prescriptionNumber": "RX12345",
      "dispensingDate": "2023-06-15T14:30:00",
      "totalAmount": 100.00,
      "patientResponsibility": 20.00,
      "insuranceCoverage": 80.00,
      "claimStatus": "PENDING"
    },
    {
      "dispensingUuid": "uuid-2",
      "prescriptionNumber": "RX12346",
      "dispensingDate": "2023-06-16T10:15:00",
      "totalAmount": 75.50,
      "patientResponsibility": 15.10,
      "insuranceCoverage": 60.40,
      "claimStatus": "PENDING"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

### 4. Create Claim from Dispensing Records

**Endpoint:** `POST /api/v1/healthConnect/integration/pharmacy/dispensing/batch-claim/{providerUuid}`

**Request Body:**
```json
[
  "dispensing-uuid-1",
  "dispensing-uuid-2"
]
```

**Response:**
```json
{
  "claimUuid": "generated-claim-uuid",
  "claimDate": "2023-06-20T09:45:30",
  "status": "SUBMITTED",
  "totalAmount": 175.50,
  "patientResponsibility": 35.10,
  "insuranceCoverage": 140.40,
  "claimType": "PHARMACY",
  "serviceDate": "2023-06-15"
}
```

## Implementation Steps for KPMS

1. **Add HealthConnect Configuration**
   - Add configuration settings for HealthConnect API URL
   - Store provider UUID and API credentials securely

2. **Eligibility Verification**
   - Add "Check Insurance" button to patient checkout screen
   - Implement API call