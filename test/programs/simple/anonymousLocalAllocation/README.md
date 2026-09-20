<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

# Anonymous Local Allocation Tests

These programs test the GNU libc function `void *alloca(size_t size)` that is not defined by ISO C
([GNU documentation on alloca](https://sourceware.org/glibc/manual/latest/html_node/Variable-Size-Automatic.html)).
GCC recognizes `alloca` as a built-in outside strict ISO C and provides corresponding built-in; 
`void *__builtin_alloca(size_t size)` ([GCC documentation on __builtin_alloca](https://gcc.gnu.org/onlinedocs/gcc/Stack-Allocation.html)).
Since the two are equivalent, we use GCCs builtin to avoid preprocessing.
The tests cover the memory-safety property, including automatic cleanup, lifetime, 
bounds, invalid frees, and integer type limits. 
Safe programs also use the error-label property to check value preservation. 
Allocations are assumed to always succeed and never return `0`.
