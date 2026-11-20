// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

long *return_stack_allocation(void) {
  long *allocation = __builtin_alloca(sizeof(*allocation));
  *allocation = 1;
  return allocation;
}

// ILP32/LP64: unsafe (valid-deref); writing after the allocation's lifetime.
int main(void) {
  long *dangling = return_stack_allocation();
  *dangling = 2; // Invalid write: allocation lifetime ended.
  return 0;
}
