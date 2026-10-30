#requires -Version 7.0
[CmdletBinding()]
param([switch] $Build, [switch] $Demo)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    $directory = Join-Path $root '.local'
    New-Item -ItemType Directory -Force -Path $directory | Out-Null
    $environment = Join-Path $directory 'fullstack.env'
    if (-not (Test-Path -LiteralPath $environment)) {
        $db = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(36))
        $jwt = [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(48))
        [IO.File]::WriteAllText($environment, "POSTGRES_PASSWORD=$db`nJWT_SECRET=$jwt`nAPP_PORT=18080`nWEB_PORT=18081`nDB_PORT=15432`nSESSION_COOKIE_SECURE=false`n")
    }
    docker info --format '{{.ServerVersion}}' *> $null
    if ($LASTEXITCODE -ne 0) {
        docker desktop start --detach
        if ($LASTEXITCODE -ne 0) { throw 'Start a Docker-compatible engine, then run this script again.' }
        $deadline = [DateTimeOffset]::UtcNow.AddSeconds(60)
        do {
            Start-Sleep -Seconds 2
            docker info --format '{{.ServerVersion}}' *> $null
            $ready = $LASTEXITCODE -eq 0
        } while (-not $ready -and [DateTimeOffset]::UtcNow -lt $deadline)
        if (-not $ready) { throw 'Docker did not become ready within 60 seconds.' }
    }
    $arguments = @('compose', '--env-file', $environment, '-p', 'ledgerflow-demo-v02', 'up', '-d', '--wait', '--wait-timeout', '180')
    if ($Build) { $arguments += '--build' }
    & docker @arguments
    if ($LASTEXITCODE -ne 0) { throw 'Full stack startup failed; inspect docker compose logs.' }
    if ($Demo) {
        # This fixture option only adds a fresh synthetic identity; existing data is preserved.
        node scripts/seed-demo.mjs --base-url http://127.0.0.1:18081 --historical --project ledgerflow-demo-v02 --env-file $environment
        if ($LASTEXITCODE -ne 0) { throw 'Demo creation failed.' }
    }
    Write-Output 'Frontend: http://localhost:18081 | API: http://localhost:18080/swagger-ui.html'
    Write-Output 'Containers remain running. Private demo credentials, if requested: .local/demo/latest.json'
} finally { Pop-Location }
