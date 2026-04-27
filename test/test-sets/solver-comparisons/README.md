<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2007-2026 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

Test-sets in this folder are designed to test multiple SMT solvers
and/or their features on a common analyses.
The included test-sets are also commonly used to benchmark which SMT solver
is best suited for a chosen analysis.

Benchmark definitions that include the suffix 'solver-test' define targeted SMT solver tests.
They use options that change analysis soundness, encodings etc. 
and should not be used to benchmark analyses or solver performance.
