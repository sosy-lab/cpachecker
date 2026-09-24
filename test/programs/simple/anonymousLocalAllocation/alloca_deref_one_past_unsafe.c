// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// ILP32/LP64: unsafe (valid-deref); writing past the allocation's end.
int main(void) {
  int *allocation = __builtin_alloca(2 * sizeof(*allocation));

  allocation[0] = 1;
  allocation[1] = 2;
  allocation += 2;
  *allocation = 3; // Invalid write: one past allocation.
  return 0;
}
