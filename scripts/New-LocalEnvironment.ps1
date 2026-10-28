[CmdletBinding()]
param(
    [string] $Path = (Join-Path (Split-Path -Parent $PSScriptRoot) '.env')
)

$resolvedPath = [System.IO.Path]::GetFullPath($Path)
if ([System.IO.File]::Exists($resolvedPath) -or [System.IO.Directory]::Exists($resolvedPath)) {
    throw "Refusing to overwrite existing path: $resolvedPath"
}

$parent = [System.IO.Path]::GetDirectoryName($resolvedPath)
if (-not [System.IO.Directory]::Exists($parent)) {
    [System.IO.Directory]::CreateDirectory($parent) | Out-Null
}

$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$dbBytes = [byte[]]::new(36)
$jwtBytes = [byte[]]::new(48)
$rng.GetBytes($dbBytes)
$rng.GetBytes($jwtBytes)
$rng.Dispose()
$dbSecret = [Convert]::ToBase64String($dbBytes)
$jwtSecret = [Convert]::ToBase64String($jwtBytes)
$contents = "POSTGRES_PASSWORD=$dbSecret`nJWT_SECRET=$jwtSecret`nAPP_PORT=8080`n"

# CreateNew guarantees a concurrent invocation cannot replace an existing environment file.
$stream = [System.IO.File]::Open($resolvedPath, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::Write, [System.IO.FileShare]::None)
try {
    $writer = [System.IO.StreamWriter]::new($stream, [System.Text.UTF8Encoding]::new($false))
    try { $writer.Write($contents) } finally { $writer.Dispose() }
} finally {
    if ($stream) { $stream.Dispose() }
}
Write-Output "Created local environment file: $resolvedPath"
