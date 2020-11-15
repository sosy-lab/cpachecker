@echo off

REM This file is part of CPAchecker,
REM a tool for configurable software verification:
REM https://cpachecker.sosy-lab.org
REM
REM SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
REM
REM SPDX-License-Identifier: Apache-2.0

java -cp bin;cpachecker.jar;lib\*;lib\java\runtime\* -Xmx1200m -ea org.sosy_lab.cpachecker.cmdline.CPAMain %*
