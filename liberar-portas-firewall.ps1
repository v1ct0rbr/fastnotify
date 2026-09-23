#Requires -Version 5.1
<#
  FastNotify - libera portas no Firewall do Windows (regras de ENTRADA).

  Le as portas em:
    config\destino.properties   -> port       (notificacao no Destino)
    config\origem.properties    -> registerPort (auto-cadastro na Origem)

  Uso (admin):
    .\liberar-portas-firewall.ps1
    .\liberar-portas-firewall.ps1 -Acao Remove
    .\liberar-portas-firewall.ps1 -Acao Status

  Regras criadas (idempotente):
    FastNotify-Destino-Notificacao  TCP  inbound  porta do Destino
    FastNotify-Origem-Cadastro      TCP  inbound  registerPort da Origem
#>
[CmdletBinding()]
param(
    [ValidateSet('Libera', 'Remove', 'Status')]
    [string]$Acao = 'Libera',

    [string]$ConfigDir
)

$ErrorActionPreference = 'Stop'
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path

if (-not $ConfigDir) {
    $ConfigDir = Join-Path $scriptDir 'config'
}

$RuleDestino = 'FastNotify-Destino-Notificacao'
$RuleOrigem = 'FastNotify-Origem-Cadastro'

function Test-IsAdmin {
    $id = [Security.Principal.WindowsIdentity]::GetCurrent()
    $p = New-Object Security.Principal.WindowsPrincipal($id)
    return $p.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
}

function Get-PropertyValue {
    param(
        [string]$Path,
        [string]$Key,
        [string]$Default
    )
    if (-not (Test-Path -LiteralPath $Path)) {
        return $Default
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
    return $Default
}

function Get-PortaDestino {
    $file = Join-Path $ConfigDir 'destino.properties'
    $raw = Get-PropertyValue -Path $file -Key 'port' -Default '9876'
    $n = 0
    if (-not [int]::TryParse($raw, [ref]$n) -or $n -lt 1 -or $n -gt 65535) {
        return 9876
    }
    return $n
}

function Get-PortaCadastro {
    $file = Join-Path $ConfigDir 'origem.properties'
    $raw = Get-PropertyValue -Path $file -Key 'registerPort' -Default '9877'
    $n = 0
    if (-not [int]::TryParse($raw, [ref]$n) -or $n -lt 1 -or $n -gt 65535) {
        return 9877
    }
    return $n
}

function Show-Portas {
    param($PortaDestino, $PortaCadastro)
    Write-Host ''
    Write-Host 'Portas lidas da configuracao:'
    Write-Host ('  Destino (notificacao)  : {0}  -> regra {1}' -f $PortaDestino, $RuleDestino)
    Write-Host ('  Origem  (cadastro)     : {0}  -> regra {1}' -f $PortaCadastro, $RuleOrigem)
    if (-not (Test-Path -LiteralPath $ConfigDir)) {
        Write-Host ('  (pasta de config nao encontrada: {0} — usando padroes)' -f $ConfigDir)
    }
    Write-Host ''
}

function Remove-FastNotifyRules {
    foreach ($name in @($RuleDestino, $RuleOrigem)) {
        $existing = Get-NetFirewallRule -Name $name -ErrorAction SilentlyContinue
        if ($existing) {
            Remove-NetFirewallRule -Name $name -ErrorAction SilentlyContinue
            Write-Host ('  Removida regra: {0}' -f $name)
        } else {
            Write-Host ('  Regra inexistente (ok): {0}' -f $name)
        }
    }
}

function Set-InboundRule {
    param(
        [string]$Name,
        [int]$Port
    )
    $existing = Get-NetFirewallRule -Name $Name -ErrorAction SilentlyContinue
    if ($existing) {
        Remove-NetFirewallRule -Name $Name -ErrorAction SilentlyContinue
        Write-Host ('  Regra antiga removida: {0}' -f $Name)
    }

    New-NetFirewallRule -Name $Name `
        -DisplayName $Name `
        -Description ('FastNotify - entrada TCP porta {0} (liberado por liberar-portas-firewall.ps1)' -f $Port) `
        -Direction Inbound `
        -Action Allow `
        -Protocol TCP `
        -LocalPort $Port `
        -Profile Any `
        -Enabled True | Out-Null

    Write-Host ('  Criada/ativa: {0}  (TCP entrada, porta {1})' -f $Name, $Port)
}

function Get-RuleStatus {
    param([string]$Name, [int]$Port)
    $rule = Get-NetFirewallRule -Name $Name -ErrorAction SilentlyContinue
    if (-not $rule) {
        return [pscustomobject]@{
            Name = $Name; Port = $Port; Exists = $false
            Enabled = $false; Action = '-'; Direction = '-'
        }
    }
    return [pscustomobject]@{
        Name = $Name; Port = $Port; Exists = $true
        Enabled = ($rule.Enabled -eq 'True')
        Action = [string]$rule.Action
        Direction = [string]$rule.Direction
    }
}

$portaDestino = Get-PortaDestino
$portaCadastro = Get-PortaCadastro
Show-Portas -PortaDestino $portaDestino -PortaCadastro $portaCadastro

switch ($Acao) {
    'Status' {
        Write-Host 'Status das regras FastNotify:'
        foreach ($item in @(
                (Get-RuleStatus -Name $RuleDestino -Port $portaDestino),
                (Get-RuleStatus -Name $RuleOrigem -Port $portaCadastro)
            )) {
            if (-not $item.Exists) {
                Write-Host ('  [AUSENTE] {0}  porta {1}' -f $item.Name, $item.Port)
            } elseif ($item.Enabled -and $item.Action -eq 'Allow') {
                Write-Host ('  [OK]      {0}  porta {1}  Allow/enabled' -f $item.Name, $item.Port)
            } else {
                Write-Host ('  [FECHADA] {0}  porta {1}  {2}/{3}' -f $item.Name, $item.Port, $item.Action, $item.Enabled)
            }
        }
        Write-Host ''
        Write-Host 'Dica: para liberar, rode de novo com -Acao Libera (ou liberar-portas-firewall.bat).'
        exit 0
    }
    'Remove' {
        if (-not (Test-IsAdmin)) {
            Write-Error 'Remocao de regras exige PowerShell como Administrador. Use remover-portas-firewall.bat.'
            exit 1
        }
        Write-Host 'Removendo regras FastNotify...'
        Remove-FastNotifyRules
        Write-Host 'Pronto.'
        exit 0
    }
    default {
        if (-not (Test-IsAdmin)) {
            Write-Error 'Liberacao de portas exige PowerShell como Administrador. Use liberar-portas-firewall.bat.'
            exit 1
        }
        Write-Host 'Liberando portas no Firewall do Windows (entrada)...'
        Set-InboundRule -Name $RuleDestino -Port $portaDestino
        Set-InboundRule -Name $RuleOrigem -Port $portaCadastro
        Write-Host ''
        Write-Host 'Pronto. Verifique com:  .\liberar-portas-firewall.ps1 -Acao Status'
        exit 0
    }
}
