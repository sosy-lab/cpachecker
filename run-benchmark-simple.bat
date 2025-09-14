@echo off
setlocal enabledelayedexpansion

echo ========================================
echo 一键运行 myWitnessBench-simple.xml
echo ========================================
echo.

:: 检查是否在正确目录
if not exist "bin\cpachecker.bat" (
    echo 错误：请在 CPAchecker 根目录运行此脚本
    pause
    exit /b 1
)

:: 设置变量
set TEST_PROGRAM=test\programs\simple\loop1.c
set PROPERTY=config\properties\unreach-label.prp
set TIMESTAMP=%date:~0,4%%date:~5,2%%date:~8,2%-%time:~0,2%%time:~3,2%%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set RESULT_DIR=benchmark-results-%TIMESTAMP%

echo 测试程序: %TEST_PROGRAM%
echo 结果目录: %RESULT_DIR%
echo.

:: 创建结果目录
mkdir "%RESULT_DIR%"

echo [步骤1/4] 生成 witness 文件...
rd /s /q output 2>nul
bin\cpachecker.bat --predicateAnalysis --spec %PROPERTY% %TEST_PROGRAM%
if errorlevel 1 (
    echo 错误：步骤1失败
    pause
    exit /b 1
)
copy output\witness-2.0.yml "%RESULT_DIR%\witness-step1.yml" >nul
xcopy output "%RESULT_DIR%\step1-witness-generation\" /e /i /q
echo ✓ 步骤1完成

echo.
echo [步骤2/4] 空精度 Value Analysis...
rd /s /q output 2>nul
bin\cpachecker.bat --valueAnalysis --spec %PROPERTY% --option witness.export.enabled=false %TEST_PROGRAM%
if errorlevel 1 (
    echo 错误：步骤2失败
    pause
    exit /b 1
)
xcopy output "%RESULT_DIR%\step2-empty-precision\" /e /i /q
echo ✓ 步骤2完成

echo.
echo [步骤3/4] witness 转换为精度文件...
rd /s /q output 2>nul
java -cp "cpachecker.jar;lib/java/runtime/*" org.sosy_lab.cpachecker.cmdline.CPAMain --config config/valueAnalysis.properties --option cpa.value.initialWitnessPrecisionFile=%RESULT_DIR%\witness-step1.yml --option cpa.value.precisionFile=valPrec-from-witness.txt --option witness.export.enabled=false %TEST_PROGRAM%
if errorlevel 1 (
    echo 错误：步骤3失败
    pause
    exit /b 1
)
copy output\valPrec-from-witness.txt "%RESULT_DIR%\" >nul
xcopy output "%RESULT_DIR%\step3-witness-to-precision\" /e /i /q
echo ✓ 步骤3完成

echo.
echo [步骤4/4] 使用 witness 精度进行 Value Analysis...
rd /s /q output 2>nul
copy "%RESULT_DIR%\valPrec-from-witness.txt" valPrec-from-witness.txt >nul
bin\cpachecker.bat --valueAnalysis --spec %PROPERTY% --option cpa.value.precisionFile=valPrec-from-witness.txt --option witness.export.enabled=false %TEST_PROGRAM%
if errorlevel 1 (
    echo 错误：步骤4失败
    pause
    exit /b 1
)
xcopy output "%RESULT_DIR%\step4-witness-precision-analysis\" /e /i /q
del valPrec-from-witness.txt 2>nul
echo ✓ 步骤4完成

echo.
echo ========================================
echo 🎉 所有步骤执行完成！
echo ========================================
echo.
echo 📊 结果对比:

:: 提取关键统计数据
echo.
echo 分析时间对比:
findstr "Time for Analysis:" "%RESULT_DIR%\step2-empty-precision\Statistics.txt" 2>nul && echo   空精度: && findstr "Time for Analysis:" "%RESULT_DIR%\step2-empty-precision\Statistics.txt"
findstr "Time for Analysis:" "%RESULT_DIR%\step4-witness-precision-analysis\Statistics.txt" 2>nul && echo   Witness精度: && findstr "Time for Analysis:" "%RESULT_DIR%\step4-witness-precision-analysis\Statistics.txt"

echo.
echo 到达集大小对比:
findstr "Size of reached set:" "%RESULT_DIR%\step2-empty-precision\Statistics.txt" 2>nul && echo   空精度: && findstr "Size of reached set:" "%RESULT_DIR%\step2-empty-precision\Statistics.txt"
findstr "Size of reached set:" "%RESULT_DIR%\step4-witness-precision-analysis\Statistics.txt" 2>nul && echo   Witness精度: && findstr "Size of reached set:" "%RESULT_DIR%\step4-witness-precision-analysis\Statistics.txt"

echo.
echo 精度文件内容:
if exist "%RESULT_DIR%\valPrec-from-witness.txt" (
    echo   Witness精度变量:
    type "%RESULT_DIR%\valPrec-from-witness.txt"
)

echo.
echo 📁 详细结果保存在: %RESULT_DIR%
echo.
pause




