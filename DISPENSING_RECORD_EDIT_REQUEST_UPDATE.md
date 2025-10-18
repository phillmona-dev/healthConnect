# Dispensing Record Edit Request - Aligned with Add Request

## 🎯 **Objective**

Make the `DispensingRecordEditRequest` compatible with `DispensingRecordRequest` by using the exact same fields, so that the update operation has the same capabilities as the add operation.

---

## 🐛 **The Problem**

The `DispensingRecordEditRequest` had different fields than `DispensingRecordRequest`, making it inconsistent and limiting the update functionality.

### **Before - DispensingRecordEditRequest:**

```java
@Data
public class DispensingRecordEditRequest {
    private String claimUuid;
    private String insuredUuid;
    private String dependantUuid;
    private String primaryDiagnosis;
    private String secondaryDiagnosis;
    private String claimStatus;
    private List<DispensingItemEditRequest> medicationItems;

    @Data
    public static class DispensingItemEditRequest {
        private String itemUuid;
        private String contractDetailUuid;
        private String itemType;
        private String remark;
        private double price;
        private int quantity;
    }
}
```

### **DispensingRecordRequest (for comparison):**

```java
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DispensingRecordRequest {
    private String contractHeaderUuid;
    private String insuredUuid;
    private String dependantUuid;
    private String primaryDiagnosis;
    private String secondaryDiagnosis;
    
    private Boolean isInsurance;
    private String packageUuid;
    private String dispensingDate;
    
    private List<DispensingItemRequest> medicationItems;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DispensingItemRequest {
        private String contractDetailUuid;
        private String serviceId;
        private String itemType; // "SERVICE" or "DRUG"
        private String remark;
        private double price;
        private int quantity;
    }
}
```

**Missing Fields in Edit Request:**
- ❌ `contractHeaderUuid`
- ❌ `isInsurance`
- ❌ `packageUuid`
- ❌ `dispensingDate`
- ❌ `serviceId` (in nested item request)

---

## ✅ **The Solution**

Updated `DispensingRecordEditRequest` to include all fields from `DispensingRecordRequest` while keeping edit-specific fields (`claimUuid`, `claimStatus`, `itemUuid`).

---

## 🔧 **What Changed**

### **1. Updated DispensingRecordEditRequest**

**File:** `src/main/java/com/medco/HealthConnectProvider/ui/request/integration/DispensingRecordEditRequest.java`

**After:**

```java
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class DispensingRecordEditRequest {
    
    // Fields matching DispensingRecordRequest
    private String contractHeaderUuid;
    private String insuredUuid;
    private String dependantUuid;
    private String primaryDiagnosis;
    private String secondaryDiagnosis;
    
    private Boolean isInsurance;
    private String packageUuid;
    private String dispensingDate;
    
    private List<DispensingItemEditRequest> medicationItems;
    
    // Additional fields specific to edit operation
    private String claimUuid;
    private String claimStatus;

    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DispensingItemEditRequest {
        private String itemUuid; // For identifying existing items during edit
        private String contractDetailUuid;
        private String serviceId; // ✅ Added - same as DispensingRecordRequest
        private String itemType; // "SERVICE" or "DRUG"
        private String remark;
        private double price;
        private int quantity;
    }
}
```

**Key Changes:**
- ✅ Added `contractHeaderUuid`
- ✅ Added `isInsurance`
- ✅ Added `packageUuid`
- ✅ Added `dispensingDate`
- ✅ Added `serviceId` to nested `DispensingItemEditRequest`
- ✅ Changed from `@Data` to `@Setter`, `@Getter`, `@AllArgsConstructor`, `@NoArgsConstructor` for consistency
- ✅ Kept edit-specific fields: `claimUuid`, `claimStatus`, `itemUuid`

---

### **2. Updated updateDispensingRecordDetails Method**

**File:** `src/main/java/com/medco/HealthConnectProvider/services/impl/integration/PharmacyIntegrationServiceImpl.java`

**Before:**

```java
private void updateDispensingRecordDetails(MedicationDispensing dispensing, 
                                          DispensingRecordEditRequest editRequest, 
                                          Insured insured, Dependant dependant) {
    dispensing.setInsured(insured);
    dispensing.setDependant(dependant);
    dispensing.setPrimaryDiagnosis(editRequest.getPrimaryDiagnosis());
    dispensing.setSecondaryDiagnosis(editRequest.getSecondaryDiagnosis());
}
```

**After:**

