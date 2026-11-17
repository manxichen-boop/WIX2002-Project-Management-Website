# One-click local setup and start for Windows (used by "Start EcoSwap.bat").
#  1. finds a Java 21 JDK and MySQL 8 already installed on this PC
#  2. on the first run only: asks for the MySQL root password, creates the ecoswap databases and user,
#     asks for the first administrator's e-mail and password, and writes .env
#  3. starts the application and opens http://localhost:8080 when it is ready
# Passwords are typed by you in this window. They go only into MySQL and your local .env file.
$ErrorActionPreference = "Stop"
Set-Location (Join-Path $PSScriptRoot "..")
$Host.UI.RawUI.WindowTitle = "EcoSwap"

function Say($text, $color = "Green") { Write-Host $text -ForegroundColor $color }
function Fail($text) {
    Write-Host ""
    Write-Host "PROBLEM: $text" -ForegroundColor Red
    Read-Host "Press Enter to close"
    exit 1
}
function New-Secret([int]$length) {
    $chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".ToCharArray()
    $bytes = New-Object byte[] $length
    [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
    -join ($bytes | ForEach-Object { $chars[$_ % $chars.Length] })
}
function Read-Plain([string]$prompt) {
    $secure = Read-Host $prompt -AsSecureString
    [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure))
}

Say "=== EcoSwap local setup ===" "Cyan"

# ---------------------------------------------------------------- Java 21
function Get-JavaMajor([string]$jdkHome) {
    # Read the JDK's "release" file (no process start, so no stderr surprises in Windows PowerShell 5.1).
    $release = Join-Path $jdkHome "release"
    if (Test-Path $release) {
        $text = Get-Content $release -Raw
        if ($text -match 'JAVA_VERSION="(\d+)') { return [int]$Matches[1] }
    }
    # Fallback: ask java itself. "java -version" writes to stderr, so errors must not be fatal here.
    $old = $ErrorActionPreference; $ErrorActionPreference = "Continue"
    try {
        $out = & (Join-Path $jdkHome "bin\java.exe") -version 2>&1 | Out-String
        if ($out -match 'version "(\d+)') { return [int]$Matches[1] }
    } catch { } finally { $ErrorActionPreference = $old }
    return 0
}
$candidates = @()
if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
$roots = "$env:USERPROFILE\.jdk", "C:\Program Files\Java", "C:\Program Files\Eclipse Adoptium", "C:\Program Files\Microsoft", "C:\Program Files\Zulu", "C:\Program Files\Amazon Corretto"
foreach ($r in $roots) {
    # JDK folders directly inside a root, and one level deeper (some extractors nest jdk-21\jdk-21).
    $candidates += Get-ChildItem -Directory -ErrorAction SilentlyContinue $r | Sort-Object Name -Descending | ForEach-Object { $_.FullName }
    $candidates += Get-ChildItem -Directory -ErrorAction SilentlyContinue "$r\*\*" | ForEach-Object { $_.FullName }
}
$javaHome = $null
foreach ($c in $candidates) {
    if ((Test-Path (Join-Path $c "bin\java.exe")) -and ((Get-JavaMajor $c) -ge 21)) { $javaHome = $c; break }
}
if (-not $javaHome) { Fail "Java 21 was not found. Install Eclipse Temurin 21 from https://adoptium.net and run this again." }
$env:JAVA_HOME = $javaHome
$env:Path = "$javaHome\bin;$env:Path"
Say "Java 21 found: $javaHome"

# ---------------------------------------------------------------- MySQL
$mysqlExe = Get-ChildItem -ErrorAction SilentlyContinue "C:\Program Files\MySQL\MySQL Server 8*\bin\mysql.exe", "C:\Program Files\MySQL\MySQL Server *\bin\mysql.exe" |
    Select-Object -First 1 -ExpandProperty FullName
if (-not $mysqlExe) { Fail "MySQL Server was not found under C:\Program Files\MySQL. Install MySQL Server 8.0 and run this again." }
Say "MySQL client found: $mysqlExe"

