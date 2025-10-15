# Service Update Fix - Duplicate Entry Error

## 🐛 Problem

When trying to update a service, the system was throwing an error:

```
BadRequestException: Duplicate Service entry is not followed
```

**Error Details:**
```
PUT /api/v1/healthConnect/healthConnectProvider/service/3fb7e531-0d14-47b8-b711-c4e4b0b1c42c

Request Body:
{
  "serviceName": "General Consultation",
  "serviceCode": "SRV001",
  "serviceCategory": "Consultation",
  "serviceSubCategory": "Outpatient",
  "price": "300",
  "serviceDescription": "",
  "status": "ACTIVE"
}

Response: 400 BAD_REQUEST
Error: "Duplicate Service entry is not followed"
```

---

## 🔍 Root Cause

### Issue 1: **Incorrect Duplicate Check Logic**

**OLD CODE (WRONG):**
```java
@Override
public ResponseEntity<?> updateService(String serviceUuid, ServicelistRequest serviceRequest) {
    Servicelist service = servicelistRepository.findByServiceUuid(serviceUuid)
            .orElseThrow(() -> new BadRequestException("Service not found"));

    // ❌ PROBLEM: This checks if ANY service has this name
    if (servicelistRepository.existsByServiceName(serviceRequest.getServiceName())) {
        throw new BadRequestException("Duplicate Service entry is not followed");
    }

    BeanUtils.copyProperties(serviceRequest, service);
    servicelistRepository.save(service);
    return ResponseEntity.ok(new MessageResponse("Service Updated Successfully!"));
}
```

**Why This Failed:**
1. When updating "General Consultation" → "General Consultation" (same name)
2. System checks: "Does a service named 'General Consultation' exist?"
3. Answer: YES (the service being updated itself!)
4. System throws error: "Duplicate Service entry is not followed"
5. ❌ **Update blocked even though it's the SAME service!**

### Issue 2: **Missing Price Field**

The `ServicelistRequest` DTO was missing the `price` field, but the frontend was sending it.

### Issue 3: **Incomplete Field Updates**

The old code used `BeanUtils.copyProperties()` which doesn't properly handle:
- Price mapping (`price` → `defaultPrice`)
- Status enum conversion
- Null value handling

---

## ✅ Solution Implemented

### Fix 1: **Proper Duplicate Check Logic**

**NEW CODE (CORRECT):**
```java
@Override
public ResponseEntity<?> updateService(String serviceUuid, ServicelistRequest serviceRequest) {
    Servicelist service = servicelistRepository.findByServiceUuid(serviceUuid)
            .orElseThrow(() -> new BadRequestException("Service not found"));

    // ✅ Check for duplicate service name only if the name is being changed
    if (!service.getServiceName().equals(serviceRequest.getServiceName())) {
        // Check if another service with this name exists for the same provider
        Optional<Servicelist> existingService = servicelistRepository
                .findByServiceNameAndProvider(serviceRequest.getServiceName(), service.getProvider());
        
        if (existingService.isPresent() && !existingService.get().getServiceUuid().equals(serviceUuid)) {
            throw new BadRequestException("A service with this name already exists for this provider");
        }
    }

    // ✅ Check for duplicate service code only if the code is being changed
    if (serviceRequest.getServiceCode() != null && 
        !serviceRequest.getServiceCode().equals(service.getServiceCode())) {
        Optional<Servicelist> existingByCode = servicelistRepository
                .findByServiceCodeAndProvider(serviceRequest.getServiceCode(), service.getProvider());
        
        if (existingByCode.isPresent() && !existingByCode.get().getServiceUuid().equals(serviceUuid)) {
            throw new BadRequestException("A service with this code already exists for this provider");
        }
    }

    // Update service fields explicitly
    service.setServiceName(serviceRequest.getServiceName());
    service.setServiceCode(serviceRequest.getServiceCode());
    service.setServiceCategory(serviceRequest.getServiceCategory());
    service.setServiceSubCategory(serviceRequest.getSubCategory());
    service.setServiceDescription(serviceRequest.getServiceDescription());
    
    // Update price - map from 'price' field in request to 'defaultPrice' in entity
    if (serviceRequest.getPrice() != null) {
        try {
            service.setDefaultPrice(Double.parseDouble(serviceRequest.getPrice()));
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid price format");
        }
    }

    // Update status
    if (serviceRequest.getStatus() != null && !serviceRequest.getStatus().isEmpty()) {
        try {
            service.setStatus(Status.valueOf(serviceRequest.getStatus().toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status value. Allowed values: ACTIVE, INACTIVE");
        }
    }

    // Save updated service
    servicelistRepository.save(service);
    
    return ResponseEntity.ok(new MessageResponse("Service Updated Successfully!"));
}
```

