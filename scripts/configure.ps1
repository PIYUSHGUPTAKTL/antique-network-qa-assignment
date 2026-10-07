$ErrorActionPreference='Stop'
$module=Join-Path $PSScriptRoot '../ui-automation'
$path=Join-Path $module '.env'
if (!(Test-Path $path)) {
 $bytes=New-Object byte[] 24
 function New-Secret { $rng=[System.Security.Cryptography.RandomNumberGenerator]::Create(); try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }; return [Convert]::ToBase64String($bytes) -replace '[^A-Za-z0-9]','' }
 @("DB_PASSWORD=$(New-Secret)","DB_ROOT_PASSWORD=$(New-Secret)","UI_ADMIN_USER=qa_$([Guid]::NewGuid().ToString('N').Substring(0,8))","UI_ADMIN_PASSWORD=$(New-Secret)") | Set-Content -Encoding ascii $path
}
Get-Content $path | ForEach-Object { $k,$v=$_.Split('=',2); [Environment]::SetEnvironmentVariable($k,$v,'Process') }
Write-Host 'Loaded local credentials into this PowerShell session. Keep .env private.'
