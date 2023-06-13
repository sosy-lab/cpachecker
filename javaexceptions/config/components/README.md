<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

This directory contains configuration files
with analyses that are not meant to be executed on their own
but inside a sequential combination of analyses
(i.e., for `restartAlgorithm.configFiles` with `analysis.restartAfterUnknown=true`.
Typically these configuration contain some low resource limit
or other restriction that would not be present in a normal configuration file.
