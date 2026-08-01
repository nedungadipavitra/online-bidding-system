param(
    [int]$StartupDelaySeconds = 15
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location -LiteralPath $repoRoot

function ConvertTo-PowerShellLiteral {
    param([Parameter(Mandatory = $true)][string]$Value)

    return "'" + $Value.Replace("'", "''") + "'"
}

$envFile = Join-Path $repoRoot ".env"
if (-not (Test-Path -LiteralPath $envFile)) {
    throw "Missing .env at $envFile. Copy .env.example to .env and fill in the required values first."
}

Get-Content -LiteralPath $envFile | ForEach-Object {
    $line = $_.Trim()

    if ($line -and -not $line.StartsWith("#")) {
        $parts = $line -split "=", 2
        if ($parts.Count -eq 2) {
            $name = $parts[0].Trim()
            $value = $parts[1].Trim().Trim('"').Trim("'")

            if ($name -match "^[A-Za-z_][A-Za-z0-9_]*$") {
                Set-Item -Path "Env:$name" -Value $value
            }
        }
    }
}

$requiredVariables = @(
    "DB_PASSWORD",
    "JWT_SECRET",
    "WALLET_INTERNAL_TOKEN"
)

$missingVariables = @(
    $requiredVariables | Where-Object {
        [string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($_, "Process"))
    }
)

if ($missingVariables.Count -gt 0) {
    throw "Missing required variables in .env: $($missingVariables -join ', ')"
}

$mavenPath = $null
$mavenCommand = Get-Command mvn.cmd -ErrorAction SilentlyContinue

if ($mavenCommand) {
    $mavenPath = $mavenCommand.Source
}

if (-not $mavenPath -and $env:MAVEN_HOME) {
    $candidate = Join-Path $env:MAVEN_HOME "bin\mvn.cmd"
    if (Test-Path -LiteralPath $candidate) {
        $mavenPath = $candidate
    }
}

if (-not $mavenPath) {
    $temporaryMaven = Join-Path $env:LOCALAPPDATA "Temp\online-bidding-platform-maven\apache-maven-3.9.16\bin\mvn.cmd"
    if (Test-Path -LiteralPath $temporaryMaven) {
        $mavenPath = $temporaryMaven
    }
}

if (-not $mavenPath) {
    throw "Maven was not found. Install Maven or set MAVEN_HOME before running this script."
}

$services = @(
    @{ Name = "user-service"; Pom = "backend\user-service\pom.xml" },
    @{ Name = "product-service"; Pom = "backend\product-service\pom.xml" },
    @{ Name = "wallet-service"; Pom = "backend\wallet-service\pom.xml" },
    @{ Name = "order-service"; Pom = "backend\order-service\pom.xml" },
    @{ Name = "bid-service"; Pom = "backend\bid-service\pom.xml" }
)

foreach ($service in $services) {
    $pomPath = Join-Path $repoRoot $service.Pom
    if (-not (Test-Path -LiteralPath $pomPath)) {
        throw "Missing Maven project file: $pomPath"
    }

    $childCommand = "Set-Location -LiteralPath $(ConvertTo-PowerShellLiteral $repoRoot); & $(ConvertTo-PowerShellLiteral $mavenPath) -f $(ConvertTo-PowerShellLiteral $pomPath) spring-boot:run"

    Start-Process powershell.exe `
        -WorkingDirectory $repoRoot `
        -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $childCommand) | Out-Null

    Write-Host "Started $($service.Name)"
}

Write-Host "Waiting $StartupDelaySeconds seconds before starting the gateway..."
Start-Sleep -Seconds $StartupDelaySeconds

$gatewayPom = Join-Path $repoRoot "backend\api-gateway\pom.xml"
$gatewayCommand = "Set-Location -LiteralPath $(ConvertTo-PowerShellLiteral $repoRoot); & $(ConvertTo-PowerShellLiteral $mavenPath) -f $(ConvertTo-PowerShellLiteral $gatewayPom) spring-boot:run"

Start-Process powershell.exe `
    -WorkingDirectory $repoRoot `
    -ArgumentList @("-NoExit", "-ExecutionPolicy", "Bypass", "-Command", $gatewayCommand) | Out-Null

Write-Host "Started api-gateway"
Write-Host "Gateway: http://localhost:8080"
