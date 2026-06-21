param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArgs = @()
)

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = Split-Path -Parent $scriptDir

$javaHomeCandidates = @()
if (-not [string]::IsNullOrWhiteSpace($env:SALT_LYRIC_JAVA_HOME)) {
    $javaHomeCandidates += $env:SALT_LYRIC_JAVA_HOME
}
if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
    $javaHomeCandidates += $env:JAVA_HOME
}
$javaHomeCandidates += @(
    (Join-Path $HOME '.jdks\temurin-21'),
    (Join-Path $env:ProgramFiles 'Eclipse Adoptium'),
    (Join-Path $env:ProgramFiles 'Java')
)

$javaHomeResolved = $false
foreach ($candidate in $javaHomeCandidates) {
    if ([string]::IsNullOrWhiteSpace($candidate)) {
        continue
    }
    $resolvedCandidates = if (Test-Path -LiteralPath $candidate -PathType Container) {
        @($candidate) + @(
            Get-ChildItem -LiteralPath $candidate -Directory -ErrorAction SilentlyContinue |
                Where-Object { $_.Name -match '(?:jdk|temurin)[-_]?21' } |
                Sort-Object Name -Descending |
                Select-Object -ExpandProperty FullName
        )
    } else {
        @()
    }
    foreach ($resolved in $resolvedCandidates) {
        if (Test-Path -LiteralPath (Join-Path $resolved 'bin\java.exe')) {
            $env:JAVA_HOME = $resolved
            $javaHomeResolved = $true
            break
        }
    }
    if ($javaHomeResolved) {
        break
    }
}
if (-not $javaHomeResolved -and
        -not [string]::IsNullOrWhiteSpace($env:JAVA_HOME) -and
        -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
    Remove-Item Env:JAVA_HOME
}

$candidates = @()
if (-not [string]::IsNullOrWhiteSpace($env:SALT_LYRIC_ASCII_DRIVE)) {
    $requestedDrive = $env:SALT_LYRIC_ASCII_DRIVE.Trim().TrimEnd('\')
    if ($requestedDrive.Length -eq 1) {
        $requestedDrive = "${requestedDrive}:"
    }
    $candidates += $requestedDrive.ToUpperInvariant()
}
$candidates += @('X:', 'Y:', 'Z:', 'W:', 'V:')

$drive = $null
foreach ($candidate in $candidates) {
    if ($candidate -notmatch '^[A-Z]:$') {
        continue
    }
    if (-not (Test-Path "${candidate}\")) {
        $drive = $candidate
        break
    }
}

if ($null -eq $drive) {
    throw 'No free drive letter is available for the temporary ASCII Gradle path.'
}

$driveRoot = "${drive}\"
$mapped = $false
$pushed = $false
$exitCode = 0

try {
    & subst $drive $repoRoot
    $mapped = $true

    if (-not (Test-Path $driveRoot)) {
        throw "Failed to create temporary ASCII Gradle path at $driveRoot"
    }

    $env:GRADLE_USER_HOME = Join-Path $driveRoot '.gradle-user-home'
    $env:SALT_LYRIC_BUILD_DIR = Join-Path $driveRoot '.gradle-local-build'

    if (($GradleArgs -notcontains '--daemon') -and ($GradleArgs -notcontains '--no-daemon')) {
        $GradleArgs += '--no-daemon'
    }

    Push-Location $driveRoot
    $pushed = $true
    & .\gradlew.bat @GradleArgs
    $exitCode = $LASTEXITCODE
} finally {
    if ($pushed) {
        Pop-Location
    }
    if ($mapped) {
        & subst $drive /d
    }
}

exit $exitCode
