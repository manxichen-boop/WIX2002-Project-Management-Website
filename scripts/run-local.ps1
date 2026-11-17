# Starts EcoSwape locally (Windows PowerShell). Reads settings from .env in the project root.
#   .\scripts\run-local.ps1          -> run the application
#   .\scripts\run-local.ps1 -Test    -> run mvn clean verify
param([switch]$Test)
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")

if (-not (Test-Path ".env")) {
    Write-Error "No .env file found. Copy .env.example to .env and fill in DB_PASSWORD (and the admin bootstrap values)."
}

Get-Content ".env" | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $name, $value = $line.Split("=", 2)
        [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
    }
}

if (-not $env:DB_PASSWORD) { Write-Error "DB_PASSWORD must be set in .env" }
$storage = if ($env:ECOSWAP_STORAGE_DIR) { $env:ECOSWAP_STORAGE_DIR } else { ".\storage" }
New-Item -ItemType Directory -Force -Path $storage | Out-Null

if ($Test) { & .\mvnw.cmd clean verify } else { & .\mvnw.cmd spring-boot:run }
