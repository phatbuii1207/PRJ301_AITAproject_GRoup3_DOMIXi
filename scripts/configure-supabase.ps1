# Windows PowerShell 5.1 / PowerShell 7. Reads .env as data; never executes its contents.
param([string]$EnvFile = (Join-Path (Split-Path $PSScriptRoot -Parent) '.env'))
$ErrorActionPreference = 'Stop'
if (-not (Test-Path -LiteralPath $EnvFile -PathType Leaf)) {
    throw 'Create .env from .env.example and fill the private database credentials first.'
}
$lines = [System.IO.File]::ReadAllLines($EnvFile, [System.Text.Encoding]::UTF8)
$values = @{}
foreach ($raw in $lines) {
    $line = $raw.Trim().TrimStart([char]0xFEFF)
    if ($line.StartsWith('export ')) { $line = $line.Substring(7).Trim() }
    if ($line -match '^([A-Za-z_][A-Za-z0-9_]*)\s*=(.*)$') {
        $value = $Matches[2].Trim()
        if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$Matches[1]] = $value
    }
}
if (-not $values['DB_URL'] -or -not $values['DB_URL'].StartsWith('jdbc:postgresql://')) {
    throw 'DB_URL must be your existing PostgreSQL JDBC URL.'
}
$uri = [System.Uri]$values['DB_URL'].Substring(5)
if (-not $uri.Host.EndsWith('.pooler.supabase.com') -or $uri.UserInfo) {
    throw 'This script only updates a Supabase shared-pooler URL. Keep credentials in DB_USER / DB_PASSWORD.'
}
if (-not $values['DB_USER'] -or $values['DB_USER'].Contains('YOUR_PROJECT_REF') -or -not $values['DB_PASSWORD']) {
    throw 'Fill your real DB_USER and DB_PASSWORD in .env first.'
}
$builder = [System.UriBuilder]$uri
$builder.Port = 6543
$parameters = New-Object 'System.Collections.Generic.List[string]'
foreach ($pair in $uri.Query.TrimStart('?').Split('&')) {
    if ($pair -and $pair -notmatch '^(?i:sslmode|prepareThreshold)=') { $parameters.Add($pair) }
}
$parameters.Add('sslmode=require')
$parameters.Add('prepareThreshold=0')
$builder.Query = [string]::Join('&', $parameters)
$values['DB_URL'] = 'jdbc:' + $builder.Uri.AbsoluteUri
$values['DB_HOST'] = $uri.Host
$values['DB_PORT'] = '6543'
$values['DB_NAME'] = [System.Uri]::UnescapeDataString($uri.AbsolutePath.TrimStart('/'))
$keys = @('DB_URL', 'DB_HOST', 'DB_PORT', 'DB_NAME')
$seen = @{}
$output = New-Object 'System.Collections.Generic.List[string]'
foreach ($line in $lines) {
    if ($line -match '^\s*(?:export\s+)?(DB_URL|DB_HOST|DB_PORT|DB_NAME)\s*=') {
        $key = $Matches[1]
        $output.Add($key + '=' + $values[$key]); $seen[$key] = $true
    } else { $output.Add($line) }
}
foreach ($key in $keys) { if (-not $seen[$key]) { $output.Add($key + '=' + $values[$key]) } }
# A backup stays in the user's private profile, never in the repository.
$backup = Join-Path ([Environment]::GetFolderPath('UserProfile')) ('.aita.env.backup-' + (Get-Date -Format 'yyyyMMdd-HHmmssfff'))
[System.IO.File]::Copy($EnvFile, $backup, $false)
$temporary = $EnvFile + '.tmp'
try {
    [System.IO.File]::WriteAllLines($temporary, $output.ToArray(), (New-Object System.Text.UTF8Encoding($false)))
    [System.IO.File]::Copy($temporary, $EnvFile, $true)
} finally { if (Test-Path -LiteralPath $temporary) { Remove-Item -LiteralPath $temporary -Force } }
Write-Output 'Updated .env: Supabase port 6543, SSL required, prepareThreshold=0. Credentials preserved. Restart Tomcat.'
