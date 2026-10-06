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

// ILP32 only: safe; ErrorLabel checks a negative size converts to space for two ints.
int main(void) {
  const long long negative_size = -4294967288LL;
  // Conversion to 32-bit size_t gives 8 bytes. This is a unsigned overflow and allowed!
  int *ptr = __builtin_alloca(negative_size);

  *ptr = 11;
  ptr++;
  *ptr = 22;
  __VERIFIER_assert(ptr[-1] == 11);
  __VERIFIER_assert(*ptr == 22);
  return 0;
}
