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

function Remove-StartupEntries {
    $removed = $false

    if (Test-Path -LiteralPath $runKey) {
        $prop = Get-ItemProperty -LiteralPath $runKey -ErrorAction SilentlyContinue
        if ($prop -and $prop.PSObject.Properties.Name -contains $Nome) {
            Remove-ItemProperty -LiteralPath $runKey -Name $Nome -Force
            Write-Host ("  Consulta: entrada JA EXISTE em HKCU Run\{0} -> removida." -f $Nome)
            $removed = $true
        } else {
            Write-Host ("  Consulta: entrada NAO EXISTE em HKCU Run\{0}." -f $Nome)
        }
    }

    Get-ChildItem -LiteralPath $startupDir -Filter '*.lnk' -ErrorAction SilentlyContinue |
        Where-Object { $_.BaseName -eq $Nome -or $_.BaseName -like ("{0}*" -f $Nome) } |
        ForEach-Object {
            Remove-Item -LiteralPath $_.FullName -Force
            Write-Host ("  Removido atalho: {0}" -f $_.FullName)
            $removed = $true
        }

    $ps1 = Join-Path $startupDir ("{0}.ps1" -f $Nome)
    if (Test-Path -LiteralPath $ps1) {
        Remove-Item -LiteralPath $ps1 -Force
        Write-Host ("  Removido script: {0}" -f $ps1)
        $removed = $true
    }

    if (-not $removed) {
        Write-Host '  Nenhuma entrada de inicializacao encontrada (ok).'
    }
}

Write-Host 'FastNotify Origem - remover inicializacao com o Windows'
Write-Host ("Pasta base: {0}" -f $scriptDir)
Write-Host ''
Remove-StartupEntries
Write-Host ''
Write-Host 'Concluido.'
exit 0