$service = Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -like "MySQL*" } | Select-Object -First 1
if ($service -and $service.Status -ne "Running") {
    Say "Starting the $($service.Name) Windows service..." "Yellow"
    try { Start-Service $service.Name } catch {
        Fail "The MySQL service '$($service.Name)' is stopped and could not be started without administrator rights. Start it from the Services app (or MySQL Workbench) and run this again."
    }
}

# ---------------------------------------------------------------- port check
$busy = $false
try { $busy = [bool](Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) } catch { }
if (-not $busy) {
    # Fallback: try to connect (works even where Get-NetTCPConnection is unavailable).
    foreach ($h in @("127.0.0.1", "::1")) {
        try {
            $tcp = New-Object System.Net.Sockets.TcpClient
            if ($tcp.ConnectAsync($h, 8080).Wait(700) -and $tcp.Connected) { $busy = $true }
            $tcp.Close()
        } catch { }
    }
}
if ($busy) {
    Fail "Port 8080 is already in use: an older EcoSwap is probably still running in another window. Close that window (or open Task Manager and end OpenJDK Platform binary), then run this again."
}

# ---------------------------------------------------------------- reuse settings from an older copy
if (-not (Test-Path ".env")) {
    $here = (Get-Location).Path
    $roots = @("$env:USERPROFILE\Downloads", "$env:USERPROFILE\Desktop", "$env:USERPROFILE\Documents", "$env:USERPROFILE\Projects", "$env:USERPROFILE\OneDrive\Desktop", "$env:USERPROFILE\OneDrive\Documents")
    $old = foreach ($r in $roots) {
        if (Test-Path $r) {
            Get-ChildItem -Path $r -Filter ".env" -File -Recurse -Depth 4 -Force -ErrorAction SilentlyContinue |
                Where-Object { $_.DirectoryName -ne $here -and (Test-Path (Join-Path $_.DirectoryName "Start EcoSwap.bat")) }
        }
    }
    $old = $old | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($old) {
        Write-Host ""
        Say "Found settings from an earlier EcoSwap copy:" "Cyan"
        Write-Host "  $($old.FullName)"
        $answer = Read-Host "Use the same database and admin settings? (Y/n)"
        if ($answer -notmatch '^(n|no)$') {
            Copy-Item $old.FullName ".env"
            Say "Settings copied."
        }
    }
}

