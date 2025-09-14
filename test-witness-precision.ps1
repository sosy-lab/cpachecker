# CPAchecker Witness Precision Test Script
# PowerShell script for testing witness-based precision

param(
    [string]$TestProgram = "test\programs\simple\branching.c",
    [string]$Property = "config\properties\unreach-label.prp"
)

Write-Host "=== CPAchecker Witness Precision Test ===" -ForegroundColor Green
Write-Host "Test Program: $TestProgram"
Write-Host "Property: $Property"
Write-Host ""

# 确保在正确的目录
if (!(Test-Path "bin\cpachecker.bat")) {
    Write-Error "请确保在 CPAchecker 根目录下运行此脚本"
    exit 1
}

# 创建结果目录
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultDir = "test-results-$timestamp"
New-Item -ItemType Directory -Path $resultDir -Force | Out-Null

Write-Host "=== 步骤 1: 生成 witness 文件 ===" -ForegroundColor Yellow
.\bin\cpachecker.bat --predicateAnalysis --spec $Property $TestProgram
if ($LASTEXITCODE -eq 0 -or $LASTEXITCODE -eq 1) {
    Copy-Item -Recurse output\ "$resultDir\witness-generation\"
    Write-Host "Witness 生成完成" -ForegroundColor Green
} else {
    Write-Warning "Witness 生成失败，但继续测试"
}

Write-Host ""
Write-Host "=== 步骤 2: 测试空精度配置 ===" -ForegroundColor Yellow
.\bin\cpachecker.bat --valueAnalysis --spec $Property $TestProgram
if ($LASTEXITCODE -eq 0 -or $LASTEXITCODE -eq 1) {
    Copy-Item -Recurse output\ "$resultDir\empty-precision\"
    Write-Host "空精度测试完成" -ForegroundColor Green
} else {
    Write-Warning "空精度测试失败"
}

Write-Host ""
Write-Host "=== 步骤 3: 测试 witness-based 初始精度 ===" -ForegroundColor Yellow
# 使用已有的 witness 文件进行测试
$witnessFile = "$resultDir\witness-generation\witness-2.0.yml"
if (Test-Path $witnessFile) {
    .\bin\cpachecker.bat --valueAnalysis --spec $Property --witness $witnessFile --setprop cpa.value.precision.witnessParser.enabled=true $TestProgram
} else {
    # 如果没有 witness 文件，尝试使用当前 output 目录中的
    .\bin\cpachecker.bat --valueAnalysis --spec $Property --setprop cpa.value.precision.witnessParser.enabled=true $TestProgram
}
if ($LASTEXITCODE -eq 0 -or $LASTEXITCODE -eq 1) {
    Copy-Item -Recurse output\ "$resultDir\witness-precision\"
    Write-Host "Witness-based 精度测试完成" -ForegroundColor Green
} else {
    Write-Warning "Witness-based 精度测试失败"
}

Write-Host ""
Write-Host "=== 测试结果汇总 ===" -ForegroundColor Green
Write-Host "结果保存在: $resultDir"

# 显示各个配置的统计信息
$configs = @("witness-generation", "empty-precision", "witness-precision")
foreach ($config in $configs) {
    $statsFile = "$resultDir\$config\Statistics.txt"
    if (Test-Path $statsFile) {
        Write-Host ""
        Write-Host "=== $config 统计信息 ===" -ForegroundColor Cyan
        Select-String -Path $statsFile -Pattern "(Time for analysis|Size of reached set|Number of refinements)" | ForEach-Object { Write-Host $_.Line }
        
        $precFile = "$resultDir\$config\valPrec.txt"
        if (Test-Path $precFile) {
            $precSize = (Get-Content $precFile).Count
            Write-Host "Precision variables: $precSize"
        }
    }
}

Write-Host ""
Write-Host "测试完成！" -ForegroundColor Green
