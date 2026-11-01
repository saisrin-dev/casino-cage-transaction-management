$ErrorActionPreference = "Stop"
if (Test-Path .env) { throw ".env already exists. Edit it directly to preserve encryption keys and database credentials." }
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
function New-Secret { param([int]$Count=32) $bytes = New-Object byte[] $Count; $rng.GetBytes($bytes); return [Convert]::ToBase64String($bytes) }
$key = New-Secret
$dbPassword = New-Secret
$demoPassword = "Demo-" + (New-Secret 18)
@"
DB_PASSWORD=$dbPassword
DATA_ENCRYPTION_KEY=$key
DEMO_PASSWORD=$demoPassword
DEMO_SEED=true
SIMULATION_CONTROLS=true
FILING_MODE=mock
COOKIE_SECURE=false
"@ | Set-Content -Path .env -Encoding ascii
$rng.Dispose()
Write-Host "Created .env. Demo password is the DEMO_PASSWORD value in that file. Do not commit .env."