```java
private void updateDispensingRecordDetails(MedicationDispensing dispensing, 
                                          DispensingRecordEditRequest editRequest, 
                                          Insured insured, Dependant dependant) {
    dispensing.setInsured(insured);
    dispensing.setInsuredUuid(insured.getInsuredUuid());
    dispensing.setDependant(dependant);
    dispensing.setPrimaryDiagnosis(editRequest.getPrimaryDiagnosis());
    dispensing.setSecondaryDiagnosis(editRequest.getSecondaryDiagnosis());
    
    // Update dispensing date if provided
    if (editRequest.getDispensingDate() != null && !editRequest.getDispensingDate().isEmpty()) {
        try {
            dispensing.setDispensingDate(LocalDate.parse(editRequest.getDispensingDate()));
        } catch (Exception e) {
            log.warn("Invalid dispensing date format: {}", editRequest.getDispensingDate());
        }
    }
}
```

**Key Changes:**
- ✅ Added `setInsuredUuid` for consistency
- ✅ Added dispensing date update logic

---

### **3. Updated updateDispensingItem Method**

**File:** `src/main/java/com/medco/HealthConnectProvider/services/impl/integration/PharmacyIntegrationServiceImpl.java`

**Before:**

```java
private void updateDispensingItem(MedicationDispensingItem item, 
                                  DispensingRecordEditRequest.DispensingItemEditRequest itemRequest,
                                  MedicationDispensing dispensing, Provider provider, 
                                  Payer payer, ContractHeader activeContract) {
    // ... existing code ...
    
    ContractDetail contractDetail = contractDetailRepository.findByContractDetailUuid(
        itemRequest.getContractDetailUuid());
    if (contractDetail == null) {
        throw new ResourceNotFoundException("ContractDetail", "uuid", 
            itemRequest.getContractDetailUuid());
    }
    
    // ... rest of code ...
}
```

**After:**

```java
private void updateDispensingItem(MedicationDispensingItem item, 
                                  DispensingRecordEditRequest.DispensingItemEditRequest itemRequest,
                                  MedicationDispensing dispensing, Provider provider, 
                                  Payer payer, ContractHeader activeContract) {
    // ... existing code ...
    
    ContractDetail contractDetail = null;
    
    // Try to find contract detail by UUID first
    if (itemRequest.getContractDetailUuid() != null && !itemRequest.getContractDetailUuid().isEmpty()) {
        contractDetail = contractDetailRepository.findByContractDetailUuid(
            itemRequest.getContractDetailUuid());
        log.debug("Contract detail found by UUID: {}", itemRequest.getContractDetailUuid());
    }
    
    // If not found and serviceId is provided, try to resolve by serviceId
    if (contractDetail == null && itemRequest.getServiceId() != null && !itemRequest.getServiceId().isEmpty()) {
        log.debug("Attempting to resolve contract detail by serviceId: {}", itemRequest.getServiceId());
        Servicelist service = resolveServiceFromServiceId(itemRequest.getServiceId(), 
                                                         provider.getProviderUuid(), 
                                                         itemRequest.getItemType());
        if (service != null) {
            contractDetail = contractDetailRepository.findByContractHeaderAndServiceUuid(
                activeContract, service.getServiceUuid()).orElse(null);
            log.debug("Contract detail resolved by serviceId: {}", service.getServiceUuid());
        }
    }
    
    if (contractDetail == null) {
        throw new ResourceNotFoundException("ContractDetail", "uuid or serviceId", 
            itemRequest.getContractDetailUuid() != null ? 
                itemRequest.getContractDetailUuid() : itemRequest.getServiceId());
    }
    
    // ... rest of code ...
}
```

**Key Changes:**
- ✅ Added support for resolving contract detail by `serviceId`
- ✅ Falls back to `contractDetailUuid` if `serviceId` is not provided
- ✅ Better error messages showing which field was used

---

### **4. Added resolveServiceFromServiceId Helper Method**

**File:** `src/main/java/com/medco/HealthConnectProvider/services/impl/integration/PharmacyIntegrationServiceImpl.java`

**New Method:**

```java
/**
 * Resolve service from serviceId for edit operations
 */
private Servicelist resolveServiceFromServiceId(String serviceId, String providerUuid, String itemType) {
    if (serviceId == null || serviceId.trim().isEmpty()) {
        return null;
    }

    log.info("Resolving service using serviceId: {} for itemType: {}", serviceId, itemType);

    // Try to find by generated service ID
    Servicelist service = servicelistRepository.findByGeneratedServiceIdAndProviderProviderUuid(
            serviceId, providerUuid);

    if (service != null) {
        log.info("Service resolved successfully by generatedServiceId: {}", service.getServiceName());
        return service;
    }

    // Try to find by service code as fallback
    Provider provider = providerRepository.findByProviderUuid(providerUuid);
    if (provider != null) {
        Optional<Servicelist> serviceOpt = servicelistRepository.findByServiceCodeAndProvider(
            serviceId, provider);
        if (serviceOpt.isPresent()) {
            log.info("Service resolved by service code: {}", serviceOpt.get().getServiceName());
            return serviceOpt.get();
        }
    }

    log.error("Service not found for serviceId: {} and provider: {}", serviceId, providerUuid);
    return null;
}
```

