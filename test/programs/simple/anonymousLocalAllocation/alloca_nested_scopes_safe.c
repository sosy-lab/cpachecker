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

long update_in_callee(long *pValue, long pNewValue) {
  *pValue = pNewValue;
  pValue[1] = *pValue;
  __VERIFIER_assert(*pValue == pNewValue);
  __VERIFIER_assert(pValue[1] == pNewValue);
  return pValue[1];
}

long *allocate_in_function(void) {
  long *outer = __builtin_alloca(2 * sizeof(*outer));
  long *middle;
  long *inner;
  *outer = 1;
  outer[1] = *outer;
  __VERIFIER_assert(*outer == 1);
  __VERIFIER_assert(outer[1] == 1);

  {
    middle = __builtin_alloca(2 * sizeof(*middle));
    *middle = *outer + 1;
    middle[1] = *middle;
    __VERIFIER_assert(*middle == 2);
    __VERIFIER_assert(middle[1] == 2);
    {
      inner = __builtin_alloca(2 * sizeof(*inner));
      *inner = *middle + 1;
      inner[1] = *inner;
      __VERIFIER_assert(*inner == 3);
      __VERIFIER_assert(inner[1] == 3);
    }
    __VERIFIER_assert(*inner == 3);
    __VERIFIER_assert(inner[1] == 3);
  }
  __VERIFIER_assert(*middle == 2);
  __VERIFIER_assert(middle[1] == 2);
  __VERIFIER_assert(*inner == 3);
  __VERIFIER_assert(inner[1] == 3);
  *middle = update_in_callee(middle, 4);
  *inner = update_in_callee(inner, 5);
  __VERIFIER_assert(*outer == 1);
  __VERIFIER_assert(outer[1] == 1);
  __VERIFIER_assert(*middle == 4);
  __VERIFIER_assert(middle[1] == 4);
  __VERIFIER_assert(*inner == 5);
  __VERIFIER_assert(inner[1] == 5);
  return outer;
}

// ILP32/LP64: safe; ErrorLabel assertions preserve values across scopes and calls.
int main(void) {
  long *main_allocation;
  {
    main_allocation = __builtin_alloca(2 * sizeof(*main_allocation));
    *main_allocation = 6;
    main_allocation[1] = *main_allocation;
    __VERIFIER_assert(*main_allocation == 6);
    __VERIFIER_assert(main_allocation[1] == 6);
  }
  __VERIFIER_assert(*main_allocation == 6);
  __VERIFIER_assert(main_allocation[1] == 6);
  *main_allocation = update_in_callee(main_allocation, 7);
  __VERIFIER_assert(*main_allocation == 7);
  __VERIFIER_assert(main_allocation[1] == 7);
  long *returned_allocation = allocate_in_function();
  // Safe: retaining this pointer without using it checks automatic cleanup is not applied twice.
  return 0;
}
