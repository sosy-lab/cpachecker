# 实验一：Witness-based 初始精度 vs 空精度对比

## 📋 概述

本实验对比了使用 witness-based 初始精度和空精度进行 Value Analysis 的效果差异。

## 🔧 配置文件

### `myWitnessBench-simple.xml`

这个 benchmark 配置文件定义了实验一的完整流程，包含4个步骤：

1. **step1-witness-generation**: 使用 Predicate Analysis 生成 witness 文件
2. **step2-empty-precision**: 使用空精度进行 Value Analysis（基准测试）
3. **step3-witness-to-precision**: 将 witness 转换为精度文件
4. **step4-witness-precision-analysis**: 使用 witness 精度进行 Value Analysis

## 🚀 使用方法

### 方法一：自动化脚本（推荐）

```powershell
# 运行完整的实验一流程
.\run-experiment1-benchmark.ps1

# 或指定特定程序
.\run-experiment1-benchmark.ps1 -TestProgram "test\programs\simple\branching.c"
```

### 方法二：手动执行各步骤

```powershell
# 步骤1：生成 witness
.\bin\cpachecker.bat --predicateAnalysis --spec config\properties\unreach-label.prp --option witness.export.file=witness-step1.yml test\programs\simple\loop1.c

# 步骤2：空精度分析
.\bin\cpachecker.bat --valueAnalysis --spec config\properties\unreach-label.prp --option witness.export.enabled=false test\programs\simple\loop1.c

# 步骤3：witness 转精度
java -cp "cpachecker.jar;lib/java/runtime/*" org.sosy_lab.cpachecker.cmdline.CPAMain --config config/valueAnalysis.properties --option cpa.value.initialWitnessPrecisionFile=witness-step1.yml --option cpa.value.precisionFile=valPrec-from-witness.txt --option witness.export.enabled=false test\programs\simple\loop1.c

# 步骤4：使用 witness 精度分析
.\bin\cpachecker.bat --valueAnalysis --spec config\properties\unreach-label.prp --option cpa.value.precisionFile=valPrec-from-witness.txt --option witness.export.enabled=false test\programs\simple\loop1.c
```

### 方法三：使用 benchmark.py（Windows 下可能有问题）

```powershell
# 运行单个步骤
python scripts\benchmark.py test\test-sets\myWitnessBench-simple.xml --no-container --rundefinition step1-witness-generation

# 运行所有步骤（需要手动处理依赖关系）
python scripts\benchmark.py test\test-sets\myWitnessBench-simple.xml --no-container
```

## ⚠️ 重要注意事项

### Witness 文件覆盖问题

在执行过程中，witness 文件可能会被覆盖，因此：

1. **步骤1** 生成的 witness 文件需要立即备份
2. **步骤2-4** 都需要禁用 witness 导出（`witness.export.enabled=false`）
3. 使用不同的文件名避免冲突（`witness.export.file=witness-step1.yml`）

### 执行顺序

必须严格按照以下顺序执行：
1. 生成 witness → 2. 空精度分析 → 3. witness 转精度 → 4. witness 精度分析

每个步骤都依赖前一步的输出。

## 📊 关键对比指标

### 性能指标
- `Time for Analysis`: 实际分析时间
- `Total time for CEGAR algorithm`: CEGAR 算法总时间
- `Total time for CPAchecker`: 整体运行时间

### 精度效果指标
- `Size of reached set`: 到达状态集大小
- `Number of CEGAR refinements`: 精化次数
- `Time for refinements`: 精化耗时

### 覆盖率指标
- `Condition coverage`: 条件覆盖率
- `Line coverage`: 行覆盖率

## 📁 输出结构

```
benchmark-e1-results-YYYYMMDD-HHMMSS/
├── witness-step1.yml                    # 生成的 witness 文件
├── valPrec-from-witness.txt             # 转换的精度文件
├── step1-witness-generation/            # 步骤1输出
├── step2-empty-precision/               # 步骤2输出（空精度）
├── step3-witness-to-precision/          # 步骤3输出（转换过程）
└── step4-witness-precision-analysis/    # 步骤4输出（witness精度）
```

## 🎯 预期结果

基于我们的测试，预期看到：

- **Witness 精度**会产生更大的到达集（更全面的状态探索）
- **空精度**通常运行更快，但可能遗漏某些状态
- **精化次数**可能因精度不同而有差异
- **总体时间**差异通常不大，但状态空间探索深度不同

## 🔍 故障排除

### 常见问题

1. **Witness 文件找不到**
   - 检查步骤1是否成功生成 witness 文件
   - 确认文件路径正确

2. **精度文件生成失败**
   - 检查 witness 文件格式是否正确
   - 确认使用了正确的配置文件

3. **benchmark.py 报错**
   - Windows 下推荐使用手动执行或自动化脚本
   - 确保添加了 `--no-container` 参数

### 调试技巧

- 检查每个步骤的 `Statistics.txt` 文件
- 查看 `CPALog.txt` 了解详细执行过程
- 使用 `--option log.level=ALL` 获取更详细的日志

## 📝 扩展实验

基于这个框架，可以扩展到：

- **E2**: witness-based vs value-based 精度对比
- **E3**: 程序修改后的精度复用效果
- **E4**: 不同分析类型（predicate、k-induction）的 witness 对比




