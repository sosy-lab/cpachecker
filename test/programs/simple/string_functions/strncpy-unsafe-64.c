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

extern char *strncpy(char *dest, const char *src, unsigned long n);

// Machine model: LP64. Expected: false for unreach-call, true for valid-memsafety.
// strncpy copies three of the six characters and adds no terminator.
int main(void) {
  char dest[5] = {'Z', 'Z', 'Z', 'Z', 'Z'};

  char *result = strncpy(dest, "abcdef", 3UL);

  __VERIFIER_assert(result == dest);
  __VERIFIER_assert(dest[0] == 'a');
  __VERIFIER_assert(dest[1] == 'b');
  __VERIFIER_assert(dest[2] == 'c');
  // Wrong on purpose: strncpy does not terminate a truncated copy.
  __VERIFIER_assert(dest[3] == '\0');

  return 0;
}
