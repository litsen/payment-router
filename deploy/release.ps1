param(
    [string]$ProjectRoot = "",
    [string]$Remote = "origin",
    [string]$Branch = "",
    [string]$TargetRef = "",
    [string]$HealthUrl = "http://localhost/api/health",
    [string[]]$Services = @("backend", "frontend", "nginx"),
    [int]$HealthRetries = 30,
    [int]$HealthDelaySeconds = 3,
    [switch]$SkipGitPull,
    [switch]$SkipBackup,
    [switch]$NoBuild
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

if (-not $ProjectRoot) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
}

$DeployDir = $PSScriptRoot
$PreviousCommit = ""
$PreviousBranch = ""
$BackupFile = ""

function Invoke-CheckedNative {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments
    )

    Write-Host "> $FilePath $($Arguments -join ' ')"
    & $FilePath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE`: $FilePath $($Arguments -join ' ')"
    }
}

function Get-GitOutput {
    param([Parameter(Mandatory = $true)][string[]]$Arguments)

    $output = & git @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Git command failed: git $($Arguments -join ' ')"
    }
    return ($output -join "`n").Trim()
}

function Get-ObjectProperty {
    param(
        [AllowNull()][object]$InputObject,
        [Parameter(Mandatory = $true)][string]$Name
    )

    if ($null -eq $InputObject) {
        return $null
    }

    $property = $InputObject.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}

function Backup-Database {
    $backupDir = Join-Path $DeployDir "backups"
    New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

    $stamp = Get-Date -Format "yyyyMMddHHmmss"
    $name = "payment-router-$stamp.sql"
    $script:BackupFile = Join-Path $backupDir $name
    $tmpFile = "/tmp/$name"

    $dumpCommand = "mysqldump --default-character-set=utf8mb4 -uroot -p`"`$MYSQL_ROOT_PASSWORD`" `"`$MYSQL_DATABASE`" > $tmpFile"
    Invoke-CheckedNative "docker" @("compose", "exec", "-T", "mysql", "sh", "-c", $dumpCommand)
    Invoke-CheckedNative "docker" @("cp", "payment-router-mysql:$tmpFile", $script:BackupFile)
    Invoke-CheckedNative "docker" @("compose", "exec", "-T", "mysql", "sh", "-c", "rm -f $tmpFile")

    Write-Host "Database backup: $script:BackupFile"
}

function Update-Code {
    if ($SkipGitPull) {
        Write-Host "Skip git update."
        return
    }

    if ($TargetRef) {
        Invoke-CheckedNative "git" @("fetch", "--tags", $Remote)
        Invoke-CheckedNative "git" @("checkout", $TargetRef)
        return
    }

    if ($Branch) {
        Invoke-CheckedNative "git" @("fetch", $Remote, $Branch)
        Invoke-CheckedNative "git" @("checkout", $Branch)
        Invoke-CheckedNative "git" @("pull", "--ff-only", $Remote, $Branch)
        return
    }

    Invoke-CheckedNative "git" @("pull", "--ff-only")
}

function Deploy-Services {
    $args = @("compose", "up", "-d")
    if (-not $NoBuild) {
        $args += "--build"
    }
    $args += $Services
    Invoke-CheckedNative "docker" $args
}

function Test-Health {
    for ($i = 1; $i -le $HealthRetries; $i++) {
        try {
            $response = Invoke-RestMethod -Uri $HealthUrl -TimeoutSec 5
            $code = Get-ObjectProperty $response "code"
            $data = Get-ObjectProperty $response "data"
            $status = Get-ObjectProperty $data "status"
            $mysql = Get-ObjectProperty $data "mysql"
            $redis = Get-ObjectProperty $data "redis"

            if (($code -eq 0 -or $null -eq $code) -and ($status -eq "UP" -or $null -eq $status)) {
                Write-Host "Health check passed: $HealthUrl"
                if ($mysql -or $redis) {
                    Write-Host "Dependencies: mysql=$mysql redis=$redis"
                }
                return
            }
        } catch {
            Write-Host "Health check attempt $i/$HealthRetries failed: $($_.Exception.Message)"
        }

        Start-Sleep -Seconds $HealthDelaySeconds
    }

    throw "Health check failed after $HealthRetries attempts: $HealthUrl"
}

Push-Location $ProjectRoot
try {
    $PreviousCommit = Get-GitOutput @("rev-parse", "HEAD")
    $PreviousBranch = Get-GitOutput @("rev-parse", "--abbrev-ref", "HEAD")
    Write-Host "Current git ref: $PreviousBranch $PreviousCommit"

    Push-Location $DeployDir
    try {
        if (-not $SkipBackup) {
            Backup-Database
        } else {
            Write-Host "Skip database backup."
        }
    } finally {
        Pop-Location
    }

    Update-Code

    Push-Location $DeployDir
    try {
        Deploy-Services
        Test-Health
    } finally {
        Pop-Location
    }

    Write-Host "Release completed."
} catch {
    Write-Error $_
    Write-Host ""
    Write-Host "Rollback commands:"
    Write-Host "cd `"$ProjectRoot`""
    if ($PreviousCommit) {
        Write-Host "git checkout $PreviousCommit"
    }
    Write-Host "cd `"$DeployDir`""
    Write-Host "docker compose up -d --build $($Services -join ' ')"
    if ($BackupFile) {
        Write-Host "Database backup created before release: $BackupFile"
    }
    exit 1
} finally {
    Pop-Location
}
