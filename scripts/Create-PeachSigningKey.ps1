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
if (Test-Path $KeyPath) { throw 'La cle Peach existe deja. Aucun ecrasement autorise.' }

$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$rng.Dispose()
$password = [Convert]::ToBase64String($bytes).Replace('+','A').Replace('/','B').Replace('=','C')

& $Keytool -genkeypair -v -keystore $KeyPath -storepass $password -keypass $password -alias 'peach-release' -keyalg RSA -keysize 4096 -validity 10000 -dname 'CN=Peach, OU=Applications, O=Tropikeau, C=FR'
if ($LASTEXITCODE -ne 0) { throw 'La creation de la cle a echoue.' }

$properties = @(
    'keyAlias=peach-release',
    ('keyPassword=' + $password),
    ('storeFile=' + ($KeyPath -replace '\\','/')),
    ('storePassword=' + $password)
)
Set-Content -Path $ProjectProperties -Value $properties -Encoding ASCII
Set-Content -Path $BackupProperties -Value $properties -Encoding ASCII

$keyInfo = & $Keytool -list -v -keystore $KeyPath -storepass $password -alias 'peach-release'
$shaLine = $keyInfo | Select-String -Pattern '^\s*SHA256:' | Select-Object -First 1
if (-not $shaLine) { throw 'Empreinte SHA-256 de certificat introuvable.' }
$certificateSha256 = ($shaLine.Line -replace '^\s*SHA256:\s*','').Trim().Replace(':','').ToLowerInvariant()
Set-Content -Path $CertificateFile -Value $certificateSha256 -Encoding ASCII

icacls $SigningDir /inheritance:r /grant:r "$env:USERNAME:(OI)(CI)F" | Out-Null
icacls $ProjectProperties /inheritance:r /grant:r "$env:USERNAME:F" | Out-Null

Write-Host ''
Write-Host 'Cle officielle Peach creee.'
Write-Host "Dossier prive : $SigningDir"
Write-Host "Empreinte certificat : $certificateSha256"
Write-Host 'Le mot de passe n a pas ete affiche.'
