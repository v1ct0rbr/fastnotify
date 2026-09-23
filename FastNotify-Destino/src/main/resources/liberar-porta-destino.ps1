#Requires -Version 5.1
<#
  FastNotify Destino - libera/remove a regra de ENTRADA da porta de notificacao.
  A porta e lida apenas de config\destino.properties (chave port).

  Uso (admin):
    .\liberar-porta-destino.ps1
    .\liberar-porta-destino.ps1 -Acao Remove
    .\liberar-porta-destino.ps1 -Acao Status

  Regra: FastNotify-Destino-Notificacao (TCP inbound)
#>
[CmdletBinding()]
param(
    [ValidateSet('Libera', 'Remove', 'Status')]
    [string]$Acao = 'Libera',

    [string]$ConfigDir
)

$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RuleName = 'FastNotify-Destino-Notificacao'
$RuleLabel = 'FastNotify - firewall porta notificacao Destino'
$ConfigFile = 'destino.properties'
$ConfigKey = 'port'

function Test-IsAdmin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $p = New-Object Security.Principal.WindowsPrincipal($id)
    return $p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Get-PropertyValue {
    param([string]$Path, [string]$Key)
    if (-not (Test-Path -LiteralPath $Path)) {
        return $null
    }
    foreach ($line in Get-Content -LiteralPath $Path -ErrorAction SilentlyContinue) {
        if ($line -match '^\s*#' -or $line -notmatch '=') { continue }
        $parts = $line -split '=', 2
        if ($parts.Count -lt 2) { continue }
        if ($parts[0].Trim() -eq $Key) {
            $value = $parts[1].Trim()
            if ($value -ne '') { return $value }
        }
    }
    return $null
}

function Find-ConfigFile {
    $name = $ConfigFile
    $candidates = @()
    if ($ConfigDir) {
        $candidates += (Join-Path $ConfigDir $name)
    }
    $dir = $scriptDir
    for ($i = 0; $i -lt 6 -and -not [string]::IsNullOrEmpty($dir); $i++) {
        $candidates += (Join-Path $dir $name)
        $candidates += (Join-Path (Join-Path $dir 'config') $name)
        $candidates += (Join-Path (Join-Path (Join-Path $dir 'target') 'config') $name)
        $parent = Split-Path -Parent $dir
        if ([string]::IsNullOrEmpty($parent) -or $parent -eq $dir) { break }
        $dir = $parent
    }
    $candidates += (Join-Path (Join-Path $PWD.Path 'config') $name)
    foreach ($p in $candidates) {
        if (Test-Path -LiteralPath $p) {
            return (Resolve-Path -LiteralPath $p).Path
        }
    }
    return $null
}

function Get-ConfiguredPort {
    $file = Find-ConfigFile
    if (-not $file) {
        throw ("Arquivo de configuracao nao encontrado: {0}" -f $ConfigFile)
    }
    $raw = Get-PropertyValue -Path $file -Key $ConfigKey
    if ([string]::IsNullOrEmpty($raw)) {
        throw ("Chave '{0}' ausente em {1}" -f $ConfigKey, $file)
    }
    $n = 0
    if (-not [int]::TryParse($raw, [ref]$n) -or $n -lt 1 -or $n -gt 65535) {
        throw ("Porta invalida em {0}: {1}" -f $file, $raw)
    }
    Write-Host ('  Config : {0}' -f $file)
    return $n
}

function Set-InboundRule {
    param([string]$Name, [int]$LocalPort, [string]$Description)
    $existing = Get-NetFirewallRule -Name $Name -ErrorAction SilentlyContinue
    if ($existing) {
        Write-Host ('  Consulta: regra JA EXISTE -> {0}' -f $Name)
        Write-Host '  Atualizando regra existente...'
        Remove-NetFirewallRule -Name $Name -ErrorAction SilentlyContinue
    } else {
        Write-Host ('  Consulta: regra NAO EXISTE -> {0}' -f $Name)
        Write-Host '  Criando nova regra...'
    }
    New-NetFirewallRule -Name $Name `
        -DisplayName $Name `
        -Description $Description `
        -Direction Inbound `
        -Action Allow `
        -Protocol TCP `
        -LocalPort $LocalPort `
        -Profile Any `
        -Enabled True | Out-Null
    Write-Host ('  OK: {0}  (TCP entrada, porta {1})' -f $Name, $LocalPort)
}

function Get-RuleStatus {
    param([string]$Name, [int]$LocalPort)
    $rule = Get-NetFirewallRule -Name $Name -ErrorAction SilentlyContinue
    if (-not $rule) {
        Write-Host ('  [AUSENTE] {0}  porta {1}' -f $Name, $LocalPort)
        return
    }
    if ($rule.Enabled -eq 'True' -and [string]$rule.Action -eq 'Allow') {
        Write-Host ('  [OK]      {0}  porta {1}  Allow/enabled' -f $Name, $LocalPort)
    } else {
        Write-Host ('  [FECHADA] {0}  porta {1}  {2}/{3}' -f $Name, $LocalPort, [string]$rule.Action, $rule.Enabled)
    }
}

try {
    $portaUsada = Get-ConfiguredPort
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
}

Write-Host 'FastNotify Destino - firewall (porta de notificacao)'
Write-Host ('  Regra : {0}' -f $RuleName)
Write-Host ('  Porta : {0}' -f $portaUsada)
Write-Host ('  Pasta : {0}' -f $scriptDir)
Write-Host ''

switch ($Acao) {
    'Status' {
        Write-Host 'Status da regra:'
        Get-RuleStatus -Name $RuleName -LocalPort $portaUsada
        exit 0
    }
    'Remove' {
        if (-not (Test-IsAdmin)) {
            Write-Error 'Remocao exige PowerShell como Administrador.'
            exit 1
        }
        $existing = Get-NetFirewallRule -Name $RuleName -ErrorAction SilentlyContinue
        if ($existing) {
            Write-Host ('  Consulta: regra JA EXISTE -> {0}' -f $RuleName)
            Remove-NetFirewallRule -Name $RuleName -ErrorAction SilentlyContinue
            Write-Host '  Removida.'
        } else {
            Write-Host ('  Consulta: regra NAO EXISTE -> {0}' -f $RuleName)
            Write-Host '  Nada a remover (ok).'
        }
        Write-Host 'Pronto.'
        exit 0
    }
    default {
        if (-not (Test-IsAdmin)) {
            Write-Error 'Liberacao exige PowerShell como Administrador.'
            exit 1
        }
        Write-Host 'Consultando regra no firewall...'
        Set-InboundRule -Name $RuleName -LocalPort $portaUsada `
            -Description ('{0} - TCP inbound porta {1}' -f $RuleLabel, $portaUsada)
        Write-Host 'Pronto.'
        exit 0
    }
}
