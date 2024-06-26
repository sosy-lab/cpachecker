@echo off

REM This file is part of CPAchecker,
REM a tool for configurable software verification:
REM https://cpachecker.sosy-lab.org
REM
REM SPDX-FileCopyrightText: 2007-2024 Dirk Beyer <https://www.sosy-lab.org>
REM
REM SPDX-License-Identifier: Apache-2.0

SET SCRIPT=%~dp0
IF "%PATH_TO_CPACHECKER%"=="" (
  REM normalize the PATH_TO_CPACHECKER
  for %%i in ("%SCRIPT%..\") do SET "PATH_TO_CPACHECKER=%%~fi"
)

"%PATH_TO_CPACHECKER%bin\cpachecker" %*
