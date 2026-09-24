// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __VERIFIER_assert(int pCondition) {
  if (!pCondition) {
  ERROR:
    goto ERROR;
  }
}

void update_in_callee(int *pValues) {
  *pValues = pValues[1];
  pValues[1] = *pValues + 1;
}

// ILP32/LP64: safe; ErrorLabel assertions preserve values across scopes and calls.
int main(void) {
  int *ptr;
  {
    ptr = __builtin_alloca(2 * sizeof(*ptr));

    *ptr = 5;
    ptr[1] = *ptr + 1;
    __VERIFIER_assert(*ptr == 5);
    __VERIFIER_assert(ptr[1] == 6);
    update_in_callee(ptr);
    __VERIFIER_assert(*ptr == 6);
    __VERIFIER_assert(ptr[1] == 7);
  }
  __VERIFIER_assert(*ptr == 6);
  __VERIFIER_assert(ptr[1] == 7);
  *ptr = ptr[1];
  ptr[1] = *ptr + 1;
  __VERIFIER_assert(*ptr == 7);
  __VERIFIER_assert(ptr[1] == 8);
  return 0;
}
