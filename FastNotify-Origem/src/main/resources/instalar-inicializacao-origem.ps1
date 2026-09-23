#Requires -Version 5.1
$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$runKey = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Run'
$startupDir = [Environment]::GetFolderPath('Startup')
$Nome = 'FastNotify-Origem'
$Arquivo = 'FastNotify-Origem.exe'

function Find-AppExe {
    $local = Join-Path $scriptDir $Arquivo
    if (Test-Path -LiteralPath $local) {
        return (Resolve-Path -LiteralPath $local).Path
    }

    $dir = $scriptDir
    for ($i = 0; $i -lt 8 -and -not [string]::IsNullOrEmpty($dir); $i++) {
        foreach ($rel in @($Arquivo, (Join-Path 'target' $Arquivo))) {
            $p = Join-Path $dir $rel
            if (Test-Path -LiteralPath $p) {
                return (Resolve-Path -LiteralPath $p).Path
            }
        }
        $parent = Split-Path -Parent $dir
        if ([string]::IsNullOrEmpty($parent) -or $parent -eq $dir) {
            break
        }
        $dir = $parent
    }

    $bases = @($scriptDir)
    $parentDir = Split-Path -Parent $scriptDir
    if (-not [string]::IsNullOrEmpty($parentDir)) {
        $bases += $parentDir
    }
    foreach ($base in $bases) {
        $hit = Get-ChildItem -LiteralPath $base -Recurse -Filter $Arquivo -File -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($hit) {
            return $hit.FullName
        }
    }
    return $null
}

function Remove-OldEntries {
    $had = $false
    if (Test-Path -LiteralPath $runKey) {
        $prop = Get-ItemProperty -LiteralPath $runKey -ErrorAction SilentlyContinue
        if ($prop -and $prop.PSObject.Properties.Name -contains $Nome) {
            Remove-ItemProperty -LiteralPath $runKey -Name $Nome -Force
            Write-Host ("  Consulta: entrada JA EXISTE em HKCU Run\{0} -> sera atualizada." -f $Nome)
            $had = $true
        }
    }

    Get-ChildItem -LiteralPath $startupDir -Filter '*.lnk' -ErrorAction SilentlyContinue |
        Where-Object { $_.BaseName -eq $Nome -or $_.BaseName -like ("{0}*" -f $Nome) } |
        ForEach-Object {
            Remove-Item -LiteralPath $_.FullName -Force
            Write-Host ("  Removido atalho antigo: {0}" -f $_.FullName)
            $had = $true
        }

    $ps1 = Join-Path $startupDir ("{0}.ps1" -f $Nome)
    if (Test-Path -LiteralPath $ps1) {
        Remove-Item -LiteralPath $ps1 -Force
        Write-Host ("  Removido script antigo: {0}" -f $ps1)
        $had = $true
    }
    if (-not $had) {
        Write-Host ("  Consulta: entrada NAO EXISTE em HKCU Run\{0} -> sera criada." -f $Nome)
    }
}

function Add-StartupEntry {
    param([string]$ExePath)

    if (-not (Test-Path -LiteralPath $ExePath)) {
        throw ("Executavel nao encontrado: {0}" -f $ExePath)
    }

    New-ItemProperty -Path $runKey -Name $Nome -Value ('"{0}"' -f $ExePath) `
        -PropertyType String -Force | Out-Null
    Write-Host ("  Criada entrada HKCU\...\Run\{0} -> {1}" -f $Nome, $ExePath)
}

Write-Host 'FastNotify Origem - inicializacao com o Windows (usuario atual)'
Write-Host ("Pasta base: {0}" -f $scriptDir)
Write-Host ''

$exe = Find-AppExe
if (-not $exe) {
    Write-Host ("Nao encontrado ({0}) - rode mvn package em FastNotify-Origem (o exe fica em target\)." -f $Arquivo) -ForegroundColor Yellow
    exit 1
}

Remove-OldEntries
Add-StartupEntry -ExePath $exe

Write-Host ''
Write-Host ("Concluido. Vale para o usuario atual ({0})." -f (whoami))
Write-Host ("Entradas em: {0}" -f $runKey)
Write-Host 'Para remover: apague a chave FastNotify-Origem no Editor do Registro ou o atalho em:'
Write-Host ("  {0}" -f $startupDir)
exit 0
