# Eligible Services - User-Friendly Response Fix

## 🎯 **Objective**

Return a simple user-friendly **string message** to the frontend instead of a JSON payload with null fields when there are no eligible services available.

---

## 🐛 **The Problem**

When calling the eligible services endpoint, if the external API returns null or empty data, the frontend receives a response with all null fields:

### **Problematic Response:**

```json
{
    "packageUuid": null,
    "packageName": null,
    "packageCategory": null,
    "packageDescription": null,
    "benefitRanges": null,
    "minLimit": null,
    "maxLimit": null,
    "status": null,
    "gender": null,
    "totalPages": null,
    "packageEligibleServices": []
}
```

**Issues:**
- ❌ Not user-friendly
- ❌ Frontend has to handle all null fields
- ❌ No clear message about why there's no data
- ❌ Difficult to distinguish between "no results" and "error"

---

## ✅ **The Solution**

Updated both the controller and service to return a **simple string message** instead of a JSON payload when:
1. External API returns null response body
2. External API returns empty eligible services list
3. User searches but no results match

**Now returns:** Plain text message (e.g., `"No data available from external system"`)
**Instead of:** JSON payload with null fields

---

## 🔧 **What Changed**

### **1. Updated Controller to Return String Message**

**File:** `ServiceCategoryMappingController.java`

**Before:**

```java
@GetMapping("/packageInsurance/eligible-services")
public ResponseEntity<ExternalPackageEligibleServicesResponse> getEligibleServices(...) {
    ExternalPackageEligibleServicesResponse response = mappingService.getEligibleServices(...);
    return ResponseEntity.ok(response);  // ❌ Always returns JSON
}
```

**After:**

```java
@GetMapping("/packageInsurance/eligible-services")
public ResponseEntity<?> getEligibleServices(...) {
    ExternalPackageEligibleServicesResponse response = mappingService.getEligibleServices(...);

    // If response indicates no data, return simple message
    if ("NO_DATA".equals(response.getStatus())) {
        String message = response.getPackageName() != null
            ? response.getPackageName()
            : "No data available";
        return ResponseEntity.ok(message);  // ✅ Returns plain string
    }

    return ResponseEntity.ok(response);  // ✅ Returns JSON only when data exists
}
```

**Key Changes:**
- ✅ Changed return type from `ResponseEntity<ExternalPackageEligibleServicesResponse>` to `ResponseEntity<?>`
- ✅ Added check for `status === "NO_DATA"`
- ✅ Returns plain string message when no data
- ✅ Returns JSON payload only when data exists

---

### **2. Updated Service Implementation**

**File:** `ServiceCategoryMappingServiceImpl.java`

#### **Before:**

```java
if (response.getStatusCode().is2xxSuccessful()) {
    ExternalPackageEligibleServicesResponse responseBody = response.getBody();

    if (responseBody == null) {
        log.warn("[External API] Response body is null for contract: {}", contractUuid);
        ExternalPackageEligibleServicesResponse emptyResponse = new ExternalPackageEligibleServicesResponse();
        emptyResponse.setPackageEligibleServices(Collections.emptyList());
        return emptyResponse;  // ❌ Returns all null fields
    }

    if (responseBody.getPackageEligibleServices() != null && !responseBody.getPackageEligibleServices().isEmpty()) {
        persistEligibleServices(contractUuid, responseBody.getPackageEligibleServices());
    } else {
        log.debug("[External API] No eligible services returned for contract: {}", contractUuid);
    }

    return responseBody;  // ❌ May have null fields
}
```

#### **After:**

