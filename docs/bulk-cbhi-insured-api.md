# Bulk CBHI Insured Creation API

## Overview
This API endpoint allows you to create multiple insured members under a single payer in one request. This is useful for bulk enrollment scenarios where multiple insured members need to be registered for the same payer.

## Endpoint Details

### Create Bulk CBHI Insured Members
- **URL**: `POST /api/v1/pharmacy-integration/insured/bulk`
- **Authentication**: Requires API Key
- **Content-Type**: `application/json`

### Request Structure

```json
{
  "payerUuid": "string (required)",
  "insuredMembers": [
    {
      "firstName": "string (required)",
      "fatherName": "string (required)", 
      "grandFatherName": "string (optional)",
      "phone": "string (optional)",
      "email": "string (optional)",
      "nationalId": "string (optional)",
      "idNumber": "string (optional)",
      "insuranceId": "string (optional)",
      "birthDate": "date (required, format: YYYY-MM-DD)",
      "gender": "string (required)",
      "address": "string (optional)",
      "city": "string (optional)",
      "state": "string (optional)",
      "country": "string (optional)"
    }
  ]
}
```

### Response Structure

#### Success Response (200 OK)
```json
{
  "payerUuid": "string",
  "payerName": "string",
  "totalRequested": "integer",
  "totalCreated": "integer", 
  "totalFailed": "integer",
  "processedAt": "datetime",
  "createdMembers": [
    {
      "insuredUuid": "string",
      "firstName": "string",
      "fatherName": "string",
      "grandFatherName": "string",
      "insuranceId": "string",
      "nationalId": "string",
      "phone": "string",
      "email": "string"
    }
  ],
  "failedMembers": [
    {
      "firstName": "string",
      "fatherName": "string", 
      "grandFatherName": "string",
      "insuranceId": "string",
      "nationalId": "string",
      "errorMessage": "string",
      "errorCode": "string"
    }
  ]
}
```

#### Error Responses

**400 Bad Request** - Invalid request data
```json
{
  "timestamp": "datetime",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/pharmacy-integration/insured/bulk"
}
```

**404 Not Found** - Payer not found
```json
{
  "timestamp": "datetime",
  "status": 404,
  "error": "Not Found", 
  "message": "Payer not found with payerUuid: {payerUuid}",
  "path": "/api/v1/pharmacy-integration/insured/bulk"
}
```

**401 Unauthorized** - Missing or invalid API key
```json
{
  "timestamp": "datetime",
  "status": 401,
  "error": "Unauthorized",
  "message": "API key is required",
  "path": "/api/v1/pharmacy-integration/insured/bulk"
}
```

## Usage Examples

### cURL Example
```bash
curl -X POST "http://localhost:8080/api/v1/pharmacy-integration/insured/bulk" \
  -H "Content-Type: application/json" \
  -H "X-API-Key: your-api-key-here" \
  -d @sample-requests/bulk-cbhi-insured-request.json
```

### Postman Example
1. Set method to `POST`
2. Set URL to `http://localhost:3012/api/v1/pharmacy-integration/insured/bulk`
3. Add header: `X-API-Key: api-key-here`
4. Set body to raw JSON and paste the request JSON

## Validation Rules

### Required Fields
- `payerUuid`: Must be a valid existing payer UUID
- `insuredMembers`: Cannot be null or empty
- `firstName`: Cannot be blank
- `fatherName`: Cannot be blank  
- `birthDate`: Must be a valid date
- `gender`: Cannot be blank

### Optional Fields
- All other fields are optional but recommended for complete member profiles

## Features

### Partial Success Handling
- The API processes all members even if some fail
- Successfully created members are returned in `createdMembers`
- Failed members are returned in `failedMembers` with error details
- Transaction is atomic per member (individual rollback on failure)

### Error Handling
- Detailed error messages for each failed member
- Error codes to identify failure types
- Comprehensive logging for troubleshooting

### Performance
- Bulk processing reduces API calls
- Optimized database operations
- Transaction management for data consistency

## Best Practices

1. **Batch Size**: Recommended batch size is 50-100 members per request
2. **Error Handling**: Always check both `createdMembers` and `failedMembers` in response
3. **Retry Logic**: Implement retry for failed members using individual create endpoint
4. **Validation**: Validate data before sending to reduce failures
5. **Monitoring**: Monitor response times for large batches

## Related Endpoints

- `POST /api/v1/pharmacy-integration/insured` - Create single insured member
- `GET /api/v1/pharmacy-integration/payers` - Get available payers
- `GET /api/v1/pharmacy-integration/insured/{insuredUuid}` - Get insured member details
