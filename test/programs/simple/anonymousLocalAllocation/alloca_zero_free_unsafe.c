// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

// ILP32/LP64: unsafe (valid-free); freeing an alloca(0) result.
// GCC special-cases a known zero size: no allocation is performed and it returns a valid
// stack-related address with zero usable bytes, which must not be dereferenced.
int main(void) {
  void *ptr = __builtin_alloca(0);
  free(ptr); // Invalid free: alloca storage.
  return 0;
}
