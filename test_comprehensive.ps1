
Add-Type -AssemblyName System.Net.Http
$BASE = "http://localhost:8080/api/documents/certificates"
$script:PASS = 0
$script:FAIL = 0

$handler = New-Object System.Net.Http.HttpClientHandler
$handler.UseProxy = $false
$client = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [TimeSpan]::FromSeconds(300)

function Show-Result {
    param([string]$name, [bool]$ok, [string]$info)
    if ($ok) {
        Write-Host ("  [PASS] " + $name) -ForegroundColor Green
        if ($info) { Write-Host ("         " + $info) -ForegroundColor Gray }
        $script:PASS++
    } else {
        Write-Host ("  [FAIL] " + $name) -ForegroundColor Red
        if ($info) { Write-Host ("         " + $info) -ForegroundColor Yellow }
        $script:FAIL++
    }
}

function Do-Get {
    param([string]$url)
    try {
        $resp = $client.GetAsync($url).Result
        $body = $resp.Content.ReadAsStringAsync().Result
        $len = $resp.Content.Headers.ContentLength
        $ct = ""
        try { $ct = $resp.Content.Headers.ContentType.ToString() } catch {}
        return @{ Status=[int]$resp.StatusCode; Body=$body; Len=$len; CT=$ct; Headers=$resp.Headers }
    } catch {
        return @{ Status=0; Body=""; Len=0; CT="" }
    }
}

function Do-Post {
    param($body)
    try {
        $json = $body | ConvertTo-Json -Depth 5
        $content = New-Object System.Net.Http.StringContent($json, [Text.Encoding]::UTF8, "application/json")
        $resp = $client.PostAsync("$BASE/pdf", $content).Result
        $respBody = $resp.Content.ReadAsStringAsync().Result
        $len = $resp.Content.ReadAsByteArrayAsync().Result.Length
        $ct = ""
        try { $ct = $resp.Content.Headers.ContentType.ToString() } catch {}
        return @{ Status=[int]$resp.StatusCode; Body=$respBody; Len=$len; CT=$ct }
    } catch {
        return @{ Status=0; Body=$_.ToString(); Len=0; CT="" }
    }
}

# [1] Server Health Check
Write-Host ""
Write-Host "=== [1] Server Health Check ===" -ForegroundColor Cyan
$h = Do-Get $BASE
Show-Result "Backend server is running" ($h.Status -eq 200) ("STATUS=" + $h.Status)
if ($h.Status -ne 200) { Write-Host "Server not reachable. Abort." -ForegroundColor Red; $client.Dispose(); exit 1 }

# [2] Validation Tests
Write-Host ""
Write-Host "=== [2] Validation Tests ===" -ForegroundColor Cyan

$r = Do-Post @{ calibrationDate="2026-01-01"; expiryDate="2027-01-01"; serialNos=@("SN001") }
Show-Result "Missing certificateDate -> 400" ($r.Status -eq 400) ("STATUS=" + $r.Status)

$r = Do-Post @{ certificateDate="2026-01-01"; calibrationDate="2026-01-01"; serialNos=@("SN001") }
Show-Result "Missing expiryDate -> 400" ($r.Status -eq 400) ("STATUS=" + $r.Status)

$r = Do-Post @{ certificateDate="2026-01-01"; calibrationDate="2026-01-01"; expiryDate="2027-01-01"; serialNos=@() }
Show-Result "Empty serialNos -> 400" ($r.Status -eq 400) ("STATUS=" + $r.Status)

$r = Do-Post @{ certificateDate="2026/01/01"; calibrationDate="2026-01-01"; expiryDate="2027-01-01"; serialNos=@("SN001") }
Show-Result "Invalid date format -> 400" ($r.Status -eq 400) ("STATUS=" + $r.Status)

$r = Do-Post @{ certificateDate="2027-01-01"; calibrationDate="2026-01-01"; expiryDate="2025-01-01"; serialNos=@("SN001") }
Show-Result "ExpiryDate < CertDate -> 400" ($r.Status -eq 400) ("STATUS=" + $r.Status)

$r = Do-Post @{ certificateDate="2026-01-01"; calibrationDate="2026-01-01"; expiryDate="2027-01-01"; serialNos=@("SN001","SN001") }
Show-Result "Duplicate serials -> 400" ($r.Status -eq 400) ("STATUS=" + $r.Status)

$r = Do-Post @{ certificateDate="2026-01-01"; calibrationDate="2026-01-01"; expiryDate="2027-01-01"; serialNos=@("SN001"); startSequenceNo="ABC" }
Show-Result "Invalid startSequenceNo format -> 400" ($r.Status -eq 400) ("STATUS=" + $r.Status)

$r = Do-Post @{ certificateDate="2026-01-01"; calibrationDate="2026-01-01"; expiryDate="2027-01-01"; serialNos=@("SN001","SN002"); startSequenceNo="0001 0002 0003" }
Show-Result "StartNo count mismatch -> 400" ($r.Status -eq 400) ("STATUS=" + $r.Status)

