// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

int *return_stack_allocation(void) {
  int *allocation = __builtin_alloca(sizeof(*allocation));
  *allocation = 5;
  return allocation;
}

// ILP32/LP64: unsafe (valid-deref); reading after the allocation's lifetime.
int main(void) {
  int *dangling = return_stack_allocation();
  return *dangling; // Invalid read: allocation lifetime ended.
}
