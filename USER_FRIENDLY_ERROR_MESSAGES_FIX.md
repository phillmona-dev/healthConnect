# User-Friendly Error Messages for Failed External Logs

## 🐛 **The Problem**

When dispensing records or claims fail to sync with the external insurance system, the error messages stored in the database were highly technical and not user-friendly:

### **Examples of Technical Errors:**

```
org.springframework.web.client.HttpServerErrorException$InternalServerError: 500 Internal Server Error: [{"message":"Error processing request: Database connection failed"}]
```

```
java.net.ConnectException: Connection refused: connect
```

```
org.springframework.web.client.ResourceAccessException: I/O error on POST request for "http://192.168.0.191:8888/api/payer/claimconnect/service-provided/synchronizeServiceProvided/684c8717-dde9-4c21-b183-ce3d5d12068d": Read timed out; nested exception is java.net.SocketTimeoutException: Read timed out
```

### **Why This Was Bad:**

- ❌ End users (pharmacists, clerks) don't understand technical jargon
- ❌ Stack traces and exception class names are confusing
- ❌ Users can't determine what action to take
- ❌ Support team gets vague complaints like "it's not working"
- ❌ Difficult to troubleshoot without technical knowledge

---

## ✅ **The Solution**

Created `ErrorMessageFormatter` utility class that converts technical errors into user-friendly messages that:

- ✅ Use plain language anyone can understand
- ✅ Explain what went wrong in simple terms
- ✅ Suggest what action to take (if applicable)
- ✅ Remove technical jargon and stack traces
- ✅ Extract meaningful details from error responses when available

---

## 🔧 **What Changed**

### **1. Created ErrorMessageFormatter Utility Class**

**File:** `src/main/java/com/medco/HealthConnectProvider/utils/ErrorMessageFormatter.java`

**Key Methods:**

```java
// Convert any technical error to user-friendly message
public static String formatErrorMessage(String technicalError, Exception exception)

// Specifically handle HTTP exceptions (400, 500, etc.)
public static String formatHttpError(Exception exception)

// Shorten long messages
public static String shortenMessage(String message, int maxLength)
```

**Error Categories Handled:**

1. **Connection Errors**
   - Connection refused → "Unable to connect to the insurance system..."
   - Timeout → "The insurance system is taking too long to respond..."
   - Unknown host → "Cannot reach the insurance system..."
   - Network unreachable → "Network connection is unavailable..."

2. **HTTP Status Errors**
   - 400 Bad Request → "Request rejected due to invalid data..."
   - 401 Unauthorized → "Authentication failed..."
   - 403 Forbidden → "Access denied..."
   - 404 Not Found → "Resource not found..."
   - 500 Internal Server Error → "Insurance system encountered an error..."
   - 503 Service Unavailable → "Insurance system is temporarily unavailable..."

3. **Business Logic Errors**
   - Duplicate → "This record already exists..."
   - Service only allowed once → "This service has already been submitted..."
   - Validation failed → "Data validation failed..."
   - Contract expired → "The contract has expired..."
   - Not eligible → "Patient is not eligible..."
   - Coverage limit → "Service limit has been exceeded..."

4. **Data Format Errors**
   - JSON/Parse errors → "Data format error occurred..."
   - Database errors → "A database error occurred..."

### **2. Updated FailedExternalDispensingServiceImpl**

**File:** `src/main/java/com/medco/HealthConnectProvider/services/impl/integration/FailedExternalDispensingServiceImpl.java`

**Changes:**

```java
// In logFailedDispensing() method
String userFriendlyError = ErrorMessageFormatter.formatErrorMessage(errorMessage, null);
failedLog.setErrorMessage(userFriendlyError);  // Store user-friendly error

// In retry catch block
String userFriendlyError = ErrorMessageFormatter.formatHttpError(e);
failedLog.setErrorMessage(userFriendlyError);
```

### **3. Updated FailedExternalClaimServiceImpl**

**File:** `src/main/java/com/medco/HealthConnectProvider/services/impl/integration/FailedExternalClaimServiceImpl.java`

**Changes:**

```java
// In logFailedClaim() method
String userFriendlyError = ErrorMessageFormatter.formatErrorMessage(errorMessage, null);
failedLog.setErrorMessage(userFriendlyError);  // Store user-friendly error

// In retry catch block
String errorMessage = ErrorMessageFormatter.formatHttpError(e);
handleRetryFailure(failedLog, errorMessage);

// In handleRetryFailure() method
String userFriendlyError = ErrorMessageFormatter.formatErrorMessage(errorMessage, null);
failedLog.setErrorMessage(userFriendlyError);
```

---

## 📊 **Before vs After Examples**

### **Example 1: Connection Timeout**

**Before:**
```
org.springframework.web.client.ResourceAccessException: I/O error on POST request for "http://192.168.0.191:8888/api/payer/claimconnect/service-provided/synchronizeServiceProvided/684c8717-dde9-4c21-b183-ce3d5d12068d": Read timed out; nested exception is java.net.SocketTimeoutException: Read timed out
```

