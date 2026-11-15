// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern unsigned int __VERIFIER_nondet_uint(void);
extern void abort(void);

void reach_error(void) {}
void __VERIFIER_error(void) {}

void __VERIFIER_assert(int cond) {
  if (!cond) {
    // Calls the error functions of both unreach-call variants (SV-COMP and CPAchecker tests).
    ERROR: { reach_error(); __VERIFIER_error(); abort(); }
  }
}

// Machine model: LP64. Expected: false for unreach-call, true for valid-memsafety.
// Like address_of/unknown-index-read-failing-assertion-64.c, but reads array[j] directly instead of
// through a pointer to it. The index j is unknown, but always within the bounds of the array.
int main(void) {
  int array[3] = { 0, 1, 2 };
  int j = (int)(__VERIFIER_nondet_uint() % 3U);

  // Fails for j == 2.
  __VERIFIER_assert(array[j] == 0 || array[j] == 1);

  return 0;
}
