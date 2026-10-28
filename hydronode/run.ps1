# Compile and run Hydronode on Windows (PowerShell). No build system needed.
# If script execution is blocked: Set-ExecutionPolicy -Scope Process Bypass

$ErrorActionPreference = 'Stop'
Set-Location -Path $PSScriptRoot

if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    Write-Host '! javac not found. Install JDK 17+ and add it to PATH.'
    exit 1
}

# UTF-8 so the box drawing and spinner glyphs render correctly.
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
if ($env:OS -eq 'Windows_NT') {
    & chcp.com 65001 > $null
}

if (Test-Path out) { Remove-Item -Recurse -Force out }
New-Item -ItemType Directory -Path out | Out-Null

$sources = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
& javac -Xlint:all -d out $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

& java "-Dstdout.encoding=UTF-8" -cp out com.hydronode.Main @args
exit $LASTEXITCODE