**Key Improvements:**

1. **✅ Only check for duplicates when name/code changes**
   - If updating "General Consultation" → "General Consultation" → No check needed
   - If updating "General Consultation" → "Emergency Consultation" → Check for duplicates

2. **✅ Exclude current service from duplicate check**
   - `!existingService.get().getServiceUuid().equals(serviceUuid)`
   - Ensures we don't compare the service with itself

3. **✅ Provider-scoped duplicate check**
   - Uses `findByServiceNameAndProvider()` instead of global `existsByServiceName()`
   - Different providers can have services with the same name

4. **✅ Explicit field updates**
   - No more `BeanUtils.copyProperties()` which can cause issues
   - Each field is set explicitly with proper validation

5. **✅ Price field mapping**
   - Maps `price` from request → `defaultPrice` in entity
   - Validates numeric format

6. **✅ Status enum validation**
   - Converts string to Status enum
   - Provides clear error message for invalid values

---

### Fix 2: **Added Price Field to Request DTO**

**File:** `ServicelistRequest.java`

```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ServicelistRequest {

    @NotBlank(message = "Service code is required")
    private String serviceCode;

    @NotBlank(message = "Service name is required")
    private String serviceName;

    private String subCategory;

    private String serviceCategory;

    private String status;

    private String serviceDescription;

    private String price;  // ✅ ADDED - Maps to defaultPrice in Servicelist entity
}
```

---

### Fix 3: **Added Repository Method**

**File:** `ServicelistRepository.java`

```java
Optional<Servicelist> findByServiceCodeAndProvider(String serviceCode, Provider provider);
```

This allows checking for duplicate service codes within the same provider.

---

### Fix 4: **Updated Create Service Method**

Also updated the `createService` method to properly handle the price field:

```java
// Set price - map from 'price' field in request to 'defaultPrice' in entity
if (serviceRequest.getPrice() != null && !serviceRequest.getPrice().isEmpty()) {
    try {
        service.setDefaultPrice(Double.parseDouble(serviceRequest.getPrice()));
    } catch (NumberFormatException e) {
        throw new BadRequestException("Invalid price format");
    }
}
```

---

## 📊 Update Logic Flow

### **Scenario 1: Update Service Name (No Change)**

```
Request: Update "General Consultation" → "General Consultation"

Flow:
1. Find service by UUID ✅
2. Check if name changed: "General Consultation" == "General Consultation" → NO
3. Skip duplicate check ✅
4. Update other fields
5. Save service ✅

Result: ✅ SUCCESS - Service updated
```

---

### **Scenario 2: Update Service Name (Changed)**

```
Request: Update "General Consultation" → "Emergency Consultation"

Flow:
1. Find service by UUID ✅
2. Check if name changed: "General Consultation" != "Emergency Consultation" → YES
3. Check for duplicates:
   - Find service with name "Emergency Consultation" for same provider
   - If found AND different UUID → ❌ Error
   - If not found OR same UUID → ✅ Continue
4. Update fields
5. Save service ✅

Result: ✅ SUCCESS (if no duplicate) or ❌ ERROR (if duplicate exists)
```

---

### **Scenario 3: Update Service Code (Changed)**

```
Request: Update service code "SRV001" → "SRV002"

Flow:
1. Find service by UUID ✅
2. Check if code changed: "SRV001" != "SRV002" → YES
3. Check for duplicates:
   - Find service with code "SRV002" for same provider
   - If found AND different UUID → ❌ Error
   - If not found OR same UUID → ✅ Continue
4. Update fields
5. Save service ✅

Result: ✅ SUCCESS (if no duplicate) or ❌ ERROR (if duplicate exists)
```

---

### **Scenario 4: Update Price**

```
Request: Update price "300" → "500"

Flow:
1. Find service by UUID ✅
2. Parse price string "500" → Double 500.0
3. Set defaultPrice = 500.0
4. Save service ✅

Result: ✅ SUCCESS
```

---

## 🧪 Test Cases

### Test 1: **Update Service Without Changing Name**
```bash
PUT /api/v1/healthConnect/healthConnectProvider/service/{serviceUuid}

{
  "serviceName": "General Consultation",  // Same name
  "serviceCode": "SRV001",
  "serviceCategory": "Consultation",
  "serviceSubCategory": "Outpatient",
  "price": "300",
  "status": "ACTIVE"
}

Expected: ✅ 200 OK - "Service Updated Successfully!"
```

