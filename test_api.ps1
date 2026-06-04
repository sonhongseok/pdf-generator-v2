$BASE_URL = "http://localhost:8080/api/documents/certificates"
$results = @()

function Test-Api {
    param(
        [string]$TestName,
        [string]$Method,
        [string]$Url,
        [string]$Body,
        [int]$ExpectedStatus,
        [string]$ExpectedContentType
    )

    Write-Host ""
    Write-Host ">>> $TestName" -ForegroundColor Cyan

    try {
        $params = @{
            Uri         = $Url
            Method      = $Method
            ContentType = "application/json"
            ErrorAction = "Stop"
        }
        if ($Body) { $params.Body = $Body }

        $res = Invoke-WebRequest @params
        $actualStatus  = $res.StatusCode
        $actualType    = $res.Headers["Content-Type"]
        $bodySize      = $res.Content.Length

        $statusOk = ($actualStatus -eq $ExpectedStatus)
        $typeOk   = ($ExpectedContentType -eq "" -or $actualType -like "*$ExpectedContentType*")

        if ($statusOk -and $typeOk) {
            Write-Host "  PASS | STATUS=$actualStatus | Content-Type=$actualType | Size=${bodySize}bytes" -ForegroundColor Green
            return "PASS"
        } else {
            Write-Host "  FAIL | Expected Status=$ExpectedStatus Got=$actualStatus | Expected Type=$ExpectedContentType Got=$actualType" -ForegroundColor Red
            return "FAIL"
        }
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq $ExpectedStatus) {
            Write-Host "  PASS | STATUS=$statusCode (error response blocked as expected)" -ForegroundColor Green
            return "PASS"
        } else {
            Write-Host "  FAIL | Expected=$ExpectedStatus Got=$statusCode | $($_.Exception.Message)" -ForegroundColor Red
            return "FAIL"
        }
    }
}

# Test 1: MERGED PDF - serial 1
$results += Test-Api `
    -TestName "Test 1: MERGED PDF - serial 1" `
    -Method POST -Url "$BASE_URL/pdf" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["TEST-M01"],"generateMode":"MERGED"}' `
    -ExpectedStatus 200 -ExpectedContentType "application/pdf"

# Test 2: MERGED PDF - serial 3
$results += Test-Api `
    -TestName "Test 2: MERGED PDF - serial 3" `
    -Method POST -Url "$BASE_URL/pdf" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["TEST-M02","TEST-M03","TEST-M04"],"generateMode":"MERGED"}' `
    -ExpectedStatus 200 -ExpectedContentType "application/pdf"

# Test 3: INDIVIDUAL ZIP - serial 3
$results += Test-Api `
    -TestName "Test 3: INDIVIDUAL ZIP - serial 3" `
    -Method POST -Url "$BASE_URL/pdf" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["TEST-I01","TEST-I02","TEST-I03"],"generateMode":"INDIVIDUAL"}' `
    -ExpectedStatus 200 -ExpectedContentType "application/zip"

# Test 4: INDIVIDUAL ZIP - serial 1
$results += Test-Api `
    -TestName "Test 4: INDIVIDUAL ZIP - serial 1" `
    -Method POST -Url "$BASE_URL/pdf" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["TEST-I04"],"generateMode":"INDIVIDUAL"}' `
    -ExpectedStatus 200 -ExpectedContentType "application/zip"

# Test 5: generateMode null -> MERGED default
$results += Test-Api `
    -TestName "Test 5: generateMode null -> default MERGED" `
    -Method POST -Url "$BASE_URL/pdf" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["TEST-D01"]}' `
    -ExpectedStatus 200 -ExpectedContentType "application/pdf"

# Test 6: Duplicate serial in input -> 400
$results += Test-Api `
    -TestName "Test 6: Duplicate serial in input -> 400" `
    -Method POST -Url "$BASE_URL/pdf" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["TEST-DUP","TEST-DUP"],"generateMode":"MERGED"}' `
    -ExpectedStatus 400 -ExpectedContentType ""

# Test 7: expiryDate before certificateDate -> 400
$results += Test-Api `
    -TestName "Test 7: expiryDate before certificateDate -> 400" `
    -Method POST -Url "$BASE_URL/pdf" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2025-01-01","serialNos":["TEST-DATE01"],"generateMode":"MERGED"}' `
    -ExpectedStatus 400 -ExpectedContentType ""

# Test 8: Empty serialNos -> 400
$results += Test-Api `
    -TestName "Test 8: Empty serialNos -> 400" `
    -Method POST -Url "$BASE_URL/pdf" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":[],"generateMode":"MERGED"}' `
    -ExpectedStatus 400 -ExpectedContentType ""

# Test 9: Duplicate issue (same as Test 1) -> 400
$results += Test-Api `
    -TestName "Test 9: Duplicate issue (same as Test 1) -> 400" `
    -Method POST -Url "$BASE_URL/pdf" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["TEST-M01"],"generateMode":"MERGED"}' `
    -ExpectedStatus 400 -ExpectedContentType ""

# Test 10: GET history list -> 200 JSON
$results += Test-Api `
    -TestName "Test 10: GET history list -> 200 JSON" `
    -Method GET -Url "$BASE_URL" -Body "" `
    -ExpectedStatus 200 -ExpectedContentType "application/json"

# Summary
$pass = ($results | Where-Object { $_ -eq "PASS" }).Count
$fail = ($results | Where-Object { $_ -eq "FAIL" }).Count

Write-Host ""
Write-Host "--------------------------------------------"
Write-Host " RESULT: Total=$($results.Count)  PASS=$pass  FAIL=$fail"
if ($fail -eq 0) {
    Write-Host " All tests passed!" -ForegroundColor Green
} else {
    Write-Host " Some tests failed. Please review above." -ForegroundColor Yellow
}
Write-Host "--------------------------------------------"
