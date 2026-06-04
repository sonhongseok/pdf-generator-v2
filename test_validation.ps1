$BASE_URL = "http://localhost:8080/api/documents/certificates"
$results = @()

function Test-Validation {
    param([string]$Name, [string]$Body, [int]$ExpectedStatus)
    Write-Host ""
    Write-Host ">>> $Name" -ForegroundColor Cyan
    try {
        $res = Invoke-WebRequest -Uri "$BASE_URL/pdf" -Method POST -ContentType "application/json" -Body $Body -ErrorAction Stop
        $code = $res.StatusCode
        if ($code -eq $ExpectedStatus) {
            Write-Host "  PASS | STATUS=$code" -ForegroundColor Green
            return "PASS"
        } else {
            Write-Host "  FAIL | Expected=$ExpectedStatus Got=$code" -ForegroundColor Red
            return "FAIL"
        }
    } catch {
        $code = $_.Exception.Response.StatusCode.value__
        if ($code -eq $ExpectedStatus) {
            Write-Host "  PASS | STATUS=$code (blocked as expected)" -ForegroundColor Green
            return "PASS"
        } else {
            Write-Host "  FAIL | Expected=$ExpectedStatus Got=$code" -ForegroundColor Red
            return "FAIL"
        }
    }
}

# --- 유효성 검사 테스트 (빠름, PDF 생성 없음) ---

$r1 = Test-Validation `
    -Name "Test 6: Serial No 중복 입력 -> 400 차단" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["DUP-A","DUP-A"],"generateMode":"MERGED"}' `
    -ExpectedStatus 400
$results += $r1

$r2 = Test-Validation `
    -Name "Test 7: 만료일이 발행일보다 이전 -> 400 차단" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2025-01-01","serialNos":["DATE-01"],"generateMode":"MERGED"}' `
    -ExpectedStatus 400
$results += $r2

$r3 = Test-Validation `
    -Name "Test 8: serialNos 빈 배열 -> 400 차단" `
    -Body '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":[],"generateMode":"MERGED"}' `
    -ExpectedStatus 400
$results += $r3

$r4 = Test-Validation `
    -Name "Test 9: certificateDate 미입력 -> 400 차단" `
    -Body '{"calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["NO-DATE"],"generateMode":"MERGED"}' `
    -ExpectedStatus 400
$results += $r4

# --- 이력 조회 테스트 ---
Write-Host ""
Write-Host ">>> Test 10: GET 발급 이력 목록 -> 200 JSON" -ForegroundColor Cyan
try {
    $res = Invoke-WebRequest -Uri "$BASE_URL" -Method GET -ErrorAction Stop
    $json = $res.Content | ConvertFrom-Json
    $cnt = $json.Count
    Write-Host "  PASS | STATUS=$($res.StatusCode) | 총 $cnt 건 조회" -ForegroundColor Green
    $results += "PASS"
} catch {
    Write-Host "  FAIL | $($_.Exception.Message)" -ForegroundColor Red
    $results += "FAIL"
}

# --- 결과 요약 ---
$pass = ($results | Where-Object { $_ -eq "PASS" }).Count
$fail = ($results | Where-Object { $_ -eq "FAIL" }).Count
Write-Host ""
Write-Host "--------------------------------------------"
Write-Host " RESULT: Total=$($results.Count)  PASS=$pass  FAIL=$fail"
if ($fail -eq 0) {
    Write-Host " All validation tests passed!" -ForegroundColor Green
} else {
    Write-Host " Some tests failed!" -ForegroundColor Yellow
}
Write-Host "--------------------------------------------"
