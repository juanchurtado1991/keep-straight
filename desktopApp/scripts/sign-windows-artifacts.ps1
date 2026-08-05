# Sign Windows EXE/MSI artifacts with Authenticode (CI or local).

param(
    [Parameter(Mandatory = $true)]
    [string]$DistDir,

    [Parameter(Mandatory = $true)]
    [string]$PfxPath,

    [Parameter(Mandatory = $true)]
    [string]$PfxPassword
)

$ErrorActionPreference = "Stop"

function Find-SignTool {
    $kitsRoot = "${env:ProgramFiles(x86)}\Windows Kits\10\bin"
    if (-not (Test-Path $kitsRoot)) {
        throw "Windows SDK not found at $kitsRoot"
    }
    $latest = Get-ChildItem $kitsRoot -Directory | Sort-Object Name -Descending | Select-Object -First 1
    $candidate = Join-Path $latest.FullName "x64\signtool.exe"
    if (-not (Test-Path $candidate)) {
        throw "signtool.exe not found under $candidate"
    }
    return $candidate
}

$signTool = Find-SignTool
$files = Get-ChildItem -Path $DistDir -Include *.exe,*.msi -Recurse -File
if ($files.Count -eq 0) {
    Write-Host "No .exe/.msi files under $DistDir — nothing to sign."
    exit 0
}

foreach ($file in $files) {
    Write-Host "Signing $($file.FullName)"
    & $signTool sign `
        /fd SHA256 `
        /f $PfxPath `
        /p $PfxPassword `
        /tr http://timestamp.digicert.com `
        /td SHA256 `
        $file.FullName
    if ($LASTEXITCODE -ne 0) {
        throw "signtool failed for $($file.FullName) with exit code $LASTEXITCODE"
    }
}

Write-Host "Signed $($files.Count) installer(s)."
