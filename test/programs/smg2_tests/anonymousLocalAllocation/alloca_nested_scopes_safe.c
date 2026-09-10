// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

long update_in_callee(long *pValue, long pNewValue) {
  *pValue = pNewValue;
  pValue[1] = *pValue;
  return pValue[1];
}

int allocate_in_function(void) {
  long *outer = __builtin_alloca(2 * sizeof(*outer));
  long *middle;
  long *inner;
  *outer = 1;
  outer[1] = *outer;

  {
    middle = __builtin_alloca(2 * sizeof(*middle));
    *middle = *outer + 1;
    middle[1] = *middle;
    {
      inner = __builtin_alloca(2 * sizeof(*inner));
      *inner = *middle + 1;
      inner[1] = *inner;
    }
  }
  *middle = update_in_callee(middle, 4);
  *inner = update_in_callee(inner, 5);
  return outer[1] != 1 || middle[1] != 4 || inner[1] != 5;
}

// ILP32/LP64: safe; storage remains valid across nested blocks and calls.
int main(void) {
  long *main_allocation;
  {
    main_allocation = __builtin_alloca(2 * sizeof(*main_allocation));
    *main_allocation = 6;
    main_allocation[1] = *main_allocation;
  }
  *main_allocation = update_in_callee(main_allocation, 7);
  return allocate_in_function() || main_allocation[1] != 7;
}
