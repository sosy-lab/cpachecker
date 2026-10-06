// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>

int *return_stack_allocation(void) {
  return __builtin_alloca(sizeof(int));
}

// ILP32/LP64: unsafe (valid-free); freeing after the allocation's lifetime.
int main(void) {
  int *dangling = return_stack_allocation();
  free(dangling); // Invalid free: allocation lifetime ended.
  return 0;
}
