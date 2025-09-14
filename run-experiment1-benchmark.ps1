# 实验一：纯 benchmark 方式运行（解决 witness 覆盖问题）
# PowerShell script for running E1 experiment using only benchmark.py

Write-Host "=== 实验一：使用纯 benchmark 方式（处理 witness 覆盖） ===" -ForegroundColor Green
Write-Host ""

# 确保在正确的目录
if (!(Test-Path "bin\cpachecker.bat")) {
    Write-Error "请确保在 CPAchecker 根目录下运行此脚本"
    exit 1
}

# 创建结果目录
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultDir = "benchmark-experiment1-$timestamp"
New-Item -ItemType Directory -Path $resultDir -Force | Out-Null

Write-Host "=== 步骤1：生成 witness 文件 ===" -ForegroundColor Yellow
try {
    # 使用手动命令确保成功
    .\bin\cpachecker.bat --predicateAnalysis --spec config\properties\unreach-label.prp test\programs\simple\loop1.c
    if (Test-Path "output\witness-2.0.yml") {
        Copy-Item "output\witness-2.0.yml" "witness-backup.yml"
        Copy-Item -Recurse output\ "$resultDir\step1-witness-generation\"
        Write-Host "✓ Witness 文件已生成并备份为 witness-backup.yml" -ForegroundColor Green
    } else {
        Write-Warning "未找到 witness-2.0.yml 文件"
    }
} catch {
    Write-Warning "步骤1失败，但继续执行"
}

Write-Host ""
Write-Host "=== 步骤2：生成精度文件 ===" -ForegroundColor Yellow
try {
    python scripts\benchmark.py test\test-sets\myWitnessBench-simple.xml --no-container --rundefinition generate-precision --output-path $resultDir\step2-precision
    if (Test-Path "$resultDir\step2-precision\*\valPrec-witness.txt") {
        # 查找生成的精度文件
        $precFiles = Get-ChildItem -Path "$resultDir\step2-precision" -Name "valPrec-witness.txt" -Recurse
        if ($precFiles) {
            Copy-Item $precFiles[0].FullName "valPrec-witness.txt"
            Write-Host "✓ 精度文件已生成并复制" -ForegroundColor Green
        }
    }
} catch {
    Write-Warning "步骤2失败，但继续执行"
}

Write-Host ""
Write-Host "=== 步骤3：运行对比实验 ===" -ForegroundColor Yellow
try {
    # 运行空精度和witness精度对比
    python scripts\benchmark.py test\test-sets\myWitnessBench-simple.xml --no-container --rundefinition empty-precision --rundefinition witness-precision --output-path $resultDir\comparison
    Write-Host "✓ 对比实验完成" -ForegroundColor Green
} catch {
    Write-Warning "步骤3失败"
}

Write-Host ""
Write-Host "=== 结果汇总 ===" -ForegroundColor Green
Write-Host "结果保存在: $resultDir"

# 尝试提取关键统计信息
if (Test-Path "$resultDir\comparison\results\*.xml") {
    Write-Host ""
    Write-Host "=== 性能对比 ===" -ForegroundColor Cyan
    
    # 查找结果文件
    $xmlFiles = Get-ChildItem -Path "$resultDir\comparison\results" -Name "*.xml"
    foreach ($xmlFile in $xmlFiles) {
        Write-Host "结果文件: $xmlFile"
    }
}

Write-Host ""
Write-Host "实验一（benchmark版）完成！" -ForegroundColor Green