@echo off

REM This file is part of CPAchecker,
REM a tool for configurable software verification:
REM https://cpachecker.sosy-lab.org
REM
REM SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
REM
REM SPDX-License-Identifier: Apache-2.0

IF [%JAVA%]==[] (
  IF NOT [%JAVA_HOME%]==[] (
    SET JAVA=%JAVA_HOME%\bin\java
  ) ELSE (
    SET JAVA=java
  )
)

REM the default heap and stack sizes of the Java VM
SET DEFAULT_HEAP_SIZE=1200M
SET DEFAULT_STACK_SIZE=1024k

REM ------------------------------------------------------------------------------
REM From here on you should not need to change anything
REM ------------------------------------------------------------------------------

SET SCRIPT=%~dp0
IF [%PATH_TO_CPACHECKER%]==[] (
  REM normalize the PATH_TO_CPACHECKER
  for %%i in ("%SCRIPT%..\") do SET "PATH_TO_CPACHECKER=%%~fi"
)

IF NOT EXIST %PATH_TO_CPACHECKER%\bin\org\sosy_lab\cpachecker\cmdline\CPAMain.class (
  IF NOT EXIST %PATH_TO_CPACHECKER%\cpachecker.jar (
    ECHO Could not find CPAchecker binary, please check path to project directory.
    EXIT 1
  )
)

SET CLASSPATH=%CLASSPATH%;%PATH_TO_CPACHECKER%\bin;%PATH_TO_CPACHECKER%\cpachecker.jar;%PATH_TO_CPACHECKER%\lib\*;%PATH_TO_CPACHECKER%\lib\java\runtime\*

REM loop over all input parameters and parse them
SET OPTIONS=
SET JAVA_ASSERTIONS=-ea

:loop
IF NOT [%1]==[] (
  IF [%1]==[-benchmark] (
    SET JAVA_ASSERTIONS=-da
    SET DEFAULT_HEAP_SIZE=xxxx
    SET "OPTIONS=%OPTIONS% %1"
  ) ELSE IF [%1]==[-heap] (
    SET JAVA_HEAP_SIZE=%2
    SHIFT
  ) ELSE IF [%1]==[-stack] (
    SET JAVA_STACK_SIZE=%2
    SHIFT
  ) ELSE IF [%1]==[-debug] (
    SET "JAVA_VM_ARGUMENTS=%JAVA_VM_ARGUMENTS% -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=5005,suspend=n"
  ) ELSE IF [%1]==[-disable-java-assertions] (
    SET JAVA_ASSERTIONS=-da
  ) ELSE IF [%1]==[-setprop] (
    IF ["%~2"]==[%2] (
      REM if option is already quoted, we do not need to restore it
      SET "OPTIONS=%OPTIONS% %1 %2"
    ) ELSE (
      REM equal sign is a separator in Batch commandline arguments, lets restore it
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

IF NOT [%TMPDIR%]==[] (
  SET "JAVA_VM_ARGUMENTS=%JAVA_VM_ARGUMENTS% -Djava.io.tmpdir^=%TMPDIR%"
) ELSE IF NOT [%TEMP%]==[] (
  SET "JAVA_VM_ARGUMENTS=%JAVA_VM_ARGUMENTS% -Djava.io.tmpdir^=%TEMP%"
) ELSE IF NOT [%TMP%]==[] (
  SET "JAVA_VM_ARGUMENTS=%JAVA_VM_ARGUMENTS% -Djava.io.tmpdir^=%TMP%"
)

IF NOT "%JAVA_HEAP_SIZE%"=="" (
  ECHO Running CPAchecker with Java heap of size %JAVA_HEAP_SIZE%.
  SET USE_JAVA_HEAP_SIZE=%JAVA_HEAP_SIZE%
) ELSE (
  SET JAVA_HEAP_SIZE=%DEFAULT_HEAP_SIZE%
  IF "%DEFAULT_HEAP_SIZE%"=="xxxx" (
    ECHO A heap size needs to be specified with -heap if -benchmark is given.
    ECHO Please see doc/Benchmark.md for further information.
    EXIT 1
  )
  ECHO Running CPAchecker with default heap size %DEFAULT_HEAP_SIZE%. Specify a larger value with -heap if you have more RAM.
)

IF NOT "%JAVA_STACK_SIZE%"=="" (
  ECHO Running CPAchecker with Java stack of size %JAVA_STACK_SIZE%.
) ELSE (
  SET JAVA_STACK_SIZE=%DEFAULT_STACK_SIZE%
  ECHO Running CPAchecker with default stack size %DEFAULT_STACK_SIZE%. Specify a larger value with -stack if needed.
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
%JAVA% ^
    -cp %CLASSPATH% ^
    -XX:+PerfDisableSharedMem ^
    -Djava.awt.headless=true ^
    %JAVA_VM_ARGUMENTS% ^
    -Xss%JAVA_STACK_SIZE% ^
    -Xmx%JAVA_HEAP_SIZE% ^
    %JAVA_ASSERTIONS% ^
    org.sosy_lab.cpachecker.cmdline.CPAMain ^
    %OPTIONS%