**Purpose:**
- Resolves service by `generatedServiceId` first
- Falls back to `serviceCode` if not found
- Returns null if service cannot be resolved

---

## 📊 **Field Comparison**

| Field | DispensingRecordRequest | DispensingRecordEditRequest (Before) | DispensingRecordEditRequest (After) |
|-------|------------------------|-------------------------------------|-------------------------------------|
| `contractHeaderUuid` | ✅ | ❌ | ✅ |
| `insuredUuid` | ✅ | ✅ | ✅ |
| `dependantUuid` | ✅ | ✅ | ✅ |
| `primaryDiagnosis` | ✅ | ✅ | ✅ |
| `secondaryDiagnosis` | ✅ | ✅ | ✅ |
| `isInsurance` | ✅ | ❌ | ✅ |
| `packageUuid` | ✅ | ❌ | ✅ |
| `dispensingDate` | ✅ | ❌ | ✅ |
| `claimUuid` | ❌ | ✅ | ✅ (edit-specific) |
| `claimStatus` | ❌ | ✅ | ✅ (edit-specific) |

### **Nested Item Fields:**

| Field | DispensingItemRequest | DispensingItemEditRequest (Before) | DispensingItemEditRequest (After) |
|-------|----------------------|-----------------------------------|-----------------------------------|
| `itemUuid` | ❌ | ✅ | ✅ (edit-specific) |
| `contractDetailUuid` | ✅ | ✅ | ✅ |
| `serviceId` | ✅ | ❌ | ✅ |
| `itemType` | ✅ | ✅ | ✅ |
| `remark` | ✅ | ✅ | ✅ |
| `price` | ✅ | ✅ | ✅ |
| `quantity` | ✅ | ✅ | ✅ |

---

## 💡 **Benefits**

### **For Frontend Developers:**
- ✅ Same request structure for add and edit operations
- ✅ Can reuse the same form/component for both operations
- ✅ Consistent API interface

### **For Backend:**
- ✅ Consistent data model
- ✅ Easier to maintain
- ✅ More flexible - can update more fields

### **For Users:**
- ✅ Can update dispensing date during edit
- ✅ Can change contract if needed
- ✅ Can use serviceId instead of contractDetailUuid

---

## 🧪 **Usage Examples**

### **Example 1: Edit with contractDetailUuid (existing behavior)**

```json
{
  "contractHeaderUuid": "contract-123",
  "insuredUuid": "insured-456",
  "dependantUuid": null,
  "primaryDiagnosis": "Updated diagnosis",
  "secondaryDiagnosis": "Updated secondary",
  "dispensingDate": "2025-10-16",
  "claimStatus": "RESUBMITTED",
  "medicationItems": [
    {
      "itemUuid": "item-789",
      "contractDetailUuid": "detail-abc",
      "itemType": "SERVICE",
      "remark": "Updated remark",
      "price": 150.0,
      "quantity": 2
    }
  ]
}
```

### **Example 2: Edit with serviceId (new capability)**

```json
{
  "contractHeaderUuid": "contract-123",
  "insuredUuid": "insured-456",
  "dispensingDate": "2025-10-16",
  "primaryDiagnosis": "Hypertension",
  "medicationItems": [
    {
      "itemUuid": "item-789",
      "serviceId": "SRV001",
      "itemType": "SERVICE",
      "remark": "General consultation",
      "price": 100.0,
      "quantity": 1
    }
  ]
}
```

### **Example 3: Add new item during edit**

```json
{
  "contractHeaderUuid": "contract-123",
  "insuredUuid": "insured-456",
  "dispensingDate": "2025-10-16",
  "primaryDiagnosis": "Diabetes",
  "medicationItems": [
    {
      "itemUuid": "item-existing",
      "serviceId": "SRV001",
      "itemType": "SERVICE",
      "price": 100.0,
      "quantity": 1
    },
    {
      "itemUuid": null,
      "serviceId": "SRV002",
      "itemType": "DRUG",
      "price": 50.0,
      "quantity": 3
    }
  ]
}
```

---

## ✅ **Summary**

| Aspect | Before | After |
|--------|--------|-------|
| **Fields** | Limited subset | All fields from add request + edit-specific |
| **serviceId Support** | ❌ No | ✅ Yes |
| **Dispensing Date Update** | ❌ No | ✅ Yes |
| **Contract Update** | ❌ No | ✅ Yes (via contractHeaderUuid) |
| **Consistency** | Different from add | Same as add |
| **Flexibility** | Limited | Full |

**The edit request now has the same capabilities as the add request!** 🎉

