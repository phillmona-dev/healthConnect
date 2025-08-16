# Dispensing Record Integration API

## Overview

The Dispensing Record API has been enhanced to support both insurance and non-insurance payers with automatic external system integration and package usage tracking.

## API Endpoint

**Endpoint:** `POST /api/v1/healthConnect/dispensing-records`

**Description:** Add a new dispensing record with automatic insurance integration or package usage tracking

## Enhanced Request Format

```json
{
  "contractHeaderUuid": "contract-uuid-here",
  "insuredUuid": "insured-uuid-here",
  "dependantUuid": "dependant-uuid-here", // Optional
  "primaryDiagnosis": "Primary diagnosis",
  "secondaryDiagnosis": "Secondary diagnosis",

  // New fields for integration
  "isInsurance": true, // Flag to determine processing type
  "packageUuid": "package-uuid-for-external-system",
  "dispensingDate": "2024-01-15",

  "medicationItems": [
    {
      "contractDetailUuid": "contract-detail-uuid", // Optional if serviceId provided
      "serviceId": "SRV0000000001", // generatedServiceId from serviceList (NEW)
      "itemType": "SERVICE", // or "DRUG"
      "remark": "Medication notes",
      "price": 150.00,
      "quantity": 2
    }
  ]
}
```

**Key Changes:**
- `serviceId` moved from request level to item level
- Each medication item can have its own `serviceId`
- Either `contractDetailUuid` or `serviceId` must be provided for each item

## Processing Logic

### Insurance Payers (isInsurance: true)

When `isInsurance` is `true`, the system:

1. **Saves the dispensing record** to the local database
2. **Sends data to external insurance system** using POST requests with JSON payload

**External API Call Format:**
```
POST http://192.168.100.85:8888/api/payer/claimconnect/service-provided/{contractUuid}
Content-Type: application/json
X-API-Key: your-api-key

{
  "totalPrice": 300.00,
  "providedDate": "2024-01-15",
  "serviceProvidedUuid": "dispensing-uuid",
  "insuredUuid": "insured-uuid",
  "dependentUuid": "",
  "items": [
    {
      "serviceId": "SRV0000000001",
      "serviceName": "Blood pressure medication",
      "serviceCode": "BP001",
      "qty": 2,
      "totalPrice": 300.00,
      "recordNumber": "dispensing-uuid",
      "packageUuid": "package-uuid"
    }
  ]
}
```

**URL Path Parameter:**
- `{contractUuid}`: The contractHeaderUuid from the dispensing request

**JSON Payload Fields:**
- `totalPrice`: Total price for all items
- `providedDate`: Date when service was provided (YYYY-MM-DD format)
- `serviceProvidedUuid`: Dispensing UUID (maps to serviceProvidedUuid in external system)
- `insuredUuid`: Insured person UUID
- `dependentUuid`: Dependent UUID (empty string if not applicable)
- `items`: Array of dispensing items with service details

### Non-Insurance Payers (isInsurance: false)

When `isInsurance` is `false`, the system:

1. **Saves the dispensing record** to the local database
2. **Reduces package usage** for the insured person using the PackageCategoryUsageService

## ContractDetailUuid Resolution

The system supports flexible service identification with the following priority:

1. **Priority 1**: Use `contractDetailUuid` if provided in the medication item
2. **Priority 2**: Use `serviceId` from the medication item if provided
   - Find the service using `generatedServiceId`
   - Locate the corresponding contract detail in the specified contract
   - Validate that the service is covered in the contract
3. **Error**: If neither `contractDetailUuid` nor `serviceId` is provided

**Validation Logic:**
- If `serviceId` is provided, the system validates that the service exists and is covered in the contract
- If `contractDetailUuid` is provided, the system validates that it exists in the contract
- Each medication item can use different identification methods

## External System Integration

### Configuration

Add these properties to your `application.properties`:

```properties
# External API Configuration
external.api.external-api-base-url=http://192.168.100.85:8888
external.api.api-key=your-external-api-key-here
external.api.connection-timeout=30000
external.api.read-timeout=60000
external.api.max-retries=3
external.api.enabled=true
```

### External API Endpoint

The system calls: `POST {external-api-base-url}/api/payer/claimconnect/service-provided/{contractUuid}`

**Example Request:**
```bash
POST http://192.168.100.85:8888/api/payer/claimconnect/service-provided/contract-123-uuid
Content-Type: application/json
X-API-Key: your-api-key

{
  "totalPrice": 500,
  "providedDate": "2025-02-02",
  "serviceProvidedUuid": "0a7b9f08-aa0e-43f2-a6d6-08bf99251dda",
  "insuredUuid": "b377610c-539b-45f2-be3d-8802287878bd",
  "dependentUuid": "",
  "items": [
    {
      "serviceId": "MRI00000007",
      "serviceName": "x-ray",
      "serviceCode": "xry0001",
      "qty": 20,
      "totalPrice": 500,
      "recordNumber": "0a7b9f08-aa0e-43f2-a6d6-08bf99251dda",
      "packageUuid": "5c0e7714-704b-42e7-a0a7-3cc649f12c35"
    }
  ]
}
```

