<#
.SYNOPSIS
    Rebuilds target\backend.exe with GraalVM Native Image.

.DESCRIPTION
    Uses the Oracle GraalVM 21.0.4 JDK and the Visual Studio 2022 Build Tools
    (MSVC + Windows SDK) installed on this machine.

    The Spring Boot 'native' profile runs the application test and the AOT
    processing step before native-image, so the application's environment
    variables (PKMS_*, ELASTIC_*) must be available. They are resolved from the
    current process first, then from the user environment, then the machine
    environment.

.PARAMETER JavaHome
    GraalVM JDK directory. Defaults to C:\tools\graalvm\graalvm-jdk-21.0.4+8.1

.PARAMETER Clean
    Run 'mvnw clean' before building.

.PARAMETER SkipTests
    Pass -DskipTests to Maven. Note: AOT processing still loads the Spring
    context, so the environment variables are still required.

.EXAMPLE
    .\build-native.ps1

.EXAMPLE
    .\build-native.ps1 -Clean

.NOTES
    If PowerShell blocks the script, run:
        powershell -NoProfile -ExecutionPolicy Bypass -File .\build-native.ps1
#>
[CmdletBinding()]
param(
    [string]$JavaHome = 'C:\tools\graalvm\graalvm-jdk-21.0.4+8.1',
    [switch]$Clean,
    [switch]$SkipTests
)

$ErrorActionPreference = 'Stop'

function Write-Step([string]$Text) {
    Write-Host ""
    Write-Host "==> $Text" -ForegroundColor Cyan
}

function Fail([string]$Text) {
    Write-Host ""
    Write-Host "ERROR: $Text" -ForegroundColor Red
    exit 1
}

function Format-EnvValue([string]$Name, [string]$Value) {
    if ($Name -match 'PASSWORD|SECRET|API_KEY') { return '********' }
    return $Value
}

# ---------------------------------------------------------------------------
# Locate the project
# ---------------------------------------------------------------------------
$projectDir = if ($PSScriptRoot) { $PSScriptRoot } else { (Get-Location).Path }
$mvnw = Join-Path $projectDir 'mvnw.cmd'
if (-not (Test-Path $mvnw)) {
    Fail "mvnw.cmd not found in '$projectDir'. Keep this script in the backend project root."
}

# ---------------------------------------------------------------------------
# 1. GraalVM
# ---------------------------------------------------------------------------
Write-Step "GraalVM: $JavaHome"
$nativeImage = Join-Path $JavaHome 'bin\native-image.cmd'
if (-not (Test-Path $nativeImage)) {
    Fail "native-image.cmd not found under '$JavaHome\bin'. Pass the correct path with -JavaHome."
}
$env:JAVA_HOME = $JavaHome
$env:Path = "$JavaHome\bin;$env:Path"
$nativeVersion = (& $nativeImage --version 2>&1 | Select-Object -First 1)
Write-Host "    $nativeVersion"

# ---------------------------------------------------------------------------
# 2. Visual Studio C++ toolchain
# ---------------------------------------------------------------------------
Write-Step "Visual Studio C++ toolchain"
$vswhere = Join-Path ${env:ProgramFiles(x86)} 'Microsoft Visual Studio\Installer\vswhere.exe'
if (Test-Path $vswhere) {
    $vsPath = & $vswhere -products * -requires Microsoft.VisualStudio.Workload.VCTools -property installationPath
    if ($vsPath) {
        Write-Host "    Found: $vsPath"
    } else {
        Write-Warning "The Visual Studio C++ workload was not detected. native-image may not find cl.exe."
    }
} else {
    Write-Warning "vswhere.exe was not found. native-image will search for MSVC on its own."
}

# ---------------------------------------------------------------------------
# 3. Application environment variables (needed by tests + AOT processing)
# ---------------------------------------------------------------------------
Write-Step "Application environment variables"

