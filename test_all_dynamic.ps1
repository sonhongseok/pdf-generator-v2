$BASE_URL = "http://localhost:8080/api/documents/certificates"
$pass = 0; $fail = 0
$tmpDir = "C:\work\pdf-generator-v2\test_tmp"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

$TS = (Get-Date).ToString("HHmmss")

function Test-Post {
    param(
        [string]$Name,
        [string]$JsonBody,
        [int]$ExpectedStatus,
        [string]$ExpectedType = ""
    )
    $jsonFile = "$tmpDir\body.json"
    $resultFile = "$tmpDir\result.bin"
    $JsonBody | Set-Content -Path $jsonFile -Encoding UTF8 -NoNewline

    $result = curl.exe -s -o $resultFile -w "%{http_code}|%{content_type}|%{size_download}" `
        --max-time 300 -X POST "$BASE_URL/pdf" `
        -H "Content-Type: application/json" `
        -d "@$jsonFile"

    $m = $result.Split("|")
    $code = $m[0]; $type = $m[1]; $size = $m[2]

    $statusOk = ($code -eq $ExpectedStatus.ToString())
    $typeOk   = ($ExpectedType -eq "" -or $type -like "*$ExpectedType*")

    if ($statusOk -and $typeOk) {
        Write-Host "  PASS | $Name | STATUS=$code$(if ($ExpectedType) {" | Type=$type | Size=${size}bytes"})" -ForegroundColor Green
        $script:pass++
    } else {
        $body = Get-Content $resultFile -Raw -ErrorAction SilentlyContinue
        Write-Host "  FAIL | $Name | Expected=$ExpectedStatus Got=$code | $body" -ForegroundColor Red
        $script:fail++
    }
}

# ─────────────────────────────────────────────
# [1] 유효성 검사 테스트
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "=== [1] 유효성 검사 테스트 ===" -ForegroundColor Yellow

Test-Post "Serial No 중복 입력 -> 400" `
    '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["DUP-A","DUP-A"],"generateMode":"MERGED"}' `
    400

Test-Post "만료일 < 발행일 -> 400" `
    '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2025-01-01","serialNos":["DATE-01"],"generateMode":"MERGED"}' `
    400

Test-Post "serialNos 빈 배열 -> 400" `
    '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":[],"generateMode":"MERGED"}' `
    400

Test-Post "certificateDate 미입력 -> 400" `
    '{"calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["NO-DATE"]}' `
    400

Test-Post "Start No 오버플로우 -> 400 (9999 + 2개)" `
    '{"certificateDate":"2026-06-04","calibrationDate":"2026-06-04","expiryDate":"2027-06-04","serialNos":["OVF-A","OVF-B"],"generateMode":"MERGED","startSequenceNo":"9999"}' `
    400

# ─────────────────────────────────────────────
# [2] 이력 조회 테스트
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "=== [2] 이력 조회 테스트 ===" -ForegroundColor Yellow

$result = curl.exe -s -o NUL -w "%{http_code}|%{content_type}|%{size_download}" --max-time 10 "$BASE_URL"
$m = $result.Split("|"); $code=$m[0]; $type=$m[1]; $size=$m[2]
if ($code -eq "200" -and $type -like "*json*") {
    Write-Host "  PASS | GET 발급 이력 목록 -> 200 JSON | SIZE=${size}bytes" -ForegroundColor Green
    $pass++
} else {
    Write-Host "  FAIL | STATUS=$code | Type=$type" -ForegroundColor Red
    $fail++
}

# ─────────────────────────────────────────────
# [3] PDF 생성 테스트
# ─────────────────────────────────────────────
Write-Host ""
Write-Host "=== [3] PDF 생성 테스트 (고유 시리얼 적용) ===" -ForegroundColor Yellow

Write-Host "  >> MERGED PDF - 시리얼 1개 (TEST-$TS-1)..." -ForegroundColor Cyan
Test-Post "MERGED PDF 1개 -> 200 PDF" `
    "{`"certificateDate`":`"2026-06-04`",`"calibrationDate`":`"2026-06-04`",`"expiryDate`":`"2027-06-04`",`"serialNos`":[`"TEST-$TS-1`"],`"generateMode`":`"MERGED`"}" `
    200 "pdf"

Write-Host "  >> 중복 발급 차단 (TEST-$TS-1 재시도) -> 400..." -ForegroundColor Cyan
Test-Post "중복 발급 차단 -> 400" `
    "{`"certificateDate`":`"2026-06-04`",`"calibrationDate`":`"2026-06-04`",`"expiryDate`":`"2027-06-04`",`"serialNos`":[`"TEST-$TS-1`"],`"generateMode`":`"MERGED`"}" `
    400

Write-Host "  >> INDIVIDUAL ZIP - 시리얼 3개 (TEST-$TS-Z1~Z3)..." -ForegroundColor Cyan
Test-Post "INDIVIDUAL ZIP 3개 -> 200 ZIP" `
    "{`"certificateDate`":`"2026-06-04`",`"calibrationDate`":`"2026-06-04`",`"expiryDate`":`"2027-06-04`",`"serialNos`":[`"TEST-$TS-Z1`",`"TEST-$TS-Z2`",`"TEST-$TS-Z3`"],`"generateMode`":`"INDIVIDUAL`"}" `
    200 "zip"

Write-Host "  >> MERGED PDF - 시리얼 3개 (TEST-$TS-M1~M3)..." -ForegroundColor Cyan
Test-Post "MERGED PDF 3개 -> 200 PDF" `
    "{`"certificateDate`":`"2026-06-04`",`"calibrationDate`":`"2026-06-04`",`"expiryDate`":`"2027-06-04`",`"serialNos`":[`"TEST-$TS-M1`",`"TEST-$TS-M2`",`"TEST-$TS-M3`"],`"generateMode`":`"MERGED`"}" `
    200 "pdf"

# ─────────────────────────────────────────────
# 정리
# ─────────────────────────────────────────────
Remove-Item -Recurse -Force $tmpDir -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "--------------------------------------------"
Write-Host " RESULT: PASS=$pass  FAIL=$fail"
if ($fail -eq 0) {
    Write-Host " All tests passed!" -ForegroundColor Green
} else {
    Write-Host " Some tests FAILED!" -ForegroundColor Yellow
}
Write-Host "--------------------------------------------"
