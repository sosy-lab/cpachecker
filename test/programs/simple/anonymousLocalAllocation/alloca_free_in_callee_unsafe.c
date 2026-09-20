// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

void free_in_callee(int *pAllocation) {
  free(pAllocation); // Invalid free: alloca storage.
}

// ILP32/LP64: unsafe (valid-free); freeing alloca storage in a callee.
int main(void) {
  int *allocation = __builtin_alloca(2 * sizeof(*allocation));

  *allocation = 1;
  allocation[1] = *allocation + 1;
  free_in_callee(allocation);
  return 0;
}
