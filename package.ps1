param([string]$Destination = (Join-Path $env:LOCALAPPDATA 'StudyTask-portable'))
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not $env:JAVA_HOME) {
    $localJdk = Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
}
if (-not $env:JAVA_HOME) { throw 'Set JAVA_HOME to JDK 21 or later.' }
$jar = Join-Path $PSScriptRoot 'target\studytask-1.0.0.jar'
$lib = Join-Path $PSScriptRoot 'target\lib'
if (-not (Test-Path -LiteralPath $jar) -or -not (Test-Path -LiteralPath $lib)) { throw 'Run .\run.ps1 -Package first.' }
$target = [System.IO.Path]::GetFullPath($Destination, $PSScriptRoot)
if ($target -match '[^\x00-\x7F]') { throw 'Choose an English-only destination path for the Windows runtime.' }
if (Test-Path -LiteralPath $target) { throw "Destination already exists: $target. Choose a new -Destination to preserve the old build." }
# Stage in an ASCII-only temp path; JDK native tools can misdecode Arabic paths.
$stage = Join-Path ([System.IO.Path]::GetTempPath()) ('studytask-link-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $stage -Force | Out-Null
$runtime = Join-Path $stage 'runtime'
& (Join-Path $env:JAVA_HOME 'bin\jlink.exe') --add-modules java.base,java.desktop,java.logging,java.xml,jdk.unsupported --strip-debug --no-header-files --no-man-pages --compress=zip-6 --output $runtime
if ($LASTEXITCODE -ne 0) { throw "jlink failed with code $LASTEXITCODE. Staging: $stage" }
New-Item -ItemType Directory -Path $target -Force | Out-Null
Copy-Item -LiteralPath $jar -Destination $target
Copy-Item -LiteralPath $lib -Destination $target -Recurse
Copy-Item -LiteralPath $runtime -Destination $target -Recurse
$launcher = Join-Path $target 'Start-StudyTask.cmd'
Set-Content -LiteralPath $launcher -Encoding Ascii -Value '@echo off', '"%~dp0runtime\bin\javaw.exe" -cp "%~dp0studytask-1.0.0.jar;%~dp0lib\*" studytask.Launcher'
$archive = Join-Path $PSScriptRoot 'StudyTask-portable.zip'
Compress-Archive -LiteralPath $target -DestinationPath $archive -Force
Write-Output $launcher
Write-Output $archive
