# Service Validation Bypass Feature

## Overview

The dispensing record API now supports bypassing service validation when `serviceId` is provided in the medication items. This allows for more flexible service identification and faster processing when the service is already known.

## How It Works

### Automatic Detection

The system automatically detects if any medication item contains a `serviceId` and skips the traditional service validation process:

```java
private boolean shouldValidateServices(List<DispensingItemRequest> medicationItems) {
    // Check if any item has serviceId provided
    boolean hasServiceId = medicationItems.stream()
            .anyMatch(item -> item.getServiceId() != null && !item.getServiceId().trim().isEmpty());
    
    if (hasServiceId) {
        return false; // Skip validation
    }
    
    return true; // Perform validation
}
```

### Processing Logic

#### With Service Validation (Traditional)
```
1. Validate all services exist for provider
2. Check service coverage in contract
3. Create dispensing items using validated services
```

#### With Service Validation Bypass (New)
```
1. Skip bulk service validation
2. Resolve services individually from serviceId/contractDetailUuid
3. Create dispensing items using resolved services
```

## Request Examples

### Example 1: Service Validation Bypass

```json
{
  "contractHeaderUuid": "contract-123-uuid",
  "insuredUuid": "insured-456-uuid",
  "isInsurance": true,
  "packageUuid": "package-789-uuid",
  "medicationItems": [
    {
      "serviceId": "SRV0000000001",
      "itemType": "SERVICE",
      "remark": "Blood pressure medication",
      "price": 75.00,
      "quantity": 1
    },
    {
      "serviceId": "CON0000000001", 
      "itemType": "SERVICE",
      "remark": "Consultation",
      "price": 50.00,
      "quantity": 1
    }
  ]
}
```

**Processing Flow:**
1. ✅ System detects `serviceId` in items
2. ✅ **Skips service validation**
3. ✅ Resolves each service individually using `serviceId`
4. ✅ Creates dispensing items with resolved services

### Example 2: Mixed Identification Methods

```json
{
  "contractHeaderUuid": "contract-123-uuid",
  "insuredUuid": "insured-456-uuid",
  "isInsurance": false,
  "medicationItems": [
    {
      "serviceId": "SRV0000000001",
      "itemType": "SERVICE",
      "price": 75.00,
      "quantity": 1
    },
    {
      "contractDetailUuid": "contract-detail-uuid-2",
      "itemType": "DRUG",
      "price": 100.00,
      "quantity": 2
    }
  ]
}
```

**Processing Flow:**
1. ✅ System detects `serviceId` in first item
2. ✅ **Skips service validation**
3. ✅ **Item 1**: Resolves service using `serviceId`
4. ✅ **Item 2**: Resolves service using `contractDetailUuid`

### Example 3: Traditional Service Validation

```json
{
  "contractHeaderUuid": "contract-123-uuid",
  "insuredUuid": "insured-456-uuid",
  "isInsurance": true,
  "medicationItems": [
    {
      "contractDetailUuid": "contract-detail-uuid-1",
      "itemType": "SERVICE",
      "price": 75.00,
      "quantity": 1
    },
    {
      "contractDetailUuid": "contract-detail-uuid-2",
      "itemType": "DRUG",
      "price": 100.00,
      "quantity": 2
    }
  ]
}
```

**Processing Flow:**
1. ✅ System detects no `serviceId` in items
2. ✅ **Performs traditional service validation**
3. ✅ Validates all services exist and are covered
4. ✅ Creates dispensing items with validated services

## Service Resolution Priority

When service validation is bypassed, each item uses the following resolution priority:

### Priority 1: ServiceId
```java
if (itemRequest.getServiceId() != null) {
    service = servicelistRepository.findByGeneratedServiceIdAndProviderProviderUuid(
            itemRequest.getServiceId(), providerUuid);
}
```

### Priority 2: ContractDetailUuid
```java
if (itemRequest.getContractDetailUuid() != null) {
    ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(
            itemRequest.getContractDetailUuid());
    service = contractDetail.getServicelist();
}
```

### Error Handling
```java
if (service == null) {
    log.error("Could not resolve service for item request");
    uncoveredItems.add("Item: Could not resolve service");
    continue; // Skip this item
}
```

## Benefits

### 1. **Performance Improvement** ⚡
- **Faster processing**: Skips bulk validation when not needed
- **Reduced database queries**: Direct service resolution
- **Optimized for known services**: When serviceId is already available

### 2. **Flexibility** 🔧
- **Mixed identification**: Can use different methods per item
- **Backward compatibility**: Traditional validation still works
- **Graceful degradation**: Handles missing services gracefully

### 3. **Better Error Handling** 🛡️
- **Item-level errors**: Failed items don't break entire request
- **Detailed logging**: Clear indication of resolution method used
- **Comprehensive feedback**: Uncovered items are tracked

## Logging Examples

### Service Validation Bypass
```
INFO: ServiceId found in medication items - skipping service validation
INFO: Processing item request 1 of 2 with serviceId resolution
INFO: Resolved Service: Blood Pressure Check, Quantity: 1, Price: 75.0
```

### Traditional Service Validation
```
INFO: No serviceId found in medication items - performing service validation
INFO: Services validated. Count: 2
INFO: Processing item request 1 of 2 with validated service
INFO: Service: Blood Pressure Check, Quantity: 1, Price: 75.0
```

### Service Resolution Failure
```
ERROR: Service not found for serviceId: INVALID001 and provider: provider-uuid
ERROR: Could not resolve service for item request 1
```

## Error Scenarios

### 1. **Invalid ServiceId**
- Service not found for given serviceId
- Service exists but not for the provider
- Item skipped, processing continues

### 2. **Invalid ContractDetailUuid**
- Contract detail not found
- Contract detail has no associated service
- Item skipped, processing continues

### 3. **Missing Identification**
- Neither serviceId nor contractDetailUuid provided
- Cannot resolve service
- Item skipped, processing continues

## Best Practices

### 1. **Use ServiceId When Available**
- Provides fastest processing
- Most direct service resolution
- Recommended for known services

### 2. **Mix Methods as Needed**
- Use serviceId for standard services
- Use contractDetailUuid for custom arrangements
- System handles both seamlessly

### 3. **Monitor Uncovered Items**
- Check response for uncovered items
- Investigate resolution failures
- Ensure all items are processed correctly

## Response Format

The response includes information about uncovered items:

```json
{
  "dispensingUuid": "dispensing-uuid",
  "totalAmount": 125.00,
  "status": "ACTIVE",
  "uncoveredItems": [
    "Item 2: Could not resolve service"
  ],
  "items": [
    {
      "itemUuid": "item-uuid-1",
      "serviceId": "SRV0000000001",
      "quantity": 1,
      "totalPrice": 75.00
    }
  ]
}
```

## Migration Guide

### Existing Integrations
- **No changes required**: Traditional validation still works
- **Gradual adoption**: Can start using serviceId incrementally
- **Backward compatible**: All existing requests continue to work

### New Integrations
- **Use serviceId**: For better performance when service is known
- **Mix methods**: Use different identification per item as needed
- **Handle errors**: Check for uncovered items in response

This feature provides significant performance improvements while maintaining full backward compatibility and flexibility for different service identification scenarios.