```java
if (response.getStatusCode().is2xxSuccessful()) {
    ExternalPackageEligibleServicesResponse responseBody = response.getBody();

    if (responseBody == null) {
        log.warn("[External API] Response body is null for contract: {}", contractUuid);
        return createEmptyResponseWithMessage(packageUuid, "No data available from external system");
    }

    // Check if response has null or empty eligible services
    if (responseBody.getPackageEligibleServices() == null || responseBody.getPackageEligibleServices().isEmpty()) {
        log.debug("[External API] No eligible services returned for contract: {}", contractUuid);
        
        // If all fields are null, return user-friendly message
        if (isResponseEmpty(responseBody)) {
            return createEmptyResponseWithMessage(packageUuid, 
                search != null && !search.isEmpty() 
                    ? "No services found matching your search criteria" 
                    : "No eligible services available for this package");
        }
        
        // Response has package info but no services
        responseBody.setPackageEligibleServices(Collections.emptyList());
        return responseBody;
    }

    // Response has eligible services - persist them
    persistEligibleServices(contractUuid, responseBody.getPackageEligibleServices());
    return responseBody;
}
```

---

### **3. Added Helper Methods**

#### **createEmptyResponseWithMessage**

```java
/**
 * Create an empty response with user-friendly message
 */
private ExternalPackageEligibleServicesResponse createEmptyResponseWithMessage(String packageUuid, String message) {
    ExternalPackageEligibleServicesResponse response = new ExternalPackageEligibleServicesResponse();
    response.setPackageUuid(packageUuid);
    response.setPackageName(message);
    response.setPackageDescription(message);
    response.setStatus("NO_DATA");
    response.setPackageEligibleServices(Collections.emptyList());
    response.setTotalPages(0);
    return response;
}
```

**Purpose:**
- Creates a response with user-friendly message
- Sets `packageName` and `packageDescription` to the message
- Sets `status` to "NO_DATA" to indicate no data available
- Sets `totalPages` to 0
- Sets `packageEligibleServices` to empty list

---

#### **isResponseEmpty**

```java
/**
 * Check if the response has all null fields (except packageEligibleServices)
 */
private boolean isResponseEmpty(ExternalPackageEligibleServicesResponse response) {
    return response.getPackageUuid() == null &&
           response.getPackageName() == null &&
           response.getPackageCategory() == null &&
           response.getPackageDescription() == null &&
           response.getStatus() == null;
}
```

**Purpose:**
- Checks if the response has all null fields
- Used to determine if we should return a user-friendly message
- Ignores `packageEligibleServices` field in the check

---

## 📊 **Response Scenarios**

### **Scenario 1: External API Returns Null Response Body**

**HTTP Response:**
```
HTTP/1.1 200 OK
Content-Type: text/plain

No data available from external system
```

**Response Body:** Plain text string (not JSON)

**User sees:** "No data available from external system"

---

### **Scenario 2: No Services Found (No Search)**

**Request:**
```
GET /api/v1/healthConnect/service-category-mappings/packageInsurance/eligible-services
    ?contractUuid=106b226b-ed4d-455b-ae8e-7c6d115cdc87
    &packageUuid=0a6058a5-658c-4803-aa36-ee6ace89d58d
    &insuredUuid=8bbc8453-c89b-41c9-8153-82e776ef9319
```

**HTTP Response:**
```
HTTP/1.1 200 OK
Content-Type: text/plain

No eligible services available for this package
```

**Response Body:** Plain text string (not JSON)

**User sees:** "No eligible services available for this package"

---

### **Scenario 3: No Services Found (With Search)**

**Request:**
```
GET /api/v1/healthConnect/service-category-mappings/packageInsurance/eligible-services
    ?contractUuid=106b226b-ed4d-455b-ae8e-7c6d115cdc87
    &packageUuid=0a6058a5-658c-4803-aa36-ee6ace89d58d
    &insuredUuid=8bbc8453-c89b-41c9-8153-82e776ef9319
    &search=aa
```

**HTTP Response:**
```
HTTP/1.1 200 OK
Content-Type: text/plain

No services found matching your search criteria
```

**Response Body:** Plain text string (not JSON)

**User sees:** "No services found matching your search criteria"

---

### **Scenario 4: External API Returns Package Info But No Services**

**Response:**
```json
{
    "packageUuid": "0a6058a5-658c-4803-aa36-ee6ace89d58d",
    "packageName": "Basic Health Package",
    "packageCategory": "HEALTH",
    "packageDescription": "Basic health coverage",
    "benefitRanges": [],
    "minLimit": 0.0,
    "maxLimit": 10000.0,
    "status": "ACTIVE",
    "gender": "ALL",
    "totalPages": 1,
    "packageEligibleServices": []
}
```