**URL Components:**
- `{contractUuid}`: The contractHeaderUuid from the dispensing request

With headers:
- `Content-Type: application/json`
- `X-API-Key: {api-key}`

## Testing Examples

### Example 1: Insurance Payer with External Integration

```bash
POST /api/v1/healthConnect/dispensing-records
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "contractHeaderUuid": "contract-123-uuid",
  "insuredUuid": "insured-456-uuid",
  "primaryDiagnosis": "Hypertension",
  "isInsurance": true,
  "packageUuid": "package-789-uuid",
  "dispensingDate": "2024-01-15",
  "medicationItems": [
    {
      "serviceId": "SRV0000000001",
      "itemType": "SERVICE",
      "remark": "Blood pressure medication",
      "price": 75.00,
      "quantity": 1
    }
  ]
}
```

**Expected Behavior:**
1. Dispensing record saved locally
2. POST request sent to external insurance system with JSON payload:
   ```json
   {
     "totalPrice": 75.00,
     "providedDate": "2024-01-15",
     "serviceProvidedUuid": "dispensing-uuid",
     "insuredUuid": "insured-456-uuid",
     "dependentUuid": "",
     "items": [
       {
         "serviceId": "SRV0000000001",
         "serviceName": "Blood pressure medication",
         "serviceCode": "BP001",
         "qty": 1,
         "totalPrice": 75.00,
         "recordNumber": "dispensing-uuid",
         "packageUuid": "package-789-uuid"
       }
     ]
   }
   ```
3. No local package usage reduction

### Example 2: Non-Insurance Payer with Package Usage

```bash
POST /api/v1/healthConnect/dispensing-records
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "contractHeaderUuid": "contract-123-uuid",
  "insuredUuid": "insured-456-uuid",
  "primaryDiagnosis": "Diabetes",
  "isInsurance": false,
  "medicationItems": [
    {
      "contractDetailUuid": "contract-detail-uuid",
      "itemType": "DRUG",
      "remark": "Insulin medication",
      "price": 120.00,
      "quantity": 2
    }
  ]
}
```

**Expected Behavior:**
1. Dispensing record saved locally
2. Package usage recorded for insured person
3. Category limits reduced accordingly
4. No external system call

### Example 3: Mixed Item Identification Methods

```bash
POST /api/v1/healthConnect/dispensing-records
Authorization: Bearer {your-jwt-token}
Content-Type: application/json

{
  "contractHeaderUuid": "contract-123-uuid",
  "insuredUuid": "insured-456-uuid",
  "primaryDiagnosis": "Mixed Services",
  "isInsurance": true,
  "packageUuid": "package-789-uuid",
  "medicationItems": [
    {
      "serviceId": "CON0000000001",
      "itemType": "SERVICE",
      "remark": "General consultation",
      "price": 50.00,
      "quantity": 1
    },
    {
      "contractDetailUuid": "contract-detail-uuid-2",
      "itemType": "DRUG",
      "remark": "Prescribed medication",
      "price": 75.00,
      "quantity": 2
    }
  ]
}
```

## Response Format

The response remains the same as the original implementation:

```json
{
  "dispensingUuid": "dispensing-uuid",
  "invoiceNumber": "INV-001",
  "totalAmount": 150.00,
  "status": "ACTIVE",
  "items": [
    {
      "itemUuid": "item-uuid",
      "medicationName": "Service Name",
      "quantity": 1,
      "totalPrice": 150.00
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

### Error Response Example:

```json
{
  "error": "Service not found with generatedServiceId: SRV0000000001",
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404
}
```

## Key Features

### 1. **Dual Processing Mode**
- Insurance: External system integration
- Non-Insurance: Local package usage tracking

### 2. **Smart ContractDetail Resolution**
- Uses provided contractDetailUuid
- Falls back to serviceId resolution
- Maintains backward compatibility

### 3. **Robust Error Handling**
- External API failures don't break main flow
- Detailed logging for troubleshooting
- Graceful degradation

### 4. **Configurable Integration**
- External API can be enabled/disabled
- Configurable timeouts and retries
- Environment-specific settings

## Prerequisites

1. Valid JWT token from provider user
2. Active contract with contract details
3. Insured person exists in the system
4. For insurance: External API configuration
5. For non-insurance: Package categories and limits configured

## Monitoring and Logging

The system provides comprehensive logging for:
- Dispensing record creation
- External API calls and responses
- Package usage recording
- Error conditions and failures

Check application logs for detailed information about the processing flow.
