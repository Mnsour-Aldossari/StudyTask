param([switch]$Test, [switch]$UiTest, [switch]$Package)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
if (-not $env:JAVA_HOME) {
    $localJdk = Get-ChildItem 'C:\Program Files\Java' -Directory -Filter 'jdk-21*' -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
}
$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if (-not $maven) { throw 'Install Maven and JDK 21 or later, then reopen PowerShell.' }
$taskArgs = @('-Dmaven.repo.local=.maven-cache', '-ntp')
if ($UiTest) { $taskArgs += @('-Pui-test', 'test') }
elseif ($Test) { $taskArgs += 'test' }
elseif ($Package) { $taskArgs += @('package', 'dependency:copy-dependencies', '-DincludeScope=runtime', '-DoutputDirectory=target/lib') }
else { $taskArgs += @('compile', 'dependency:copy-dependencies', '-DincludeScope=runtime', '-DoutputDirectory=target/lib') }
& $maven.Source @taskArgs
if ($LASTEXITCODE -ne 0) { throw "Maven exited with code $LASTEXITCODE." }
if (-not ($Test -or $UiTest -or $Package)) {
    & (Join-Path $env:JAVA_HOME 'bin\java.exe') -cp 'target\classes;target\lib\*' studytask.Launcher
    if ($LASTEXITCODE -ne 0) { throw "StudyTask exited with code $LASTEXITCODE." }
}
