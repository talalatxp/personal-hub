param(
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$SdkRoot = $env:ANDROID_HOME,
    [switch]$Connected
)

$ErrorActionPreference = 'Stop'
if (!$JdkHome -or !(Test-Path (Join-Path $JdkHome 'bin/java.exe'))) { throw 'Pass -JdkHome with JDK 17.' }
$env:JAVA_HOME = $JdkHome
if ($SdkRoot) { $env:ANDROID_HOME = $SdkRoot }
$projectRoot = Split-Path $PSScriptRoot -Parent
Push-Location $projectRoot
try {
    & .\gradlew.bat :app:ktlintCheck :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease :app:assembleDebugAndroidTest --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'Gradle validation failed.' }
    & (Join-Path $PSScriptRoot 'verify-privacy.ps1') -SdkRoot $SdkRoot
    if ($Connected) {
        if (!$SdkRoot) { throw 'Pass -SdkRoot or set ANDROID_HOME for process persistence checks.' }
        & .\gradlew.bat :app:connectedDebugAndroidTest --no-daemon
        if ($LASTEXITCODE -ne 0) { throw 'Instrumented tests failed.' }
        $adb = Join-Path $SdkRoot 'platform-tools/adb.exe'
        $devices = & $adb devices
        if ($LASTEXITCODE -ne 0) { throw 'Cannot list Android devices.' }
        foreach ($line in $devices) {
            if ($line -match '^(\S+)\s+device$') {
                & (Join-Path $PSScriptRoot 'verify-process-persistence.ps1') -SdkRoot $SdkRoot -Serial $Matches[1]
            }
        }
    }
    & git diff --check
    if ($LASTEXITCODE -ne 0) { throw 'Whitespace check failed.' }
} finally { Pop-Location }