**After:**
```
The insurance system is taking too long to respond. Please try again later.
```

---

### **Example 2: 500 Internal Server Error**

**Before:**
```
org.springframework.web.client.HttpServerErrorException$InternalServerError: 500 Internal Server Error: [{"message":"Error processing request: A service is only allowed once per claim."}]
```

**After:**
```
This service has already been submitted for this claim. Each service can only be submitted once per claim.
```

---

### **Example 3: Connection Refused**

**Before:**
```
java.net.ConnectException: Connection refused: connect
```

**After:**
```
Unable to connect to the insurance system. The service may be temporarily unavailable. Please try again later.
```

---

### **Example 4: 400 Bad Request with Details**

**Before:**
```
org.springframework.web.client.HttpClientErrorException$BadRequest: 400 Bad Request: [{"message":"Invalid contract UUID provided"}]
```

**After:**
```
Invalid contract UUID provided
```
*(Extracts the meaningful message from the response)*

---

## 🎯 **How It Works**

### **Pattern Matching Flow:**

1. **Check for null/empty** → Return generic message
2. **Check connection errors** → "Unable to connect..." / "Timeout..." / "Network unavailable..."
3. **Check HTTP status codes** → Map to user-friendly messages
4. **Check business logic keywords** → "Duplicate..." / "Not eligible..." / "Expired..."
5. **Try to extract details** → Look for JSON messages, error descriptions
6. **Fallback** → Generic "An error occurred..." message

### **Detail Extraction Patterns:**

The formatter tries to extract meaningful messages from:

1. **Pattern 1:** `"Error processing request: <message>"`
2. **Pattern 2:** JSON `{"message": "...", "error": "..."}`
3. **Pattern 3:** Last part after colon in exception messages

---

## 💡 **Benefits**

### **For End Users:**
- ✅ Clear, understandable error messages
- ✅ Know what went wrong without technical knowledge
- ✅ Can determine if they should retry or contact support
- ✅ Better user experience

### **For Support Team:**
- ✅ Users can describe issues more clearly
- ✅ Faster troubleshooting
- ✅ Reduced support tickets for "it's not working"
- ✅ Can still see technical details in logs if needed

### **For Developers:**
- ✅ Centralized error message formatting
- ✅ Easy to add new error patterns
- ✅ Consistent error messages across the application
- ✅ Technical errors still logged for debugging

---

## 🧪 **Testing Scenarios**

### **Test Case 1: Network Timeout**
**Trigger:** External API takes > 30 seconds to respond  
**Expected Error:** "The insurance system is taking too long to respond. Please try again later."

### **Test Case 2: Service Unavailable**
**Trigger:** External API returns 503  
**Expected Error:** "The insurance system is temporarily unavailable. Please try again later."

### **Test Case 3: Duplicate Service**
**Trigger:** Submit same service twice  
**Expected Error:** "This service has already been submitted for this claim. Each service can only be submitted once per claim."

### **Test Case 4: Invalid Data**
**Trigger:** Send invalid contract UUID  
**Expected Error:** "The request was rejected by the insurance system due to invalid data. Please verify all information and try again."

### **Test Case 5: Authentication Failure**
**Trigger:** Invalid API credentials  
**Expected Error:** "Authentication failed with the insurance system. Please contact system administrator."

---

## 📝 **Database Impact**

### **Tables Affected:**

1. **`failed_external_dispensing_log`**
   - `error_message` column now contains user-friendly messages
   - Technical details still available in `last_response` column

2. **`failed_external_claim_log`**
   - `error_message` column now contains user-friendly messages
   - Technical details still available in `last_response` column

### **No Schema Changes Required:**
- ✅ Uses existing columns
- ✅ No migration needed
- ✅ Backward compatible

---

## 🔍 **Technical Details Preservation**

**Important:** Technical error details are NOT lost!

- **User-friendly message** → Stored in `error_message` column (shown to users)
- **Technical details** → Stored in `last_response` column (for debugging)
- **Debug logs** → Original error logged with `log.debug()`

**Example:**
```java
log.debug("Original error: {}", technicalError);
log.debug("User-friendly error: {}", userFriendlyError);
```

---

## 🚀 **Future Enhancements**

### **Potential Improvements:**

1. **Internationalization (i18n)**
   - Support multiple languages
   - Load messages from resource bundles

2. **Error Code System**
   - Assign error codes (e.g., ERR-001, ERR-002)
   - Link to knowledge base articles

3. **Contextual Help**
   - Add "Learn More" links
   - Provide step-by-step resolution guides

4. **Error Analytics**
   - Track most common errors
   - Identify patterns for proactive fixes

---

## ✅ **Summary**

| Aspect | Before | After |
|--------|--------|-------|
| **Error Messages** | Technical stack traces | Plain language |
| **User Understanding** | Confused | Clear |
| **Action Guidance** | None | Suggested actions |
| **Support Burden** | High | Reduced |
| **User Experience** | Poor | Improved |
| **Technical Details** | In error message | In last_response + logs |

**The system now provides user-friendly error messages while preserving technical details for debugging!** 🎉

