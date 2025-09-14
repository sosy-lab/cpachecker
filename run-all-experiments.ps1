# 完整的实验运行指南
# 按照专家建议的步骤运行所有实验

Write-Host "=== CPAchecker Witness Precision 实验套件 ===" -ForegroundColor Green
Write-Host ""

# 确保在正确的目录
if (!(Test-Path "bin\cpachecker.bat")) {
    Write-Error "请确保在 CPAchecker 根目录下运行此脚本"
    exit 1
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$baseResultDir = "experiments-$timestamp"
New-Item -ItemType Directory -Path $baseResultDir -Force | Out-Null

Write-Host "=== 实验 E1 & E2：基础精度对比 ===" -ForegroundColor Yellow
Write-Host "步骤1：运行基础配置（生成初始精度和witness）"
# python scripts\benchmark.py test\test-sets\E1-E2-witness-precision.xml --no-container --rundefinition empty-precision-baseline --output-path $baseResultDir\E1-E2-baseline

Write-Host "步骤2：从witness生成精度"
# python scripts\benchmark.py test\test-sets\E1-E2-witness-precision.xml --no-container --rundefinition witness-to-precision --output-path $baseResultDir\E1-E2-witness-gen

Write-Host "步骤3：运行所有对比配置"
# python scripts\benchmark.py test\test-sets\E1-E2-witness-precision.xml --no-container --rundefinition witness-based-precision --rundefinition learned-precision --output-path $baseResultDir\E1-E2-comparison

Write-Host ""
Write-Host "=== 实验 E3：精度复用测试 ===" -ForegroundColor Yellow
Write-Host "注意：需要先创建 loop1-modified.c 和对应的 .yml 文件"

# 创建修改版本的示例
Write-Host "创建修改版本的测试程序..."
# 这里需要手动创建修改版本的程序

Write-Host "运行E3实验..."
# python scripts\benchmark.py test\test-sets\E3-precision-reuse-loop1.xml --no-container --output-path $baseResultDir\E3-reuse

Write-Host ""
Write-Host "=== 实验 E4：多分析方法witness对比 ===" -ForegroundColor Yellow
Write-Host "步骤1：生成不同类型的witness"
# python scripts\benchmark.py test\test-sets\E4-multi-analysis-witness.xml --no-container --rundefinition generate-predicate-witness --rundefinition generate-kinduction-witness --rundefinition generate-value-witness --output-path $baseResultDir\E4-witness-gen

Write-Host "步骤2：从不同witness生成精度"
# python scripts\benchmark.py test\test-sets\E4-multi-analysis-witness.xml --no-container --rundefinition predicate-witness-to-precision --rundefinition kinduction-witness-to-precision --rundefinition value-witness-to-precision --output-path $baseResultDir\E4-precision-gen

Write-Host "步骤3：使用不同精度进行对比"
# python scripts\benchmark.py test\test-sets\E4-multi-analysis-witness.xml --no-container --rundefinition use-predicate-derived-precision --rundefinition use-kinduction-derived-precision --rundefinition use-value-derived-precision --output-path $baseResultDir\E4-comparison

Write-Host ""
Write-Host "=== 运行指南 ===" -ForegroundColor Cyan
Write-Host "由于Windows环境限制，请手动运行以下命令："
Write-Host ""
Write-Host "E1 & E2 实验："
Write-Host "1. python scripts\benchmark.py test\test-sets\E1-E2-witness-precision.xml --no-container --rundefinition empty-precision-baseline"
Write-Host "2. 手动备份生成的 witness-2.0.yml 和 valPrec-empty.txt"
Write-Host "3. python scripts\benchmark.py test\test-sets\E1-E2-witness-precision.xml --no-container --rundefinition witness-to-precision"
Write-Host "4. python scripts\benchmark.py test\test-sets\E1-E2-witness-precision.xml --no-container --rundefinition witness-based-precision --rundefinition learned-precision"
Write-Host ""
Write-Host "E3 实验："
Write-Host "1. 创建修改版本的测试程序"
Write-Host "2. python scripts\benchmark.py test\test-sets\E3-precision-reuse-loop1.xml --no-container"
Write-Host ""
Write-Host "E4 实验："
Write-Host "1. python scripts\benchmark.py test\test-sets\E4-multi-analysis-witness.xml --no-container --rundefinition generate-predicate-witness"
Write-Host "2. 依次运行其他配置..."
Write-Host ""
Write-Host "结果将保存在: $baseResultDir"

