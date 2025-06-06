@echo off
setlocal enableDelayedExpansion

REM This file is part of CPAchecker,
REM a tool for configurable software verification:
REM https://cpachecker.sosy-lab.org
REM
REM SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
REM
REM SPDX-License-Identifier: Apache-2.0

SETLOCAL

IF "%JAVA%"=="" (
  IF NOT "%JAVA_HOME%"=="" (
    SET "JAVA=%JAVA_HOME%\bin\java"
  ) ELSE (
    SET JAVA=java
  )
)

REM ------------------------------------------------------------------------------
REM From here on you should not need to change anything
REM ------------------------------------------------------------------------------

SET SCRIPT=%~dp0
IF "%PATH_TO_CPACHECKER%"=="" (
  REM normalize the PATH_TO_CPACHECKER
  for %%i in ("%SCRIPT%..\") do SET "PATH_TO_CPACHECKER=%%~fi"
)

IF NOT EXIST "%PATH_TO_CPACHECKER%\classes\org\sosy_lab\cpachecker\cmdline\CPAMain.class" (
  IF NOT EXIST "%PATH_TO_CPACHECKER%\cpachecker.jar" (
    ECHO Could not find CPAchecker binary, please check path to project directory.
    EXIT /B 1
  )
)

SET "CLASSPATH=%CLASSPATH%;%PATH_TO_CPACHECKER%\classes;%PATH_TO_CPACHECKER%\cpachecker.jar;%PATH_TO_CPACHECKER%\lib\*;%PATH_TO_CPACHECKER%\lib\java\runtime\*"

REM loop over all input parameters and parse them
SET OPTIONS=
SET BENCHMARK_MODE=

:loop
IF NOT [%1]==[] (
  ECHO %1 | findstr /b /c:-X > nul
  IF NOT errorlevel 1 (
    REM params starting with "-X" are used for JVM
    ECHO %1| findstr "^\-XX:+.*GC$" > nul
    IF NOT errorlevel 1 (
      REM looks like an option for choosing a GC, skip our default
      SET JAVA_GC=ignore_default
    )
    SET "JAVA_VM_ARGUMENTS=%JAVA_VM_ARGUMENTS% %1"
  ) ELSE IF [%1]==[--benchmark] (
    SET BENCHMARK_MODE=true
    SET "OPTIONS=%OPTIONS% %1"
  ) ELSE IF [%1]==[-benchmark] (
    SET BENCHMARK_MODE=true
    SET "OPTIONS=%OPTIONS% %1"
  ) ELSE IF [%1]==[--heap] (
    SET JAVA_HEAP_SIZE=%2
    SHIFT
  ) ELSE IF [%1]==[-heap] (
    ECHO Argument '-heap' is deprecated, we recommend replacing with '--heap'.
    SET JAVA_HEAP_SIZE=%2
    SHIFT
  ) ELSE IF [%1]==[--stack] (
    SET JAVA_STACK_SIZE=%2
    SHIFT
  ) ELSE IF [%1]==[-stack] (
    ECHO Argument '-stack' is deprecated, we recommend replacing with '--stack'.
    SET JAVA_STACK_SIZE=%2
    SHIFT
  ) ELSE IF [%1]==[--jvm-debug] (
    SET "JAVA_VM_ARGUMENTS=%JAVA_VM_ARGUMENTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n"
  ) ELSE IF [%1]==[-debug] (
    ECHO Argument '-debug' is deprecated, we recommend replacing with '--jvm-debug'.
    SET "JAVA_VM_ARGUMENTS=%JAVA_VM_ARGUMENTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n"
  ) ELSE IF [%1]==[--disable-java-assertions] (
    SET JAVA_ASSERTIONS=-da
  ) ELSE IF [%1]==[-disable-java-assertions] (
    ECHO Argument '-disable-java-assertions' is deprecated, we recommend replacing with '--disable-java-assertions'.
    SET JAVA_ASSERTIONS=-da
  ) ELSE IF [%1]==[--option] (
    IF ["%~2"]==[%2] (
      REM if option is already quoted, we do not need to restore it
      SET "OPTIONS=%OPTIONS% %1 %2"
    ) ELSE (
      REM equal sign is a separator in Batch commandline arguments, let's restore it
      SET "OPTIONS=%OPTIONS% %1 %2^=%3"
      SHIFT
    )
    SHIFT
  ) ELSE IF [%1]==[-setprop] (
    IF ["%~2"]==[%2] (
      REM if option is already quoted, we do not need to restore it
      SET "OPTIONS=%OPTIONS% %1 %2"
    ) ELSE (
      REM equal sign is a separator in Batch commandline arguments, let's restore it
      SET "OPTIONS=%OPTIONS% %1 %2^=%3"
      SHIFT
    )
    SHIFT
  ) ELSE (
    SET "OPTIONS=%OPTIONS% %1"
  )
  SHIFT
  GOTO :loop
)

REM Determine temp dir to use for JVM
IF NOT "%TMPDIR%"=="" (
  SET "JAVA_VM_ARGUMENTS=%JAVA_VM_ARGUMENTS% -Djava.io.tmpdir^=%TMPDIR%"
) ELSE IF NOT "%TEMP%"=="" (
  SET "JAVA_VM_ARGUMENTS=%JAVA_VM_ARGUMENTS% -Djava.io.tmpdir^=%TEMP%"
) ELSE IF NOT "%TMP%"=="" (
  SET "JAVA_VM_ARGUMENTS=%JAVA_VM_ARGUMENTS% -Djava.io.tmpdir^=%TMP%"
)

REM Determine whether to enable Java assertions
IF NOT defined JAVA_ASSERTIONS (
  IF defined BENCHMARK_MODE (
    SET JAVA_ASSERTIONS=-da
  ) ELSE (
    SET JAVA_ASSERTIONS=-ea
  )
)

REM Determine heap size
IF NOT "%JAVA_HEAP_SIZE%"=="" (
  ECHO Running CPAchecker with Java heap of size %JAVA_HEAP_SIZE%.
  SET USE_JAVA_HEAP_SIZE=%JAVA_HEAP_SIZE%
) ELSE IF defined BENCHMARK_MODE (
  ECHO A heap size needs to be specified with --heap if --benchmark is given.
  ECHO Please see doc/Benchmark.md for further information.
  EXIT /B 1
) ELSE (
  SET JAVA_HEAP_SIZE=1200M
  ECHO Running CPAchecker with default heap size !JAVA_HEAP_SIZE!. Specify a larger value with --heap if you have more RAM.
)

REM Determine garbage collector
IF not defined JAVA_GC (
  REM Recommendations are from BSc thesis
  REM "Evaluation of JVM Garbage Collectors for CPAchecker", Tobias Maget, 2024.
  REM https://www.sosy-lab.org/research/bib/All/index.html#MagetEvaluationJVMGarbageCollectorsCPAchecker
  IF defined BENCHMARK_MODE (
    REM SerialGC improves CPU time and memory consumption,
    REM and should provide more consistent performance.
    SET JAVA_GC=-XX:+UseSerialGC
  ) ELSE (
    REM This configuration of parallel GC is good overall for CPU time, wall time, and memory.
    SET "JAVA_GC=-XX:+UseParallelGC -XX:MinHeapFreeRatio=80"
  )
) ELSE (
  SET JAVA_GC=
)

REM Determine stack size
IF NOT "%JAVA_STACK_SIZE%"=="" (
  ECHO Running CPAchecker with Java stack of size %JAVA_STACK_SIZE%.
) ELSE (
  SET JAVA_STACK_SIZE=1024k
  ECHO Running CPAchecker with default stack size !JAVA_STACK_SIZE!. Specify a larger value with --stack if needed.
)


IF NOT "%JAVA_VM_ARGUMENTS%"=="" (
  ECHO Running CPAchecker with the following extra VM options:%JAVA_VM_ARGUMENTS%.
)

REM Run CPAchecker.
REM Order of arguments for JVM:
REM - options hard-coded in this script (to allow overriding them)
REM - options specified in environment variable
REM - options specified on command-line via "-X..." (except stack/head/tmpdir)
REM - options specified on command-line to this script via direct token (stack/heap) and tmpdir
REM - CPAchecker class and options
REM Stack size is set because on some systems it is too small for recursive algorithms and very large programs.
REM PerfDisableSharedMem avoids hsperfdata in /tmp (disable it to connect easily with VisualConsole and Co.).
"%JAVA%" ^
    -cp "%CLASSPATH%" ^
    -XX:+PerfDisableSharedMem ^
    -Djava.awt.headless=true ^
    %JAVA_GC% ^
    %JAVA_VM_ARGUMENTS% ^
    -Xss%JAVA_STACK_SIZE% ^
    -Xmx%JAVA_HEAP_SIZE% ^
    %JAVA_ASSERTIONS% ^
    org.sosy_lab.cpachecker.cmdline.CPAMain ^
    %OPTIONS%