$r = Do-Post @{ certificateDate="2026-01-01"; calibrationDate="2026-01-01"; expiryDate="2027-01-01"; serialNos=@("SN001","SN002"); startSequenceNo="9999" }
Show-Result "StartNo overflow -> 400" ($r.Status -eq 400) ("STATUS=" + $r.Status)

# [3] Merged PDF Generation
Write-Host ""
Write-Host "=== [3] Merged PDF Generation ===" -ForegroundColor Cyan
Write-Host "  NOTE: MS Word conversion may take 1~2 minutes..." -ForegroundColor DarkGray

$TODAY = (Get-Date).ToString("yyyy-MM-dd")
$NEXT_YEAR = (Get-Date).AddYears(1).ToString("yyyy-MM-dd")
$SERIAL = "AUTOTEST" + (Get-Date).ToString("HHmmss")

$r3 = Do-Post @{ certificateDate=$TODAY; calibrationDate=$TODAY; expiryDate=$NEXT_YEAR; serialNos=@($SERIAL); generateMode="MERGED" }
$ok3 = ($r3.Status -eq 200) -and ($r3.CT -like "*pdf*") -and ($r3.Len -gt 1000)
Show-Result "Merged PDF (1 serial)" $ok3 ("STATUS=" + $r3.Status + " | Type=" + $r3.CT + " | Size=" + $r3.Len + "bytes")

# [4] History List
Write-Host ""
Write-Host "=== [4] History List ===" -ForegroundColor Cyan
$gh = Do-Get $BASE
$latestId = 0; $latestCertNo = ""
if ($gh.Status -eq 200) {
    $hj = $gh.Body | ConvertFrom-Json
    $histCount = $hj.Count
    Show-Result "Get history list" $true ("STATUS=200 | Count=" + $histCount)
    if ($histCount -gt 0) {
        $latestId = $hj[0].id
        $latestCertNo = $hj[0].certificateNo
        Write-Host ("  Latest: ID=" + $latestId + " CertNo=" + $latestCertNo) -ForegroundColor Gray
    }
} else {
    Show-Result "Get history list" $false ("STATUS=" + $gh.Status)
}

# [5] Re-download MERGED
if ($latestId -gt 0) {
    Write-Host ""
    Write-Host "=== [5] Re-download MERGED ===" -ForegroundColor Cyan
    Write-Host "  NOTE: MS Word conversion may take 1~2 minutes..." -ForegroundColor DarkGray
    $dr = Do-Get ("$BASE/" + $latestId + "/download?mode=MERGED")
    $ok5 = ($dr.Status -eq 200) -and ($dr.CT -like "*pdf*") -and ($dr.Len -gt 1000)
    Show-Result "Re-download MERGED -> PDF" $ok5 ("STATUS=" + $dr.Status + " | Type=" + $dr.CT + " | Size=" + $dr.Len + "bytes")

    # [6] Re-download INDIVIDUAL ZIP
    Write-Host ""
    Write-Host "=== [6] Re-download INDIVIDUAL ZIP ===" -ForegroundColor Cyan
    Write-Host "  NOTE: MS Word conversion may take 1~2 minutes..." -ForegroundColor DarkGray
    $zr = Do-Get ("$BASE/" + $latestId + "/download?mode=INDIVIDUAL")
    $ok6 = ($zr.Status -eq 200) -and ($zr.CT -like "*zip*") -and ($zr.Len -gt 100)
    Show-Result "Re-download INDIVIDUAL -> ZIP" $ok6 ("STATUS=" + $zr.Status + " | Type=" + $zr.CT + " | Size=" + $zr.Len + "bytes")
} else {
    Write-Host "  No history - skipping re-download tests" -ForegroundColor Yellow
}

# [7] Not Found ID -> 404
Write-Host ""
Write-Host "=== [7] Not Found ID -> 404 ===" -ForegroundColor Cyan
$nf = Do-Get "$BASE/999999/download"
Show-Result "ID=999999 -> 404" ($nf.Status -eq 404) ("STATUS=" + $nf.Status)

# [8] Duplicate Detection
Write-Host ""
Write-Host "=== [8] Duplicate Issue Prevention ===" -ForegroundColor Cyan
$r8 = Do-Post @{ certificateDate=$TODAY; calibrationDate=$TODAY; expiryDate=$NEXT_YEAR; serialNos=@($SERIAL); generateMode="MERGED" }
Show-Result "Exact duplicate blocked -> 400" ($r8.Status -eq 400) ("STATUS=" + $r8.Status)

$client.Dispose()

# Final Result
$TOTAL = $script:PASS + $script:FAIL
Write-Host ""
Write-Host "==========================================" -ForegroundColor White
$msg = " RESULT: PASS=" + $script:PASS + "  FAIL=" + $script:FAIL + "  TOTAL=" + $TOTAL
if ($script:FAIL -eq 0) {
    Write-Host $msg -ForegroundColor Green
    Write-Host " ALL TESTS PASSED!" -ForegroundColor Green
} else {
    Write-Host $msg -ForegroundColor Yellow
    Write-Host (" " + $script:FAIL + " test(s) FAILED.") -ForegroundColor Red
}
Write-Host "==========================================" -ForegroundColor White
