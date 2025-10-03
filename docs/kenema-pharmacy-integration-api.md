# Kenema Pharmacy Integration API

## Overview

The Kenema Pharmacy Integration API allows recording medication dispensing from the Kenema pharmacy management system with external insurance system integration. The system automatically resolves integration configuration from your internal system without requiring changes to the external Kenema request format.

## API Endpoint

**Endpoint:** `POST /api/v1/healthConnect/kenema/dispensing`

**Description:** Record medication dispensing from Kenema pharmacy system with automatic insurance integration

## Request Format 

```json
{
  "cbhid": "CBH123456",
  "identifier": "INS001234567",
  "mrn": "MRN789012",
  "providerBranchName": "Kenema Main Branch",
  "dispensedDate": "2024-01-15",
  "physicianFullName": "Dr. John Smith",
  "providerType": "PHARMACY",
  "providerName": "Kenema Pharmacy",
  "totalPrice": 250.00,
  "prescriptionDetails": [
    {
      "description": "Blood pressure medication",
      "outOfStock": "false",
      "medicationName": "Amlodipine 5mg",
      "quantity": 30,
      "unitOfMeasure": "tablets",
      "price": 150.00,
      "dosage": 5.0,
      "route": "oral",
      "frequency": "once daily",
      "duration": "30 days"
    },
    {
      "description": "Pain relief medication",
      "medicationName": "Ibuprofen 400mg",
      "quantity": 20,
      "unitOfMeasure": "tablets",
      "price": 100.00,
      "dosage": 400.0,
      "route": "oral",
      "frequency": "twice daily",
      "duration": "10 days"
    }
  ]
}
```

## Integration Configuration Resolution

The system automatically resolves the following fields from the internal system:

- **`contractHeaderUuid`**: Resolved from provider-payer contract lookup
- **`isInsurance`**: Determined based on payer type/name analysis
- **`packageUuid`**: Retrieved from insured person's package assignment
- **`dependantUuid`**: Resolved if applicable
- **`serviceId`**: Default pharmacy service or derived from contract details

## Processing Logic

### Automatic Configuration Resolution

The system automatically determines processing mode based on payer analysis:

1. **Payer Analysis**: Examines payer name/type to determine if it's insurance
2. **Contract Resolution**: Finds active contract between provider and payer
3. **Service Resolution**: Uses default pharmacy service or derives from contract details
4. **Package Resolution**: Gets package assignment for insured person (if applicable)

### Insurance Payers (Automatically Detected)

When the system detects an insurance payer:

1. **Records the dispensing** in the local database
2. **Sends data to external insurance system** using the same endpoint format as the main dispensing API

**External API Call:**
```
POST http://192.168.21.234:8888/api/payer/claimconnect/service-provided/{contractUuid}
Content-Type: application/json

{
  "totalPrice": 250.00,
  "providedDate": "2024-01-15",
  "serviceProvidedUuid": "kenema-dispensing-uuid",
  "insuredUuid": "insured-uuid",
  "dependentUuid": "",
  "items": [
    {
      "serviceId": "PHARMACY_DEFAULT_001",
      "serviceName": "Amlodipine 5mg",
      "serviceCode": "MED001",
      "qty": 30,
      "totalPrice": 150.00,
      "recordNumber": "kenema-dispensing-uuid",
      "packageUuid": "resolved-package-uuid"
    }
  ]
}
```

### Non-Insurance Payers (Automatically Detected)

When the system detects a non-insurance payer:

