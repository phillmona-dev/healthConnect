@echo off
echo ========================================
echo   Testing Bulk CBHI Insured Endpoint
echo ========================================
echo.

echo This script will test the new bulk insured creation endpoint.
echo Make sure your application is running before proceeding.
echo.

set /p API_KEY="Enter your API key: "
set /p PAYER_UUID="Enter a valid payer UUID: "
set /p BASE_URL="Enter base URL (default: http://localhost:8080): "

if "%BASE_URL%"=="" set BASE_URL=http://localhost:8080

echo.
echo Testing with:
echo - API Key: %API_KEY%
echo - Payer UUID: %PAYER_UUID%
echo - Base URL: %BASE_URL%
echo.

echo Creating test request file...
(
echo {
echo   "payerUuid": "%PAYER_UUID%",
echo   "insuredMembers": [
echo     {
echo       "firstName": "Test",
echo       "fatherName": "User",
echo       "grandFatherName": "Demo",
echo       "phone": "+251911000001",
echo       "email": "test.user@example.com",
echo       "nationalId": "TEST123456789",
echo       "idNumber": "TEST001",
echo       "insuranceId": "TESTINS001",
echo       "birthDate": "1990-01-01",
echo       "gender": "MALE",
echo       "address": "Test Address",
echo       "city": "Test City",
echo       "state": "Test State",
echo       "country": "Ethiopia"
echo     },
echo     {
echo       "firstName": "Demo",
echo       "fatherName": "Person",
echo       "grandFatherName": "Sample",
echo       "phone": "+251911000002",
echo       "email": "demo.person@example.com",
echo       "nationalId": "DEMO987654321",
echo       "idNumber": "DEMO002",
echo       "insuranceId": "DEMOINS002",
echo       "birthDate": "1985-06-15",
echo       "gender": "FEMALE",
echo       "address": "Demo Address",
echo       "city": "Demo City",
echo       "state": "Demo State",
echo       "country": "Ethiopia"
echo     }
echo   ]
echo }
) > test-bulk-request.json

echo.
echo Sending request to bulk insured endpoint...
echo.

curl -X POST "%BASE_URL%/api/v1/pharmacy-integration/insured/bulk" ^
  -H "Content-Type: application/json" ^
  -H "X-API-Key: %API_KEY%" ^
  -d @test-bulk-request.json ^
  -w "\n\nHTTP Status: %%{http_code}\nResponse Time: %%{time_total}s\n"

echo.
echo Cleaning up test file...
del test-bulk-request.json

echo.
echo ========================================
echo   Test Complete
echo ========================================
echo.
echo Check the response above to verify:
echo 1. HTTP Status should be 200
echo 2. Response should show created members
echo 3. No failed members (unless there were validation errors)
echo.
pause
