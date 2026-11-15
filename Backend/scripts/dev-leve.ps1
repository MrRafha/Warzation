<#
.SYNOPSIS
  Sobe o Backend consumindo pouca memória (perfil dev-leve).

.DESCRIPTION
  Compila o jar (a menos que -SkipBuild) e roda uma única JVM limitada, em vez
  de "mvn spring-boot:run" (que mantém o Maven e o Backend abertos ao mesmo
  tempo). A conexão do banco vem de application-local.yml ou das variáveis
  DB_URL, DB_USERNAME e DB_PASSWORD; veja Docs/04_GUIA_DESENVOLVIMENTO_LOCAL.md.

.EXAMPLE
  .\scripts\dev-leve.ps1
  .\scripts\dev-leve.ps1 -SkipBuild
  .\scripts\dev-leve.ps1 -MaxMemory 512m
#>
param(
    [switch]$SkipBuild,
    [string]$MaxMemory = "384m"
)

$ErrorActionPreference = "Stop"
$backend = Split-Path -Parent $PSScriptRoot
Push-Location $backend
try {
    $localConfig = Join-Path $backend "src\main\resources\application-local.yml"
    if (-not (Test-Path $localConfig) -and -not $env:DB_URL) {
        Write-Warning "Sem application-local.yml e sem DB_URL: o Backend tentará o PostgreSQL local padrão."
    }

    if (-not $SkipBuild) {
        Write-Host "Compilando o Backend (sem testes)..."
        mvn -q -DskipTests package
        if ($LASTEXITCODE -ne 0) { throw "Falha na compilação" }
    }

    $jar = Get-ChildItem (Join-Path $backend "target\*.jar") |
        Where-Object { $_.Name -notlike "*-plain.jar" } |
        Select-Object -First 1
    if (-not $jar) { throw "Jar não encontrado em target\. Rode sem -SkipBuild." }

    Write-Host "Subindo $($jar.Name) com até $MaxMemory de heap (perfis local,dev-leve)..."
    java "-Xmx$MaxMemory" -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 `
        -jar $jar.FullName --spring.profiles.active=local,dev-leve
}
finally {
    Pop-Location
}