**User sees:** Package information with empty services list

---

### **Scenario 5: External API Returns Services (Normal Case)**

**Response:**
```json
{
    "packageUuid": "0a6058a5-658c-4803-aa36-ee6ace89d58d",
    "packageName": "Basic Health Package",
    "packageCategory": "HEALTH",
    "packageDescription": "Basic health coverage",
    "benefitRanges": [],
    "minLimit": 0.0,
    "maxLimit": 10000.0,
    "status": "ACTIVE",
    "gender": "ALL",
    "totalPages": 1,
    "packageEligibleServices": [
        {
            "eligibleServiceUuid": "service-1",
            "serviceId": "SRV001",
            "itemCode": "CONS",
            "item": "General Consultation",
            "subCategory": "Consultation",
            "category": "Medical",
            "price": 100.0
        }
    ]
}
```

**User sees:** Package information with list of services

---

## 💡 **Benefits**

### **For Frontend Developers:**
- ✅ Clear, user-friendly messages
- ✅ Can display message directly from `packageName` or `packageDescription`
- ✅ Can check `status === "NO_DATA"` to show appropriate UI
- ✅ No need to handle multiple null fields

### **For Users:**
- ✅ Clear feedback about why there are no results
- ✅ Different messages for search vs. no search
- ✅ Better user experience

### **For Backend:**
- ✅ Consistent response structure
- ✅ Better logging
- ✅ Easier to debug

---

## 🧪 **Frontend Usage Example**

### **React/TypeScript Example:**

```typescript
const response = await fetch(url);
const contentType = response.headers.get("content-type");

if (contentType && contentType.includes("text/plain")) {
    // Response is a plain text message
    const message = await response.text();
    showMessage(message);  // Display: "No services found matching your search criteria"
} else {
    // Response is JSON with data
    const data = await response.json();
    if (data.packageEligibleServices && data.packageEligibleServices.length > 0) {
        displayServices(data.packageEligibleServices);
    } else {
        showMessage("No services available");
    }
}
```

### **Vue.js Example:**

```vue
<script setup>
import { ref } from 'vue';

const message = ref('');
const services = ref([]);

async function fetchServices() {
  const response = await fetch(url);
  const contentType = response.headers.get("content-type");

  if (contentType && contentType.includes("text/plain")) {
    message.value = await response.text();
  } else {
    const data = await response.json();
    services.value = data.packageEligibleServices || [];
  }
}
</script>

<template>
  <div v-if="message" class="alert alert-info">
    {{ message }}
  </div>
  <div v-else-if="services.length === 0" class="alert alert-warning">
    No services available
  </div>
  <div v-else>
    <service-list :services="services" />
  </div>
</template>
```

### **Simple Fetch Example:**

```javascript
fetch(url)
  .then(response => {
    const contentType = response.headers.get("content-type");

    if (contentType && contentType.includes("text/plain")) {
      return response.text();  // Returns: "No data available from external system"
    } else {
      return response.json();  // Returns: { packageEligibleServices: [...] }
    }
  })
  .then(data => {
    if (typeof data === 'string') {
      alert(data);  // Show message
    } else {
      displayServices(data.packageEligibleServices);
    }
  });
```

---

## ✅ **Summary**

| Aspect | Before | After |
|--------|--------|-------|
| **Response Type** | Always JSON | Plain text when no data, JSON when data exists |
| **Null Response** | JSON with all null fields | Plain text: "No data available from external system" |
| **Empty Services** | JSON with all null fields | Plain text: "No eligible services available for this package" |
| **Search No Results** | JSON with all null fields | Plain text: "No services found matching your search criteria" |
| **Content-Type** | Always `application/json` | `text/plain` when no data, `application/json` when data exists |
| **Frontend Handling** | Parse JSON, check all null fields | Check content-type, display text directly |
| **User Experience** | ❌ Confusing JSON payload | ✅ Clear plain text message |

**The system now returns a simple string message instead of a JSON payload with null fields!** 🎉

