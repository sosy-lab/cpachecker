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

// Machine model: LP64. Expected: true for unreach-call and valid-memsafety.
// With size 0, snprintf writes nothing, str may be a null pointer (C11 7.21.6.5),
// and the result is the length of the complete output.
int main(void) {
  char *null = 0;

  int result = snprintf(null, 0UL, "%s", "abc");

  __VERIFIER_assert(result == 3);

  return 0;
}