# ---------------------------------------------------------------- first run: database + .env
if (-not (Test-Path ".env")) {
    Write-Host ""
    Say "First run: creating the database and your settings." "Cyan"
    $dbPassword = New-Secret 20
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $rootPassword = Read-Plain "Enter your MySQL 'root' password (input is hidden)"
        $cnf = Join-Path $env:TEMP ("ecoswap-" + [guid]::NewGuid() + ".cnf")
        $escaped = $rootPassword.Replace('\', '\\').Replace('"', '\"')
        [IO.File]::WriteAllText($cnf, "[client]`r`nuser=root`r`npassword=`"$escaped`"`r`nhost=localhost`r`nport=3306`r`n")
        $sql = @"
CREATE DATABASE IF NOT EXISTS ecoswap_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE DATABASE IF NOT EXISTS ecoswap_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS 'ecoswap'@'localhost' IDENTIFIED BY '$dbPassword';
CREATE USER IF NOT EXISTS 'ecoswap'@'127.0.0.1' IDENTIFIED BY '$dbPassword';
CREATE USER IF NOT EXISTS 'ecoswap'@'::1' IDENTIFIED BY '$dbPassword';
ALTER USER 'ecoswap'@'localhost' IDENTIFIED BY '$dbPassword';
ALTER USER 'ecoswap'@'127.0.0.1' IDENTIFIED BY '$dbPassword';
ALTER USER 'ecoswap'@'::1' IDENTIFIED BY '$dbPassword';
GRANT ALL PRIVILEGES ON ecoswap_db.*   TO 'ecoswap'@'localhost', 'ecoswap'@'127.0.0.1', 'ecoswap'@'::1';
GRANT ALL PRIVILEGES ON ecoswap_test.* TO 'ecoswap'@'localhost', 'ecoswap'@'127.0.0.1', 'ecoswap'@'::1';
FLUSH PRIVILEGES;
"@
        $ErrorActionPreference = "Continue"
        $mysqlOutput = $sql | & $mysqlExe "--defaults-extra-file=$cnf" 2>&1
        $ok = $LASTEXITCODE -eq 0
        $ErrorActionPreference = "Stop"
        Remove-Item $cnf -Force -ErrorAction SilentlyContinue
        if ($ok) { break }
        Write-Host ($mysqlOutput | Out-String) -ForegroundColor Red
        if ($attempt -eq 3) { Fail "Could not log in to MySQL as root. Check the password (you can reset it with MySQL Installer > Reconfigure)." }
        Say "That didn't work. Please try again." "Yellow"
    }
    Say "Databases ecoswap_db and ecoswap_test are ready (user 'ecoswap')."

    Write-Host ""
    Say "Now choose the first EcoSwap ADMINISTRATOR account." "Cyan"
    do { $adminEmail = (Read-Host "Admin e-mail").Trim() } until ($adminEmail -match '^[^\s@]+@[^\s@]+\.[^\s@]+$')
    while ($true) {
        $adminPassword = Read-Plain "Admin password (8+ characters, with a letter and a number; hidden)"
        if ($adminPassword.Length -lt 8 -or $adminPassword -notmatch '[A-Za-z]' -or $adminPassword -notmatch '\d') {
            Say "Too weak: use 8+ characters with at least one letter and one number." "Yellow"; continue
        }
        $confirm = Read-Plain "Type the admin password again"
        if ($confirm -ne $adminPassword) { Say "The passwords don't match." "Yellow"; continue }
        break
    }
    $rememberKey = New-Secret 48
    $envText = Get-Content ".env.example" -Raw
    $values = @{
        "DB_PASSWORD" = $dbPassword; "TEST_DB_PASSWORD" = $dbPassword; "ECOSWAP_ADMIN_EMAIL" = $adminEmail
        "ECOSWAP_ADMIN_PASSWORD" = $adminPassword; "ECOSWAP_REMEMBER_ME_KEY" = $rememberKey
    }
    foreach ($k in $values.Keys) {
        $envText = [regex]::Replace($envText, "(?m)^$k=.*$", { param($m) "$k=" + $values[$k] })
    }
    [IO.File]::WriteAllText((Join-Path (Get-Location) ".env"), $envText)
    Say "Settings saved to .env (keep this file private)."
}

# ---------------------------------------------------------------- start
Get-Content ".env" | ForEach-Object {
    $line = $_.Trim()
    if ($line -and -not $line.StartsWith("#") -and $line.Contains("=")) {
        $name, $value = $line.Split("=", 2)
        [Environment]::SetEnvironmentVariable($name.Trim(), $value.Trim(), "Process")
    }
}
New-Item -ItemType Directory -Force -Path ".\storage" | Out-Null

# Open the browser once the server answers (checked in the background for up to 5 minutes).
Start-Job -ScriptBlock {
    for ($i = 0; $i -lt 150; $i++) {
        Start-Sleep -Seconds 2
        try { Invoke-WebRequest "http://localhost:8080/" -UseBasicParsing -TimeoutSec 2 | Out-Null; Start-Process "http://localhost:8080/login"; break } catch { }
    }
} | Out-Null

Write-Host ""
Say "Starting EcoSwap. The first start downloads libraries and can take a few minutes." "Cyan"
Say "Your browser opens automatically when it is ready. Close this window (or press Ctrl+C) to stop the app." "Cyan"
$ErrorActionPreference = "Continue"
& ".\mvnw.cmd" spring-boot:run
Read-Host "EcoSwap stopped. Press Enter to close"
