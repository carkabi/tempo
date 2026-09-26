param()

$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$SigningDir = 'C:\Users\MAXIME\Developpement\Peach-Signing'
$KeyPath = Join-Path $SigningDir 'peach-release.jks'
$ProjectProperties = Join-Path $ProjectRoot 'keystore.properties'
$BackupProperties = Join-Path $SigningDir 'keystore.properties'
$CertificateFile = Join-Path $SigningDir 'certificate-sha256.txt'
$Keytool = 'C:\Users\MAXIME\.jdks\ms-21.0.12.1\bin\keytool.exe'

if (-not (Test-Path $Keytool)) { throw "keytool introuvable : $Keytool" }
New-Item -ItemType Directory -Force -Path $SigningDir | Out-Null

function Read-Properties([string]$Path) {
    $result = @{}
    Get-Content $Path | ForEach-Object {
        if ($_ -match '=') {
            $key, $value = $_ -split '=', 2
            $result[$key.Trim()] = $value.Trim()
        }
    }
    return $result
}

if (Test-Path $KeyPath) {
    if (-not (Test-Path $ProjectProperties)) {
        throw 'La cle existe mais keystore.properties est absent. Ne pas regenerer la cle.'
    }
    $props = Read-Properties $ProjectProperties
    $password = $props['storePassword']
    $alias = $props['keyAlias']
    if (-not $password -or -not $alias) { throw 'Configuration de signature incomplete.' }
    Write-Host 'Cle Peach existante detectee : reprise de la finalisation.'
} else {
    $bytes = New-Object byte[] 48
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($bytes)
    $rng.Dispose()
    $password = [Convert]::ToBase64String($bytes).Replace('+','A').Replace('/','B').Replace('=','C')
    $alias = 'peach-release'
    & $Keytool -genkeypair -v -keystore $KeyPath -storepass $password -keypass $password -alias $alias -keyalg RSA -keysize 4096 -validity 10000 -dname 'CN=Peach, OU=Applications, O=Tropikeau, C=FR'
    if ($LASTEXITCODE -ne 0) { throw 'La creation de la cle a echoue.' }
    $properties = @(
        ('keyAlias=' + $alias),
        ('keyPassword=' + $password),
        ('storeFile=' + ($KeyPath -replace '\\','/')),
        ('storePassword=' + $password)
    )
    Set-Content -Path $ProjectProperties -Value $properties -Encoding ASCII
    Set-Content -Path $BackupProperties -Value $properties -Encoding ASCII
}

if (-not (Test-Path $BackupProperties)) { Copy-Item $ProjectProperties $BackupProperties -Force }

$certDer = Join-Path $env:TEMP ('peach-cert-' + [guid]::NewGuid().ToString('N') + '.der')
try {
    & $Keytool -exportcert -alias $alias -keystore $KeyPath -storepass $password -file $certDer | Out-Null
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path $certDer)) { throw 'Export du certificat impossible.' }
    $cert = New-Object System.Security.Cryptography.X509Certificates.X509Certificate2($certDer)
    $certificateSha256 = $cert.GetCertHashString([System.Security.Cryptography.HashAlgorithmName]::SHA256).ToLowerInvariant()
    if (-not $certificateSha256) { throw 'Empreinte SHA-256 du certificat introuvable.' }
    Set-Content -Path $CertificateFile -Value $certificateSha256 -Encoding ASCII
} finally {
    Remove-Item $certDer -Force -ErrorAction SilentlyContinue
}

icacls $SigningDir /inheritance:r | Out-Null
icacls $SigningDir /grant:r "$($env:USERNAME):(OI)(CI)F" | Out-Null
icacls $ProjectProperties /inheritance:r | Out-Null
icacls $ProjectProperties /grant:r "$($env:USERNAME):F" | Out-Null

Write-Host ''
Write-Host 'Cle officielle Peach validee.'
Write-Host "Dossier prive : $SigningDir"
Write-Host "Empreinte certificat : $certificateSha256"
Write-Host 'Le mot de passe n a pas ete affiche.'
