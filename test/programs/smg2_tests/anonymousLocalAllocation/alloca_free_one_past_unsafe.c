// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

// ILP32/LP64: unsafe (valid-free); freeing one past the allocation's end.
int main(void) {
  int *allocation = __builtin_alloca(2 * sizeof(*allocation));

  *allocation = 1;
  allocation[1] = 2;
  allocation += 2;
  free(allocation); // Invalid free: one past allocation.
  return 0;
}
