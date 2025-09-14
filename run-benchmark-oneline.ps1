# 一键运行 myWitnessBench-simple.xml 的所有步骤
# 使用方法：powershell -ExecutionPolicy Bypass -File run-benchmark-oneline.ps1

param(
    [string]$TestProgram = "test\programs\simple\loop1.c",
    [string]$Property = "config\properties\unreach-label.prp"
)

function Write-Step {
    param([string]$Message, [string]$Color = "Yellow")
    Write-Host "`n[$Message]" -ForegroundColor $Color
}

function Write-Success {
    param([string]$Message)
    Write-Host "✓ $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

# 检查环境
if (!(Test-Path "bin\cpachecker.bat")) {
    Write-Error "请在 CPAchecker 根目录运行此脚本"
    exit 1
}

# 创建结果目录
$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$resultDir = "benchmark-results-$timestamp"
New-Item -ItemType Directory -Path $resultDir -Force | Out-Null

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "🚀 一键运行 myWitnessBench-simple.xml" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "测试程序: $TestProgram"
Write-Host "结果目录: $resultDir"

# 步骤1：生成 witness
Write-Step "步骤1/4: 生成 witness 文件"
Remove-Item -Recurse -Force output\* -ErrorAction SilentlyContinue
$result = & .\bin\cpachecker.bat --predicateAnalysis --spec $Property $TestProgram
if ($LASTEXITCODE -ne 0) {
    Write-Error "步骤1失败"
    exit 1
}
Copy-Item output\witness-2.0.yml "$resultDir\witness-step1.yml"
Copy-Item -Recurse output\ "$resultDir\step1-witness-generation\"
Write-Success "步骤1完成"

# 步骤2：空精度分析
Write-Step "步骤2/4: 空精度 Value Analysis"
Remove-Item -Recurse -Force output\* -ErrorAction SilentlyContinue
$result = & .\bin\cpachecker.bat --valueAnalysis --spec $Property --option witness.export.enabled=false $TestProgram
if ($LASTEXITCODE -ne 0) {
    Write-Error "步骤2失败"
    exit 1
}
Copy-Item -Recurse output\ "$resultDir\step2-empty-precision\"
Write-Success "步骤2完成"

# 步骤3：witness 转精度
Write-Step "步骤3/4: witness 转换为精度文件"
Remove-Item -Recurse -Force output\* -ErrorAction SilentlyContinue
$result = & java -cp "cpachecker.jar;lib/java/runtime/*" org.sosy_lab.cpachecker.cmdline.CPAMain --config config/valueAnalysis.properties --option cpa.value.initialWitnessPrecisionFile=$resultDir\witness-step1.yml --option cpa.value.precisionFile=valPrec-from-witness.txt --option witness.export.enabled=false $TestProgram
if ($LASTEXITCODE -ne 0) {
    Write-Error "步骤3失败"
    exit 1
}
Copy-Item output\valPrec-from-witness.txt "$resultDir\"
Copy-Item -Recurse output\ "$resultDir\step3-witness-to-precision\"
Write-Success "步骤3完成"

# 步骤4：使用精度分析
Write-Step "步骤4/4: 使用 witness 精度进行 Value Analysis"
Remove-Item -Recurse -Force output\* -ErrorAction SilentlyContinue
Copy-Item "$resultDir\valPrec-from-witness.txt" "valPrec-from-witness.txt"
$result = & .\bin\cpachecker.bat --valueAnalysis --spec $Property --option cpa.value.precisionFile=valPrec-from-witness.txt --option witness.export.enabled=false $TestProgram
if ($LASTEXITCODE -ne 0) {
    Write-Error "步骤4失败"
    exit 1
}
Copy-Item -Recurse output\ "$resultDir\step4-witness-precision-analysis\"
Remove-Item "valPrec-from-witness.txt" -ErrorAction SilentlyContinue
Write-Success "步骤4完成"

# 结果汇总
Write-Host "`n========================================" -ForegroundColor Green
Write-Host "🎉 所有步骤执行完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

Write-Host "`n📊 结果对比:" -ForegroundColor Cyan

# 分析时间对比
Write-Host "`n分析时间对比:" -ForegroundColor Yellow
$emptyTime = Select-String -Path "$resultDir\step2-empty-precision\Statistics.txt" -Pattern "Time for Analysis:" -ErrorAction SilentlyContinue
$witnessTime = Select-String -Path "$resultDir\step4-witness-precision-analysis\Statistics.txt" -Pattern "Time for Analysis:" -ErrorAction SilentlyContinue
if ($emptyTime) { Write-Host "  空精度:    $($emptyTime.Line)" }
if ($witnessTime) { Write-Host "  Witness精度: $($witnessTime.Line)" }

# 到达集大小对比
Write-Host "`n到达集大小对比:" -ForegroundColor Yellow
$emptyReached = Select-String -Path "$resultDir\step2-empty-precision\Statistics.txt" -Pattern "Size of reached set:" -ErrorAction SilentlyContinue
$witnessReached = Select-String -Path "$resultDir\step4-witness-precision-analysis\Statistics.txt" -Pattern "Size of reached set:" -ErrorAction SilentlyContinue
if ($emptyReached) { Write-Host "  空精度:    $($emptyReached.Line)" }
if ($witnessReached) { Write-Host "  Witness精度: $($witnessReached.Line)" }

# 精度文件内容
Write-Host "`n精度文件内容:" -ForegroundColor Yellow
if (Test-Path "$resultDir\valPrec-from-witness.txt") {
    Write-Host "  Witness精度变量:"
    Get-Content "$resultDir\valPrec-from-witness.txt" | ForEach-Object { Write-Host "    $_" }
}

Write-Host "`n📁 详细结果保存在: $resultDir" -ForegroundColor Cyan
Write-Host "`n按任意键继续..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")




