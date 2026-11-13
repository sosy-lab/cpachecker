// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

extern void __VERIFIER_error(void) __attribute__((__noreturn__));

void __VERIFIER_assert(int condition) {
  if (!condition) {
    __VERIFIER_error();
  }
}

extern int snprintf(char *str, unsigned long size, const char *format, ...);

// Machine model: LP64. Expected: false for unreach-call, true for valid-memsafety.
// The output is truncated to three characters and a terminator.
int main(void) {
  char dest[4] = {'Z', 'Z', 'Z', 'Z'};

  int result = snprintf(dest, 4UL, "%s", "abcdef");

  __VERIFIER_assert(dest[0] == 'a');
  __VERIFIER_assert(dest[3] == '\0');
  // Wrong on purpose: snprintf returns the length of the complete output (6).
  __VERIFIER_assert(result == 3);

  return 0;
}
