// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void update_in_callee(int *pValues) {
  *pValues = pValues[1];
  pValues[1] = *pValues + 1;
}

// ILP32/LP64: safe; storage remains valid after block exit and a callee call.
int main(void) {
  int *ptr;
  {
    ptr = __builtin_alloca(2 * sizeof(*ptr));

    *ptr = 5;
    ptr[1] = *ptr + 1;
    update_in_callee(ptr);
  }
  *ptr = ptr[1];
  ptr[1] = *ptr + 1;
  return 0;
}