1. **Records the dispensing** in the local database
2. **No external integration** (pharmacy dispensing doesn't require package management)

## Service Resolution Logic

Since the external Kenema system doesn't provide service identification, the system uses:

1. **Drug-based resolution**: Creates contract details for medications automatically
2. **Default service mapping**: Uses configured default pharmacy services
3. **Contract detail creation**: Automatically creates missing contract details

## Failed External System Integration

The system includes comprehensive retry mechanism for failed external calls:

- **Automatic logging** of failed external API calls
- **Scheduled retries** at 12:00 PM daily
- **Exponential backoff** strategy
- **Management APIs** for monitoring and manual retry

## Testing Examples

### Example 1: Insurance Payer (Automatically Detected)

```bash
POST /api/v1/healthConnect/kenema/dispensing
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "cbhid": "CBH123456",
  "insuredUuid": "INS001234567",
  "mrn": "MRN789012",
  "providerBranchName": "Kenema Main Branch",
  "dispensedDate": "2024-01-15",
  "physicianFullName": "Dr. John Smith",
  "providerType": "PHARMACY",
  "providerName": "Kenema Pharmacy",
  "totalPrice": 150.00,
  "prescriptionDetails": [
    {
      "description": "Blood pressure medication",
      "medicationName": "Amlodipine 5mg",
      "quantity": 30,
      "unitOfMeasure": "tablets",
      "price": 150.00,
      "dosage": 5.0,
      "route": "oral",
      "frequency": "once daily",
      "duration": "30 days"
    }
  ]
}
```

**Processing Flow:**
1. ✅ System identifies insured person using `identifier`
2. ✅ System analyzes payer and detects insurance type
3. ✅ System resolves contract, package, and service configuration
4. ✅ Dispensing record saved locally
5. ✅ External insurance system called with resolved configuration
6. ❌ No package management (pharmacy doesn't need it)

### Example 2: Non-Insurance Payer (Automatically Detected)

```bash
POST /api/v1/healthConnect/kenema/dispensing
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "cbhid": "CBH123456",
  "insuredUuid": "EMP001234567",
  "mrn": "MRN789012",
  "dispensedDate": "2024-01-15",
  "totalPrice": 200.00,
  "prescriptionDetails": [
    {
      "medicationName": "Insulin",
      "quantity": 2,
      "price": 200.00,
      "dosage": 10.0,
      "route": "injection",
      "frequency": "twice daily",
      "duration": "30 days"
    }
  ]
}
```

**Processing Flow:**
1. ✅ System identifies insured person using `identifier`
2. ✅ System analyzes payer and detects non-insurance type
3. ✅ Dispensing record saved locally
4. ❌ No external system call (non-insurance)
5. ❌ No package management (pharmacy doesn't need it)

### Example 3: Automatic Configuration Resolution

```bash
POST /api/v1/healthConnect/kenema/dispensing
{
  "insuredUuid": "INS001234567",
  "dispensedDate": "2024-01-15",
  "totalPrice": 300.00,
  "prescriptionDetails": [
    {
      "medicationName": "Amlodipine 5mg",
      "quantity": 30,
      "price": 150.00
    },
    {
      "medicationName": "Metformin 500mg",
      "quantity": 60,
      "price": 150.00
    }
  ]
}
```

**Automatic Resolution:**
1. ✅ `contractHeaderUuid`: Resolved from provider-payer contract
2. ✅ `isInsurance`: Determined from payer analysis
3. ✅ `packageUuid`: Retrieved from insured's package assignment
4. ✅ `serviceId`: Uses default pharmacy service configuration
5. ✅ Contract details: Created automatically for each medication

## Response Format

```json
{
  "dispensingUuid": "kenema-dispensing-uuid",
  "invoiceNumber": "KEN-001",
  "totalAmount": 250.00,
  "status": "ACTIVE",
  "items": [
    {
      "itemUuid": "item-uuid-1",
      "medicationName": "Amlodipine 5mg",
      "quantity": 30,
      "totalPrice": 150.00
    },
    {
      "itemUuid": "item-uuid-2",
      "medicationName": "Ibuprofen 400mg",
      "quantity": 20,
      "totalPrice": 100.00
    }
  ]
}
```

## Error Handling

### Common Errors:

1. **Service Not Found**: When serviceId doesn't match any service
2. **Contract Detail Not Found**: When contractDetailUuid resolution fails
3. **External API Failure**: When insurance system is unavailable
4. **Package Limit Exceeded**: When non-insurance usage exceeds limits
5. **Insured Not Found**: When identifier doesn't match any insured person

### Error Response Example:

```json
{
  "error": "Service not found with serviceId: MED0000000001",
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404
}
```

## Key Features

### 1. **Dual Processing Mode**
- **Insurance**: External system integration
- **Non-Insurance**: Local package usage tracking

### 2. **Flexible Service Identification**
- Uses `serviceId` or `contractDetailUuid` per item
- Automatic drug-based contract detail creation
- Maintains backward compatibility

### 3. **Comprehensive Integration**
- Same external API endpoint as main dispensing
- Failed request retry mechanism
- Package category usage tracking

### 4. **Robust Error Handling**
- External API failures don't break main flow
- Detailed logging for troubleshooting
- Graceful degradation

## Prerequisites

1. Valid API key for Kenema integration
2. Active contract between provider and payer
3. Insured person exists in the system
4. For insurance: External API configuration
5. For non-insurance: Package categories and limits configured

## Monitoring and Logging

The system provides comprehensive logging for:
- Kenema dispensing record creation
- External API calls and responses
- Package usage recording
- Error conditions and failures

Check application logs for detailed information about the processing flow.
