<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# Anonymous Local Allocation Tests

These programs test `__builtin_alloca` with the memory-safety property,
including automatic cleanup, lifetime, bounds, invalid frees, and integer type
limits. Safe programs also use the error-label property to check value
preservation. Allocations are assumed to succeed and never return `0`.