$requiredVars = @(
    'PKMS_MYSQL_DATABASE',
    'PKMS_MYSQL_USERNAME',
    'PKMS_MYSQL_PASSWORD',
    'ELASTIC_URL',
    'ELASTIC_USER',
    'ELASTIC_PASSWORD'
)
# Variables that have defaults in application.properties. Set them only if present.
$optionalVars = @(
    'PKMS_MYSQL_HOST',
    'PKMS_MYSQL_PORT',
    'PKMS_API_KEY',
    'PKMS_PUBLIC_API',
    'PKMS_PUBLIC_URL',
    'ELASTIC_INDEX_NAME',
    'PKMS_UPLOAD_DIR'
)

function Resolve-AppEnv([string]$Name) {
    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if (-not $value) { $value = [Environment]::GetEnvironmentVariable($Name, 'User') }
    if (-not $value) { $value = [Environment]::GetEnvironmentVariable($Name, 'Machine') }
    return $value
}

$missing = @()
foreach ($name in $requiredVars) {
    $value = Resolve-AppEnv $name
    if ($value) {
        Set-Item -Path "Env:$name" -Value $value
        Write-Host ("    {0} = {1}" -f $name, (Format-EnvValue $name $value))
    } else {
        $missing += $name
    }
}

foreach ($name in $optionalVars) {
    $value = Resolve-AppEnv $name
    if ($value) {
        Set-Item -Path "Env:$name" -Value $value
        Write-Host ("    {0} = {1}" -f $name, (Format-EnvValue $name $value))
    }
}

if ($missing.Count -gt 0) {
    Fail ("Missing environment variables: {0}.`nSet them as user environment variables (see README) and reopen the terminal." -f ($missing -join ', '))
}

# ---------------------------------------------------------------------------
# 4. Build
# ---------------------------------------------------------------------------
$targetDir = Join-Path $projectDir 'target'
$logFile = Join-Path $targetDir 'native-build.log'
New-Item -ItemType Directory -Force -Path $targetDir | Out-Null

Push-Location $projectDir
try {
    if ($Clean) {
        Write-Step "Cleaning (mvnw clean)"
        & cmd.exe /c '.\mvnw.cmd clean 2>&1' | Tee-Object -FilePath $logFile
        if ($LASTEXITCODE -ne 0) { Fail "mvnw clean failed (exit code $LASTEXITCODE)." }
    }

    $mvnArgs = @('-Pnative', 'native:compile')
    if ($SkipTests) { $mvnArgs += '-DskipTests' }
    $mvnLine = '.\mvnw.cmd ' + ($mvnArgs -join ' ') + ' 2>&1'

    Write-Step "Building: .\mvnw.cmd $($mvnArgs -join ' ')"
    Write-Host "    Log: $logFile"
    $started = Get-Date

    # Run through cmd.exe so Maven's stderr is merged into the log without
    # PowerShell turning it into terminating error records.
    $ErrorActionPreference = 'Continue'
    & cmd.exe /c $mvnLine | Tee-Object -FilePath $logFile
    $exitCode = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
}
finally {
    Pop-Location
}

if ($exitCode -ne 0) {
    Fail "Native build failed (exit code $exitCode). See $logFile"
}

# ---------------------------------------------------------------------------
# 5. Report
# ---------------------------------------------------------------------------
$exe = Join-Path $targetDir 'backend.exe'
if (-not (Test-Path $exe)) {
    Fail "Build reported success but '$exe' was not produced."
}

$file = Get-Item $exe
$elapsed = (Get-Date) - $started

Write-Host ""
Write-Host "Native build succeeded in $([int]$elapsed.TotalMinutes)m $($elapsed.Seconds)s." -ForegroundColor Green
Write-Host "  Executable : $($file.FullName)"
Write-Host "  Size       : $([math]::Round($file.Length / 1MB, 1)) MB ($($file.Length) bytes)"
Write-Host "  Built      : $($file.LastWriteTime)"
Write-Host "  SHA256     : $((Get-FileHash $exe -Algorithm SHA256).Hash)"
Write-Host ""
