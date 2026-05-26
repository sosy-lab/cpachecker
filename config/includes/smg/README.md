<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

This directory contains configuration files
that are only meant to be included from other configuration files with `#include`
and do not contain a full usable configuration on their own,
i.e., should not be given to CPAchecker as configuration file.
All configs in this folder use Symbolic Memory Graphs (SMGs) in some form.
