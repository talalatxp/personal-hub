param([string]$SdkRoot = $env:ANDROID_HOME)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$manifestPath = Join-Path $projectRoot 'app/build/intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml'
if (!(Test-Path -LiteralPath $manifestPath)) {
    throw 'Build :app:assembleRelease before checking privacy.'
}
[xml]$manifest = Get-Content -LiteralPath $manifestPath -Raw
$androidNamespace = 'http://schemas.android.com/apk/res/android'
$permissions = @($manifest.manifest.'uses-permission' | ForEach-Object { $_.GetAttribute('name', $androidNamespace) })
if ($permissions -contains 'android.permission.INTERNET' -or $permissions -contains 'android.permission.ACCESS_NETWORK_STATE') {
    throw 'Merged release manifest requests network access.'
}
$application = $manifest.manifest.application
if ($application.GetAttribute('allowBackup', $androidNamespace) -ne 'false') { throw 'Auto Backup is enabled.' }
if ($application.GetAttribute('fullBackupContent', $androidNamespace) -ne '@xml/backup_rules' -or
    $application.GetAttribute('dataExtractionRules', $androidNamespace) -ne '@xml/data_extraction_rules') {
    throw 'Merged manifest does not reference the reviewed backup rules.'
}
foreach ($component in $application.ChildNodes) {
    if ($component.Name -in @('activity', 'activity-alias', 'service', 'receiver', 'provider')) {
        $exported = $component.GetAttribute('exported', $androidNamespace)
        $name = $component.GetAttribute('name', $androidNamespace)
        if ($exported -ne 'false' -and !($exported -eq 'true' -and $name -eq 'com.talania.personalhub.app.MainActivity')) {
            throw "Unapproved public component: $name"
        }
    }
}
$domains = @('root', 'file', 'database', 'sharedpref', 'external', 'device_root', 'device_file', 'device_database', 'device_sharedpref')
[xml]$legacy = Get-Content (Join-Path $projectRoot 'app/src/main/res/xml/backup_rules.xml') -Raw
[xml]$modern = Get-Content (Join-Path $projectRoot 'app/src/main/res/xml/data_extraction_rules.xml') -Raw
foreach ($rules in @($legacy.'full-backup-content', $modern.'data-extraction-rules'.'cloud-backup', $modern.'data-extraction-rules'.'device-transfer')) {
    foreach ($domain in $domains) {
        if (!@($rules.exclude | Where-Object { $_.domain -eq $domain -and $_.path -eq '.' }).Count) {
            throw "Missing complete backup exclusion: $domain"
        }
    }
}
if (!$SdkRoot) {
    $localProperties = Join-Path $projectRoot 'local.properties'
    if (Test-Path $localProperties) {
        $SdkRoot = ((Get-Content $localProperties | Where-Object { $_ -like 'sdk.dir=*' }) -replace '^sdk.dir=', '')
    }
}
if (!$SdkRoot) { throw 'Set ANDROID_HOME or pass -SdkRoot to inspect the APK.' }
$aapt = Join-Path $SdkRoot 'build-tools/36.0.0/aapt.exe'
$apk = Join-Path $projectRoot 'app/build/outputs/apk/release/app-release-unsigned.apk'
$badging = & $aapt dump badging $apk
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect release APK.' }
if ($badging -match 'android.permission.(INTERNET|ACCESS_NETWORK_STATE)') { throw 'Packaged release APK requests network access.' }
Write-Output 'PASS: merged release manifest, packaged APK and cloud/transfer rules.'
