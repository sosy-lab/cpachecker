# 实验一：witness-based 初始精度 vs 空精度对比测试
# PowerShell script for E1 testing

param(
    [string]$TestProgram = "test\programs\simple\loop1.c",
    [string]$Property = "config\properties\unreach-label.prp"
)

Write-Host "=== 实验一：witness-based 初始精度 vs 空精度对比 ===" -ForegroundColor Green
Write-Host "测试程序: $TestProgram"
Write-Host "属性文件: $Property"
Write-Host ""

# 确保在正确的目录
if (!(Test-Path "bin\cpachecker.bat")) {
    Write-Error "请确保在 CPAchecker 根目录下运行此脚本"
    exit 1
}

# 创建结果目录
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultDir = "experiment1-results-$timestamp"
New-Item -ItemType Directory -Path $resultDir -Force | Out-Null

Write-Host "=== 步骤 1: 生成 witness 文件 ===" -ForegroundColor Yellow
Remove-Item -Recurse -Force output\* -ErrorAction SilentlyContinue
.\bin\cpachecker.bat --predicateAnalysis --spec $Property $TestProgram
if ($LASTEXITCODE -eq 0) {
    Copy-Item -Recurse output\ "$resultDir\witness-generation\"
    Copy-Item output\witness-2.0.yml "$resultDir\witness-backup.yml"
    Write-Host "Witness 生成成功" -ForegroundColor Green
} else {
    Write-Warning "Witness 生成失败"
    exit 1
}

Write-Host ""
Write-Host "=== 步骤 2: 测试空精度配置 ===" -ForegroundColor Yellow
Remove-Item -Recurse -Force output\* -ErrorAction SilentlyContinue
.\bin\cpachecker.bat --valueAnalysis --spec $Property --option cpa.value.ValueAnalysisCPAStatistics.precisionFile=valPrec-empty.txt $TestProgram
if ($LASTEXITCODE -eq 0) {
    Copy-Item -Recurse output\ "$resultDir\empty-precision\"
    if (Test-Path "valPrec-empty.txt") {
        Copy-Item "valPrec-empty.txt" "$resultDir\"
    }
    Write-Host "空精度测试完成" -ForegroundColor Green
} else {
    Write-Warning "空精度测试失败"
}

Write-Host ""
Write-Host "=== 步骤 3: 测试 witness-based 初始精度 ===" -ForegroundColor Yellow
Remove-Item -Recurse -Force output\* -ErrorAction SilentlyContinue
.\bin\cpachecker.bat --valueAnalysis --spec $Property --witness "$resultDir\witness-backup.yml" --option cpa.value.precision.witnessParser.enabled=true --option cpa.value.ValueAnalysisCPAStatistics.precisionFile=valPrec-witness.txt $TestProgram
if ($LASTEXITCODE -eq 0) {
    Copy-Item -Recurse output\ "$resultDir\witness-precision\"
    if (Test-Path "valPrec-witness.txt") {
        Copy-Item "valPrec-witness.txt" "$resultDir\"
    }
    Write-Host "Witness-based 精度测试完成" -ForegroundColor Green
} else {
    Write-Warning "Witness-based 精度测试失败"
}

Write-Host ""
Write-Host "=== 实验结果汇总 ===" -ForegroundColor Green
Write-Host "结果保存在: $resultDir"

# 显示各个配置的统计信息
$configs = @("witness-generation", "empty-precision", "witness-precision")
foreach ($config in $configs) {
    $statsFile = "$resultDir\$config\Statistics.txt"
    if (Test-Path $statsFile) {
        Write-Host ""
        Write-Host "=== $config 统计信息 ===" -ForegroundColor Cyan
        Select-String -Path $statsFile -Pattern "(Time for analysis|Size of reached set|Number of refinements)" | ForEach-Object { Write-Host $_.Line }
    }
}

# 比较精度文件
Write-Host ""
Write-Host "=== 精度文件对比 ===" -ForegroundColor Cyan
if (Test-Path "$resultDir\valPrec-empty.txt") {
    $emptyPrecSize = (Get-Content "$resultDir\valPrec-empty.txt").Count
    Write-Host "空精度变量数: $emptyPrecSize"
    Write-Host "空精度内容:"
    Get-Content "$resultDir\valPrec-empty.txt" | ForEach-Object { Write-Host "  $_" }
}

if (Test-Path "$resultDir\valPrec-witness.txt") {
    $witnessPrecSize = (Get-Content "$resultDir\valPrec-witness.txt").Count
    Write-Host "Witness精度变量数: $witnessPrecSize"
    Write-Host "Witness精度内容:"
    Get-Content "$resultDir\valPrec-witness.txt" | ForEach-Object { Write-Host "  $_" }
}

Write-Host ""
Write-Host "实验一完成！" -ForegroundColor Green




