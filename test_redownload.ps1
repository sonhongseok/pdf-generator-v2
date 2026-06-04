$BASE_URL = "http://localhost:8080/api/documents/certificates"
$pass = 0; $fail = 0
$tmpDir = "C:\work\pdf-generator-v2\test_tmp_redownload"
New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

Write-Host "=== [1] 가장 최근 발급 이력 ID 조회 ===" -ForegroundColor Yellow
$result = curl.exe -s "$BASE_URL"
$json = $result | ConvertFrom-Json
if ($json.Count -eq 0) {
    Write-Host "발급 이력이 없습니다. 테스트를 종료합니다." -ForegroundColor Red
    exit
}
$targetId = $json[0].id
$certNo = $json[0].certificateNo
Write-Host "  Target ID: $targetId (CertNo: $certNo)" -ForegroundColor Cyan

function Test-Download {
    param(
        [string]$Name,
        [string]$Mode,
        [int]$ExpectedStatus,
        [string]$ExpectedType
    )
    $resultFile = "$tmpDir\result_$Mode.bin"
    $url = "$BASE_URL/$targetId/download?mode=$Mode"
    
    $result = curl.exe -s -o $resultFile -w "%{http_code}|%{content_type}|%{size_download}" --max-time 300 $url

    $m = $result.Split("|")
    $code = $m[0]; $type = $m[1]; $size = $m[2]

    $statusOk = ($code -eq $ExpectedStatus.ToString())
    $typeOk   = ($type -like "*$ExpectedType*")

    if ($statusOk -and $typeOk) {
        Write-Host "  PASS | $Name | STATUS=$code | Type=$type | Size=${size}bytes" -ForegroundColor Green
        $script:pass++
    } else {
        $body = Get-Content $resultFile -Raw -ErrorAction SilentlyContinue
        Write-Host "  FAIL | $Name | Expected=$ExpectedStatus Got=$code | ExpectedType=$ExpectedType GotType=$type | $body" -ForegroundColor Red
        $script:fail++
    }
}

Write-Host ""
Write-Host "=== [2] 재다운로드(MERGED) 테스트 ===" -ForegroundColor Yellow
Test-Download "재다운로드 (통합 PDF)" "MERGED" 200 "pdf"

Write-Host ""
Write-Host "=== [3] 재다운로드(INDIVIDUAL ZIP) 테스트 ===" -ForegroundColor Yellow
Test-Download "재다운로드 (개별 ZIP)" "INDIVIDUAL" 200 "zip"

# 정리
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
