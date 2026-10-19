$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $deliveryJavac = 'javac.exe'
    $deliveryJava = 'java.exe'
    if ($env:JAVA_HOME) {
        $deliveryJavac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
        $deliveryJava = Join-Path $env:JAVA_HOME 'bin/java.exe'
    }
    New-Item -ItemType Directory -Path 'target/classes' -Force | Out-Null
    $deliverySources = @(Get-ChildItem -LiteralPath 'src/main/java' -Filter '*.java' -Recurse |
        ForEach-Object { $_.FullName })
    & $deliveryJavac --release 17 -encoding UTF-8 -d 'target/classes' @deliverySources
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed. Use JDK 17 or newer.' }
    & $deliveryJava -cp 'target/classes' edu.assignment.delivery.Main
    if ($LASTEXITCODE -ne 0) { throw 'Demo failed.' }
} finally {
    Pop-Location
}