### Test 2: **Update Service Name to New Unique Name**
```bash
PUT /api/v1/healthConnect/healthConnectProvider/service/{serviceUuid}

{
  "serviceName": "Emergency Consultation",  // New unique name
  "serviceCode": "SRV001",
  "serviceCategory": "Consultation",
  "serviceSubCategory": "Emergency",
  "price": "500",
  "status": "ACTIVE"
}

Expected: ✅ 200 OK - "Service Updated Successfully!"
```

### Test 3: **Update Service Name to Existing Name (Different Service)**
```bash
PUT /api/v1/healthConnect/healthConnectProvider/service/{serviceUuid}

{
  "serviceName": "X-Ray Chest",  // Name already exists for another service
  "serviceCode": "SRV001",
  "serviceCategory": "Consultation",
  "price": "300",
  "status": "ACTIVE"
}

Expected: ❌ 400 BAD_REQUEST - "A service with this name already exists for this provider"
```

### Test 4: **Update Service Code to Existing Code**
```bash
PUT /api/v1/healthConnect/healthConnectProvider/service/{serviceUuid}

{
  "serviceName": "General Consultation",
  "serviceCode": "XRAY001",  // Code already exists for another service
  "serviceCategory": "Consultation",
  "price": "300",
  "status": "ACTIVE"
}

Expected: ❌ 400 BAD_REQUEST - "A service with this code already exists for this provider"
```

### Test 5: **Update Price**
```bash
PUT /api/v1/healthConnect/healthConnectProvider/service/{serviceUuid}

{
  "serviceName": "General Consultation",
  "serviceCode": "SRV001",
  "serviceCategory": "Consultation",
  "price": "999.99",  // New price
  "status": "ACTIVE"
}

Expected: ✅ 200 OK - Service updated with defaultPrice = 999.99
```

### Test 6: **Update Status**
```bash
PUT /api/v1/healthConnect/healthConnectProvider/service/{serviceUuid}

{
  "serviceName": "General Consultation",
  "serviceCode": "SRV001",
  "serviceCategory": "Consultation",
  "price": "300",
  "status": "INACTIVE"  // Change status
}

Expected: ✅ 200 OK - Service updated with status = INACTIVE
```

### Test 7: **Invalid Price Format**
```bash
PUT /api/v1/healthConnect/healthConnectProvider/service/{serviceUuid}

{
  "serviceName": "General Consultation",
  "serviceCode": "SRV001",
  "price": "invalid",  // Invalid price
  "status": "ACTIVE"
}

Expected: ❌ 400 BAD_REQUEST - "Invalid price format"
```

### Test 8: **Invalid Status**
```bash
PUT /api/v1/healthConnect/healthConnectProvider/service/{serviceUuid}

{
  "serviceName": "General Consultation",
  "serviceCode": "SRV001",
  "price": "300",
  "status": "INVALID_STATUS"  // Invalid status
}

Expected: ❌ 400 BAD_REQUEST - "Invalid status value. Allowed values: ACTIVE, INACTIVE"
```

---

## 📁 Files Modified

1. **ServicelistServiceImpl.java**
   - Fixed `updateService()` method with proper duplicate checking
   - Fixed `createService()` method to handle price field

2. **ServicelistRequest.java**
   - Added `price` field

3. **ServicelistRepository.java**
   - Added `findByServiceCodeAndProvider()` method

---

## ✅ Compilation Status

```
[INFO] BUILD SUCCESS
```

No errors! ✅

---

## 🚀 Next Steps

1. ✅ **Restart your application**
2. ✅ **Test the update endpoint:**
   ```bash
   PUT http://localhost:3012/api/v1/healthConnect/healthConnectProvider/service/3fb7e531-0d14-47b8-b711-c4e4b0b1c42c
   
   {
     "serviceName": "General Consultation",
     "serviceCode": "SRV001",
     "serviceCategory": "Consultation",
     "serviceSubCategory": "Outpatient",
     "price": "300",
     "serviceDescription": "",
     "status": "ACTIVE"
   }
   ```
3. ✅ **Verify:** Service updates successfully without duplicate error
4. ✅ **Test:** Try changing the name to verify duplicate detection still works

---

## 💡 Key Takeaways

| Before | After |
|--------|-------|
| ❌ Blocked updates even when name unchanged | ✅ Allows updates when name unchanged |
| ❌ Global duplicate check (all providers) | ✅ Provider-scoped duplicate check |
| ❌ Compared service with itself | ✅ Excludes current service from check |
| ❌ Missing price field | ✅ Price field added and validated |
| ❌ Used BeanUtils (unreliable) | ✅ Explicit field updates |
| ❌ No validation | ✅ Validates price format and status enum |

**Now you can update services without getting false duplicate errors!** 🎉

