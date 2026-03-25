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

Please note that some run-definitions in this folder use options that change the soundness,
analysis encoding etc. This is acceptable for solver/analysis integration-tests, but benchmarking
solvers for analyses or solver performance should be based on equal and/or sensible settings!
