param(
    [Parameter(Mandatory = $true)][string]$SdkRoot,
    [Parameter(Mandatory = $true)][string]$Serial
)

$ErrorActionPreference = 'Stop'
$adb = Join-Path $SdkRoot 'platform-tools/adb.exe'
$projectRoot = Split-Path $PSScriptRoot -Parent
# Gradle's device test runner removes its APKs when the suite finishes.
foreach ($apk in @('app/build/outputs/apk/debug/app-debug.apk', 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk')) {
    & $adb -s $Serial install -r (Join-Path $projectRoot $apk)
    if ($LASTEXITCODE -ne 0) { throw "Cannot install test artifact: $apk" }
}
$processIds = @()
foreach ($phase in @('write', 'read')) {
    $result = & $adb -s $Serial shell am instrument -w -r -e class 'com.talania.personalhub.app.ProcessPersistenceTest#persistedValues' -e phase $phase 'com.talania.personalhub.test/androidx.test.runner.AndroidJUnitRunner'
    if ($LASTEXITCODE -ne 0 -or ($result -join "`n") -notmatch 'OK \(1 test\)') {
        $result | Write-Output
        throw "Persistence phase failed: $phase"
    }
    $pidLine = $result | Where-Object { $_ -match 'INSTRUMENTATION_STATUS: f01_pid=\d+' } | Select-Object -First 1
    if (!$pidLine) { throw "No process identity reported: $phase" }
    $processIds += [int]($pidLine -replace '^.*f01_pid=', '')
    Write-Output "$phase PASS: PID $($processIds[-1])"
}
if ($processIds[0] -eq $processIds[1]) { throw 'Both phases used the same process; restart has not been proved.' }
Write-Output "PASS: Room and DataStore values survived separate Android processes on $Serial."
