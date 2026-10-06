// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void __VERIFIER_assert(int condition) {
  if (!condition) {
    ERROR:
      goto ERROR;
  }
}

// ILP32 and LP64: safe for unreach-label, valid-memsafety, and no-overflow.
int main(void) {
  int result = 0;
  int overflow = __builtin_add_overflow(1, 2, &result);
  __VERIFIER_assert(result == 3);
  __VERIFIER_assert(overflow == 0);

  int *pointer = &result;
  overflow = __builtin_add_overflow(2147483647, 1, pointer);
  __VERIFIER_assert(result == (-2147483647 - 1));
  __VERIFIER_assert(overflow == 1);

  int **pointer_to_pointer = &pointer;
  overflow = __builtin_add_overflow(4, 5, *pointer_to_pointer);
  __VERIFIER_assert(result == 9);
  __VERIFIER_assert(overflow == 0);
  __VERIFIER_assert(pointer == &result);
  return 0;
}
