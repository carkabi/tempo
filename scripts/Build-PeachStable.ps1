param(
    [Parameter(Mandatory=$true)][int]$VersionCode,
    [Parameter(Mandatory=$true)][string]$VersionName
)

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$BuildGradle = Join-Path $ProjectRoot 'app\build.gradle'
$SigningDir = 'C:\Users\MAXIME\Developpement\Peach-Signing'
$CertificateFile = Join-Path $SigningDir 'certificate-sha256.txt'
$PropertiesFile = Join-Path $ProjectRoot 'keystore.properties'
$BuildOutput = Join-Path $ProjectRoot 'app\build\outputs\apk\peach\release\app-peach-release.apk'
$StableDir = 'C:\Users\MAXIME\Developpement\Peach-Builds\Stable'
$JavaHome = 'C:\Users\MAXIME\.jdks\ms-21.0.12.1'
$Sdk = 'C:\Users\MAXIME\AppData\Local\Android\Sdk'

if (-not (Test-Path $PropertiesFile)) { throw 'keystore.properties absent. Lance Create-PeachSigningKey.ps1 une seule fois.' }
if (-not (Test-Path $CertificateFile)) { throw 'Empreinte officielle Peach absente.' }

$gradle = Get-Content $BuildGradle -Raw
$gradle = [regex]::Replace($gradle, 'versionCode\s+\d+', ('versionCode ' + $VersionCode), 1)
$gradle = [regex]::Replace($gradle, "versionName\s+'[^']+'", ("versionName '" + $VersionName + "'"), 1)
$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText(
    $BuildGradle,
    $gradle,
    $utf8NoBom
)

$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
Push-Location $ProjectRoot
try {
    .\gradlew.bat clean assemblePeachRelease testPeachReleaseUnitTest
    if ($LASTEXITCODE -ne 0) { throw 'Build Peach release en echec.' }
} finally {
    Pop-Location
}

if (-not (Test-Path $BuildOutput)) { throw "APK release introuvable : $BuildOutput" }

$Aapt = Get-ChildItem "$Sdk\build-tools" -Recurse -Filter aapt.exe | Sort-Object FullName -Descending | Select-Object -First 1
$Apksigner = Get-ChildItem "$Sdk\build-tools" -Recurse -Filter apksigner.bat | Sort-Object FullName -Descending | Select-Object -First 1
if (-not $Aapt -or -not $Apksigner) { throw 'Android build-tools incomplets.' }

$badging = & $Aapt.FullName dump badging $BuildOutput | Select-Object -First 1
if ($badging -notmatch "name='fr\.tropikeau\.peach'") { throw 'Package Android inattendu.' }
if ($badging -notmatch ("versionCode='" + $VersionCode + "'")) { throw 'VersionCode APK inattendu.' }
if ($badging -notmatch ("versionName='" + [regex]::Escape($VersionName) + "'")) { throw 'VersionName APK inattendue.' }

$certOutput = & $Apksigner.FullName verify --print-certs $BuildOutput
$certLine = $certOutput | Select-String -Pattern 'Signer #1 certificate SHA-256 digest:' | Select-Object -First 1
if (-not $certLine) { throw 'Certificat APK introuvable.' }
$actualCert = ($certLine.Line -split ': ', 2)[1].Trim().ToLowerInvariant()
$expectedCert = (Get-Content $CertificateFile -Raw).Trim().ToLowerInvariant()
if ($actualCert -ne $expectedCert) { throw 'SIGNATURE REFUSEE : APK non signee avec la cle officielle Peach.' }

New-Item -ItemType Directory -Force -Path $StableDir | Out-Null
$SafeVersion = $VersionName -replace '[^0-9A-Za-z._-]', '-'
$FinalApk = Join-Path $StableDir ("Peach-$SafeVersion-v$VersionCode-stable-release.apk")
Copy-Item $BuildOutput $FinalApk -Force

$file = Get-Item $FinalApk
$sha = (Get-FileHash $FinalApk -Algorithm SHA256).Hash.ToLowerInvariant()
$commit = (git -C $ProjectRoot rev-parse HEAD).Trim()
$manifest = [ordered]@{
    application_id = 'fr.tropikeau.peach'
    channel = 'stable'
    version_code = $VersionCode
    version_name = $VersionName
    apk = $file.Name
    size_bytes = $file.Length
    sha256 = $sha
    certificate_sha256 = $actualCert
    git_commit = $commit
    built_at = (Get-Date).ToUniversalTime().ToString('o')
}
$ManifestPath = [System.IO.Path]::ChangeExtension($FinalApk, '.json')
$manifest | ConvertTo-Json -Depth 4 | Set-Content -Path $ManifestPath -Encoding UTF8

Write-Host ''
Write-Host 'Peach stable prete.'
Write-Host "APK      : $FinalApk"
Write-Host "Manifest : $ManifestPath"
Write-Host "SHA-256  : $sha"
Write-Host "Certificat verifie : $actualCert"
